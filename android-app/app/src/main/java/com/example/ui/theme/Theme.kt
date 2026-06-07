package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    onPrimary = CosmicTextPrimary,
    secondary = CosmicSecondary,
    onSecondary = CosmicTextPrimary,
    tertiary = CosmicTertiary,
    onTertiary = CosmicTextPrimary,
    background = CosmicBackground,
    onBackground = CosmicTextPrimary,
    surface = CosmicSurface,
    onSurface = CosmicTextPrimary,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = CosmicTextSecondary,
    outline = CosmicBorder,
    error = CosmicError,
    onError = CosmicTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark "Cosmic Slate" theme by default
    content: @Composable () -> Unit
) {
    // We strictly use the optimized Cosmic colors scheme to honor user intent 
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
