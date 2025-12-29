package com.pulselink.callid

import android.util.Log
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

/**
 * Truecaller lookup client via RapidAPI's Truecaller4 API.
 * Provides caller ID, spam detection, and contact information.
 *
 * QA TEST: Configure TRUECALLER_API_KEY in local.properties or env vars
 * EXPECTED: Phone lookups return caller name, carrier, spam score
 * EXPECTED: API key is never exposed in logs or source code
 */
@Singleton
class TruecallerLookupClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val keyProvider: TruecallerKeyProvider,
    @Named("TruecallerApiHost") private val apiHost: String
) : CallerIdProvider {

    override val providerName: String = "Truecaller"
    override val priority: Int = 2 // Higher priority than generic RapidAPI lookup

    override suspend fun lookup(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        val apiKey = keyProvider.currentKey()
        if (apiKey.isBlank()) {
            Log.w(TAG, "[$providerName] API key missing; skipping")
            return@withContext null
        }

        // Truecaller API expects phone number with country code
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val encoded = URLEncoder.encode(cleanNumber, "UTF-8")

        val request = Request.Builder()
            .url("https://$apiHost/api/v1/getDetails?phone=$encoded")
            .get()
            .addHeader("x-rapidapi-key", apiKey)
            .addHeader("x-rapidapi-host", apiHost)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "[$providerName] failed code=${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val payload = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                ?: return@withContext null

            // Parse Truecaller response format
            val data = payload["data"]?.jsonObject ?: payload

            val name = data.str("name")
                ?: data.str("displayName")
                ?: data.str("callerName")
            val carrier = data.str("carrier")
                ?: data.str("operator")
            val location = buildLocation(data)
            val lineType = data.str("type")
                ?: data.str("phoneType")

            // Truecaller spam scoring
            val spamScore = data["spamScore"]?.jsonPrimitive?.intOrNull
                ?: data["spam"]?.jsonObject?.get("score")?.jsonPrimitive?.intOrNull
            val spamType = data.str("spamType")
            val isSpam = (spamScore ?: 0) >= 70 || spamType != null

            val valid = data.bool("valid") ?: true

            val summary = buildString {
                append(cleanNumber)
                if (!name.isNullOrBlank()) append(" | ").append(name)
                if (!carrier.isNullOrBlank()) append(" | ").append(carrier)
                if (!lineType.isNullOrBlank()) append(" | ").append(lineType)
                if (isSpam) append(" | SPAM")
            }.ifBlank { cleanNumber }

            CallerIdLookupResult(
                number = cleanNumber,
                carrier = carrier,
                location = location,
                lineType = lineType,
                valid = valid,
                spamScore = spamScore,
                isLikelySpam = isSpam,
                source = providerName.lowercase(),
                summary = summary
            )
        }
    }

    private fun buildLocation(data: JsonObject): String? {
        val locations = mutableListOf<String>()

        // Try different location field names
        data.str("location")?.let { locations.add(it) }
        data.str("city")?.let { locations.add(it) }
        data.str("state")?.let { locations.add(it) }
        data.str("country")?.let { locations.add(it) }
        data.str("countryCode")?.let { locations.add(it) }

        // Check for nested address object
        data["address"]?.jsonObject?.let { address ->
            address.str("city")?.let { locations.add(it) }
            address.str("state")?.let { locations.add(it) }
            address.str("country")?.let { locations.add(it) }
        }

        return locations.distinct().joinToString(", ").ifBlank { null }
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.bool(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    private val kotlinx.serialization.json.JsonPrimitive.intOrNull: Int?
        get() = this.contentOrNull?.toIntOrNull()

    companion object {
        private const val TAG = "TruecallerLookupClient"
    }
}
