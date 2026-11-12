package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class NothingColors(
    val mainBackground: Color,
    val cardBackground: Color,
    val elevatedBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accentRed: Color,
    val subtleBorder: Color,
    val statusSaved: Color = StatusSaved,
    val statusSaving: Color = StatusSaving,
    val statusUnsaved: Color = StatusUnsaved,
    val statusFailed: Color = StatusFailed
)

val LocalNothingColors = staticCompositionLocalOf {
    NothingColors(
        mainBackground = DarkBackground,
        cardBackground = DarkCard,
        elevatedBackground = DarkElevated,
        primaryText = DarkPrimaryText,
        secondaryText = DarkSecondaryText,
        accentRed = NothingRed,
        subtleBorder = DarkBorder
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    primaryContainer = DarkElevated,
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkSecondaryText,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkCard,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    primaryContainer = LightElevated,
    onPrimaryContainer = LightPrimaryText,
    secondary = LightSecondaryText,
    onSecondary = LightBackground,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightCard,
    onSurface = LightPrimaryText,
    surfaceVariant = LightElevated,
    onSurfaceVariant = LightSecondaryText,
    outline = LightBorder
)

@Composable
fun ReelLogTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val nothingColors = if (isDark) {
        NothingColors(
            mainBackground = DarkBackground,
            cardBackground = DarkCard,
            elevatedBackground = DarkElevated,
            primaryText = DarkPrimaryText,
            secondaryText = DarkSecondaryText,
            accentRed = NothingRed,
            subtleBorder = DarkBorder
        )
    } else {
        NothingColors(
            mainBackground = LightBackground,
            cardBackground = LightCard,
            elevatedBackground = LightElevated,
            primaryText = LightPrimaryText,
            secondaryText = LightSecondaryText,
            accentRed = NothingRed,
            subtleBorder = LightBorder
        )
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalNothingColors provides nothingColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ReelLogTypography,
            content = content
        )
    }
}
