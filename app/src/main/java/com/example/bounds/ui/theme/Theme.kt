package com.example.bounds.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val target = if (darkTheme) DarkColorScheme else LightColorScheme

    // Snap on first composition so the initial render has no animation;
    // subsequent theme toggles smoothly interpolate over 300 ms.
    var isInitial by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { isInitial = false }
    val spec: AnimationSpec<Color> = if (isInitial) snap() else tween(300)

    val primary              by animateColorAsState(target.primary,              spec, label = "primary")
    val onPrimary            by animateColorAsState(target.onPrimary,            spec, label = "onPrimary")
    val primaryContainer     by animateColorAsState(target.primaryContainer,     spec, label = "primaryContainer")
    val onPrimaryContainer   by animateColorAsState(target.onPrimaryContainer,   spec, label = "onPrimaryContainer")
    val secondary            by animateColorAsState(target.secondary,            spec, label = "secondary")
    val onSecondary          by animateColorAsState(target.onSecondary,          spec, label = "onSecondary")
    val background           by animateColorAsState(target.background,           spec, label = "background")
    val onBackground         by animateColorAsState(target.onBackground,         spec, label = "onBackground")
    val surface              by animateColorAsState(target.surface,              spec, label = "surface")
    val onSurface            by animateColorAsState(target.onSurface,            spec, label = "onSurface")
    val surfaceVariant       by animateColorAsState(target.surfaceVariant,       spec, label = "surfaceVariant")
    val onSurfaceVariant     by animateColorAsState(target.onSurfaceVariant,     spec, label = "onSurfaceVariant")
    val outline              by animateColorAsState(target.outline,              spec, label = "outline")
    val error                by animateColorAsState(target.error,                spec, label = "error")
    val onError              by animateColorAsState(target.onError,              spec, label = "onError")

    val animatedScheme = target.copy(
        primary            = primary,
        onPrimary          = onPrimary,
        primaryContainer   = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary          = secondary,
        onSecondary        = onSecondary,
        background         = background,
        onBackground       = onBackground,
        surface            = surface,
        onSurface          = onSurface,
        surfaceVariant     = surfaceVariant,
        onSurfaceVariant   = onSurfaceVariant,
        outline            = outline,
        error              = error,
        onError            = onError
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography  = Typography,
        content     = content
    )
}
