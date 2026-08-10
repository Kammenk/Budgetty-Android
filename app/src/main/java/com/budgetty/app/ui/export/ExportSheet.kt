package com.budgetty.app.ui.export

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.export.ExportBuilder
import com.budgetty.app.data.export.ExportData
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.home.DateRangeFilter
import com.budgetty.app.ui.savings.PrimaryPill
import com.budgetty.app.ui.savings.SavingsSheetLabel
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The Export options sheet: pick CSV or PDF and a period, then hand the file to the share sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(onDismiss: () -> Unit, viewModel: ExportViewModel = koinViewModel()) {
    val source by viewModel.source.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    var isPdf by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(DateRangeFilter.CURRENT_MONTH) }
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val range = remember(source, filter) { filter.toRange(monthStartDay = source.monthStartDay) }
    val core = remember(source, filter) { ExportBuilder.buildCore(source, range.first, range.second) }
    val allTime = stringResource(R.string.period_all_time)
    val periodLabel = if (filter == DateRangeFilter.ALL_TIME) allTime
    else ExportBuilder.periodLabel(range.first, range.second)

    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())) }
    val generatedLabel = stringResource(R.string.export_generated, today, source.currencySymbol, core.receiptCount)
    val totalRowLabel = stringResource(R.string.export_total_row, periodLabel)
    val shareTitle = stringResource(R.string.export_share_title)

    fun export() {
        val data = ExportData(
            periodLabel = periodLabel,
            generatedLabel = generatedLabel,
            currencySymbol = source.currencySymbol,
            rows = core.rows,
            totalSpent = core.totalSpent,
            income = core.income,
            net = core.net,
            byCategory = core.byCategory,
            totalRowLabel = totalRowLabel,
        )
        val safe = "Budgetty " + periodLabel.replace(Regex("[^\\p{L}\\p{N} \\-]"), "").trim()
        // The VM renders/writes the file off the main thread, then hands it back here (on the main
        // thread) to become a share Intent — startActivity + FileProvider need the Context.
        viewModel.export(
            data = data,
            isPdf = isPdf,
            fileBaseName = safe,
            cacheDir = context.cacheDir,
            onReady = { file, mime ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, shareTitle))
                onDismiss()
            },
        )
    }

    AdaptiveSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.dimens.xl)) {
            Text(stringResource(R.string.export_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.export_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = MaterialTheme.dimens.xs),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SavingsSheetLabel(stringResource(R.string.export_format))
            Spacer(Modifier.height(6.dp))
            SegmentedToggle(
                options = listOf(stringResource(R.string.export_csv), stringResource(R.string.export_pdf)),
                selectedIndex = if (isPdf) 1 else 0,
                onSelect = { isPdf = it == 1 },
            )
            Text(
                stringResource(if (isPdf) R.string.export_pdf_help else R.string.export_csv_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 7.dp),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SavingsSheetLabel(stringResource(R.string.export_period))
            Spacer(Modifier.height(6.dp))
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable { menuOpen = true }
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(MaterialTheme.dimens.sm))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(filter.labelRes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(periodLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DateRangeFilter.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(stringResource(f.labelRes)) },
                            onClick = { filter = f; menuOpen = false },
                            trailingIcon = if (f == filter) {
                                { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                        )
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            if (core.isEmpty) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(13.dp),
                ) {
                    Text(stringResource(R.string.export_empty_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.export_empty_body), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        pluralStringResource(R.plurals.export_summary, core.receiptCount, core.receiptCount, core.totalSpent.formatMoney()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            PrimaryPill(
                text = stringResource(if (isPdf) R.string.export_do_pdf else R.string.export_do_csv),
                onClick = { if (!core.isEmpty && !exporting) export() },
                fillWidth = true,
            )
            Text(
                stringResource(R.string.export_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.sm),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
