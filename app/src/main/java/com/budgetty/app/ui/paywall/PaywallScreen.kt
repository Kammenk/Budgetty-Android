package com.budgetty.app.ui.paywall

import com.budgetty.app.ui.theme.dimens
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.android.billingclient.api.ProductDetails
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.data.quota.ScanQuota
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.isWideWidth
import org.koin.androidx.compose.koinViewModel

@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = koinViewModel(),
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    PaywallScreenContent(
        products = products,
        isPremium = isPremium,
        isWide = isWideWidth(),
        onNavigateBack = onNavigateBack,
        onPurchase = { activity, productId -> viewModel.purchase(activity, productId) },
        onRestore = { viewModel.restore() },
        modifier = modifier,
    )
}

/** One line of the Premium feature list: what you get, and what the free plan gives instead. */
private data class PremiumBenefit(
    val title: String,
    val detail: String,
    /** True for a benefit that hasn't shipped yet: shown muted, with a clock instead of a check. */
    val soon: Boolean = false,
)

/**
 * Everything the subscription unlocks, listed once so the portrait and landscape layouts can't drift
 * apart. Each free-plan detail interpolates the constant that actually enforces the cap, so retuning
 * a limit can never leave the paywall quoting a stale number.
 */
@Composable
private fun premiumBenefits(): List<PremiumBenefit> = listOf(
    PremiumBenefit(
        title = stringResource(R.string.paywall_benefit_scans),
        detail = stringResource(R.string.paywall_benefit_scans_detail, ScanQuota.FREE_LIMIT),
    ),
    PremiumBenefit(
        title = stringResource(R.string.paywall_benefit_categories),
        detail = stringResource(R.string.paywall_benefit_categories_detail, Categories.FREE_CUSTOM_LIMIT),
    ),
    PremiumBenefit(
        title = stringResource(R.string.paywall_benefit_recurring),
        detail = stringResource(R.string.paywall_benefit_recurring_detail, RecurringRepository.FREE_RECURRING_LIMIT),
    ),
    PremiumBenefit(
        title = stringResource(R.string.paywall_benefit_themes),
        detail = stringResource(R.string.paywall_benefit_themes_detail),
    ),
    PremiumBenefit(
        title = stringResource(R.string.paywall_benefit_cloud),
        detail = stringResource(R.string.paywall_benefit_soon),
        soon = true,
    ),
)

@Composable
private fun PaywallScreenContent(
    products: List<ProductDetails>,
    isPremium: Boolean,
    isWide: Boolean,
    onNavigateBack: () -> Unit,
    onPurchase: (Activity, String) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findActivity()
    val benefits = premiumBenefits()

    // The hero spans behind the status bar; keep its icons readable over the gradient while shown,
    // then restore the app's normal status-bar appearance on exit.
    val view = LocalView.current
    val heroColor = MaterialTheme.colorScheme.primary
    DisposableEffect(view, heroColor) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = heroColor.luminance() > 0.5f
        onDispose {
            if (controller != null && previous != null) {
                controller.isAppearanceLightStatusBars = previous
            }
        }
    }

    // Plans / premium-status block, shared by both the portrait and landscape layouts.
    val planCards: @Composable () -> Unit = {
        when {
            isPremium -> StatusCard(
                icon = Icons.Filled.Check,
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.paywall_premium_title),
                subtitle = stringResource(R.string.paywall_premium_subtitle),
            )
            products.isEmpty() -> StatusCard(
                icon = Icons.Filled.Info,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(R.string.paywall_unavailable_title),
                subtitle = stringResource(R.string.paywall_unavailable_subtitle),
            )
            else -> products.forEach { product ->
                PlanCard(
                    product = product,
                    bestValue = product.isYearly(),
                    onSubscribe = { activity?.let { onPurchase(it, product.productId) } },
                )
                Spacer(Modifier.height(MaterialTheme.dimens.md))
            }
        }
    }

    if (isWide) {
        // Landscape: gradient brand + features on the left, plan cards on the right —
        // matching the TabletLs Paywall design's two-pane split.
        Row(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp, vertical = MaterialTheme.dimens.xl),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.width(MaterialTheme.dimens.xs))
                    Text(
                        text = stringResource(R.string.paywall_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                // The full benefit list is taller than a short landscape window (~410dp), so it
                // scrolls between the fixed header and Restore rather than squeezing them off-panel.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CrownIcon(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(MaterialTheme.dimens.lg))
                    Text(
                        text = stringResource(R.string.paywall_hero_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.paywall_hero_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(22.dp))
                    benefits.forEach { WhiteBenefit(it) }
                    Spacer(Modifier.height(MaterialTheme.dimens.lg))
                }
                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = stringResource(R.string.paywall_restore),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.dimens.xxxl, vertical = 28.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                planCards()
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                PaywallHero()
                Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                Column(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xxl)) {
                    benefits.forEach { Benefit(it) }

                    Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                    planCards()

                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    TextButton(
                        onClick = { onRestore() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.paywall_restore))
                    }
                    Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                }
            }

            // Transparent app bar overlaid on the gradient hero.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(MaterialTheme.dimens.buttonHeight)
                    .padding(start = MaterialTheme.dimens.xs, end = MaterialTheme.dimens.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.width(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(R.string.paywall_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/** White-on-gradient feature row for the landscape Paywall brand panel. */
@Composable
private fun WhiteBenefit(benefit: PremiumBenefit) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = if (benefit.soon) 0.12f else 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (benefit.soon) Icons.Filled.Schedule else Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (benefit.soon) 0.7f else 1f),
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = benefit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (benefit.soon) 0.62f else 0.92f),
            )
            Text(
                text = benefit.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
            )
        }
    }
}

/** Full-bleed gradient hero with a crown motif; extends behind the status bar and app bar. */
@Composable
private fun PaywallHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 64.dp, bottom = 36.dp, start = MaterialTheme.dimens.xxl, end = MaterialTheme.dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.xxl))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                CrownIcon(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.paywall_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.paywall_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )
        }
    }
}

/** Simple monochrome crown drawn with a Path, tinted [color]. */
@Composable
private fun CrownIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val crown = Path().apply {
            moveTo(w * 0.12f, h * 0.70f)
            lineTo(w * 0.88f, h * 0.70f)
            lineTo(w * 0.96f, h * 0.30f)
            lineTo(w * 0.70f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.16f)
            lineTo(w * 0.30f, h * 0.48f)
            lineTo(w * 0.04f, h * 0.30f)
            close()
        }
        drawPath(crown, color)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.72f),
            size = Size(w * 0.76f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
        )
    }
}

/** Feature row for the portrait Paywall, on the plain surface below the hero. */
@Composable
private fun Benefit(benefit: PremiumBenefit) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (benefit.soon) Icons.Filled.Schedule else Icons.Filled.Check,
                contentDescription = null,
                tint = if (benefit.soon) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Column {
            Text(
                text = benefit.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (benefit.soon) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = benefit.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Centered status card (You're Premium / Not available) with vertically-centered content. */
@Composable
private fun StatusCard(icon: ImageVector, iconTint: Color, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = MaterialTheme.dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(MaterialTheme.dimens.avatar))
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlanCard(product: ProductDetails, bestValue: Boolean, onSubscribe: () -> Unit) {
    val price = product.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()
        ?.formattedPrice
        .orEmpty()
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.lg),
        ) {
            if (bestValue) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.paywall_best_value),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name.ifBlank { product.productId },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (price.isNotBlank()) {
                        Text(
                            text = price,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(MaterialTheme.dimens.md))
                Button(onClick = onSubscribe, modifier = Modifier.height(MaterialTheme.dimens.buttonHeight)) { Text(stringResource(R.string.paywall_subscribe)) }
            }
        }
    }
}

/** Heuristic: treat a yearly/annual subscription as the best-value plan for the badge. */
private fun ProductDetails.isYearly(): Boolean {
    val haystack = "$name $productId".lowercase()
    return "year" in haystack || "annual" in haystack
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PaywallScreenPreview() {
    BudgettyTheme {
        PaywallScreenContent(
            products = emptyList(),
            isPremium = false,
            isWide = false,
            onNavigateBack = {},
            onPurchase = { _, _ -> },
            onRestore = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun PaywallScreenLandscapePreview() {
    BudgettyTheme {
        PaywallScreenContent(
            products = emptyList(),
            isPremium = false,
            isWide = true,
            onNavigateBack = {},
            onPurchase = { _, _ -> },
            onRestore = {},
        )
    }
}
