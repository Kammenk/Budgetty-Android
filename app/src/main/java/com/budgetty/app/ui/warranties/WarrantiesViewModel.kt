package com.budgetty.app.ui.warranties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.local.WarrantyEntity
import com.budgetty.app.data.repository.WarrantyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One warranty row with its derived status, for the list. */
data class WarrantyCardUi(val entity: WarrantyEntity, val status: WarrantyStatus)

data class WarrantiesUiState(
    val isLoaded: Boolean = false,
    val isPremium: Boolean = false,
    val expiringSoon: List<WarrantyCardUi> = emptyList(),
    val active: List<WarrantyCardUi> = emptyList(),
    val expired: List<WarrantyCardUi> = emptyList(),
) {
    val total: Int get() = expiringSoon.size + active.size + expired.size
    val expiringSoonCount: Int get() = expiringSoon.size
    /** A free user at the 5-warranty cap: adding routes to the paywall, existing ones keep working. */
    val atCap: Boolean get() = !isPremium && total >= Warranties.FREE_LIMIT
}

/**
 * Warranty tracker: groups tracked warranties by urgency (expiring soon → active → expired) with each
 * row's elapsed status, and enforces the free-tier cap on adding. Expiry and status are derived live
 * from the purchase date + length (see [Warranties]); nothing is scheduled (the app has no push
 * reminders — warranties surface in-app).
 */
class WarrantiesViewModel(
    private val repository: WarrantyRepository,
    billingManager: BillingManager,
) : ViewModel() {

    val uiState: StateFlow<WarrantiesUiState> =
        combine(repository.warranties, billingManager.isPremium) { list, premium ->
            build(list, premium)
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WarrantiesUiState(),
        )

    private fun build(list: List<WarrantyEntity>, premium: Boolean): WarrantiesUiState {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val cards = list.map { w ->
            val purchase = Instant.ofEpochMilli(w.purchaseDate).atZone(zone).toLocalDate()
            WarrantyCardUi(w, Warranties.status(purchase, w.durationMonths, today))
        }
        return WarrantiesUiState(
            isLoaded = true,
            isPremium = premium,
            expiringSoon = cards.filter { it.status.state == WarrantyState.EXPIRING_SOON }
                .sortedBy { it.status.daysLeft },
            active = cards.filter { it.status.state == WarrantyState.ACTIVE }
                .sortedBy { it.status.daysLeft },
            expired = cards.filter { it.status.state == WarrantyState.EXPIRED }
                .sortedByDescending { it.status.expiryDate },
        )
    }

    /** Creates or updates a warranty; [original] is the row being edited (null when adding). */
    fun save(
        original: WarrantyEntity?,
        name: String,
        emoji: String,
        store: String,
        category: String,
        purchaseDateMillis: Long,
        durationMonths: Int,
        coverageNote: String,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || durationMonths <= 0) return
        val base = original ?: WarrantyEntity(
            name = trimmed,
            purchaseDate = purchaseDateMillis,
            durationMonths = durationMonths,
            createdAt = System.currentTimeMillis(),
        )
        val entity = base.copy(
            name = trimmed,
            emoji = emoji.ifBlank { "🛡️" },
            store = store.trim(),
            category = category,
            purchaseDate = purchaseDateMillis,
            durationMonths = durationMonths,
            coverageNote = coverageNote.trim(),
        )
        viewModelScope.launch { repository.upsert(entity) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }
}
