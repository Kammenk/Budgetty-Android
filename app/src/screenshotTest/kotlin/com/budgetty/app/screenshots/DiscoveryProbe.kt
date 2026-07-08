package com.budgetty.app.screenshots

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// TEMPORARY discovery probe — delete after diagnosing screenshot preview detection.
@Preview(name = "probe-direct", showBackground = true, widthDp = 200, heightDp = 100)
@Composable
fun ProbeDirect() {
    Text("probe-direct")
}

@BudgettyScreens
@Composable
fun ProbeMulti() {
    Text("probe-multi")
}
