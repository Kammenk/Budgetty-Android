package com.budgetty.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Budgetty corner system (from the design system):
//   cards = 20dp · list rows / fields = 16dp · store tiles & chips = 12dp
//   buttons = fully rounded (the Material 3 default pill). Do NOT set an explicit `shape` on
//   Button / OutlinedButton / TextButton — leaving it unset keeps every button consistent.
val BudgettyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp), // bottom sheets
)
