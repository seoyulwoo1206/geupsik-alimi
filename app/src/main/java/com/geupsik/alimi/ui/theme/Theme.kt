package com.geupsik.alimi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 흰 바탕 + 연회색 카드로 눈이 편하게 하고, 로고의 노랑·주황은 강조할 곳(선택된 버튼, 오늘 표시)에만 쓴다
private val LightColors = lightColorScheme(
    primary = Color(0xFFC2541B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE9A8),
    onPrimaryContainer = Color(0xFF3D2B00),
    secondary = Color(0xFF6E5B2E),
    secondaryContainer = Color(0xFFFFF0C4),
    onSecondaryContainer = Color(0xFF2A1F00),
    background = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF1F1F22),
    onSurfaceVariant = Color(0xFF5F5E66),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainer = Color(0xFFF4F4F6),
    surfaceContainerHigh = Color(0xFFEDEDF0),
    surfaceContainerHighest = Color(0xFFE6E6EA),
    outlineVariant = Color(0xFFDEDDE3),
    error = Color(0xFFC62828),
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF8C1D18),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFFDCC48C),
    secondaryContainer = Color(0xFF54452A),
    onSecondaryContainer = Color(0xFFFBE7B5),
    background = Color(0xFF1C1A15),
    surface = Color(0xFF1C1A15),
    surfaceContainerLow = Color(0xFF221F18),
    surfaceContainer = Color(0xFF27241C),
    surfaceContainerHigh = Color(0xFF322E25),
    surfaceContainerHighest = Color(0xFF3D392F),
    error = Color(0xFFFFB4AB),
)

@Composable
fun GeupsikTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
