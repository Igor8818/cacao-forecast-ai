package com.cacaoforecast.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CacaoForestPrimary,
    onPrimary = CacaoForestOnPrimary,
    primaryContainer = CacaoForestContainer,
    onPrimaryContainer = CacaoForestOnContainer,
    secondary = CacaoPodSecondary,
    onSecondary = CacaoPodOnSecondary,
    secondaryContainer = CacaoPodContainer,
    onSecondaryContainer = CacaoPodOnContainer,
    tertiary = CacaoSoilTertiary,
    background = CacaoSurfaceLight,
    onBackground = CacaoTextPrimary,
    surface = CacaoSurfaceCard,
    onSurface = CacaoTextPrimary,
    surfaceVariant = CacaoSurfaceVariant,
    onSurfaceVariant = CacaoTextSecondary,
    outline = CacaoOutline,
    error = SeverityCriticalRed,
    errorContainer = SeverityCriticalBg
)

@Composable
fun CacaoForecastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = CacaoTypography,
        shapes = CacaoShapes,
        content = content
    )
}
