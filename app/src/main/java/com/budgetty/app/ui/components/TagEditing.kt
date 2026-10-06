package com.budgetty.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import com.budgetty.app.data.local.TagCount

/**
 * The ambient tag-editing context for the review screen: the tag catalog (name + count) and recent
 * tags for the input sheet's autocomplete, plus add/remove callbacks keyed by a review row's
 * clientId. Provided once at the top of the Upload screen so the per-row [TagField] (and the sheet it
 * opens) can reach it without threading four params through every layer; empty by default.
 */
data class TagEditing(
    val catalog: List<TagCount> = emptyList(),
    val recent: List<String> = emptyList(),
    val onAdd: (clientId: String, raw: String) -> Unit = { _, _ -> },
    val onRemove: (clientId: String, tag: String) -> Unit = { _, _ -> },
)

val LocalTagEditing = compositionLocalOf { TagEditing() }
