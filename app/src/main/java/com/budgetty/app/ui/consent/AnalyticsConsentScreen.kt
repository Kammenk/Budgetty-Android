package com.budgetty.app.ui.consent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import com.budgetty.app.ui.util.isExpandedWidth

/**
 * First-run telemetry consent screen. Gated after onboarding and before auth (see
 * [com.budgetty.app.ui.navigation.BudgettyApp]): shown once, until the user decides. Both toggles
 * start OFF and stay off unless the user turns them on — the initial position is deliberately local
 * state, never seeded from stored settings, so the screen can never present a pre-ticked opt-in.
 *
 * [onContinue] applies whatever the two switches are set to; [onNotNow] is the express "don't share"
 * path (both off) regardless of the switches. [onPrivacyPolicy] defaults to opening the app's
 * privacy policy (mirroring the Account screen's row); callers may override it.
 */
@Composable
fun AnalyticsConsentScreen(
    onContinue: (analyticsEnabled: Boolean, crashReportingEnabled: Boolean) -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    onPrivacyPolicy: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    AnalyticsConsentContent(
        onContinue = onContinue,
        onNotNow = onNotNow,
        onPrivacyPolicy = onPrivacyPolicy ?: { openPrivacyPolicy(context) },
        modifier = modifier,
    )
}

@Composable
private fun AnalyticsConsentContent(
    onContinue: (Boolean, Boolean) -> Unit,
    onNotNow: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local, never seeded from stored settings: the screen must never show a pre-ticked opt-in.
    // Hoisted here so the phone and tablet layouts share the exact same state, defaults and contract;
    // only the presentation differs below.
    var analyticsOn by remember { mutableStateOf(false) }
    var crashOn by remember { mutableStateOf(false) }
    var detailExpanded by remember { mutableStateOf(false) }

    if (isExpandedWidth()) {
        TabletConsentLayout(
            analyticsOn = analyticsOn,
            onAnalyticsChange = { analyticsOn = it },
            crashOn = crashOn,
            onCrashChange = { crashOn = it },
            detailExpanded = detailExpanded,
            onDetailToggle = { detailExpanded = !detailExpanded },
            onContinue = { onContinue(analyticsOn, crashOn) },
            onNotNow = onNotNow,
            onPrivacyPolicy = onPrivacyPolicy,
            modifier = modifier,
        )
    } else {
        PhoneConsentLayout(
            analyticsOn = analyticsOn,
            onAnalyticsChange = { analyticsOn = it },
            crashOn = crashOn,
            onCrashChange = { crashOn = it },
            detailExpanded = detailExpanded,
            onDetailToggle = { detailExpanded = !detailExpanded },
            onContinue = { onContinue(analyticsOn, crashOn) },
            onNotNow = onNotNow,
            onPrivacyPolicy = onPrivacyPolicy,
            modifier = modifier,
        )
    }
}

/**
 * Phone / compact-width presentation: a centered single column that scrolls above a fixed footer.
 * Unchanged from the original screen; only the state is now hoisted into [AnalyticsConsentContent].
 */
@Composable
private fun PhoneConsentLayout(
    analyticsOn: Boolean,
    onAnalyticsChange: (Boolean) -> Unit,
    crashOn: Boolean,
    onCrashChange: (Boolean) -> Unit,
    detailExpanded: Boolean,
    onDetailToggle: () -> Unit,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Scrolling body: everything above the footer scrolls on short screens.
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = SinglePaneMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(MaterialTheme.dimens.xxl))
            ConsentHeader()
            Spacer(Modifier.height(MaterialTheme.dimens.xxl))
            ConsentToggleCard(
                icon = Icons.Filled.BarChart,
                title = stringResource(R.string.consent_analytics_label),
                subtitle = stringResource(R.string.consent_analytics_desc),
                checked = analyticsOn,
                onCheckedChange = onAnalyticsChange,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            ConsentToggleCard(
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.consent_crash_label),
                subtitle = stringResource(R.string.consent_crash_desc),
                checked = crashOn,
                onCheckedChange = onCrashChange,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            CollectAndNeverCard(
                expanded = detailExpanded,
                onToggle = onDetailToggle,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
        }

        // Fixed footer: stays put while the body scrolls.
        Column(
            modifier = Modifier
                .widthIn(max = SinglePaneMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.xxl)
                .padding(top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.lg),
        ) {
            ConsentFooter(
                onContinue = onContinue,
                onNotNow = onNotNow,
                onPrivacyPolicy = onPrivacyPolicy,
            )
        }
    }
}

/**
 * Tablet / expanded-width presentation: the whole window is the background, with a single centered
 * elevated card (capped at [SinglePaneMaxWidth]). The card wraps its content and is vertically
 * centered; on a short tablet the card itself scrolls, so the actions need not be a pinned footer.
 * All state, defaults and the [onContinue]/[onNotNow]/[onPrivacyPolicy] contract are identical to the
 * phone layout — only the composition differs.
 */
@Composable
private fun TabletConsentLayout(
    analyticsOn: Boolean,
    onAnalyticsChange: (Boolean) -> Unit,
    crashOn: Boolean,
    onCrashChange: (Boolean) -> Unit,
    detailExpanded: Boolean,
    onDetailToggle: () -> Unit,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(MaterialTheme.dimens.radiusXxl)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(MaterialTheme.dimens.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = SinglePaneMaxWidth)
                .fillMaxWidth()
                .shadow(elevation = MaterialTheme.dimens.sm, shape = cardShape)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                // Wraps content and centers in the window; scrolls within the card if a short
                // tablet can't fit it, rather than pushing the actions off-screen.
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.dimens.xxl),
        ) {
            TabletConsentHeader()
            Spacer(Modifier.height(MaterialTheme.dimens.xxl))
            // Toggle cards sit ON the surfaceContainer card, so their surface is `background` for
            // contrast (mirrors the mockup); same icon/label/subline/Switch idiom as the phone.
            ConsentToggleCard(
                icon = Icons.Filled.BarChart,
                title = stringResource(R.string.consent_analytics_label),
                subtitle = stringResource(R.string.consent_analytics_desc),
                checked = analyticsOn,
                onCheckedChange = onAnalyticsChange,
                containerColor = MaterialTheme.colorScheme.background,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            ConsentToggleCard(
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.consent_crash_label),
                subtitle = stringResource(R.string.consent_crash_desc),
                checked = crashOn,
                onCheckedChange = onCrashChange,
                containerColor = MaterialTheme.colorScheme.background,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            CollectAndNeverCard(
                expanded = detailExpanded,
                onToggle = onDetailToggle,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            TabletConsentActions(
                onContinue = onContinue,
                onNotNow = onNotNow,
                onPrivacyPolicy = onPrivacyPolicy,
            )
        }
    }
}

/** Tablet header: brand tile on the LEFT, title + subtitle stacked to its right (not centered). */
@Composable
private fun TabletConsentHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.consent_title),
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = stringResource(R.string.consent_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Tablet actions: the full-width reassurance band with the Privacy Policy link inline at its right
 * edge, then a right-aligned action row — quiet **Not now** then filled **Continue** pill (the
 * tablet-dialog convention), instead of the phone's stacked full-width buttons.
 */
@Composable
private fun TabletConsentActions(
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    onPrivacyPolicy: () -> Unit,
) {
    ReassuranceBand(
        trailing = {
            TextButton(onClick = onPrivacyPolicy) {
                Text(
                    text = stringResource(R.string.account_privacy_policy),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
    Spacer(Modifier.height(MaterialTheme.dimens.lg))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onNotNow) {
            Text(
                text = stringResource(R.string.consent_not_now),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Button(
            onClick = onContinue,
            modifier = Modifier.height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(stringResource(R.string.consent_continue))
        }
    }
}

/** Centered brand tile + title + subtitle. */
@Composable
private fun ConsentHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.xl))
        Text(
            text = stringResource(R.string.consent_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.consent_subtitle),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 246.dp),
        )
    }
}

/**
 * One opt-in toggle card. The whole row is the control (the [Switch] itself takes
 * `onCheckedChange = null`), matching the Account screen's toggle rows. When on, the leading icon
 * tile lifts to the primary container to echo the switch state.
 */
@Composable
private fun ConsentToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    // Defaults to `surfaceContainer` (the phone treatment). The tablet passes `background` because
    // these cards sit ON the surfaceContainer card and need the extra contrast.
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(containerColor)
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                .background(
                    if (checked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Collapsed-by-default disclosure of exactly what is and isn't collected. Expands to a two-column
 * "Collected" / "Never" grid inside the same bordered container, with an honesty footnote.
 */
@Composable
private fun CollectAndNeverCard(expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .border(
                width = MaterialTheme.dimens.hairline,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.consent_detail_toggle),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "consentChevron")
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(
                    start = MaterialTheme.dimens.lg,
                    end = MaterialTheme.dimens.lg,
                    bottom = MaterialTheme.dimens.lg,
                ),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.lg)) {
                    DetailColumn(
                        header = stringResource(R.string.consent_collected_header),
                        headerColor = budgetGoodColor(),
                        glyph = Icons.Filled.Check,
                        items = listOf(
                            stringResource(R.string.consent_collected_1),
                            stringResource(R.string.consent_collected_2),
                            stringResource(R.string.consent_collected_3),
                            stringResource(R.string.consent_collected_4),
                            stringResource(R.string.consent_collected_5),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    DetailColumn(
                        header = stringResource(R.string.consent_never_header),
                        headerColor = MaterialTheme.colorScheme.error,
                        glyph = Icons.Filled.Close,
                        items = listOf(
                            stringResource(R.string.consent_never_1),
                            stringResource(R.string.consent_never_2),
                            stringResource(R.string.consent_never_3),
                            stringResource(R.string.consent_never_4),
                            stringResource(R.string.consent_never_5),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Text(
                    text = stringResource(R.string.consent_detail_footnote),
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One column of the disclosure grid: a colored header (glyph + uppercase label) over its items. */
@Composable
private fun DetailColumn(
    header: String,
    headerColor: Color,
    glyph: ImageVector,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = headerColor,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
            Spacer(Modifier.width(MaterialTheme.dimens.xs))
            Text(
                text = header.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = headerColor,
                letterSpacing = 0.6.sp,
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        items.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}

/**
 * The "no financial data" reassurance strip (secondaryContainer + shield), shared by both layouts.
 * On the phone it stands alone and the Privacy Policy link sits below it; on the tablet the caller
 * passes a [trailing] slot so the Privacy Policy link sits inline at the band's right edge. The
 * default (no trailing) reproduces the phone band exactly — the label keeps its intrinsic width.
 */
@Composable
private fun ReassuranceBand(
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(MaterialTheme.dimens.icon),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = stringResource(R.string.consent_reassurance),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = if (trailing != null) Modifier.weight(1f) else Modifier,
        )
        if (trailing != null) {
            Spacer(Modifier.width(MaterialTheme.dimens.md))
            trailing()
        }
    }
}

/** Reassurance band, privacy-policy link, and the Continue / Not-now actions. Does not scroll. */
@Composable
private fun ColumnScope.ConsentFooter(
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    onPrivacyPolicy: () -> Unit,
) {
    ReassuranceBand()
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    TextButton(
        onClick = onPrivacyPolicy,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        Text(
            text = stringResource(R.string.account_privacy_policy),
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(Modifier.height(MaterialTheme.dimens.xs))
    Button(
        onClick = onContinue,
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.buttonHeight),
    ) {
        Text(stringResource(R.string.consent_continue))
    }
    TextButton(
        onClick = onNotNow,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        Text(
            text = stringResource(R.string.consent_not_now),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Privacy policy destination. Mirrors AccountScreen's privacy-policy row (same public Budgetty site,
// which hosts the policy) so the two open the identical page; kept in sync by hand as the URL is not
// centralised yet.
private const val URL_PRIVACY = "https://budgetty-96a3d.web.app/"

// The only failure here is "no app can open a link" (no browser); the exception carries nothing more
// to act on, and the toast already tells the user. Same intentional swallow as AccountScreen.openUrl.
@Suppress("SwallowedException")
private fun openPrivacyPolicy(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_link_app), Toast.LENGTH_SHORT).show()
    }
}

@Preview(name = "Consent · light", showBackground = true, heightDp = 900)
@Composable
private fun AnalyticsConsentPreviewLight() {
    BudgettyTheme {
        AnalyticsConsentContent(onContinue = { _, _ -> }, onNotNow = {}, onPrivacyPolicy = {})
    }
}

@Preview(name = "Consent · dark", showBackground = true, heightDp = 900)
@Composable
private fun AnalyticsConsentPreviewDark() {
    BudgettyTheme(darkTheme = true) {
        AnalyticsConsentContent(onContinue = { _, _ -> }, onNotNow = {}, onPrivacyPolicy = {})
    }
}

@Preview(name = "Consent · tablet light", showBackground = true, widthDp = 900, heightDp = 800)
@Composable
private fun AnalyticsConsentTabletPreviewLight() {
    BudgettyTheme {
        AnalyticsConsentContent(onContinue = { _, _ -> }, onNotNow = {}, onPrivacyPolicy = {})
    }
}

@Preview(name = "Consent · tablet dark", showBackground = true, widthDp = 900, heightDp = 800)
@Composable
private fun AnalyticsConsentTabletPreviewDark() {
    BudgettyTheme(darkTheme = true) {
        AnalyticsConsentContent(onContinue = { _, _ -> }, onNotNow = {}, onPrivacyPolicy = {})
    }
}
