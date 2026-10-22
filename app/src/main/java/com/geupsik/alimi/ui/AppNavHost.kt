package com.geupsik.alimi.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.geupsik.alimi.ui.allergy.AllergyScreen
import com.geupsik.alimi.ui.meal.MealScreen
import com.geupsik.alimi.ui.onboarding.SchoolSearchScreen
import com.geupsik.alimi.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data class SchoolSearchRoute(val fromSettings: Boolean = false)

@Serializable
data object MealRoute

@Serializable
data object SettingsRoute

@Serializable
data object AllergyRoute

@Composable
fun AppNavHost(hasSchool: Boolean) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = if (hasSchool) MealRoute else SchoolSearchRoute(),
    ) {
        composable<SchoolSearchRoute> { entry ->
            val route = entry.toRoute<SchoolSearchRoute>()
            SchoolSearchScreen(
                fromSettings = route.fromSettings,
                onBack = { nav.popBackStack() },
                onSchoolSelected = {
                    if (route.fromSettings) {
                        nav.popBackStack()
                    } else {
                        // 처음 등록이면 검색 화면을 지우고 급식 화면으로 간다
                        nav.navigate(MealRoute) {
                            popUpTo<SchoolSearchRoute> { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<MealRoute> {
            MealScreen(
                onOpenSettings = { nav.navigate(SettingsRoute) },
                onOpenAllergy = { nav.navigate(AllergyRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onChangeSchool = { nav.navigate(SchoolSearchRoute(fromSettings = true)) },
                onOpenAllergy = { nav.navigate(AllergyRoute) },
            )
        }
        composable<AllergyRoute> {
            AllergyScreen(onDone = { nav.popBackStack() })
        }
    }
}
