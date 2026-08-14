package com.starledger.app.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StarLedgerColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF070A12),
    secondary = StarPurple,
    onSecondary = Color(0xFF070A12),
    tertiary = SurplusGold,
    onTertiary = Color(0xFF070A12),
    background = SpaceBackground,
    onBackground = TextPrimary,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    error = RiskRed,
    onError = Color(0xFF070A12),
    outline = DividerDark,
    outlineVariant = DividerDark,
)

@Composable
fun StarLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StarLedgerColorScheme,
        typography = StarLedgerTypography,
        content = content
    )
}
