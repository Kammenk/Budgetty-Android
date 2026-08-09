package com.budgetty.app.data.billing

import android.app.Activity
import android.content.Context
import com.budgetty.app.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wraps Google Play Billing for the two subscriptions. [isPremium] is true while an active
 * subscription is owned; [products] holds the queried plans (for the paywall).
 *
 * Inert until the subscription products exist in the Play Console and the app is installed from a
 * test track — until then [products] is empty and [isPremium] stays false.
 */
class BillingManager(context: Context, private val auth: FirebaseAuth) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Debug-only Premium unlock (see [FORCE_PREMIUM]); always false in release builds. */
    private val forcePremium = BuildConfig.DEBUG && FORCE_PREMIUM

    /**
     * Caches the last known account-comp entitlement (the server-granted `premium` auth claim) so a
     * comped account shows Premium instantly on cold start, before the async token read completes.
     */
    private val prefs = context.getSharedPreferences("entitlement", Context.MODE_PRIVATE)

    // The inputs [recompute] folds into [isPremium], alongside the debug-only [forcePremium].
    @Volatile private var subscribed = false // an owned Play Billing subscription

    @Volatile private var comp = prefs.getBoolean(KEY_COMP, false) // the server-granted `premium` claim

    private val _isPremium = MutableStateFlow(forcePremium || comp)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private fun recompute() {
        _isPremium.value = forcePremium || comp || subscribed
    }

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { purchases.forEach { handlePurchase(it) } }
        }
    }

    private val client = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // The library re-establishes the service connection itself. Before this existed the
        // disconnect callback below was empty and [refresh] is gated on `client.isReady`, so a
        // single disconnect left ownership permanently stale until the process restarted.
        .enableAutoServiceReconnection()
        .build()

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { refreshInternal() }
                }
            }

            /** Intentionally empty: reconnection is handled by `enableAutoServiceReconnection`. */
            override fun onBillingServiceDisconnected() {}
        })
        // Account-comp entitlement: re-read the `premium` claim whenever a user is signed in (startup
        // or sign-in). A null user is ignored so the cached grant doesn't flicker off during the brief
        // signed-out window at launch; the next signed-in read reconciles both a grant and a revoke.
        auth.addAuthStateListener { fb ->
            if (fb.currentUser != null) refreshComp()
        }
    }

    /** Re-query products + ownership (e.g. when opening the paywall). */
    fun refresh() {
        if (client.isReady) scope.launch { refreshInternal() }
    }

    /**
     * Re-reads the server-granted `premium` custom claim for the signed-in user and folds it into
     * [isPremium]. Forces a token refresh so a revoked claim is picked up; on network failure the
     * cached value is kept. Comp is tied to the account, so it restores on any device after sign-in.
     */
    fun refreshComp() {
        scope.launch {
            val user = auth.currentUser ?: return@launch
            val granted = premiumClaim(user) ?: return@launch // couldn't read — keep the cached value
            if (granted != comp) {
                comp = granted
                prefs.edit().putBoolean(KEY_COMP, granted).apply()
            }
            recompute()
        }
    }

    /**
     * The `premium` custom claim from a freshly-refreshed ID token, or null when the token couldn't be
     * fetched (offline / transient) so the caller keeps the cached entitlement.
     */
    private suspend fun premiumClaim(user: FirebaseUser): Boolean? =
        suspendCancellableCoroutine { cont ->
            user.getIdToken(true).addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result?.claims?.get("premium") == true else null)
            }
        }

    private suspend fun refreshInternal() {
        queryProductsInternal()
        queryPurchasesInternal()
    }

    private suspend fun queryProductsInternal() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                PRODUCT_IDS.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                },
            )
            .build()
        val (result, details) = queryProductDetails(params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            // A product the Play Console doesn't serve yet no longer vanishes silently — it comes
            // back in `unfetchedProductList` with a status code instead of being omitted.
            _products.value = details.productDetailsList
        }
    }

    private suspend fun queryPurchasesInternal() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val purchases = queryPurchases(params)
        subscribed = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        recompute()
        purchases.forEach { acknowledge(it) }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            subscribed = true
            recompute()
            acknowledge(purchase)
        }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            suspendCancellableCoroutine { cont ->
                client.acknowledgePurchase(params) { cont.resume(Unit) }
            }
        }
    }

    /*
     * Billing 9 dropped the `billing-ktx` artifact from this project: its 9.1.0 build carries
     * Kotlin 2.3 metadata, which the Kotlin 2.0.21 this project is pinned to cannot read. The base
     * `billing` artifact is plain Java and has no such constraint, so the three suspend calls the
     * KTX extensions used to provide are wrapped by hand below.
     */

    private suspend fun queryProductDetails(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, QueryProductDetailsResult> = suspendCancellableCoroutine { cont ->
        client.queryProductDetailsAsync(params) { result, details -> cont.resume(result to details) }
    }

    private suspend fun queryPurchases(params: QueryPurchasesParams): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { _, purchases -> cont.resume(purchases) }
        }

    /** Launches the Play purchase flow for [productId] (its first available offer). */
    fun purchase(activity: Activity, productId: String) {
        val product = _products.value.firstOrNull { it.productId == productId } ?: return
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    companion object {
        /**
         * Testing switch: when true, the account is treated as Premium without subscribing.
         * Gated by [BuildConfig.DEBUG], so it has no effect on release builds. Set back to
         * false to exercise the real free-tier / paywall flow in debug.
         */
        private const val FORCE_PREMIUM = true

        private const val KEY_COMP = "comp_premium"

        const val MONTHLY = "budgetty_premium_monthly"
        const val YEARLY = "budgetty_premium_yearly"
        val PRODUCT_IDS = listOf(MONTHLY, YEARLY)
    }
}
