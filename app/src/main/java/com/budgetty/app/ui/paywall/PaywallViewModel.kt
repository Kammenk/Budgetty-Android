package com.budgetty.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.budgetty.app.data.billing.BillingManager
import kotlinx.coroutines.flow.StateFlow

class PaywallViewModel(private val billing: BillingManager) : ViewModel() {

    val products: StateFlow<List<ProductDetails>> = billing.products
    val isPremium: StateFlow<Boolean> = billing.isPremium

    init {
        billing.refresh()
    }

    fun purchase(activity: Activity, productId: String) = billing.purchase(activity, productId)

    fun restore() = billing.refresh()
}
