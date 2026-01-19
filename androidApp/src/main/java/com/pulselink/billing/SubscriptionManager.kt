package com.pulselink.billing

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.pulselink.BuildConfig
import com.pulselink.domain.repository.SettingsRepository
import com.google.firebase.functions.FirebaseFunctions
import com.pulselink.auth.FirebaseAuthManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SubscriptionState(
    val available: Boolean = BuildConfig.SUBSCRIPTION_ENABLED,
    val loading: Boolean = false,
    val product: ProductDetails? = null,
    val isPremiumActive: Boolean = false,
    val tierBeforePremium: String? = null,
    val statusMessage: String? = null
)

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val functions: FirebaseFunctions,
    private val authManager: FirebaseAuthManager,
    private val smsSyncTrigger: com.pulselink.data.sms.SmsSyncTrigger
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(SubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _state

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    init {
        if (BuildConfig.SUBSCRIPTION_ENABLED) {
            startConnection()
            scope.launch {
                while (true) {
                    refreshPremiumStatus()
                    delay(6 * 60 * 60 * 1000L) // every 6h
                }
            }
        }
    }

    private fun startConnection() {
        if (!isPlayStoreAvailable()) {
            _state.value = _state.value.copy(
                available = false,
                loading = false,
                statusMessage = "Google Play Store unavailable; billing disabled"
            )
            return
        }
        if (!isPlayServicesReady()) {
            _state.value = _state.value.copy(
                available = false,
                loading = false,
                statusMessage = "Google Play Services unavailable; billing disabled"
            )
            return
        }
        if (billingClient.isReady) {
            queryProductsAndPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val featureResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                    if (featureResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        _state.value = _state.value.copy(
                            available = false,
                            loading = false,
                            statusMessage = "Subscriptions not supported on this device"
                        )
                        return
                    }
                    queryProductsAndPurchases()
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        statusMessage = "Billing unavailable (${result.responseCode})"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(
                    loading = false,
                    statusMessage = "Billing disconnected"
                )
            }
        })
        _state.value = _state.value.copy(loading = true, statusMessage = null)
    }

    private fun isPlayServicesReady(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val status = availability.isGooglePlayServicesAvailable(context)
        if (status == ConnectionResult.SUCCESS) return true
        // Errors here are non-fatal for the app, but billing cannot proceed without Play Services/Store.
        return false
    }

    private fun isPlayStoreAvailable(): Boolean {
        val packageManager = context.packageManager
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    "com.android.vending",
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo("com.android.vending", 0)
            }
            true
        }.getOrDefault(false)
    }

    private fun queryProductsAndPurchases() {
        val productParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.SUBS_MONTHLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(productParams) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    product = details.productDetailsList.firstOrNull(),
                    loading = false
                )
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    statusMessage = "Unable to load subscription products"
                )
            }
        }

        val purchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(purchaseParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    fun launchSubscribe(activity: Activity) {
        if (!isPlayStoreAvailable()) {
            _state.value = _state.value.copy(
                statusMessage = "Google Play Store unavailable; billing disabled"
            )
            return
        }
        if (!isPlayServicesReady()) {
            _state.value = _state.value.copy(
                statusMessage = "Google Play Services unavailable; billing disabled"
            )
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            _state.value = _state.value.copy(statusMessage = "Cannot start billing right now")
            return
        }
        if (!billingClient.isReady) {
            _state.value = _state.value.copy(statusMessage = "Billing not ready; retrying connection")
            startConnection()
            return
        }
        val product = _state.value.product ?: run {
            _state.value = _state.value.copy(statusMessage = "Subscription not available")
            return
        }
        val offerToken = product.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?.takeIf { it.isNotBlank() }
            ?: run {
            _state.value = _state.value.copy(statusMessage = "No active base plan/offer configured")
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        val result = runCatching {
            billingClient.launchBillingFlow(activity, flowParams)
        }.getOrElse {
            _state.value = _state.value.copy(statusMessage = "Purchase failed to start")
            return
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(
                statusMessage = "Purchase failed to start: ${result.responseCode}"
            )
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _state.value = _state.value.copy(statusMessage = "Purchase failed: ${result.responseCode}")
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val active = purchases.any { purchase ->
            purchase.products.contains(BuildConfig.SUBS_MONTHLY_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        val wasActive = _state.value.isPremiumActive
        _state.value = _state.value.copy(isPremiumActive = active, loading = false)
        scope.launch {
            val currentSettings = settingsRepository.settings.first()
            if (!currentSettings.premiumUnlocked) {
                val tierBefore = when {
                    currentSettings.proUnlocked -> "pro"
                    else -> "free"
                }
                settingsRepository.setTierBeforePremium(tierBefore)
            }
            settingsRepository.setPremiumUnlocked(active)

            // Auto-enable remote web access when premium becomes active        
            if (active && !wasActive) {
                settingsRepository.setRemoteWebAccessEnabled(true)
                // Kick off an immediate sync so web inbox populates without waiting for UI flows.
                smsSyncTrigger.triggerSync()
            }
            purchases.firstOrNull { it.products.contains(BuildConfig.SUBS_MONTHLY_PRODUCT_ID) }
                ?.let {
                    settingsRepository.setPremiumPurchaseToken(it.purchaseToken)
                    verifyRemote(it)
                }
        }
        purchases.filter { it.isAcknowledged.not() }.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* no-op */ }
        }
    }

    private suspend fun verifyRemote(purchase: Purchase) {
        runCatching {
            functions
                .getHttpsCallable("verifySubscription")
                .call(
                    hashMapOf(
                        "purchaseToken" to purchase.purchaseToken,
                        "packageName" to context.packageName,
                        "productId" to BuildConfig.SUBS_MONTHLY_PRODUCT_ID
                    )
                )
                .await()
        }.onFailure {
            _state.value = _state.value.copy(statusMessage = "Server verification failed")
        }.onSuccess {
            authManager.refreshClaims()
        }
    }

    suspend fun refreshPremiumStatus() {
        if (!BuildConfig.SUBSCRIPTION_ENABLED) return
        runCatching {
            functions.getHttpsCallable("getPremiumStatus")
                .call()
                .await()
        }.onSuccess { result ->
            val data = result.data as? Map<*, *> ?: emptyMap<String, Any>()
            val active = (data["hasClaim"] as? Boolean) == true
            val wasActive = _state.value.isPremiumActive
            _state.value = _state.value.copy(isPremiumActive = active)
            settingsRepository.setPremiumUnlocked(active)

            // Auto-enable remote web access when premium becomes active        
            if (active && !wasActive) {
                settingsRepository.setRemoteWebAccessEnabled(true)
                smsSyncTrigger.triggerSync()
            }
            if (!active) {
                settingsRepository.setPremiumPurchaseToken(null)
            }
            authManager.refreshClaims()
        }.onFailure {
            _state.value = _state.value.copy(statusMessage = "Status sync failed")
        }
    }
}
