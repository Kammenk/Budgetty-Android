package com.budgetty.app

import android.app.Application
import com.budgetty.app.analytics.Analytics
import com.budgetty.app.appcheck.installAppCheck
import com.budgetty.app.category.Categories
import com.budgetty.app.crash.CrashReporting
import com.budgetty.app.data.local.BudgettyDatabase
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
        // Harden access to the shared Firebase backend before Koin starts or any Firebase request
        // fires: attach App Check attestation tokens (monitor mode — non-enforcing until the console
        // turns on per-service enforcement, so this is behaviour-neutral today).
        installAppCheck(this)
        val koin = startKoin {
            androidContext(this@BudgettyApplication)
            modules(appModule)
        }.koin
        // Telemetry is opt-in: nothing collects until the user makes the first-run choice on the
        // consent screen (analyticsConsentDecided). Until then both SDKs stay off regardless of the
        // stored flags — so an upgrading user who was on the old default-on behaviour is also held off
        // until they decide. SettingsStore loads synchronously from SharedPreferences, so the choice is
        // ready immediately after Koin starts; the consent screen and Account toggles keep it in sync.
        val settings = koin.get<SettingsStore>().settings.value
        val consented = settings.analyticsConsentDecided
        koin.get<CrashReporting>().setEnabled(consented && settings.crashReportingEnabled)
        // Static crash key: the Room schema version, so a crash report names the DB version it hit
        // (see CrashReporting.setDatabaseVersion / BudgettyDatabase.VERSION).
        koin.get<CrashReporting>().setDatabaseVersion(BudgettyDatabase.VERSION)
        koin.get<Analytics>().setEnabled(consented && settings.analyticsEnabled)
        // Keep the home-screen widgets in sync while the process is alive.
        koin.get<WidgetUpdater>().start()
        // Mirror user-created categories into the Categories cache so their emoji + color resolve
        // everywhere a category renders (rows, charts, history), not only in the picker.
        koin.get<CategoryRepository>().categories
            .onEach { cats ->
                Categories.setCategories(
                    cats.map { Categories.CategoryData(it.name, it.icon, it.colorArgb, it.parent, it.isCustom) },
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
