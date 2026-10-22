package com.geupsik.alimi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFC2541B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF3A1100),
    secondary = Color(0xFF77574A),
    secondaryContainer = Color(0xFFFFDBCE),
    onSecondaryContainer = Color(0xFF2C160C),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5),
    surfaceContainer = Color(0xFFF7EEE8),
    surfaceContainerHigh = Color(0xFFF1E7E1),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB693),
    onPrimary = Color(0xFF5C1A00),
    primaryContainer = Color(0xFF7F2C00),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFE7BEAD),
    secondaryContainer = Color(0xFF5D4034),
    onSecondaryContainer = Color(0xFFFFDBCE),
    background = Color(0xFF1C1B1A),
    surface = Color(0xFF1C1B1A),
    surfaceContainer = Color(0xFF272321),
    surfaceContainerHigh = Color(0xFF322D2B),
    error = Color(0xFFFFB4AB),
)

@Composable
fun GeupsikTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
