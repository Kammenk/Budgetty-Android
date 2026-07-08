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
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.account.AccountScreen
import com.budgetty.app.ui.auth.AuthState
import com.budgetty.app.ui.auth.AuthViewModel
import com.budgetty.app.ui.auth.LoginScreen
import com.budgetty.app.ui.budget.BudgetScreen
import com.budgetty.app.ui.history.HistoryScreen
import com.budgetty.app.ui.home.HomeScreen
import com.budgetty.app.ui.insights.InsightsScreen
import com.budgetty.app.ui.onboarding.OnboardingScreen
import com.budgetty.app.ui.paywall.PaywallScreen
import com.budgetty.app.ui.rules.CategoryRulesScreen
import com.budgetty.app.ui.upload.UploadScreen
import com.budgetty.app.ui.widgets.WidgetsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun BudgettyApp(
    startRoute: String? = null,
    onStartRouteHandled: () -> Unit = {},
    authViewModel: AuthViewModel = koinViewModel(),
    settingsStore: SettingsStore = koinInject(),
) {
    val settings by settingsStore.settings.collectAsStateWithLifecycle()

    // First launch: show the onboarding carousel before anything else (login included).
    if (!settings.onboardingSeen) {
        OnboardingScreen(onDone = { settingsStore.setOnboardingSeen() })
        return
    }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        // No guest mode: unauthenticated users only ever see the login screen.
        AuthState.SignedOut -> LoginScreen()
        is AuthState.SignedIn -> MainScaffold(
            startRoute = startRoute,
            onStartRouteHandled = onStartRouteHandled,
        )
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
    val isImmersive = currentRoute == Routes.UPLOAD || currentRoute == Routes.PAYWALL
    val showBottomBar = !expanded && !isImmersive &&
        currentRoute != Routes.BUDGET && currentRoute != Routes.WIDGETS
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
    // top inset; every other route gets the standard scaffold insets.
    val contentModifier = if (currentRoute == Routes.PAYWALL) {
        modifier.padding(bottom = padding.calculateBottomPadding())
    } else {
        modifier.padding(padding)
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
            )
        }
        composable(Routes.INSIGHTS) {
            InsightsScreen(onNavigateToBudget = { navController.navigate(Routes.BUDGET) })
        }
        composable(Routes.ACCOUNT) {
            AccountScreen(
                onOpenPaywall = { navController.navigate(Routes.PAYWALL) },
                onOpenBudget = { navController.navigate(Routes.BUDGET) },
                onOpenWidgets = { navController.navigate(Routes.WIDGETS) },
                onOpenCategoryRules = { navController.navigate(Routes.CATEGORY_RULES) },
            )
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
            WidgetsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.CATEGORY_RULES) {
            CategoryRulesScreen(onNavigateBack = { navController.popBackStack() })
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
