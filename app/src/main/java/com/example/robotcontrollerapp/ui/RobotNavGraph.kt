package com.example.robotcontrollerapp.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun RobotNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "controller"
    ) {
        composable(
            route = "controller",
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(120)) },
            popExitTransition = { fadeOut(tween(180)) }
        ) {
            ControllerScreen(
                onOpenPinEditor = { navController.navigate("pin_editor") }
            )
        }

        composable(
            route = "pin_editor",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    tween(300)
                ) + fadeIn(tween(160))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    tween(220)
                ) + fadeOut(tween(150))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(300)
                ) + fadeIn(tween(160))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(220)
                ) + fadeOut(tween(150))
            }
        ) {
            PinEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
