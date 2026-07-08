package com.budgetty.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use the Material 3 default type scale (Roboto). Budgetty leans on a few roles:
//   • displaySmall  → big headline money totals (e.g. "Total spent this month")
//   • titleLarge / titleMedium (semibold) → section & card headers
//   • bodyMedium    → row primary text
//   • bodySmall + onSurfaceVariant → labels / secondary lines
// Start from the M3 baseline and only override weights where the design is bolder.
val Typography = Typography(
    displaySmall = Typography().displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

// Money figure helper style — apply currency formatting via NumberFormat with the
// user's symbol, render the amount with this and the symbol slightly smaller.
val MoneyTotal = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-1).sp)
