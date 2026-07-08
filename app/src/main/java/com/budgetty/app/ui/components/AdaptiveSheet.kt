package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.budgetty.app.ui.util.isExpandedWidth

/**
 * A modal surface that adapts to the form factor: a bottom sheet on phones (compact width) and a
 * centered dialog card on tablets (expanded width) — matching the design handoff, where the phone
 * bottom sheets become centered dialogs on the larger canvas. [content] runs in a [ColumnScope] just
 * like [ModalBottomSheet]'s content, so a call site can swap `ModalBottomSheet` for `AdaptiveSheet`
 * with no other changes. The dialog card is width-capped and height-capped (90% of the screen) so
 * tall content scrolls within whatever scroll container the [content] already provides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isExpandedWidth()) {
        val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.72f)
                    .widthIn(max = 560.dp)
                    .heightIn(max = maxHeight),
                shape = RoundedCornerShape(MaterialTheme.dimens.radiusXxl),
                color = containerColor,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    content = content,
                )
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = containerColor,
            modifier = modifier,
        ) {
            content()
        }
    }
}
