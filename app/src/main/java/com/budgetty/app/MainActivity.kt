package com.budgetty.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.data.settings.LocaleHelper
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.data.settings.ThemeMode
import com.budgetty.app.review.ReviewPrompter
import com.budgetty.app.review.ReviewTracker
import com.budgetty.app.ui.navigation.BudgettyApp
import com.budgetty.app.ui.navigation.Routes
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.update.InAppUpdateManager
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsStore: SettingsStore by inject()

    // Google Play In-App Updates — actively prompts eligible users to move to the latest build.
    private lateinit var inAppUpdateManager: InAppUpdateManager

    // Google Play In-App Review — the rating card, asked after enough successful scans.
    private val reviewTracker: ReviewTracker by inject()
    private lateinit var reviewPrompter: ReviewPrompter

    // The nav route a home-screen-widget tap asked us to open (null = normal launch).
    private val startRoute = mutableStateOf<String?>(null)

    // Apply the saved language as a per-app locale before any resources are resolved.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyOverride(newBase))
    }

    // testTagsAsResourceId (used on the root Surface below) is still an experimental Compose API.
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Construct + register before the activity reaches STARTED (Activity Result API requirement),
        // then ask Play whether a newer build is available for this user.
        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.checkForUpdate()
        reviewPrompter = ReviewPrompter(this)
        startRoute.value = startRouteFor(intent)
        enableEdgeToEdge()
        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle()
            // Refresh the formatting globals (read by formatMoney / formatDate) on each change.
            AppFormats.currencySymbol = settings.currency.symbol
            AppFormats.datePattern = settings.dateFormat.pattern
            AppFormats.dayMonthPattern = settings.dateFormat.dayMonthPattern

            // Re-create the activity when the language changes so attachBaseContext re-applies the
            // new locale and every string/resource re-resolves. The captured initial value resets
            // on recreate, so this fires once per change (no loop).
            val initialLanguage = remember { settings.language }
            LaunchedEffect(settings.language) {
                if (settings.language != initialLanguage) recreate()
            }
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            BudgettyTheme(darkTheme = dark, accent = settings.accent) {
                // A themed Surface paints colorScheme.background behind every screen, so screens
                // that don't draw their own background (e.g. LoginScreen, the auth-loading spinner)
                // follow the theme instead of falling through to the XML window background.
                Surface(
                    // Expose Compose testTags as view resource-ids so tools that match on resource
                    // ids — Firebase Test Lab's Robo login directives, UI Automator — can find tagged
                    // elements (e.g. the login fields). Invisible at runtime; no UX impact.
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // Once a FLEXIBLE update finishes downloading, prompt the user to restart to install.
                    val updateDownloaded by inAppUpdateManager.updateDownloaded
                        .collectAsStateWithLifecycle()
                    val snackbarHostState = remember { SnackbarHostState() }
                    val downloadedMessage = stringResource(R.string.update_downloaded_message)
                    val restartAction = stringResource(R.string.update_restart_action)
                    LaunchedEffect(updateDownloaded) {
                        if (updateDownloaded) {
                            val result = snackbarHostState.showSnackbar(
                                message = downloadedMessage,
                                actionLabel = restartAction,
                                duration = SnackbarDuration.Indefinite,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                inAppUpdateManager.completeUpdate()
                            }
                        }
                    }
                    // Ask for a Play rating once a scan has earned one. Driven from here rather than
                    // from the upload screen on purpose: by the time this fires the upload flow has
                    // already navigated away, so the card lands on a settled screen instead of
                    // fighting a transition.
                    val promptForReview by reviewTracker.pendingPrompt
                        .collectAsStateWithLifecycle()
                    LaunchedEffect(promptForReview) {
                        if (promptForReview) {
                            reviewTracker.onPromptRequested()
                            reviewPrompter.request()
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        BudgettyApp(
                            startRoute = startRoute.value,
                            onStartRouteHandled = { startRoute.value = null },
                        )
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .safeDrawingPadding(),
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume an interrupted IMMEDIATE update and surface any FLEXIBLE download that finished.
        inAppUpdateManager.onResume()
    }

    override fun onDestroy() {
        inAppUpdateManager.dispose()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startRoute.value = startRouteFor(intent)
    }

    /**
     * The nav route a launch intent asks us to open, or null for a normal launch: a home-screen
     * widget passes its target as [EXTRA_START_ROUTE]; an app-launcher shortcut (long-press the icon)
     * arrives as one of the shortcut actions below (see res/xml/shortcuts.xml).
     */
    private fun startRouteFor(intent: Intent?): String? = when (intent?.action) {
        ACTION_SCAN_RECEIPT -> Routes.upload("camera")
        ACTION_ADD_MANUAL -> Routes.upload("manual")
        else -> intent?.getStringExtra(EXTRA_START_ROUTE)
    }

    companion object {
        /** Intent extra naming the nav route to open when a home-screen widget is tapped. */
        const val EXTRA_START_ROUTE = "com.budgetty.app.START_ROUTE"

        /** App-launcher shortcut actions, mapped to nav routes in [startRouteFor]. */
        const val ACTION_SCAN_RECEIPT = "com.budgetty.app.action.SCAN_RECEIPT"
        const val ACTION_ADD_MANUAL = "com.budgetty.app.action.ADD_MANUAL"
    }
}
