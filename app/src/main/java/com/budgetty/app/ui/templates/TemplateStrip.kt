package com.budgetty.app.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.TemplateEntity
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel

/**
 * The templates row at the top of the Add sheet: a horizontally-scrolling strip of saved-regular chips
 * (tap to log one in a pre-filled review) plus a "＋ New", with a "Manage" link in the header. When
 * nothing is saved yet it shows a one-line invite instead. Self-contained — it reads its own
 * [TemplatesViewModel], so the host only supplies the two navigation callbacks.
 */
@Composable
fun AddSheetTemplateStrip(
    onPickTemplate: (Long) -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = koinViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    if (templates.isEmpty()) {
        TemplateInvite(onClick = onOpenTemplates, modifier = modifier)
        return
    }
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = MaterialTheme.dimens.xs),
        ) {
            Text(
                text = stringResource(R.string.templates_section),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.templates_manage),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onOpenTemplates),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        ) {
            templates.forEach { template ->
                TemplateChip(template = template, onClick = { onPickTemplate(template.id) })
            }
            NewChip(onClick = onOpenTemplates)
        }
    }
}

@Composable
private fun TemplateChip(template: TemplateEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(
                start = MaterialTheme.dimens.sm,
                end = MaterialTheme.dimens.md,
                top = MaterialTheme.dimens.sm,
                bottom = MaterialTheme.dimens.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(template.emoji.ifBlank { "🧾" }, fontSize = 16.sp)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Column {
            Text(
                text = template.name.ifBlank { stringResource(R.string.templates_untitled) },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = if (template.askAmount) {
                    stringResource(R.string.templates_ask_amount_short)
                } else {
                    template.amount.formatMoney()
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NewChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.xs))
        Text(
            text = stringResource(R.string.templates_new_short),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TemplateInvite(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(MaterialTheme.dimens.md),
    ) {
        Text(
            text = stringResource(R.string.templates_invite_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.templates_invite_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
