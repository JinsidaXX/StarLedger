package com.starledger.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.starledger.app.core.design.components.CircleIcon
import com.starledger.app.core.starmap.StarCanvas
import com.starledger.app.core.starmap.StarVisual
import com.starledger.app.feature.allocation.TemplateEditScreen
import com.starledger.app.feature.home.HomeScreen
import com.starledger.app.feature.ledger.AccountsScreen
import com.starledger.app.feature.ledger.CategoriesScreen
import com.starledger.app.feature.ledger.LedgerScreen
import com.starledger.app.feature.ledger.TransactionEditScreen
import com.starledger.app.feature.onboarding.OnboardingScreen
import com.starledger.app.feature.planning.OwnedItemsScreen
import com.starledger.app.feature.planning.PlanDetailScreen
import com.starledger.app.feature.planning.PlanEditScreen
import com.starledger.app.feature.planning.PlanningScreen
import com.starledger.app.feature.review.ReviewScreen
import com.starledger.app.feature.settings.SettingsScreen
import com.starledger.app.feature.starmap.StarDetailScreen
import com.starledger.app.feature.starmap.StarmapScreen

@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val ready by mainViewModel.ready.collectAsStateWithLifecycle()
    val onboardingDone by mainViewModel.onboardingDone.collectAsStateWithLifecycle()

    if (!ready) {
        SplashScreen()
        return
    }

    val startDestination = if (onboardingDone) Routes.MAIN else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    mainViewModel.completeOnboarding()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onAddTransaction = { navController.navigate(Routes.TRANSACTION_ADD) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                onEditTransaction = { id -> navController.navigate(Routes.transactionEdit(id)) },
                onPlanDetail = { id -> navController.navigate(Routes.planDetail(id)) },
                onPlanEdit = { id -> navController.navigate(Routes.planEdit(id)) },
                onStarDetail = { cycleId -> navController.navigate(Routes.starDetail(cycleId)) },
                onReview = { cycleId -> navController.navigate(Routes.review(cycleId)) },
                onTemplateEdit = { templateId -> navController.navigate(Routes.templateEdit(templateId)) },
                onOwnedItems = { navController.navigate(Routes.OWNED_ITEMS) },
            )
        }
        composable(Routes.TRANSACTION_ADD) {
            TransactionEditScreen(
                transactionId = null,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TRANSACTION_EDIT,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
        ) {
            TransactionEditScreen(
                transactionId = it.arguments?.getLong("transactionId"),
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.PLAN_EDIT,
            arguments = listOf(
                navArgument("planId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
        ) {
            val planId = it.arguments?.getLong("planId") ?: -1L
            PlanEditScreen(
                planId = planId.takeIf { id -> id > 0 },
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PLAN_DETAIL,
            arguments = listOf(navArgument("planId") { type = NavType.LongType }),
        ) {
            PlanDetailScreen(
                planId = it.arguments?.getLong("planId") ?: 0L,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.planEdit(id)) },
            )
        }
        composable(
            route = Routes.STAR_DETAIL,
            arguments = listOf(navArgument("cycleId") { type = NavType.LongType }),
        ) {
            StarDetailScreen(
                cycleId = it.arguments?.getLong("cycleId") ?: 0L,
                onBack = { navController.popBackStack() },
                onReview = { cycleId -> navController.navigate(Routes.review(cycleId)) },
            )
        }
        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument("cycleId") { type = NavType.LongType }),
        ) {
            ReviewScreen(
                cycleId = it.arguments?.getLong("cycleId") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TEMPLATE_EDIT,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType }),
        ) {
            TemplateEditScreen(
                templateId = it.arguments?.getLong("templateId") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.OWNED_ITEMS) {
            OwnedItemsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                StarCanvas(
                    visual = StarVisual(
                        brightness = 0.7f,
                        rays = emptyList(),
                        hasTransactions = true,
                        allocationDone = true,
                    ),
                    modifier = Modifier.size(120.dp),
                )
            }
            Text("星图账本", style = MaterialTheme.typography.titleLarge)
            Text("让每一笔收支，都有自己的轨道", style = MaterialTheme.typography.bodySmall)
        }
    }
}
