package com.example.taptarget.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taptarget.ui.screens.about.AboutScreen
import com.example.taptarget.ui.screens.game.GameScreen
import com.example.taptarget.ui.screens.home.HomeScreen
import com.example.taptarget.ui.screens.result.ResultScreen
import com.example.taptarget.ui.screens.settings.SettingsScreen
import com.example.taptarget.ui.screens.splash.SplashScreen
import com.example.taptarget.ui.viewmodel.SettingsViewModel
import com.example.taptarget.ui.viewmodel.TapTargetViewModel

@Composable
fun TapTargetNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    gameViewModel: TapTargetViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = gameViewModel,
                onStartGame = {
                    navController.navigate(Screen.Game.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                }
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                viewModel = gameViewModel,
                onNavigateToResult = {
                    navController.navigate(Screen.Result.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Result.route) {
            ResultScreen(
                viewModel = gameViewModel,
                onPlayAgain = {
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Result.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
