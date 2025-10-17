package com.example.robotcontrollerapp.ui

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
        composable("controller") {
            ControllerScreen(
                onOpenPinEditor = { navController.navigate("pin_editor") }
            )
        }

        composable("pin_editor") {
            PinEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
