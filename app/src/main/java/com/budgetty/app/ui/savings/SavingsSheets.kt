package com.budgetty.app.ui.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** Create (initial == null) or edit a savings goal. [onDelete] is non-null only when editing. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SavingsGoalEditSheet(
    initial: SavingsGoalEntity?,
    onSave: (name: String, emoji: String, target: BigDecimal, targetDate: Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: SAVINGS_EMOJIS.first()) }
    var target by remember { mutableStateOf(initial?.targetAmount?.toPlainString() ?: "") }
    var hasDate by remember { mutableStateOf(initial?.targetDate != null) }
    var dateMillis by remember { mutableStateOf(initial?.targetDate) }
    var showPicker by remember { mutableStateOf(false) }
    val parsedTarget = target.toSavingsAmount()
    val valid = name.isNotBlank() && parsedTarget != null

    AdaptiveSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        if (initial == null) R.string.savings_new_goal_title else R.string.savings_edit_goal_title,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.savings_delete_goal),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))

            SavingsSheetLabel(stringResource(R.string.savings_field_emoji))
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SAVINGS_EMOJIS.forEach { e ->
                    val selected = e == emoji
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLowest,
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SavingsSheetLabel(stringResource(R.string.savings_field_name))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.savings_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = SavingsFieldShape,
                colors = savingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SavingsSheetLabel(stringResource(R.string.savings_field_target))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = target,
                onValueChange = { input -> target = input.filter { it.isDigit() || it == '.' || it == ',' } },
                placeholder = { Text("0") },
                suffix = { Text(AppFormats.currencySymbol) },
                singleLine = true,
                shape = SavingsFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = savingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.savings_target_date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.savings_target_date_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = hasDate,
                    onCheckedChange = {
                        hasDate = it
                        if (it && dateMillis == null) showPicker = true
                    },
                )
            }
            if (hasDate) {
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SavingsFieldShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { showPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = dateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().formatDayMonth()
                        } ?: stringResource(R.string.savings_pick_date),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (initial == null) {
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(
                    text = stringResource(R.string.savings_create_free_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Button(
                onClick = { onSave(name.trim(), emoji, parsedTarget!!, dateMillis.takeIf { hasDate }) },
                enabled = valid,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = stringResource(
                        if (initial == null) R.string.savings_create else R.string.action_save,
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateMillis = state.selectedDateMillis
                    if (dateMillis == null) hasDate = false
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

/** Add-to-savings ([isWithdraw] = false) or Withdraw sheet — the same form, verb and confirm apart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsContributionSheet(
    goalName: String,
    goalEmoji: String,
    saved: BigDecimal,
    target: BigDecimal,
    isWithdraw: Boolean,
    onConfirm: (amount: BigDecimal, note: String, dateMillis: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showPicker by remember { mutableStateOf(false) }
    val parsed = amount.toSavingsAmount()
    val signed = parsed?.let { if (isWithdraw) it.negate() else it }
    val newSaved = saved + (signed ?: BigDecimal.ZERO)
    val pct = if (target.signum() > 0) (newSaved.toDouble() / target.toDouble() * 100).roundToInt() else 0

    AdaptiveSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.xl),
        ) {
            Text(
                text = stringResource(if (isWithdraw) R.string.savings_withdraw_title else R.string.savings_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiChip(goalEmoji, size = 20.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$goalName · ${saved.formatMoney()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SavingsSheetLabel(stringResource(R.string.savings_field_amount))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = amount,
                onValueChange = { input -> amount = input.filter { it.isDigit() || it == '.' || it == ',' } },
                placeholder = { Text("0") },
                suffix = { Text(AppFormats.currencySymbol) },
                singleLine = true,
                shape = SavingsFieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = savingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SavingsSheetLabel(stringResource(R.string.savings_field_note))
            Spacer(Modifier.height(6.dp))
            TextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(stringResource(R.string.savings_note_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = SavingsFieldShape,
                colors = savingsFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SavingsSheetLabel(stringResource(R.string.savings_field_date))
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SavingsFieldShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .clickable { showPicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().formatDayMonth(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (parsed != null) {
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(
                    text = stringResource(R.string.savings_new_total, newSaved.formatMoney(), target.formatMoney(), pct),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isWithdraw) {
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(R.string.savings_withdraw_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Button(
                onClick = { onConfirm(signed!!, note.trim(), dateMillis) },
                enabled = parsed != null,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = stringResource(
                        if (isWithdraw) R.string.savings_confirm_withdraw else R.string.savings_confirm_add,
                        (parsed ?: BigDecimal.ZERO).formatMoney(),
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = it }
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}
