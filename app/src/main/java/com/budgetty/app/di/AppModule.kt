package com.budgetty.app.di

import androidx.room.Room
import com.budgetty.app.data.backup.BackupManager
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.data.ingest.HaikuReceiptExtractor
import com.budgetty.app.data.ingest.ReceiptIngestManager
import com.budgetty.app.data.local.BudgettyDatabase
import com.budgetty.app.data.local.MIGRATION_1_2
import com.budgetty.app.data.local.MIGRATION_2_3
import com.budgetty.app.data.local.MIGRATION_3_4
import com.budgetty.app.data.local.MIGRATION_4_5
import com.budgetty.app.data.local.MIGRATION_5_6
import com.budgetty.app.data.local.MIGRATION_6_7
import com.budgetty.app.data.local.MIGRATION_7_8
import com.budgetty.app.data.local.MIGRATION_8_9
import com.budgetty.app.data.local.MIGRATION_9_10
import com.budgetty.app.data.local.MIGRATION_10_11
import com.budgetty.app.data.local.MIGRATION_11_12
import com.budgetty.app.data.local.MIGRATION_12_13
import com.budgetty.app.data.local.MIGRATION_13_14
import com.budgetty.app.data.local.MIGRATION_14_15
import com.budgetty.app.data.local.MIGRATION_15_16
import com.budgetty.app.data.local.MIGRATION_16_17
import com.budgetty.app.data.local.categorySeedCallback
import com.budgetty.app.data.remote.RECEIPT_API_BASE_URL
import com.budgetty.app.data.remote.ReceiptApi
import com.budgetty.app.data.repository.AuthRepository
import com.budgetty.app.data.repository.BudgetRepository
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.repository.CategoryRuleRepository
import com.budgetty.app.data.repository.ReceiptRepository
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.widget.WidgetDataProvider
import com.budgetty.app.widget.WidgetUpdater
import com.budgetty.app.ui.account.AccountViewModel
import com.budgetty.app.ui.auth.AuthViewModel
import com.budgetty.app.ui.budget.BudgetViewModel
import com.budgetty.app.ui.history.HistoryViewModel
import com.budgetty.app.ui.home.HomeViewModel
import com.budgetty.app.ui.insights.InsightsViewModel
import com.budgetty.app.ui.paywall.PaywallViewModel
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
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            BudgettyDatabase::class.java,
            "budgetty.db",
        ).addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
            MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
            MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
        )
            .addCallback(categorySeedCallback)
            .build()
    }
    single { get<BudgettyDatabase>().transactionDao() }
    single { get<BudgettyDatabase>().categoryDao() }
    single { get<BudgettyDatabase>().budgetDao() }
    single { get<BudgettyDatabase>().receiptDao() }
    single { get<BudgettyDatabase>().categoryRuleDao() }
    single { get<BudgettyDatabase>().recurringDao() }

    // Repository
    single { TransactionRepository(get()) }
    single { CategoryRepository(get()) }
    single { BudgetRepository(get()) }
    single { ReceiptRepository(get()) }
    single { CategoryRuleRepository(get()) }
    single { RecurringRepository(get()) }

    // Backup / restore (import-export)
    single { BackupManager(get(), get(), get(), get(), get(), get()) }

    // Free-tier scan quota
    single { ScanQuota(androidContext()) }

    // Play Billing (subscriptions)
    single { BillingManager(androidContext()) }

    // App settings (theme / currency / date format)
    single { SettingsStore(androidContext()) }

    // Home-screen widgets: snapshot provider + auto-refresh on budget/transaction/currency changes
    single { WidgetDataProvider(get(), get(), get(), get(), get(), get(), get()) }
    single { WidgetUpdater(androidContext(), get(), get(), get(), get()) }

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
    viewModel { AuthViewModel(get()) }
    viewModel { AccountViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { BudgetViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get(), get(), get()) }
    viewModel { InsightsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { UploadViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CategoryRulesViewModel(get(), get(), get()) }
    viewModel { PaywallViewModel(get()) }
}
