package app.otakureader.feature.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.otakureader.core.navigation.Route
import app.otakureader.feature.onboarding.OnboardingScreen

fun NavGraphBuilder.onboardingScreen(
    onComplete: () -> Unit,
    onNavigateToExtensions: () -> Unit,
) {
    composable<Route.Onboarding> {
        OnboardingScreen(
            onComplete = onComplete,
            onNavigateToExtensions = onNavigateToExtensions
        )
    }
}
