package com.budgetty.app.ui.upload

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.ingest.HaikuReceiptExtractor
import com.budgetty.app.data.ingest.ReceiptIngestManager
import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.remote.RECEIPT_API_BASE_URL
import com.budgetty.app.data.remote.ReceiptApi
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.review.ReviewTracker
import com.budgetty.app.data.quota.ScanQuota
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit

/**
 * Regression test for the duplicate-receipt save guard. A tester found that double-tapping Save on the
 * review screen created a whole duplicate receipt: [UploadViewModel.finalizeUpload] minted a fresh
 * upload id per call and only the editing path deleted old rows first, so a second invocation inserted
 * a second receipt + line items. The fix guards the function on `stage == REVIEW`.
 *
 * Runs the real dependency graph on the JVM (Robolectric + a real Room database in the test sandbox) —
 * there is no mocking library in the project and the repositories are wired to a concrete
 * [UserDatabaseManager], so the ViewModel is exercised as it ships. The Main dispatcher is set to
 * [Dispatchers.Unconfined] so `finalizeUpload`'s coroutine runs eagerly and drives itself to
 * completion across Room's own executors; the test then waits for [UploadStage.DONE] before asserting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelDuplicateGuardTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Robolectric doesn't run FirebaseInitProvider, and the repositories/billing/extractor all take
        // a FirebaseAuth. Initialise a default app with throwaway options — no user ever signs in and no
        // network call is made, so this stays fully offline (the DB resolves to the signed-out scratch db).
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun doubleFinalize_createsSingleReceiptAndItems() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val auth = FirebaseAuth.getInstance()
        val dbManager = UserDatabaseManager(context, auth)

        val transactionRepo = TransactionRepository(dbManager)
        val receiptRepo = ReceiptRepository(dbManager)
        val categoryRepo = CategoryRepository(dbManager)
        val ruleRepo = CategoryRuleRepository(dbManager)
        val budgetRepo = BudgetRepository(dbManager)
        val api = Retrofit.Builder().baseUrl(RECEIPT_API_BASE_URL).build().create(ReceiptApi::class.java)
        val ingest = ReceiptIngestManager(context, HaikuReceiptExtractor(context, auth, api))

        val vm = UploadViewModel(
            ingest,
            transactionRepo,
            categoryRepo,
            receiptRepo,
            ScanQuota(context),
            ruleRepo,
            BillingManager(context, auth),
            budgetRepo,
            ReviewTracker(context),
        )

        // Land on the review screen with one filled-in row.
        vm.startManual()
        val clientId = vm.uiState.value.transactions.first().clientId
        vm.updateName(clientId, "Milk")
        vm.updatePrice(clientId, "2.50")

        // The double-tap: two Save invocations back to back. The first flips stage to SAVING
        // synchronously; the guard must make the second a no-op.
        vm.finalizeUpload(NO_ITEMS, NO_AMOUNT) {}
        vm.finalizeUpload(NO_ITEMS, NO_AMOUNT) {}

        runBlocking {
            withTimeout(TIMEOUT_MS) {
                while (vm.uiState.value.stage != UploadStage.DONE) delay(POLL_MS)
            }
            // Drain: had the guard let the second call through, its independent save would land here.
            delay(DRAIN_MS)
        }

        val receipts = runBlocking { receiptRepo.getAll().first() }
        val items = runBlocking { transactionRepo.getAllOnce() }
        assertThat(receipts).hasSize(1)
        assertThat(items).hasSize(1)
        assertThat(items.single().name).isEqualTo("Milk")
    }

    private companion object {
        const val NO_ITEMS = "Add at least one item"
        const val NO_AMOUNT = "Add an amount"
        const val TIMEOUT_MS = 10_000L
        const val POLL_MS = 20L
        const val DRAIN_MS = 300L
    }
}
