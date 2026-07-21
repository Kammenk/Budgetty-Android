package com.budgetty.app

import android.app.Application
import com.budgetty.app.category.Categories
import com.budgetty.app.crash.CrashReporting
import com.budgetty.app.data.repository.CategoryRepository
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.di.appModule
import com.budgetty.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BudgettyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@BudgettyApplication)
            modules(appModule)
        }.koin
        // Apply the persisted crash-reporting choice to Crashlytics before anything can crash. The
        // preference (default-on, opt-out in Account) is the source of truth; SettingsStore loads it
        // synchronously from SharedPreferences, so it's ready immediately after Koin starts.
        koin.get<CrashReporting>().setEnabled(koin.get<SettingsStore>().settings.value.crashReportingEnabled)
        // Keep the home-screen widgets in sync while the process is alive.
        koin.get<WidgetUpdater>().start()
        // Mirror user-created categories into the Categories cache so their emoji + color resolve
        // everywhere a category renders (rows, charts, history), not only in the picker.
        koin.get<CategoryRepository>().categories
            .onEach { cats ->
                Categories.setCustomCategories(
                    cats.filter { it.isCustom }.map { Triple(it.name, it.icon, it.colorArgb) },
                )
            }
            .launchIn(koin.get<CoroutineScope>())
        purgeStoredImages()
    }

    /**
     * The app no longer stores any images — only the transactions read from receipts. This is a
     * one-time, best-effort cleanup of images written by older versions (captured receipt photos in
     * filesDir/receipts, and chosen avatar photos as filesDir/avatar_*.jpg).
     */
    private fun purgeStoredImages() {
        Thread {
            runCatching {
                filesDir.resolve("receipts").deleteRecursively()
                filesDir.listFiles { _, name -> name.startsWith("avatar_") }
                    ?.forEach { it.delete() }
            }
        }.start()
    }
}
