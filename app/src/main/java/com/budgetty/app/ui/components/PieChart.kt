package com.budgetty.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.ui.util.categoryDisplayName
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** One slice of the pie: a product/transaction's spend and its display color. */
data class PieSlice(
    val label: String,
    val value: BigDecimal,
    val color: Color,
    /** The categories this slice's spend covers, so the transactions sheet can list them all. Just
     *  the slice's own category normally; a rolled-up group slice carries every member it sums. */
    val members: Set<String> = setOf(label),
)

/**
 * Fallback palette cycled across slices when a category has no saved color: 100 muted, fully
 * opaque colors whose hues are spread by the golden angle so neighbouring slices stay distinct.
 * Same deep/bland tone as the seeded category colors (see `Categories.hsvColor`).
 */
val pieColors: List<Color> = List(100) { i -> Color.hsv((i * 137.508f) % 360f, 0.53f, 0.75f) }

/** How many top categories the donut draws and the inline legend lists before the
 *  "See all categories" sheet. The ring and the legend stay in sync on this count; the
 *  long tail is reachable only through the sheet. */
private const val TOP_CATEGORY_LIMIT = 6

/** Donut geometry: ring thickness as a fraction of the diameter, and the width of the gap
 *  between segments (also a fraction of the diameter). The gap is carved by insetting each
 *  slice's straight sides parallel to the boundary radius, so it stays a constant-width strip
 *  rather than a wedge that fans out toward the rim. */
private const val STROKE_FRACTION = 0.10f
private const val SLICE_GAP_FRACTION = 0.025f

/** Minimum arc (degrees) every slice is guaranteed, so small categories stay visible/tappable. */
private const val MIN_SLICE_DEGREES = 9f

/**
 * Room reserved around the ring so each slice's outside percentage label and its short leader line
 * have space instead of clipping; the donut shrinks within its box to make room. Side labels run
 * wider than the top/bottom ones, so they reserve more horizontally.
 */
private val LABEL_RESERVE_H: Dp = 52.dp
private val LABEL_RESERVE_V: Dp = 26.dp

/** Percentage-label leader: gap from the rim, the line's own length, then a gap before the text. */
private val LEADER_GAP: Dp = 2.dp
private val LEADER_LEN: Dp = 5.dp
private val LABEL_GAP: Dp = 3.dp

/** Layout for one ring segment: its start angle and sweep, in degrees clockwise from
 *  12 o'clock. Slices tile the circle; the visual gap between them is carved at draw time. */
private data class ArcSpec(
    val startAngle: Float,
    val sweep: Float,
)

/**
 * Lays slices around the ring. Every slice gets a [MIN_SLICE_DEGREES] baseline arc; whatever
 * budget remains after the baselines is split proportionally by spend. This keeps small
 * categories clearly visible/tappable, at the cost of slightly compressing the largest slice.
 * Slices tile the full circle — the gap between them is carved later (see [drawDonutSlice]) —
 * and drawing and hit-testing share this layout, so taps map to the visible segment.
 */
private fun buildArcs(slices: List<PieSlice>, total: BigDecimal): List<ArcSpec> {
    val n = slices.size
    // Baseline per slice, capped so there's always room left for proportional distribution.
    val minDeg = if (n > 1) minOf(MIN_SLICE_DEGREES, 360f / n * 0.6f) else 0f
    val proportionalBudget = 360f - n * minDeg
    val totalF = total.toFloat().coerceAtLeast(0.0001f)
    var cursor = 0f
    return slices.map { slice ->
        val share = slice.value.toFloat() / totalF
        val sweep = minDeg + share * proportionalBudget
        val spec = ArcSpec(startAngle = cursor, sweep = sweep)
        cursor += sweep
        spec
    }
}

/**
 * Fills one donut segment as an annular sector whose two straight sides are inset by half
 * [gap] *parallel to the boundary radius*. Because neighbouring slices' facing sides stay
 * parallel (rather than radial), the space between them is a constant-width strip — a clean
 * rectangle — instead of a wedge that fans out toward the rim.
 *
 * [startAngle] is in degrees, 0° = 3 o'clock, clockwise (Compose's arc convention).
 */
private fun DrawScope.drawDonutSlice(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    gap: Float,
    color: Color,
) {
    if (sweepAngle <= 0f || innerRadius <= 0f) return
    val half = gap / 2f
    val startRad = Math.toRadians(startAngle.toDouble())
    val endRad = Math.toRadians((startAngle + sweepAngle).toDouble())

    // Turn the fixed linear half-gap into an angular inset at each radius (larger nearer the
    // centre). Clamp so a thin slice's sides can't cross past its arc and invert the path.
    val maxInset = Math.toRadians(sweepAngle / 2.0 - 0.5).coerceAtLeast(0.0)
    val innerInset = minOf(asin((half / innerRadius).toDouble().coerceIn(0.0, 1.0)), maxInset)
    val outerInset = minOf(asin((half / outerRadius).toDouble().coerceIn(0.0, 1.0)), maxInset)

    fun point(angleRad: Double, radius: Float) = Offset(
        center.x + radius * cos(angleRad).toFloat(),
        center.y + radius * sin(angleRad).toFloat(),
    )

    val innerStart = point(startRad + innerInset, innerRadius)
    val outerStart = point(startRad + outerInset, outerRadius)
    val innerEnd = point(endRad - innerInset, innerRadius)

    val path = Path().apply {
        moveTo(innerStart.x, innerStart.y)
        lineTo(outerStart.x, outerStart.y)
        arcTo(
            rect = Rect(center, outerRadius),
            startAngleDegrees = Math.toDegrees(startRad + outerInset).toFloat(),
            sweepAngleDegrees = Math.toDegrees(endRad - outerInset - (startRad + outerInset)).toFloat(),
            forceMoveTo = false,
        )
        lineTo(innerEnd.x, innerEnd.y)
        arcTo(
            rect = Rect(center, innerRadius),
            startAngleDegrees = Math.toDegrees(endRad - innerInset).toFloat(),
            sweepAngleDegrees = Math.toDegrees(startRad + innerInset - (endRad - innerInset)).toFloat(),
            forceMoveTo = false,
        )
        close()
    }
    drawPath(path, color)
}

/**
 * Draws one slice's percentage just outside the ring: a short leader line from the rim, then the
 * text — vertically centred on the leader's end and horizontally anchored so it reads away from the
 * ring (extends rightward on the ring's right half, leftward on its left, centred at top/bottom).
 * [midDeg] is the slice's mid-angle in Compose arc degrees (0° = 3 o'clock, clockwise).
 */
private fun DrawScope.drawSliceLabel(
    center: Offset,
    outerRadius: Float,
    midDeg: Float,
    text: String,
    leaderColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
) {
    val rad = Math.toRadians(midDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val startR = outerRadius + LEADER_GAP.toPx()
    val endR = startR + LEADER_LEN.toPx()
    drawLine(
        color = leaderColor,
        start = Offset(center.x + startR * cosA, center.y + startR * sinA),
        end = Offset(center.x + endR * cosA, center.y + endR * sinA),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val measured = textMeasurer.measure(text, textStyle)
    val labelR = endR + LABEL_GAP.toPx()
    val anchorX = center.x + labelR * cosA
    val anchorY = center.y + labelR * sinA
    // Push the text off the anchor toward the ring's outside: rightward on the right half, leftward
    // on the left, and centred where a slice sits near the very top or bottom (cosA ≈ 0).
    val topLeftX = when {
        cosA > 0.1f -> anchorX
        cosA < -0.1f -> anchorX - measured.size.width
        else -> anchorX - measured.size.width / 2f
    }
    drawText(
        textLayoutResult = measured,
        color = textColor,
        topLeft = Offset(topLeftX, anchorY - measured.size.height / 2f),
    )
}

/**
 * Donut chart with the [total] in its hollow center and a two-column legend below
 * listing each product's spend. The donut is drawn at a fixed [chartSize] so the
 * whole composable measures its own height and can live inside a scrolling parent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PieChart(
    slices: List<PieSlice>,
    total: BigDecimal,
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
    periodLabel: String? = null,
    onCategoryClick: (PieSlice) -> Unit = {},
) {
    val sliceTotal = slices.fold(BigDecimal.ZERO) { acc, slice -> acc + slice.value }

    if (slices.isEmpty() || sliceTotal <= BigDecimal.ZERO) {
        PieChartEmptyState(modifier = modifier, chartSize = chartSize)
        return
    }

    var showSheet by remember { mutableStateOf(false) }
    // Index of the tapped slice, or null when nothing is selected (center shows the total).
    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }
    // The ring draws only the top categories; the long tail lives in the "See all" sheet.
    // Drawing and hit-testing share this trimmed list, so taps map to the visible segment.
    val donutSlices = remember(slices) { slices.take(TOP_CATEGORY_LIMIT) }
    val donutTotal = remember(donutSlices) {
        donutSlices.fold(BigDecimal.ZERO) { acc, slice -> acc + slice.value }
    }
    // Precomputed arc layout (proportional + gapped). Sized against the drawn slices' own
    // total so the ring closes cleanly; the spend figures and percentages below stay honest
    // against the full-period [sliceTotal].
    val arcs = remember(donutSlices) { buildArcs(donutSlices, donutTotal) }

    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface
    // Slice % labels, enlarged 50% over the labelMedium base for legibility on the chart. Line
    // height is scaled with the font (labelMedium's own is too short for the bigger glyphs → clips).
    val labelFontSize = MaterialTheme.typography.labelMedium.fontSize * 1.5f
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = labelFontSize,
        lineHeight = labelFontSize * 1.25f,
    )
    // Each drawn slice's honest share of the full-period spend (matches the on-tap "% of spend"
    // figure), rounded to a whole percent for the outside label.
    val donutPercents = remember(donutSlices, sliceTotal) {
        donutSlices.map { slice ->
            if (sliceTotal <= BigDecimal.ZERO) 0
            else slice.value.multiply(BigDecimal(100))
                .divide(sliceTotal, 0, java.math.RoundingMode.HALF_UP).toInt()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Donut, sized to a fixed square in the middle of the available width.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartSize)
                .pointerInput(slices) {
                    detectTapGestures { tap ->
                        // Match the drawn ring: shrunk to leave room for the outside labels.
                        val diameter = minOf(
                            size.width - 2f * LABEL_RESERVE_H.toPx(),
                            size.height - 2f * LABEL_RESERVE_V.toPx(),
                        ).coerceAtLeast(0f)
                        val dx = (tap.x - size.width / 2f).toDouble()
                        val dy = (tap.y - size.height / 2f).toDouble()
                        val dist = hypot(dx, dy)
                        val strokeW = diameter * STROKE_FRACTION
                        val ringRadius = diameter / 2f - strokeW / 2f
                        // Generous band so the thin ring is still easy to hit.
                        val tolerance = strokeW / 2f + diameter * 0.06f
                        if (abs(dist - ringRadius) <= tolerance) {
                            // Angle from 12 o'clock, clockwise, in [0, 360).
                            var angle = Math.toDegrees(atan2(dy, dx)).toFloat()
                            angle = ((angle + 90f) % 360f + 360f) % 360f
                            val hit = arcs.indexOfFirst {
                                angle >= it.startAngle && angle < it.startAngle + it.sweep
                            }
                            selectedIndex = if (hit < 0 || hit == selectedIndex) null else hit
                        } else {
                            selectedIndex = null
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Shrink the ring to leave a margin for the outside percentage labels; the tap
                // hit-test above reserves the same margin so taps still land on the visible ring.
                val diameter = minOf(
                    size.width - 2f * LABEL_RESERVE_H.toPx(),
                    size.height - 2f * LABEL_RESERVE_V.toPx(),
                ).coerceAtLeast(0f)
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = diameter / 2f
                val innerRadius = outerRadius - diameter * STROKE_FRACTION
                val gap = diameter * SLICE_GAP_FRACTION
                arcs.forEachIndexed { index, arc ->
                    val slice = donutSlices[index]
                    val dimmed = selectedIndex != null && selectedIndex != index
                    drawDonutSlice(
                        center = center,
                        innerRadius = innerRadius,
                        outerRadius = outerRadius,
                        startAngle = -90f + arc.startAngle,
                        sweepAngle = arc.sweep,
                        gap = gap,
                        color = if (dimmed) slice.color.copy(alpha = 0.25f) else slice.color,
                    )
                    val pct = donutPercents[index]
                    if (pct >= 1) {
                        drawSliceLabel(
                            center = center,
                            outerRadius = outerRadius,
                            midDeg = -90f + arc.startAngle + arc.sweep / 2f,
                            text = "$pct%",
                            leaderColor = if (dimmed) slice.color.copy(alpha = 0.25f) else slice.color,
                            textColor = if (dimmed) labelColor.copy(alpha = 0.3f) else labelColor,
                            textMeasurer = textMeasurer,
                            textStyle = labelStyle,
                        )
                    }
                }
            }

            // Center shows the period total, or the tapped slice's category + spend.
            val selected = selectedIndex?.let { donutSlices.getOrNull(it) }
            Column(
                modifier = Modifier.widthIn(max = chartSize * 0.6f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = selected?.label ?: "Total",
                    style = MaterialTheme.typography.labelMedium,
                    // When a slice is tapped, tint its name with the slice's own color.
                    color = selected?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = (selected?.value ?: total).formatMoney(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                if (selected != null) {
                    val pct = selected.value
                        .multiply(BigDecimal(100))
                        .divide(sliceTotal, 0, java.math.RoundingMode.HALF_UP)
                    Text(
                        text = "$pct% of spend",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Two-column legend of the top categories, each with its spend. Tapping a row opens
        // that category's transactions.
        LegendGrid(
            slices = slices.take(TOP_CATEGORY_LIMIT),
            onCategoryClick = onCategoryClick,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        )

        if (slices.size > TOP_CATEGORY_LIMIT) {
            TextButton(onClick = { showSheet = true }) {
                Text(stringResource(R.string.see_all_categories), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showSheet) {
        AllCategoriesSheet(
            slices = slices,
            total = total,
            periodLabel = periodLabel,
            onCategoryClick = {
                showSheet = false
                onCategoryClick(it)
            },
            onDismiss = { showSheet = false },
        )
    }
}

/**
 * The "All categories" bottom sheet behind the "See all categories" link: every category for the
 * period, sorted by spend, each a coloured icon tile with its amount, share of the total, and a
 * mini progress bar. A header line names the period and total, with a close affordance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCategoriesSheet(
    slices: List<PieSlice>,
    total: BigDecimal,
    periodLabel: String?,
    onCategoryClick: (PieSlice) -> Unit,
    onDismiss: () -> Unit,
) {
    // Percentages and bar widths read against the period total; fall back to the slice sum if the
    // caller passed no total, so a stray zero can't blank out every bar.
    val basis = (if (total > BigDecimal.ZERO) total else slices.fold(BigDecimal.ZERO) { a, s -> a + s.value })
        .toFloat().coerceAtLeast(0.0001f)
    AdaptiveSheet(
        onDismiss = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 10.dp),
        ) {
            Text(
                text = "All categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = listOfNotNull(periodLabel, "${total.formatMoney()} total")
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                bottom = 24.dp,
            ),
        ) {
            items(slices) { slice ->
                CategoryBreakdownRow(
                    slice = slice,
                    fraction = (slice.value.toFloat() / basis).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(slice) },
                )
            }
        }
    }
}

/** One "All categories" row: a coloured emoji tile, the category name with its amount and share,
 *  and a thin progress bar sized to the category's slice of the period total. */
@Composable
private fun CategoryBreakdownRow(
    slice: PieSlice,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val emoji = Categories.emojiOf(slice.label)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(slice.color),
            contentAlignment = Alignment.Center,
        ) {
            if (emoji.isNotEmpty()) {
                Text(text = emoji, fontSize = 21.sp)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = categoryDisplayName(slice.label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = slice.value.formatMoney(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(slice.color),
                )
            }
        }
    }
}

/**
 * Empty-period placeholder shown in place of the donut: a hollow grey ring with a globe at its
 * center and a caption beneath. Keeps the donut's footprint (same [chartSize]) so the Breakdown
 * card stays the same height whether or not there is spend to chart.
 */
@Composable
private fun PieChartEmptyState(
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
) {
    val ringColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = minOf(size.width, size.height)
                val topLeft = Offset(
                    x = (size.width - diameter) / 2f,
                    y = (size.height - diameter) / 2f,
                )
                drawArc(
                    color = ringColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = diameter * STROKE_FRACTION, cap = StrokeCap.Round),
                )
            }
            Text(text = "🌍", fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No spending in this period",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Lays the slices out two-per-row so the legend reads like the mockup's grid. */
@Composable
private fun LegendGrid(
    slices: List<PieSlice>,
    onCategoryClick: (PieSlice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        slices.chunked(2).forEach { rowSlices ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowSlices.forEach { slice ->
                    SliceRow(
                        slice = slice,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryClick(slice) },
                    )
                }
                // Balance the row when there's an odd number of slices.
                if (rowSlices.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** A single legend/sheet entry: color dot, product name, and its spend. */
@Composable
private fun SliceRow(
    slice: PieSlice,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(slice.color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = categoryDisplayName(slice.label),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = slice.value.formatMoney(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
