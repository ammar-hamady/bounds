package com.example.bounds.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark scheme (default / always-on for Bounds) ─────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Amber,
    onPrimary        = BgPrimary,
    primaryContainer = AmberDim,
    onPrimaryContainer = Amber,
    secondary        = TextMuted,
    onSecondary      = BgPrimary,
    background       = BgPrimary,
    onBackground     = TextPrimary,
    surface          = BgSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = BgElevated,
    onSurfaceVariant = TextMuted,
    outline          = BorderDim,
    error            = ErrorRed,
    onError          = Color.White
)

// ── Light scheme ──────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Amber,
    onPrimary        = Color.White,
    primaryContainer = AmberDim,
    onPrimaryContainer = Amber,
    secondary        = TextMuted,
    onSecondary      = Color.White,
    background       = Color(0xFFF5F5F5),
    onBackground     = Color(0xFF111111),
    surface          = Color.White,
    onSurface        = Color(0xFF111111),
    surfaceVariant   = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF666666),
    outline          = Color(0xFFDDDDDD),
    error            = ErrorRed,
    onError          = Color.White
)

@Composable
fun BoundsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // disabled — always use Bounds palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
