package com.budgetty.app.ui.upload

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.budgetty.app.analytics.Analytics
import com.budgetty.app.crash.CrashReporting
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.ingest.HaikuReceiptExtractor
import com.budgetty.app.data.ingest.ReceiptIngestManager
import com.budgetty.app.data.local.TemplateEntity
import com.budgetty.app.data.local.TripEntity
import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.data.remote.RECEIPT_API_BASE_URL
import com.budgetty.app.data.remote.ReceiptApi
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.TagRepository
import com.budgetty.app.data.repository.TemplateRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.repository.TripRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.review.ReviewTracker
import com.budgetty.app.ui.buyinglimits.BuyingLimitNudgeBus
import com.budgetty.app.ui.buyinglimits.BuyingLimitNudger
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import java.math.BigDecimal

/**
 * Travel mode's core promise: while a trip is active, a new expense starts out tagged with the trip.
 * The add screen calls [UploadViewModel.startManual] the moment the ViewModel exists, so the trip
 * must be looked up then — the tag used to come from a collector that hadn't delivered yet, leaving
 * every manual and template entry untagged. Real dependency graph on the JVM, as in
 * [UploadViewModelDuplicateGuardTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTripTagTest {

    private lateinit var dbManager: UserDatabaseManager

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("budgetty-test")
                    .setApplicationId("1:1234567890:android:0000000000000000")
                    .setApiKey("AIzaSyTestKeyForRobolectricOnly0000000000")
                    .build(),
            )
        }
        dbManager = UserDatabaseManager(context, FirebaseAuth.getInstance(), CrashReporting())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun manualEntry_duringActiveTrip_carriesTheTripTag() {
        startTrip("lisbon-2026")
        val vm = newViewModel()

        vm.startManual()

        assertThat(awaitFirstRowTags(vm)).containsExactly("lisbon-2026")
    }

    @Test
    fun templateEntry_duringActiveTrip_carriesTheTripTag() {
        startTrip("lisbon-2026")
        val templateId = runBlocking {
            TemplateRepository(dbManager).upsert(TemplateEntity(name = "Coffee", amount = BigDecimal("3.20")))
        }
        val vm = newViewModel()

        vm.startManual(templateId)

        assertThat(awaitFirstRowTags(vm)).containsExactly("lisbon-2026")
        assertThat(vm.uiState.value.transactions.single().name).isEqualTo("Coffee")
    }

    @Test
    fun manualEntry_withNoActiveTrip_isUntagged() {
        val vm = newViewModel()

        vm.startManual()
        runBlocking { delay(SETTLE_MS) }

        assertThat(vm.uiState.value.transactions.single().tags).isEmpty()
    }

    private fun startTrip(tag: String) = runBlocking {
        TripRepository(dbManager).start(TripEntity(name = "Lisbon", tag = tag, createdAt = 1L))
    }

    /** The first row's tags once the trip lookup has landed (or whatever it has after the timeout). */
    private fun awaitFirstRowTags(vm: UploadViewModel): List<String> = runBlocking {
        withTimeoutOrNull(TIMEOUT_MS) {
            while (vm.uiState.value.transactions.firstOrNull()?.tags.isNullOrEmpty()) delay(POLL_MS)
        }
        vm.uiState.value.transactions.first().tags
    }

    private fun newViewModel(): UploadViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val auth = FirebaseAuth.getInstance()
        val analytics = Analytics(context)
        val crashReporting = CrashReporting()
        val transactionRepo = TransactionRepository(dbManager)
        val api = Retrofit.Builder().baseUrl(RECEIPT_API_BASE_URL).build().create(ReceiptApi::class.java)
        return UploadViewModel(
            ReceiptIngestManager(context, HaikuReceiptExtractor(context, auth, api)),
            transactionRepo,
            CategoryRepository(dbManager),
            ReceiptRepository(dbManager),
            ScanQuota(context),
            CategoryRuleRepository(dbManager),
            TagRepository(dbManager),
            TripRepository(dbManager),
            BillingManager(context, auth, analytics, crashReporting),
            BudgetRepository(dbManager),
            ReviewTracker(context),
            BuyingLimitNudger(
                transactionRepo,
                BuyingLimitsRepository(dbManager),
                SettingsStore(context),
                BuyingLimitNudgeBus(),
            ),
            analytics,
            crashReporting,
            TemplateRepository(dbManager),
        )
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val POLL_MS = 20L
        const val SETTLE_MS = 300L
    }
}
