package com.example.appenergytracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.appenergytracker.ui.screens.ChargeScreen
import com.example.appenergytracker.ui.screens.HistoryScreen
import com.example.appenergytracker.ui.screens.MainScreen
import com.example.appenergytracker.ui.screens.SettingScreen
import com.example.appenergytracker.ui.screens.TestScreen

object Routes {
    const val MAIN = "main"
    const val HISTORY = "history"
    const val CHARGE = "charge"
    const val SETTINGS = "settings"
    const val TEST = "test"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) { MainScreen(navController) }
        composable(Routes.HISTORY) { HistoryScreen(navController) }
        composable(Routes.CHARGE) { ChargeScreen(navController) }
        composable(Routes.SETTINGS) { SettingScreen() }
        composable(Routes.TEST) { TestScreen() }
    }
} 