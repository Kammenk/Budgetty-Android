package com.budgetty.app.ui.onboarding

import com.budgetty.app.ui.theme.dimens
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

private val pages = listOf(
    OnboardingPage("📸", R.string.onb1_title, R.string.onb1_body),
    OnboardingPage("✨", R.string.onb2_title, R.string.onb2_body),
    OnboardingPage("📊", R.string.onb3_title, R.string.onb3_body),
    OnboardingPage("🎯", R.string.onb4_title, R.string.onb4_body),
)

/**
 * First-launch onboarding carousel (shown before login until completed/skipped).
 * Calls [onDone] when the user finishes the last slide or taps Skip.
 *
 * Phones get the original stacked carousel; tablets get a side-by-side split (illustration on the
 * left, copy and controls on the right) that fills the window top-to-bottom in landscape.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.lastIndex
    if (isWideWidth()) {
        // Landscape tablet: illustration beside the copy + CTA.
        TabletOnboarding(pagerState, isLastPage, onDone, modifier)
    } else {
        // Phone & portrait tablet: the single-column carousel.
        PhoneOnboarding(pagerState, isLastPage, onDone, modifier)
    }
}

/** The original single-column carousel used on phones. */
@Composable
private fun PhoneOnboarding(
    pagerState: PagerState,
    isLastPage: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = MaterialTheme.dimens.xxl),
    ) {
        // Brand + Skip row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.dimens.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Budgetty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onDone,
                enabled = !isLastPage,
            ) {
                Text(if (isLastPage) "" else stringResource(R.string.onb_skip))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        OnboardingDots(
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.dimens.xxl),
        )

        Button(
            onClick = {
                if (isLastPage) onDone()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            AnimatedContent(targetState = isLastPage, label = "ctaLabel") { last ->
                Text(stringResource(if (last) R.string.onb_get_started else R.string.onb_next))
            }
        }
        Spacer(Modifier.height(MaterialTheme.dimens.xxl))
    }
}

/**
 * Tablet/expanded onboarding: a full-bleed split with the illustration on the left and the copy on
 * the right. Built in three layers so the carousel swipes cleanly while the chrome stays put:
 *
 *  1. a fixed two-tone background (lavender | surface), edge-to-edge under the system bars,
 *  2. a transparent [HorizontalPager] whose pages lay the illustration and copy across the split —
 *     so a swipe animates both halves together, and the identical backgrounds make it read static,
 *  3. fixed chrome (Skip, page dots, Next) that must not slide with the pages.
 */
@Composable
private fun TabletOnboarding(
    pagerState: PagerState,
    isLastPage: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Fixed background split (extends under the status bar for a full-bleed look).
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
            )
        }

        // 2. Swipeable content. Pages are transparent; padding keeps content clear of the chrome.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) { page ->
            Row(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 40.dp, end = MaterialTheme.dimens.xxl, top = MaterialTheme.dimens.xxl, bottom = 88.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OnboardingIllustration(
                        emoji = pages[page].emoji,
                        containerColor = MaterialTheme.colorScheme.surface,
                        elevation = 6.dp,
                        widthFraction = 0.78f,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 40.dp, end = 40.dp, top = 80.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(Modifier.widthIn(max = 460.dp)) {
                        OnboardingBrandBadge()
                        Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                        Text(
                            text = stringResource(pages[page].titleRes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(MaterialTheme.dimens.md))
                        Text(
                            text = stringResource(pages[page].bodyRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // 3. Fixed chrome, mirrored across the same split so each control lands in the right half.
        Row(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // Left half: page dots under the illustration.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                OnboardingDots(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                )
            }
            // Right half: Skip (top) and the Next/Get-started CTA (bottom).
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 40.dp),
            ) {
                TextButton(
                    onClick = onDone,
                    enabled = !isLastPage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = MaterialTheme.dimens.sm),
                ) {
                    Text(if (isLastPage) "" else stringResource(R.string.onb_skip))
                }
                Button(
                    onClick = {
                        if (isLastPage) onDone()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 40.dp)
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    AnimatedContent(targetState = isLastPage, label = "ctaLabel") { last ->
                        Text(stringResource(if (last) R.string.onb_get_started else R.string.onb_next))
                    }
                }
            }
        }
    }
}

/** Animated page-position dots, shared by both layouts. */
@Composable
private fun OnboardingDots(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pages.size) { index ->
            val selected = pagerState.currentPage == index
            val width by animateDpAsState(if (selected) MaterialTheme.dimens.xxl else MaterialTheme.dimens.sm, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.dimens.xs)
                    .size(width = width, height = MaterialTheme.dimens.sm)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Small brand mark above the copy on the tablet layout. */
@Composable
private fun OnboardingBrandBadge() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Receipt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OnboardingIllustration(page.emoji)
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.sm),
        )
    }
}

/**
 * Branded illustration placeholder: layered soft circles behind a large emoji glyph. [containerColor]
 * and [elevation] let the tablet layout lift it to a lighter card against the lavender panel, while
 * [widthFraction] sizes it relative to its parent.
 */
@Composable
private fun OnboardingIllustration(
    emoji: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    elevation: Dp = 0.dp,
    widthFraction: Float = 0.7f,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .aspectRatio(1f)
            .shadow(elevation, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative offset circles for a little depth.
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 36.dp, y = (-36).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .offset(x = (-48).dp, y = 48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
        )
        Text(text = emoji, fontSize = 88.sp)
    }
}

@Preview(showBackground = true, heightDp = 740)
@Composable
private fun OnboardingScreenPreview() {
    BudgettyTheme {
        OnboardingScreen(onDone = {})
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun OnboardingScreenTabletPreview() {
    BudgettyTheme {
        OnboardingScreen(onDone = {})
    }
}
