package com.touchbridge.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.touchbridge.mobile.presentation.connect.ConnectScreen
import com.touchbridge.mobile.presentation.theme.TouchBridgeTheme
import com.touchbridge.mobile.presentation.touchpad.TouchpadScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            TouchBridgeTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "connect",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("connect") {
                        ConnectScreen(
                            onConnected = {
                                navController.navigate("touchpad") {
                                    popUpTo("connect") { inclusive = false }
                                }
                            }
                        )
                    }
                    composable("touchpad") {
                        TouchpadScreen(
                            onDisconnect = {
                                navController.navigate("connect") {
                                    popUpTo("connect") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
