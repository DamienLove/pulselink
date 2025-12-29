package com.pulselink.callid

import com.pulselink.data.remoteconfig.RemoteConfigService
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manages Truecaller API keys securely.
 * Keys can be rotated remotely via Firebase Remote Config without app updates.
 */
@Singleton
class TruecallerKeyProvider @Inject constructor(
    private val remoteConfigService: RemoteConfigService,
    @Named("TruecallerApiKeyDefault") private val defaultKey: String
) {
    /**
     * Returns the current Truecaller API key.
     * Priority: Firebase Remote Config > BuildConfig default > empty string
     */
    fun currentKey(): String =
        remoteConfigService.getString("truecaller_api_key").ifBlank { defaultKey }
}
