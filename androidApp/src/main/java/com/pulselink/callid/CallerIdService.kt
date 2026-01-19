package com.pulselink.callid

import android.util.Log
import com.pulselink.BuildConfig
import com.pulselink.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class CallerIdService @Inject constructor(
    providers: Set<@JvmSuppressWildcards CallerIdProvider>,
    private val cacheRepository: CallerIdCacheRepository,
    private val usageLimiter: CallerIdUsageLimiter,
    private val settingsRepository: SettingsRepository
) {
    private val orderedProviders: List<CallerIdProvider> = providers.sortedBy { it.priority }
    private val providerCaps: Map<String, Int> = mapOf(
        "TwilioLookup" to BuildConfig.TWILIO_LOOKUP_MONTHLY_CAP,
        "NumLookupApi" to BuildConfig.NUMLOOKUP_MONTHLY_CAP,
        "Numverify" to BuildConfig.NUMVERIFY_MONTHLY_CAP,
        "IPQualityScore" to BuildConfig.IPQS_MONTHLY_CAP,
        "RapidLookup" to BuildConfig.RAPID_LOOKUP_MONTHLY_CAP,
        "RealCall" to BuildConfig.REALCALL_MONTHLY_CAP
    )

    suspend fun lookup(rawNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        val normalized = rawNumber.filter { it.isDigit() || it == '+' }
        if (normalized.isBlank()) return@withContext null
        cacheRepository.lookupUserMapping(normalized)?.let { return@withContext it }
        cacheRepository.getCached(normalized)?.let { return@withContext it }

        val settings = settingsRepository.settings.first()
        val truecallerEnabled = settings.truecallerEnabled

        val providerChain = orderedProviders.shuffled().filter { provider ->
            if (provider.providerName == "Truecaller" && !truecallerEnabled) {
                false
            } else {
                true
            }
        }

        for (provider in providerChain) {
            val redacted = normalized.takeLast(4).padStart(normalized.length, '*')
            val cap = providerCaps[provider.providerName] ?: Int.MAX_VALUE
            if (!usageLimiter.tryConsume(provider.providerName, cap)) {
                Log.w(TAG, "[${provider.providerName}] monthly cap reached; skipping")
                continue
            }
            val result = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                provider.lookup(normalized)
            }
            if (result != null) {
                Log.i(TAG, "[${provider.providerName}] hit for $redacted -> ${result.summary}")
                cacheRepository.store(normalized, result)
                return@withContext result
            } else {
                Log.w(TAG, "[${provider.providerName}] no result for $redacted")
            }
        }
        null
    }

    companion object {
        private const val TAG = "CallerIdService"
        private const val PROVIDER_TIMEOUT_MS = 3_000L
    }
}
