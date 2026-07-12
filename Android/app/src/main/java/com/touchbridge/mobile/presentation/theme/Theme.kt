package com.touchbridge.mobile.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TouchBridgeColorScheme = darkColorScheme(
    primary = Color(0xFF4F8EF7),
    onPrimary = Color.White,
    secondary = Color(0xFF6B7280),
    background = Color.Black,
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFE8E8F0),
    onBackground = Color(0xFFE8E8F0),
    tertiary = Color(0xFF4ADE80),
    error = Color(0xFFF87171)
)

@Composable
fun TouchBridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TouchBridgeColorScheme,
        typography = Typography(),
        content = content
    )
}
