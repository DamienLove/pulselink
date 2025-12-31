package com.pulselink.callid

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.pulselink.BuildConfig
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Minimal caller ID stub: logs the number and lets the call through.
 * Future work: add spam lookup + on-screen overlay.
 */
@AndroidEntryPoint
class CallerIdScreeningService : CallScreeningService() {

    @Inject lateinit var callerIdService: CallerIdService
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        if (number.isBlank()) {
            respondToCall(callDetails, allowResponse())
            return
        }

        serviceScope.launch {
            val settings = settingsRepository.settings.first()
            val isPremium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!isPremium) {
                respondToCall(callDetails, allowResponse())
                return@launch
            }

            val lookup = withTimeoutOrNull(OVERALL_TIMEOUT_MS) {
                callerIdService.lookup(number)
            }
            val shouldSilence = lookup?.isLikelySpam == true
            Log.i(TAG, "Incoming call from $number; lookup=${lookup?.summary ?: "none"} silence=$shouldSilence")
            val builder = CallResponse.Builder()
                .setDisallowCall(false)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                builder.setSilenceCall(shouldSilence)
            }
            respondToCall(callDetails, builder.build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun allowResponse(): CallResponse =
        CallResponse.Builder()
            .setDisallowCall(false)
            .build()

    companion object {
        private const val TAG = "CallerIdScreeningSvc"
        private const val OVERALL_TIMEOUT_MS = 10_000L
    }
}
