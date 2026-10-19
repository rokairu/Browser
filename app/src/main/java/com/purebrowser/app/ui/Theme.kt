package com.purebrowser.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B0E14),
    background = Color(0xFF0B0E14),
    surface = Color(0xFF131722),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1E2430),
    onSurfaceVariant = Color(0xFFBDC3CE),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE3E9F2),
    onSurfaceVariant = Color(0xFF44474E),
)

/**
 * Theme PureBrowser — otomatis mengikuti mode gelap/terang sistem.
 * Bisa dioverride manual via parameter darkTheme (untuk pengaturan nanti).
 */
@Composable
fun PureBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
