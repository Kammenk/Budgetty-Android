package com.budgetty.app.ui.csvimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.csvimport.CsvField
import com.budgetty.app.data.csvimport.CsvImport
import com.budgetty.app.data.csvimport.CsvTable
import com.budgetty.app.data.csvimport.ParsedImportRow
import com.budgetty.app.data.csvimport.RowKind
import com.budgetty.app.data.csvimport.SignConvention
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONO = FontFamily.Monospace
private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val fullDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

private fun Long.asShortDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(shortDate)

private fun Long.asFullDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(fullDate)

@Composable
fun ImportCsvScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ImportCsvViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = uri.fileName(context)
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            viewModel.onFileRead(name, text)
        }
    }
    ImportCsvContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToHistory = onNavigateToHistory,
        onPick = { picker.launch(CSV_MIME_TYPES) },
        onClearFile = viewModel::clearFile,
        onGoToMap = viewModel::goToMap,
        onCycleColumn = viewModel::cycleColumn,
        onGoToReview = viewModel::goToReview,
        onBack = viewModel::back,
        onSetDateFormat = viewModel::setDateFormat,
        onSetSign = viewModel::setSign,
        onSetSkipDuplicates = viewModel::setSkipDuplicates,
        onImport = viewModel::import,
        onUndo = viewModel::undo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList") // A stateless wizard body that hoists every import callback.
@Composable
internal fun ImportCsvContent(
    state: ImportUiState,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onPick: () -> Unit,
    onClearFile: () -> Unit,
    onGoToMap: () -> Unit,
    onCycleColumn: (Int) -> Unit,
    onGoToReview: () -> Unit,
    onBack: () -> Unit,
    onSetDateFormat: (ImportDateFormat) -> Unit,
    onSetSign: (SignConvention) -> Unit,
    onSetSkipDuplicates: (Boolean) -> Unit,
    onImport: () -> Unit,
    onUndo: () -> Unit,
) {
    val stepNumber = state.step.ordinal + 1
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_csv_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (state.step == ImportStep.PICK) onNavigateBack() else onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (state.step != ImportStep.DONE) {
                        Text(
                            text = stringResource(R.string.import_step, stepNumber),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = MaterialTheme.dimens.lg),
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.step != ImportStep.DONE) StepProgress(stepNumber)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                when (state.step) {
                    ImportStep.PICK -> PickStep(state, onPick, onClearFile)
                    ImportStep.MAP -> MapStep(state, onCycleColumn)
                    ImportStep.REVIEW -> ReviewStep(state, onSetDateFormat, onSetSign, onSetSkipDuplicates)
                    ImportStep.DONE -> DoneStep(state, onUndo)
                }
            }
            BottomCta(state, onGoToMap, onGoToReview, onImport, onNavigateToHistory)
        }
    }
}

@Composable
private fun StepProgress(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.screenPadding, vertical = MaterialTheme.dimens.sm),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
    ) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (i < step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            )
        }
    }
}

@Composable
private fun PickStep(state: ImportUiState, onPick: () -> Unit, onClearFile: () -> Unit) {
    StepHeading(stringResource(R.string.import_choose_file))
    state.error?.let { ErrorNote(it) }
    if (state.hasFile) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.dimens.lg)) {
                Text(state.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(
                        R.string.import_file_summary,
                        state.table.rows.size,
                        state.table.headers.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(
                    text = state.table.headers.joinToString(","),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MONO),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OutlinedButton(
            onClick = onClearFile,
            modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(stringResource(R.string.import_choose_another))
        }
    } else {
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight)) {
            Text(stringResource(R.string.import_choose_file))
        }
    }
    Text(
        text = stringResource(R.string.import_pick_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MapStep(state: ImportUiState, onCycleColumn: (Int) -> Unit) {
    StepHeading(stringResource(R.string.import_map_title))
    Text(
        text = stringResource(R.string.import_map_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
    ) {
        Column {
            state.table.headers.forEachIndexed { index, header ->
                val sample = state.table.rows.firstOrNull()?.getOrNull(index).orEmpty()
                ColumnMapRow(
                    header = header,
                    sample = sample,
                    field = state.mapping.getOrElse(index) { CsvField.IGNORE },
                ) { onCycleColumn(index) }
            }
        }
    }
    state.mappingError?.let { MappingWarning(it) }

    val preview = remember(state.table, state.mapping, state.dateFormat, state.sign) {
        state.table.rows.take(3).map {
            CsvImport.parseRow(it, state.mapping, state.dateFormat.pattern, state.sign)
        }
    }
    if (preview.isNotEmpty()) {
        Text(
            text = stringResource(R.string.import_preview_count, preview.size, state.table.rows.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        ) {
            Column(modifier = Modifier.padding(vertical = MaterialTheme.dimens.xs)) {
                preview.forEach { PreviewRow(it) }
            }
        }
    }
}

@Composable
private fun ColumnMapRow(header: String, sample: String, field: CsvField, onCycle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = header,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MONO),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (sample.isNotBlank()) {
                Text(
                    text = sample,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MONO),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val active = field != CsvField.IGNORE
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (active) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                .clickable(onClick = onCycle)
                .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.xs),
        ) {
            Text(
                text = stringResource(field.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewRow(p: ParsedImportRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Text(
            text = p.dateMillis?.asShortDate() ?: "?",
            style = MaterialTheme.typography.bodySmall,
            color = if (p.dateMillis == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = p.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val amountText = when (p.kind) {
            RowKind.EXPENSE -> "−" + (p.amount?.formatMoney() ?: "")
            RowKind.INCOME -> "+" + (p.amount?.formatMoney() ?: "")
            RowKind.INVALID -> "?"
        }
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when (p.kind) {
                RowKind.INCOME -> budgetGoodColor()
                RowKind.INVALID -> MaterialTheme.colorScheme.error
                RowKind.EXPENSE -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun ReviewStep(
    state: ImportUiState,
    onSetDateFormat: (ImportDateFormat) -> Unit,
    onSetSign: (SignConvention) -> Unit,
    onSetSkipDuplicates: (Boolean) -> Unit,
) {
    StepHeading(stringResource(R.string.import_review_title))
    OptionCard(stringResource(R.string.import_date_format)) {
        SegmentedToggle(
            options = ImportDateFormat.entries.map { stringResource(it.labelRes) },
            selectedIndex = state.dateFormat.ordinal,
            onSelect = { onSetDateFormat(ImportDateFormat.entries[it]) },
        )
    }
    OptionCard(stringResource(R.string.import_amounts)) {
        SegmentedToggle(
            options = listOf(
                stringResource(R.string.import_sign_minus_expense),
                stringResource(R.string.import_sign_minus_income),
            ),
            selectedIndex = if (state.sign == SignConvention.MINUS_IS_EXPENSE) 0 else 1,
            onSelect = { onSetSign(if (it == 0) SignConvention.MINUS_IS_EXPENSE else SignConvention.MINUS_IS_INCOME) },
        )
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSetSkipDuplicates(!state.skipDuplicates) }
                .padding(MaterialTheme.dimens.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.import_skip_dupes_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.import_skip_dupes_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.skipDuplicates, onCheckedChange = onSetSkipDuplicates)
        }
    }
    if (state.planning) {
        Box(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.lg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
    } else {
        state.plan?.let { PlanCounts(it) }
    }
}

@Composable
private fun PlanCounts(plan: ImportPlan) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
    ) {
        Column(modifier = Modifier.padding(vertical = MaterialTheme.dimens.xs)) {
            CountRow(stringResource(R.string.import_ready), plan.toImport.size)
            if (plan.duplicates > 0) CountRow(stringResource(R.string.import_dupes_skipped), plan.duplicates)
            if (plan.income > 0) CountRow(stringResource(R.string.import_income_skipped), plan.income)
            if (plan.invalid > 0) CountRow(stringResource(R.string.import_invalid_skipped), plan.invalid)
            if (plan.filedAsOther > 0) CountRow(stringResource(R.string.import_filed_other), plan.filedAsOther)
        }
    }
    if (plan.income > 0) {
        Text(
            text = stringResource(R.string.import_income_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DoneStep(state: ImportUiState, onUndo: () -> Unit) {
    val result = state.result ?: return
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(50))
            .background(budgetGoodColor().copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", style = MaterialTheme.typography.headlineSmall, color = budgetGoodColor())
    }
    Text(
        text = stringResource(R.string.import_done_title, result.imported),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    val skipped = result.skippedDuplicates + result.income + result.invalid
    if (skipped > 0) {
        Text(
            text = stringResource(R.string.import_done_sub, skipped),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
    ) {
        Column(modifier = Modifier.padding(vertical = MaterialTheme.dimens.xs)) {
            if (result.minDate != null && result.maxDate != null) {
                SummaryRow(
                    stringResource(R.string.import_summary_range),
                    "${result.minDate.asFullDate()} – ${result.maxDate.asFullDate()}",
                )
            }
            SummaryRow(
                stringResource(R.string.import_summary_expenses),
                "${result.imported} · ${result.expenseTotal.formatMoney()}",
            )
            if (result.income > 0) SummaryRow(stringResource(R.string.import_summary_income), result.income.toString())
            if (result.filedAsOther > 0) {
                SummaryRow(stringResource(R.string.import_summary_other), result.filedAsOther.toString())
            }
        }
    }
    Text(
        text = stringResource(R.string.import_done_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onUndo, modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight)) {
        Text(stringResource(R.string.import_undo))
    }
}

@Composable
private fun BottomCta(
    state: ImportUiState,
    onGoToMap: () -> Unit,
    onGoToReview: () -> Unit,
    onImport: () -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    val config = when (state.step) {
        ImportStep.PICK ->
            if (state.hasFile) CtaConfig(stringResource(R.string.import_cta_map), true, onGoToMap) else null
        ImportStep.MAP ->
            CtaConfig(stringResource(R.string.import_cta_review), state.mappingError == null, onGoToReview)
        ImportStep.REVIEW -> {
            val count = state.plan?.toImport?.size ?: 0
            CtaConfig(stringResource(R.string.import_cta_import, count), count > 0 && !state.importing, onImport)
        }
        ImportStep.DONE -> CtaConfig(stringResource(R.string.import_view_history), true, onNavigateToHistory)
    } ?: return
    Button(
        onClick = config.onClick,
        enabled = config.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.dimens.screenPadding)
            .height(MaterialTheme.dimens.buttonHeight),
    ) {
        Text(config.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class CtaConfig(val label: String, val enabled: Boolean, val onClick: () -> Unit)

@Composable
private fun StepHeading(text: String) {
    Spacer(Modifier.height(MaterialTheme.dimens.xs))
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun OptionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun CountRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MappingWarning(error: CsvImport.MappingError) {
    // Resolve the field labels up front — stringResource can't be called inside the joinToString lambda.
    val labels = mapOf(
        CsvField.DATE to stringResource(R.string.import_field_date),
        CsvField.AMOUNT to stringResource(R.string.import_field_amount),
        CsvField.STORE to stringResource(R.string.import_field_store),
        CsvField.CATEGORY to stringResource(R.string.import_field_category),
    )
    val text = when (error) {
        is CsvImport.MappingError.Missing ->
            stringResource(
                R.string.import_map_missing,
                error.fields.joinToString(" & ") { labels[it].orEmpty() },
            )
        is CsvImport.MappingError.Duplicated ->
            stringResource(R.string.import_map_duplicate, labels[error.field].orEmpty())
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs),
    )
}

@Composable
private fun ErrorNote(error: ImportError) {
    val res = when (error) {
        ImportError.READ_FAILED -> R.string.import_error_read
        ImportError.EMPTY_FILE -> R.string.import_error_empty
    }
    Text(stringResource(res), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

private fun CsvField.labelRes(): Int = when (this) {
    CsvField.DATE -> R.string.import_field_date
    CsvField.AMOUNT -> R.string.import_field_amount
    CsvField.STORE -> R.string.import_field_store
    CsvField.CATEGORY -> R.string.import_field_category
    CsvField.IGNORE -> R.string.import_field_ignore
}

private fun Uri.fileName(context: Context): String {
    var name = "import.csv"
    runCatching {
        context.contentResolver.query(this, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx)?.let { name = it }
        }
    }
    return name
}

private val CSV_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/octet-stream",
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ImportCsvMapPreview() {
    val table = CsvTable(
        headers = listOf("Booking date", "Payee", "Amount", "Category"),
        rows = listOf(
            listOf("12/10/2026", "LIDL SAGT DANKE", "-42.80", "Groceries"),
            listOf("11/10/2026", "SPOTIFY", "-10.99", "Subscriptions"),
            listOf("10/10/2026", "ACME GMBH", "2450.00", "Income"),
        ),
    )
    BudgettyTheme {
        ImportCsvContent(
            state = ImportUiState(
                step = ImportStep.MAP,
                fileName = "statement_oct.csv",
                table = table,
                mapping = listOf(CsvField.DATE, CsvField.STORE, CsvField.AMOUNT, CsvField.CATEGORY),
            ),
            onNavigateBack = {}, onNavigateToHistory = {}, onPick = {}, onClearFile = {}, onGoToMap = {},
            onCycleColumn = {}, onGoToReview = {}, onBack = {}, onSetDateFormat = {}, onSetSign = {},
            onSetSkipDuplicates = {}, onImport = {}, onUndo = {},
        )
    }
}
