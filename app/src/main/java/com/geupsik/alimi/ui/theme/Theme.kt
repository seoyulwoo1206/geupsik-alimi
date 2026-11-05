package com.geupsik.alimi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 로고에 맞춘 색: 버튼·글자는 확성기의 주황, 강조 배경은 로고 바탕의 노랑, 화면 바탕은 따뜻한 크림색
private val LightColors = lightColorScheme(
    primary = Color(0xFFC2541B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Color(0xFF3D2B00),
    secondary = Color(0xFF6E5B2E),
    secondaryContainer = Color(0xFFFBE7B5),
    onSecondaryContainer = Color(0xFF2A1F00),
    background = Color(0xFFFFFCF3),
    surface = Color(0xFFFFFCF3),
    surfaceContainerLow = Color(0xFFFDF7E8),
    surfaceContainer = Color(0xFFFAF2DF),
    surfaceContainerHigh = Color(0xFFF4EAD3),
    surfaceContainerHighest = Color(0xFFEEE3CA),
    error = Color(0xFFBA1A1A),
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
