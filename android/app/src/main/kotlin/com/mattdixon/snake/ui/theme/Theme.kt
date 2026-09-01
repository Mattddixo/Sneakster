package com.mattdixon.snake.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SneaksterColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = ArenaBackground,
    background = ArenaBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextMuted,
    error = DangerColor,
)

/** Always dark: the glowing arena is the whole visual identity, independent of system theme. */
@Composable
fun SneaksterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SneaksterColorScheme,
        typography = SneaksterTypography,
        content = content,
    )
}
