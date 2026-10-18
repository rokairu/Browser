package com.purebrowser.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B0E14),
    background = Color(0xFF0B0E14),
    surface = Color(0xFF131722),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1E2430),
)

@Composable
fun PureBrowserTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
