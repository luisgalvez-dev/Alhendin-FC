package com.luis.alhendinfc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AlhendinColorScheme = darkColorScheme(
    primary = GreenAccent,
    onPrimary = TextWhite,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenAccentLight,
    secondary = GreenAccentLight,
    onSecondary = GreenDeep,
    secondaryContainer = GreenSurface,
    onSecondaryContainer = TextWhite,
    background = GreenDeep,
    onBackground = TextWhite,
    surface = GreenMedium,
    onSurface = TextWhite,
    surfaceVariant = GreenLight,
    onSurfaceVariant = TextSecondary,
    outline = TextDisabled,
    error = ErrorRed,
    onError = TextWhite,
)

@Composable
fun AlhendinFCTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AlhendinColorScheme,
        typography = Typography,
        content = content
    )
}
