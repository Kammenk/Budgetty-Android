package com.budgetty.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ── Material 3 baseline (violet/purple) — DEFAULT ACCENT ──────────────────────
// Light
val md_primary = Color(0xFF6650A4)
val md_onPrimary = Color(0xFFFFFFFF)
val md_primaryContainer = Color(0xFFEADDFF)
val md_onPrimaryContainer = Color(0xFF21005D)
val md_secondary = Color(0xFF625B71)
val md_onSecondary = Color(0xFFFFFFFF)
val md_secondaryContainer = Color(0xFFE8DEF8)
val md_onSecondaryContainer = Color(0xFF1D192B)
val md_tertiary = Color(0xFF7D5260)
val md_onTertiary = Color(0xFFFFFFFF)
val md_tertiaryContainer = Color(0xFFFFD8E4)
val md_onTertiaryContainer = Color(0xFF31111D)
val md_error = Color(0xFFB3261E)
val md_onError = Color(0xFFFFFFFF)
val md_errorContainer = Color(0xFFF9DEDC)
val md_onErrorContainer = Color(0xFF410E0B)
val md_background = Color(0xFFFEF7FF)
val md_onBackground = Color(0xFF1D1B20)
val md_surface = Color(0xFFFEF7FF)
val md_onSurface = Color(0xFF1D1B20)
val md_surfaceVariant = Color(0xFFE7E0EC)
// M3 baseline muted gray for labels & secondary text — matches the design mockups/handoff.
val md_onSurfaceVariant = Color(0xFF49454F)
val md_outline = Color(0xFF79747E)
val md_outlineVariant = Color(0xFFCAC4D0)
val md_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_surfaceContainerLow = Color(0xFFF7F2FA)
val md_surfaceContainer = Color(0xFFF3EDF7)
val md_surfaceContainerHigh = Color(0xFFECE6F0)
val md_surfaceContainerHighest = Color(0xFFE6E0E9)
val md_inverseSurface = Color(0xFF322F35)
val md_inverseOnSurface = Color(0xFFF5EFF7)
val md_inversePrimary = Color(0xFFD0BCFF)
val md_scrim = Color(0xFF000000)

// Dark
val md_primary_dark = Color(0xFFD0BCFF)
val md_onPrimary_dark = Color(0xFF381E72)
val md_primaryContainer_dark = Color(0xFF4F378B)
val md_onPrimaryContainer_dark = Color(0xFFEADDFF)
val md_secondary_dark = Color(0xFFCCC2DC)
val md_onSecondary_dark = Color(0xFF332D41)
val md_secondaryContainer_dark = Color(0xFF4A4458)
val md_onSecondaryContainer_dark = Color(0xFFE8DEF8)
val md_tertiary_dark = Color(0xFFEFB8C8)
val md_onTertiary_dark = Color(0xFF492532)
val md_tertiaryContainer_dark = Color(0xFF633B48)
val md_onTertiaryContainer_dark = Color(0xFFFFD8E4)
val md_error_dark = Color(0xFFF2B8B5)
val md_onError_dark = Color(0xFF601410)
val md_errorContainer_dark = Color(0xFF8C1D18)
val md_onErrorContainer_dark = Color(0xFFF9DEDC)
val md_background_dark = Color(0xFF141218)
val md_onBackground_dark = Color(0xFFE6E0E9)
val md_surface_dark = Color(0xFF141218)
val md_onSurface_dark = Color(0xFFE6E0E9)
val md_surfaceVariant_dark = Color(0xFF49454F)
val md_onSurfaceVariant_dark = Color(0xFFCAC4D0)
val md_outline_dark = Color(0xFF938F99)
val md_outlineVariant_dark = Color(0xFF49454F)
val md_surfaceContainerLowest_dark = Color(0xFF0F0D13)
val md_surfaceContainerLow_dark = Color(0xFF1D1B20)
val md_surfaceContainer_dark = Color(0xFF211F26)
val md_surfaceContainerHigh_dark = Color(0xFF2B2930)
val md_surfaceContainerHighest_dark = Color(0xFF36343B)
val md_inverseSurface_dark = Color(0xFFE6E0E9)
val md_inverseOnSurface_dark = Color(0xFF322F35)
val md_inversePrimary_dark = Color(0xFF6650A4)

// ── Budget status (traffic-light). Same hue intent in both schemes; dark = lighter. ──
val BudgetGood = Color(0xFF2E7D32);  val BudgetGoodDark = Color(0xFF7DD487)
val BudgetWarn = Color(0xFFF9A825);  val BudgetWarnDark = Color(0xFFFBC02D)
val BudgetBad  = Color(0xFFD32F2F);  val BudgetBadDark  = Color(0xFFEF5350)

// ── Premium accent seeds (light / dark primary). Build alternate ColorSchemes from these. ──
// Sage   #3E5E41 / #A8C6AA
// Ocean  #1C5C6E / #8FC8D8
// Plum   #6A2E78 / #CFA6D6

// ── Theme-aware accessors for the budget status (traffic-light) colors ────────
// Resolve to the lighter dark-mode variant when the active scheme is dark. Darkness is read from
// the scheme itself (not isSystemInDarkTheme) so a manual Light/Dark theme override still picks the
// correct variant.

@Composable
@ReadOnlyComposable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
@ReadOnlyComposable
fun budgetGoodColor(): Color = if (isDarkTheme()) BudgetGoodDark else BudgetGood

@Composable
@ReadOnlyComposable
fun budgetWarnColor(): Color = if (isDarkTheme()) BudgetWarnDark else BudgetWarn

@Composable
@ReadOnlyComposable
fun budgetBadColor(): Color = if (isDarkTheme()) BudgetBadDark else BudgetBad
