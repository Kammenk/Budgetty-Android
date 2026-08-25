package com.budgetty.app.ui.navigation

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.budgetty.app.R
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budgetty.app.data.settings.AppSettings
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.debug.DebugAuth
import com.budgetty.app.ui.recap.RecapDue
import com.budgetty.app.ui.recap.RecapLoadingBackdrop
import com.budgetty.app.ui.recap.RecapReopenScreen
import com.budgetty.app.ui.recap.RecapScheduler
import com.budgetty.app.ui.recap.RecapStory
import com.budgetty.app.ui.recap.RecapStoryScreen
import com.budgetty.app.ui.recap.RecapViewModel
import com.budgetty.app.ui.util.BuyingLimitCounter
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.account.AccountScreen
import com.budgetty.app.ui.auth.AuthState
import com.budgetty.app.ui.auth.AuthViewModel
import com.budgetty.app.ui.auth.LoginScreen
import com.budgetty.app.ui.budget.BudgetScreen
import com.budgetty.app.ui.buyinglimits.BuyingLimitsScreen
import com.budgetty.app.ui.savings.SavingsGoalDetailScreen
import com.budgetty.app.ui.lock.SetPinScreen
import com.budgetty.app.ui.subscriptions.SubscriptionsScreen
import com.budgetty.app.ui.wellbeing.WellbeingNav
import com.budgetty.app.ui.wellbeing.WellbeingScreen
import com.budgetty.app.ui.history.HistoryScreen
import com.budgetty.app.ui.home.HomeScreen
import com.budgetty.app.ui.insights.InsightsScreen
import com.budgetty.app.ui.onboarding.OnboardingScreen
import com.budgetty.app.ui.paywall.PaywallScreen
import com.budgetty.app.ui.quiz.InsightsQuizScreen
import com.budgetty.app.ui.categories.ManageCategoriesScreen
import com.budgetty.app.ui.rules.CategoryRulesScreen
import com.budgetty.app.ui.upload.UploadScreen
import com.budgetty.app.ui.widgets.WidgetsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalDate

@Composable
fun BudgettyApp(
    startRoute: String? = null,
    onStartRouteHandled: () -> Unit = {},
    authViewModel: AuthViewModel = koinViewModel(),
    settingsStore: SettingsStore = koinInject(),
) {
    // Debug-only test bypass: skip onboarding + login + quiz straight to the app. Compile-time false
    // in release (see DebugAuth), and set once in MainActivity.onCreate before this composes, so no
    // Compose state is needed to observe it.
    if (DebugAuth.skipAuth) {
        MainScaffold(startRoute = startRoute, onStartRouteHandled = onStartRouteHandled)
        return
    }

    val settings by settingsStore.settings.collectAsStateWithLifecycle()

    // First launch: show the onboarding carousel before anything else (login included).
    if (!settings.onboardingSeen) {
        OnboardingScreen(onDone = { settingsStore.setOnboardingSeen() })
        return
    }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Only meaningful once signed in (see below); while signed out the login screen shows its own
    // progress, and a reset-password send would otherwise replace it mid-dialog.
    val authInProgress by authViewModel.loading.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Loading -> AuthProgress()
        // No guest mode: unauthenticated users only ever see the login screen.
        AuthState.SignedOut -> LoginScreen()
        // A fresh sign-up arms the one-time Insights setup quiz; it gates the main app until
        // finished or skipped (both clear the pending flag, which recomposes us into the scaffold).
        is AuthState.SignedIn -> when {
            // A Google sign-up only learns it created the account after the credential exchange, by
            // which point Firebase has already flipped us to SignedIn. Waiting for the in-flight
            // call to finish is what keeps Home from showing for a frame before the quiz — an email
            // sign-up dodges this by arming the flag before it calls Firebase at all.
            authInProgress -> AuthProgress()
            settings.insightsQuizPending -> InsightsQuizScreen()
            // Layered like the quiz gate: on the first open on/after a period boundary the recap
            // interstitial shows over the app, then dismisses to Home. Everyday opens skip it.
            else -> RecapGate(
                startRoute = startRoute,
                onStartRouteHandled = onStartRouteHandled,
                settings = settings,
            )
        }
    }
}

/**
 * Decides whether the end-of-period recap interstitial shows before the main app. The cheap
 * [RecapScheduler.due] check (settings + clock, no DB) short-circuits the common "nothing due" open
 * straight to [MainScaffold] with no flash; only when a boundary is due does it spin up the
 * [RecapViewModel] to load the story, holding a neutral backdrop until the data guard has run.
 */
@Composable
private fun RecapGate(
    startRoute: String?,
    onStartRouteHandled: () -> Unit,
    settings: AppSettings,
) {
    // "See details" on the last card exits to Insights; a plain close exits to Home (or the widget route).
    var pendingStart by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveStart = pendingStart ?: startRoute
    val onHandled = {
        pendingStart = null
        onStartRouteHandled()
    }

    // Frozen once per open (rememberSaveable, not reactive to settings): whether to run the recap path
    // at all. Kept frozen so the in-story frequency control (§1.4) can write recapEnabled/recapFrequency
    // without re-running the scheduler and tearing down the story the user is mid-read of — the new
    // cadence applies on the NEXT open.
    val runRecapPath = rememberSaveable {
        RecapScheduler.due(
            enabled = settings.recapEnabled,
            frequency = settings.recapFrequency,
            lastShownWeek = settings.recapLastShownWeek,
            lastShownMonth = settings.recapLastShownMonth,
            today = LocalDate.now(),
            monthStartDay = settings.monthStartDay,
            firstDayOfWeek = BuyingLimitCounter.localeFirstDayOfWeek(),
        ) != null
    }

    // When a boundary is due, load + show the story (or a neutral hold); the guard-skip path falls
    // through to the single MainScaffold call below, so closing the recap never recreates the NavHost.
    if (runRecapPath) {
        val recapViewModel: RecapViewModel = koinViewModel()
        val state by recapViewModel.interstitial.collectAsStateWithLifecycle()
        var closed by rememberSaveable { mutableStateOf(false) }
        // Latch the first built story + its due decision for this open. A mid-story settings edit
        // (the §1.4 control) re-emits the interstitial, so without this latch the story could be
        // swapped or dropped under the user's finger; latching freezes it until they close it.
        val locked = remember { mutableStateOf<Pair<RecapDue, RecapStory>?>(null) }
        if (locked.value == null) {
            val due = state.due
            val story = state.story
            if (due != null && story != null) locked.value = due to story
        }
        val shown = locked.value
        when {
            closed -> Unit // closed by the user → fall through to MainScaffold below
            shown != null -> {
                RecapStoryScreen(
                    story = shown.second,
                    onClose = {
                        recapViewModel.markShown(shown.first)
                        closed = true
                    },
                    onSeeDetails = {
                        pendingStart = Routes.INSIGHTS
                        recapViewModel.markShown(shown.first)
                        closed = true
                    },
                    onShown = recapViewModel::onRecapShown,
                    onCompleted = recapViewModel::onRecapCompleted,
                    onStreakSurfaced = recapViewModel::onStreakSurfaced,
                    recapEnabled = settings.recapEnabled,
                    recapFrequency = settings.recapFrequency,
                    onRecapFrequencyChange = recapViewModel::setRecapFrequencyChoice,
                )
                return
            }
            // Rare hold on the one due open, until the DB loads + the guard runs — never flashes Home.
            !state.isLoaded -> {
                RecapLoadingBackdrop()
                return
            }
            // Guard skipped (first-run / no data / weekly under floor): stamp the period(s) and fall through.
            else -> LaunchedEffect(state.due) { state.due?.let(recapViewModel::markShown) }
        }
    }
    MainScaffold(startRoute = effectiveStart, onStartRouteHandled = onHandled)
}

@Composable
private fun AuthProgress() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MainScaffold(
    startRoute: String? = null,
    onStartRouteHandled: () -> Unit = {},
) {
    val navController = rememberNavController()

    // A widget tap launches the app with a target route; navigate to it once, then clear it.
    LaunchedEffect(startRoute) {
        val route = startRoute ?: return@LaunchedEffect
        // Tabs switch via navigateToTab; pushed destinations (Budget, the scan/upload flow) navigate directly.
        if (route == Routes.BUDGET || route.startsWith("upload/")) navController.navigate(route)
        else navController.navigateToTab(route)
        onStartRouteHandled()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val expanded = isExpandedWidth()

    // Upload and Paywall are immersive full-screen flows on every form factor. Budget is immersive
    // on the phone (no bottom bar), but on a tablet it's a primary rail destination, so the rail
    // stays visible there.
    val isImmersive = currentRoute == Routes.UPLOAD || currentRoute == Routes.PAYWALL ||
        currentRoute == Routes.RECAP
    // The bottom bar belongs to the top-level tabs only. Every pushed/detail route — Budget, Widgets,
    // Category rules, the Savings-goal detail, Subscriptions, Set-PIN — hides it. An allowlist of the
    // tab routes (rather than a blocklist of pushed ones) keeps new pushed routes correct by default.
    val isTabRoute = BottomNavDestination.entries.any { it.route == currentRoute }
    val showBottomBar = !expanded && isTabRoute
    val showRail = expanded && !isImmersive

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavDestination.entries.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(destination.route) },
                            icon = {
                                Icon(destination.icon, contentDescription = stringResource(destination.labelRes))
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (showRail) {
                BudgettyNavRail(
                    currentEntry = backStackEntry,
                    onSelect = { route -> navController.navigateToTab(route) },
                )
            }
            BudgettyNavHost(
                navController = navController,
                currentRoute = currentRoute,
                padding = padding,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/** Vertical navigation rail shown on tablets in place of the phone's bottom bar. */
@Composable
private fun BudgettyNavRail(
    currentEntry: NavBackStackEntry?,
    onSelect: (String) -> Unit,
) {
    NavigationRail(
        header = {
            // Brand mark at the top of the rail — the app icon, composited from its adaptive-icon
            // layers (painterResource can't load the AdaptiveIconDrawable directly) and scaled so the
            // safe zone fills the rounded tile.
            Box(
                modifier = Modifier
                    .padding(vertical = MaterialTheme.dimens.md)
                    .size(MaterialTheme.dimens.avatar)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd)),
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.5f),
                )
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.5f),
                )
            }
        },
    ) {
        RailDestination.entries.forEach { destination ->
            val selected = currentEntry?.destination?.hierarchy
                ?.any { it.route == destination.route } == true
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(destination.route) },
                icon = {
                    Icon(destination.icon, contentDescription = stringResource(destination.labelRes))
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@Composable
private fun BudgettyNavHost(
    navController: NavHostController,
    currentRoute: String?,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Paywall draws its gradient hero edge-to-edge (behind the status bar), so it manages its own
    // top inset; the recap story is full-bleed (its band backdrops run behind both system bars and it
    // handles its own insets); every other route gets the standard scaffold insets.
    val contentModifier = when (currentRoute) {
        Routes.PAYWALL -> modifier.padding(bottom = padding.calculateBottomPadding())
        Routes.RECAP -> modifier
        else -> modifier.padding(padding)
    }
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = contentModifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToUpload = { source -> navController.navigate(Routes.upload(source)) },
                onNavigateToEdit = { receiptId -> navController.navigate(Routes.editReceipt(receiptId)) },
                onNavigateToBudget = { navController.navigate(Routes.BUDGET) },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                onNavigateToHistory = { navController.navigateToTab(Routes.HISTORY) },
                onNavigateToInsights = { navController.navigateToTab(Routes.INSIGHTS) },
                onNavigateToAccount = { navController.navigateToTab(Routes.ACCOUNT) },
                onNavigateToWellbeing = { navController.navigate(Routes.WELLBEING) },
                onNavigateToBuyingLimits = { navController.navigate(Routes.BUYING_LIMITS) },
            )
        }
        composable(Routes.INSIGHTS) {
            InsightsScreen(
                onNavigateToBudget = { navController.navigate(Routes.BUDGET) },
                onNavigateToSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                onNavigateToWellbeing = { navController.navigate(Routes.WELLBEING) },
                onNavigateToRecap = { navController.navigate(Routes.RECAP) },
            )
        }
        composable(Routes.SUBSCRIPTIONS) {
            SubscriptionsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.WELLBEING) {
            WellbeingScreen(
                onNavigateBack = { navController.popBackStack() },
                nav = WellbeingNav(
                    toBudget = { navController.navigate(Routes.BUDGET) },
                    toSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                    toGoals = { navController.navigate(Routes.BUDGET) },
                    toInsights = { navController.navigateToTab(Routes.INSIGHTS) },
                    toHistory = { navController.navigateToTab(Routes.HISTORY) },
                    addReceipt = { navController.navigate(Routes.upload("camera")) },
                ),
            )
        }
        composable(Routes.ACCOUNT) {
            AccountScreen(
                onOpenPaywall = { navController.navigate(Routes.PAYWALL) },
                onOpenBudget = { navController.navigate(Routes.BUDGET) },
                onOpenWidgets = { navController.navigate(Routes.WIDGETS) },
                onOpenCategoryRules = { navController.navigate(Routes.CATEGORY_RULES) },
                onOpenBuyingLimits = { navController.navigate(Routes.BUYING_LIMITS) },
                onOpenManageCategories = { navController.navigate(Routes.MANAGE_CATEGORIES) },
                onSetupPin = { navController.navigate(Routes.SET_PIN) },
            )
        }
        composable(Routes.SET_PIN) {
            SetPinScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.RECAP) {
            RecapReopenScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.UPLOAD,
            arguments = listOf(
                navArgument(Routes.UPLOAD_ARG_SOURCE) { type = NavType.StringType },
                navArgument(Routes.UPLOAD_ARG_RECEIPT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            UploadScreen(
                source = entry.arguments?.getString(Routes.UPLOAD_ARG_SOURCE) ?: "file",
                receiptId = entry.arguments?.getLong(Routes.UPLOAD_ARG_RECEIPT_ID) ?: -1L,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
            )
        }
        composable(Routes.BUDGET) {
            BudgetScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                onNavigateToGoal = { navController.navigate(Routes.savingsGoal(it)) },
            )
        }
        composable(
            route = Routes.SAVINGS_GOAL,
            arguments = listOf(navArgument(Routes.SAVINGS_GOAL_ARG) { type = NavType.LongType }),
        ) { entry ->
            SavingsGoalDetailScreen(
                goalId = entry.arguments?.getLong(Routes.SAVINGS_GOAL_ARG) ?: -1L,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PAYWALL) {
            PaywallScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateToReceipt = { navController.navigate(Routes.editReceipt(it)) },
                onNavigateToBudget = { navController.navigate(Routes.BUDGET) },
            )
        }
        composable(Routes.WIDGETS) {
            WidgetsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
            )
        }
        composable(Routes.CATEGORY_RULES) {
            CategoryRulesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.BUYING_LIMITS) {
            BuyingLimitsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
            )
        }
        composable(Routes.MANAGE_CATEGORIES) {
            ManageCategoriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenPaywall = { navController.navigate(Routes.PAYWALL) },
            )
        }
    }
}

/** Navigates to a primary tab/rail destination, preserving each tab's own back stack. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
