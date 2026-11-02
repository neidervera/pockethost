package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF8CF3FF),

    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF321A63),
    onSecondaryContainer = Color(0xFFE2D6FF),

    tertiary = EmeraldActive,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF005227),
    onTertiaryContainer = Color(0xFF6CFFA0),

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextSecondary,

    outline = BorderDark,
    outlineVariant = Color(0xFF1E283D),

    error = CrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force consistent cyber dark server theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
