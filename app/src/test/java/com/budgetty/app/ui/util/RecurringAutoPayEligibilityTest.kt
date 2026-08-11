package com.budgetty.app.ui.util

import com.budgetty.app.data.local.RecurringEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/**
 * Guards the autopay-eligibility rule shared by persistence (BudgetViewModel.saveRecurring) and
 * display (the Budget MoneyRow): autopay is monthly/weekly bills only. This is what stops a bill
 * switched to yearly/one-off from keeping a stuck, unclearable "Auto" chip. Pure logic, no Android.
 */
class RecurringAutoPayEligibilityTest {

    private fun entry(cadence: String, autoPay: Boolean, isIncome: Boolean = false) =
        RecurringEntity(
            label = "x",
            amount = BigDecimal.TEN,
            isIncome = isIncome,
            cadence = cadence,
            autoPay = autoPay,
        )

    @Test
    fun `monthly and weekly bills are autopay-eligible`() {
        assertThat(entry(RecurringEntity.Cadence.MONTHLY, autoPay = false).autoPayEligible()).isTrue()
        assertThat(entry(RecurringEntity.Cadence.WEEKLY, autoPay = false).autoPayEligible()).isTrue()
    }

    @Test
    fun `yearly, one-off, and income are not autopay-eligible`() {
        assertThat(entry(RecurringEntity.Cadence.YEARLY, autoPay = false).autoPayEligible()).isFalse()
        assertThat(entry(RecurringEntity.Cadence.ONCE, autoPay = false).autoPayEligible()).isFalse()
        assertThat(entry(RecurringEntity.Cadence.MONTHLY, autoPay = false, isIncome = true).autoPayEligible())
            .isFalse()
    }

    @Test
    fun `isAutoPayActive requires the switch on AND an eligible cadence`() {
        assertThat(entry(RecurringEntity.Cadence.MONTHLY, autoPay = true).isAutoPayActive()).isTrue()
        assertThat(entry(RecurringEntity.Cadence.WEEKLY, autoPay = true).isAutoPayActive()).isTrue()
        assertThat(entry(RecurringEntity.Cadence.MONTHLY, autoPay = false).isAutoPayActive()).isFalse()
        // The bug: a yearly/one-off bill that still carries autoPay=true must NOT read as auto-managed
        // (otherwise its row shows a permanent "Auto" chip with no way to mark it paid).
        assertThat(entry(RecurringEntity.Cadence.YEARLY, autoPay = true).isAutoPayActive()).isFalse()
        assertThat(entry(RecurringEntity.Cadence.ONCE, autoPay = true).isAutoPayActive()).isFalse()
    }
}
