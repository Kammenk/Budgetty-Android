package com.budgetty.app.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.local.TagEntity
import com.budgetty.app.data.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One tag in the Manage list: its name and how many transactions carry it. */
data class TagRow(
    val name: String,
    val count: Int,
)

/** [isLoaded] gates the empty state so a brand-new screen doesn't flash "no tags" before the first
 *  emission (the loading-state-gate convention). */
data class TagsUiState(
    val tags: List<TagRow> = emptyList(),
    val isLoaded: Boolean = false,
)

/**
 * Account → Tags. Lists every tag with its transaction count and backs the per-row Rename / Merge /
 * Delete actions. Rename and Merge are the same repository operation — renaming into a name that
 * already exists merges — so both route through [TagRepository.renameOrMerge]. Deleting a tag never
 * deletes transactions.
 */
class TagsViewModel(
    private val tagRepository: TagRepository,
) : ViewModel() {

    val uiState: StateFlow<TagsUiState> = tagRepository.tagCounts
        .map { counts -> TagsUiState(counts.map { TagRow(it.name, it.count) }, isLoaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState())

    /** Renames [from] to the normalized [rawTo]; merges automatically if that name already exists. */
    fun rename(from: String, rawTo: String) {
        val to = TagEntity.normalize(rawTo)
        if (to.isEmpty() || to == from) return
        viewModelScope.launch { tagRepository.renameOrMerge(from, to) }
    }

    /** Merges [from] into the existing tag [into] (both already normalized). */
    fun merge(from: String, into: String) {
        if (into.isEmpty() || into == from) return
        viewModelScope.launch { tagRepository.renameOrMerge(from, into) }
    }

    fun delete(name: String) {
        viewModelScope.launch { tagRepository.deleteTag(name) }
    }
}
