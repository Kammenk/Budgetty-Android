package com.budgetty.app.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.budgetty.app.MainActivity
import com.budgetty.app.R

/**
 * The rounded card every widget sits on: fills the cell, paints the day/night card background, and
 * opens the app to [dest] when tapped. [content] is laid out in a padded [Column].
 */
@Composable
fun WidgetScaffold(
    dest: String,
    padding: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_card_bg))
            .clickable(openAppAction(dest))
            .padding(padding),
        content = content,
    )
}

/** A tap action that launches [MainActivity], routing it to [dest] (a nav route). */
@Composable
private fun openAppAction(dest: String): Action {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java)
        .setAction("com.budgetty.app.widget.OPEN.$dest") // unique per dest so PendingIntents don't collide
        .putExtra(MainActivity.EXTRA_START_ROUTE, dest)
    return actionStartActivity(intent)
}

/** Brand-colored rounded chip holding the type's [iconRes], tinted for the active theme. */
@Composable
fun IconChip(iconRes: Int, size: Dp = 28.dp) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(ImageProvider(R.drawable.widget_chip_brand)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(size * 0.56f),
            colorFilter = ColorFilter.tint(WidgetColors.onBrand),
        )
    }
}

/** A small filled dot used to key a category to its color in the summary widget. */
@Composable
fun Dot(argb: Int, size: Dp = 8.dp) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(size / 2)
            .background(ColorProvider(Color(argb))),
    ) {}
}

/** A rounded progress bar with a muted track; [color] fills to [ratio] (0..1). */
@Composable
fun WidgetBar(
    ratio: Float,
    color: ColorProvider,
    modifier: GlanceModifier,
    height: Dp = 8.dp,
) {
    LinearProgressIndicator(
        progress = ratio.coerceIn(0f, 1f),
        modifier = modifier.height(height).cornerRadius(height / 2),
        color = color,
        backgroundColor = WidgetColors.track,
    )
}
