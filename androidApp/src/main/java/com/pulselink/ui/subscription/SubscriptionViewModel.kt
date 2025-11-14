package com.pulselink.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.pulselink.data.billing.BillingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingService: BillingService
) : ViewModel() {

    val subscriptionProducts = billingService.subscriptionProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val purchases = billingService.purchases
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        billingService.launchPurchaseFlow(activity, productDetails)
    }

    fun connectToBilling() {
        billingService.connect()
    }
}
