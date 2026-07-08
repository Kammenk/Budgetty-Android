package com.budgetty.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.budgetty.app.data.settings.AccentTheme
import com.budgetty.app.ui.util.isExpandedWidth

private val LightColors = lightColorScheme(
    primary = md_primary, onPrimary = md_onPrimary,
    primaryContainer = md_primaryContainer, onPrimaryContainer = md_onPrimaryContainer,
    secondary = md_secondary, onSecondary = md_onSecondary,
    secondaryContainer = md_secondaryContainer, onSecondaryContainer = md_onSecondaryContainer,
    tertiary = md_tertiary, onTertiary = md_onTertiary,
    tertiaryContainer = md_tertiaryContainer, onTertiaryContainer = md_onTertiaryContainer,
    error = md_error, onError = md_onError,
    errorContainer = md_errorContainer, onErrorContainer = md_onErrorContainer,
    background = md_background, onBackground = md_onBackground,
    surface = md_surface, onSurface = md_onSurface,
    surfaceVariant = md_surfaceVariant, onSurfaceVariant = md_onSurfaceVariant,
    outline = md_outline, outlineVariant = md_outlineVariant,
    surfaceContainerLowest = md_surfaceContainerLowest, surfaceContainerLow = md_surfaceContainerLow,
    surfaceContainer = md_surfaceContainer, surfaceContainerHigh = md_surfaceContainerHigh,
    surfaceContainerHighest = md_surfaceContainerHighest,
    inverseSurface = md_inverseSurface, inverseOnSurface = md_inverseOnSurface,
    inversePrimary = md_inversePrimary, scrim = md_scrim,
)

private val DarkColors = darkColorScheme(
    primary = md_primary_dark, onPrimary = md_onPrimary_dark,
    primaryContainer = md_primaryContainer_dark, onPrimaryContainer = md_onPrimaryContainer_dark,
    secondary = md_secondary_dark, onSecondary = md_onSecondary_dark,
    secondaryContainer = md_secondaryContainer_dark, onSecondaryContainer = md_onSecondaryContainer_dark,
    tertiary = md_tertiary_dark, onTertiary = md_onTertiary_dark,
    tertiaryContainer = md_tertiaryContainer_dark, onTertiaryContainer = md_onTertiaryContainer_dark,
    error = md_error_dark, onError = md_onError_dark,
    errorContainer = md_errorContainer_dark, onErrorContainer = md_onErrorContainer_dark,
    background = md_background_dark, onBackground = md_onBackground_dark,
    surface = md_surface_dark, onSurface = md_onSurface_dark,
    surfaceVariant = md_surfaceVariant_dark, onSurfaceVariant = md_onSurfaceVariant_dark,
    outline = md_outline_dark, outlineVariant = md_outlineVariant_dark,
    surfaceContainerLowest = md_surfaceContainerLowest_dark, surfaceContainerLow = md_surfaceContainerLow_dark,
    surfaceContainer = md_surfaceContainer_dark, surfaceContainerHigh = md_surfaceContainerHigh_dark,
    surfaceContainerHighest = md_surfaceContainerHighest_dark,
    inverseSurface = md_inverseSurface_dark, inverseOnSurface = md_inverseOnSurface_dark,
    inversePrimary = md_inversePrimary_dark, scrim = md_scrim,
)

/** Accent primary override; null keeps the base scheme's default. */
private fun accentPrimary(accent: AccentTheme, dark: Boolean): Color? = when (accent) {
    AccentTheme.DEFAULT -> null
    AccentTheme.SAGE -> if (dark) Color(0xFFA8C6AA) else Color(0xFF3E5E41)
    AccentTheme.OCEAN -> if (dark) Color(0xFF8FC8D8) else Color(0xFF1C5C6E)
    AccentTheme.PLUM -> if (dark) Color(0xFFCFA6D6) else Color(0xFF6A2E78)
}

/**
 * App theme. [darkTheme] comes from the user's Theme setting (System / Light / Dark),
 * [accent] from their (premium) accent choice.
 *
 * NOTE: the design deliberately does NOT use dynamic color (Material You wallpaper tint),
 * because Budgetty ships its own brand violet + premium accents. Keep dynamicColor OFF so
 * the brand palette above is always used.
 */
@Composable
fun BudgettyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentTheme = AccentTheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    val base: ColorScheme = if (darkTheme) DarkColors else LightColors
    val primary = accentPrimary(accent, darkTheme)
    val colorScheme = if (primary != null) base.copy(primary = primary) else base

    // Swap the dimension set by window size so `MaterialTheme.dimens.<token>` yields larger page
    // padding / section gaps on tablets without any per-call-site branching. Tracks the live
    // configuration (rotation, fold, split-screen) via isExpandedWidth().
    val dimens = if (isExpandedWidth()) ExpandedDimens else CompactDimens

    CompositionLocalProvider(LocalDimens provides dimens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,   // Type.kt
            shapes = BudgettyShapes,   // Shape.kt
            content = content,
        )
    }
}
