package com.luis.alhendinfc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmberSoftContainer = Color(0xFF3D2E0A)

private val AlhendinColorScheme = darkColorScheme(
    primary = GreenAccent,
    onPrimary = GreenDeep,
    primaryContainer = GreenPitch,
    onPrimaryContainer = CreamSoft,
    secondary = GreenMint,
    onSecondary = GreenDeep,
    secondaryContainer = GreenSurface,
    onSecondaryContainer = CreamSoft,
    tertiary = AmberAccent,
    onTertiary = GreenDeep,
    tertiaryContainer = AmberSoftContainer,
    onTertiaryContainer = AmberSoft,
    background = GreenDeep,
    onBackground = TextWhite,
    surface = GreenMedium,
    onSurface = TextWhite,
    surfaceVariant = GreenLight,
    onSurfaceVariant = TextSecondary,
    outline = TextDisabled,
    outlineVariant = GreenSurface,
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
