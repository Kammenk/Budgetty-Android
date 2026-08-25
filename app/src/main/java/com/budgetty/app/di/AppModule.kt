package com.budgetty.app.di

import com.budgetty.app.data.backup.BackupManager
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.analytics.Analytics
import com.budgetty.app.crash.CrashReporting
import com.budgetty.app.review.ReviewTracker
import com.budgetty.app.data.ingest.HaikuReceiptExtractor
import com.budgetty.app.data.ingest.ReceiptIngestManager
import com.budgetty.app.data.local.UserDatabaseManager
import com.budgetty.app.data.remote.RECEIPT_API_BASE_URL
import com.budgetty.app.data.remote.ReceiptApi
import com.budgetty.app.data.repository.AuthRepository
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.BudgetRolloverRepository
import com.budgetty.app.data.repository.BuyingLimitsRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.SavingsRepository
import com.budgetty.app.data.repository.SubscriptionsRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.repository.WellbeingScoreRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.widget.WidgetDataProvider
import com.budgetty.app.widget.WidgetUpdater
import com.budgetty.app.ui.account.AccountViewModel
import com.budgetty.app.ui.auth.AuthViewModel
import com.budgetty.app.ui.budget.BudgetViewModel
import com.budgetty.app.ui.buyinglimits.BuyingLimitNudgeBus
import com.budgetty.app.ui.buyinglimits.BuyingLimitNudger
import com.budgetty.app.ui.buyinglimits.BuyingLimitsViewModel
import com.budgetty.app.ui.savings.SavingsGoalViewModel
import com.budgetty.app.ui.export.ExportViewModel
import com.budgetty.app.ui.subscriptions.SubscriptionsViewModel
import com.budgetty.app.ui.recap.RecapProvider
import com.budgetty.app.ui.recap.RecapViewModel
import com.budgetty.app.ui.wellbeing.WellbeingProvider
import com.budgetty.app.ui.wellbeing.WellbeingViewModel
import com.budgetty.app.ui.history.HistoryViewModel
import com.budgetty.app.ui.home.HomeViewModel
import com.budgetty.app.ui.insights.InsightsViewModel
import com.budgetty.app.ui.paywall.PaywallViewModel
import com.budgetty.app.ui.lock.AppLockViewModel
import com.budgetty.app.ui.quiz.InsightsQuizViewModel
import com.budgetty.app.ui.categories.ManageCategoriesViewModel
import com.budgetty.app.ui.rules.CategoryRulesViewModel
import com.budgetty.app.ui.upload.UploadViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    // Database: one file per signed-in account, so accounts never see each other's data.
    // Repositories resolve their DAOs through the manager on every use (no DAO singletons).
    single { UserDatabaseManager(androidContext(), get()) }

    // Repository
    single { TransactionRepository(get()) }
    single { CategoryRepository(get()) }
    single { BudgetRepository(get()) }
    single { BudgetRolloverRepository(get()) }
    single { ReceiptRepository(get()) }
    single { CategoryRuleRepository(get()) }
    single { RecurringRepository(get()) }
    single { SavingsRepository(get()) }
    single { SubscriptionsRepository(get()) }
    single { BuyingLimitsRepository(get()) }
    single { WellbeingScoreRepository(get()) }

    // App-scoped hand-off for the buying-limit save-time nudge (Upload posts, Home observes).
    single { BuyingLimitNudgeBus() }
    single { BuyingLimitNudger(get(), get(), get(), get()) }

    // Wellbeing score + tips — shared by the Wellbeing screen, the Home banner and the Insights row.
    single { WellbeingProvider(get(), get(), get(), get(), get(), get(), get(), get()) }

    // End-of-period recap — builds the just-closed period's story from the same repositories.
    single { RecapProvider(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    // Backup / restore (import-export)
    single { BackupManager(get()) }

    // Free-tier scan quota
    single { ScanQuota(androidContext()) }

    // Decides when to ask for a Play rating (the asking itself lives in MainActivity)
    single { ReviewTracker(androidContext()) }

    // Crash reporting (Crashlytics collection control)
    single { CrashReporting() }

    // Product analytics (Firebase Analytics collection control + typed event logging)
    single { Analytics(androidContext()) }

    // Play Billing (subscriptions) + the server-granted account-comp entitlement
    single { BillingManager(androidContext(), get()) }

    // App settings (theme / currency / date format)
    single { SettingsStore(androidContext()) }

    // Home-screen widgets: snapshot provider + auto-refresh on budget/transaction/currency changes
    single { WidgetDataProvider(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { WidgetUpdater(androidContext(), get(), get(), get(), get(), get()) }

    // Auth
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single { AuthRepository(get()) }

    // Application-scoped coroutine scope: for work that must finish even after a ViewModel is
    // cleared (e.g. wiping local data during account deletion).
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // Ingestion
    single { ReceiptIngestManager(androidContext(), get()) }

    // Networking + Haiku extraction (proxy). Lazy: no request fires until invoked.
    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(RECEIPT_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single { get<Retrofit>().create(ReceiptApi::class.java) }
    single { HaikuReceiptExtractor(androidContext(), get(), get()) }

    // ViewModels
    viewModel { AuthViewModel(get(), get()) }
    viewModel { AccountViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { BudgetViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (goalId: Long) -> SavingsGoalViewModel(get(), get(), goalId) }
    viewModel { SubscriptionsViewModel(get(), get(), get(), get(), get()) }
    viewModel { ExportViewModel(get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get(), get(), get()) }
    viewModel { InsightsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { WellbeingViewModel(get(), get(), get()) }
    viewModel { RecapViewModel(get(), get(), get()) }
    viewModel {
        UploadViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { CategoryRulesViewModel(get(), get(), get()) }
    viewModel { BuyingLimitsViewModel(get(), get(), get(), get(), get()) }
    viewModel { ManageCategoriesViewModel(get(), get(), get(), get(), get()) }
    viewModel { PaywallViewModel(get()) }
    viewModel { InsightsQuizViewModel(get(), get(), get()) }
    viewModel { AppLockViewModel(get(), get()) }
}
