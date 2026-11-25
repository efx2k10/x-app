package com.efx.twitterwrapper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EfxColorScheme = darkColorScheme(
    primary = Color(0xFF1D9BF0),
    onPrimary = Color.White,
    secondary = Color(0xFF1D9BF0),
    onSecondary = Color.White,
    background = Color.Black,
    onBackground = Color(0xFFE7E9EA),
    surface = Color(0xFF16181C),
    onSurface = Color(0xFFE7E9EA),
    surfaceVariant = Color(0xFF1F2328),
    onSurfaceVariant = Color(0xFF8B98A5),
    outline = Color(0xFF2F3336),
    error = Color(0xFFF4212E),
    onError = Color.White,
)

@Composable
fun EfxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EfxColorScheme,
        content = content,
    )
}
