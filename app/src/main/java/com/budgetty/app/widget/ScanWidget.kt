package com.budgetty.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.budgetty.app.R
import com.budgetty.app.ui.navigation.Routes

private val CompactSize = DpSize(150.dp, 150.dp)
private val LargeSize = DpSize(250.dp, 120.dp)

/**
 * The Scan Receipt widget: a one-tap shortcut straight into the camera scan flow (the app's core
 * action). Tapping anywhere on the card opens upload/camera. Large shows a label, a "Scan" button
 * and the free-tier quota; Compact is a centred camera button.
 */
class ScanWidget : BudgettyGlanceWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, LargeSize))

    @Composable
    override fun Content(data: WidgetData) = ScanWidgetContent(data)
}

@Composable
private fun ScanWidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val large = LocalSize.current.width >= 200.dp

    WidgetScaffold(dest = Routes.upload("camera"), padding = if (large) 18.dp else 14.dp) {
        if (large) {
            Spacer(GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconChip(R.drawable.widget_ic_camera, size = 64.dp)
                Spacer(GlanceModifier.width(16.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = context.getString(R.string.widget_scan),
                        maxLines = 1,
                        style = TextStyle(color = WidgetColors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.widget_scan_body),
                        maxLines = 2,
                        style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 12.sp),
                    )
                }
                Spacer(GlanceModifier.width(12.dp))
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(50.dp)
                        .background(WidgetColors.brand)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = context.getString(R.string.widget_scan_action),
                        style = TextStyle(color = WidgetColors.onBrand, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (!data.isPremium) {
                Text(
                    text = context.getString(R.string.widget_scans_left, data.scansRemaining),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = WidgetColors.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        } else {
            // Compact: a centred camera button over a "Scan" label.
            Spacer(GlanceModifier.defaultWeight())
            Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconChip(R.drawable.widget_ic_camera, size = 60.dp)
                    Spacer(GlanceModifier.height(10.dp))
                    Text(
                        text = context.getString(R.string.widget_scan_action),
                        style = TextStyle(color = WidgetColors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
            Spacer(GlanceModifier.defaultWeight())
        }
    }
}
