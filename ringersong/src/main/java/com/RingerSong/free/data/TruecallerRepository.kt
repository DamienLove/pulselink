package com.RingerSong.free.data

import android.content.Context
import android.util.Log
import com.RingerSong.free.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TruecallerResponse(
    val data: List<TruecallerData>?,
    val trace_id: String?
)

data class TruecallerData(
    val id: String?,
    val name: String?,
    val gender: String?,
    val image: String?,
    val score: Double?,
    val phones: List<TruecallerPhone>?,
    val addresses: List<TruecallerAddress>?,
    val spamInfo: TruecallerSpamInfo?
)

data class TruecallerPhone(
    val e164Format: String?,
    val numberType: String?,
    val nationalFormat: String?,
    val countryCode: String?,
    val carrier: String?
)

data class TruecallerAddress(
    val city: String?,
    val countryCode: String?
)

data class TruecallerSpamInfo(
    val spamType: String?,
    val spamScore: Int?,
    val reportCount: Int?
)

data class CallerInfo(
    val phoneNumber: String,
    val name: String?,
    val image: String?,
    val isSpam: Boolean = false,
    val spamScore: Int = 0,
    val location: String? = null,
    val carrier: String? = null
)

class TruecallerRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val callerCache = mutableMapOf<String, CallerInfo>()

    companion object {
        private const val TAG = "Truecaller"
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.RAPIDAPI_KEY.isEmpty()) {
                Log.e(TAG, "RapidAPI key not configured")
                return@withContext false
            }

            val apiUrl = "https://${BuildConfig.RAPIDAPI_TRUECALLER_HOST}/api/v1/test"

            val request = Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY)
                .addHeader("x-rapidapi-host", BuildConfig.RAPIDAPI_TRUECALLER_HOST)
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            Log.d(TAG, "Test: ${if (success) "SUCCESS" else "FAILED (${response.code})"}")
            return@withContext success

        } catch (e: Exception) {
            Log.e(TAG, "Test failed", e)
            return@withContext false
        }
    }

    suspend fun getCallerInfo(phoneNumber: String, countryCode: String = "US"): CallerInfo? = withContext(Dispatchers.IO) {
        try {
            callerCache[phoneNumber]?.let {
                Log.d(TAG, "Returning cached info for $phoneNumber")
                return@withContext it
            }

            if (BuildConfig.RAPIDAPI_KEY.isEmpty()) {
                Log.e(TAG, "RapidAPI key not configured")
                return@withContext null
            }

            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val encodedNumber = URLEncoder.encode(cleanNumber, "UTF-8")
            val apiUrl = "https://${BuildConfig.RAPIDAPI_TRUECALLER_HOST}/api/v1/search?phone=$encodedNumber&countryCode=$countryCode"

            val request = Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY)
                .addHeader("x-rapidapi-host", BuildConfig.RAPIDAPI_TRUECALLER_HOST)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Lookup failed: ${response.code}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val truecallerResponse = gson.fromJson(responseBody, TruecallerResponse::class.java)
            val data = truecallerResponse.data?.firstOrNull() ?: return@withContext null

            val callerInfo = CallerInfo(
                phoneNumber = cleanNumber,
                name = data.name,
                image = data.image,
                isSpam = (data.spamInfo?.spamScore ?: 0) > 50,
                spamScore = data.spamInfo?.spamScore ?: 0,
                location = data.addresses?.firstOrNull()?.city,
                carrier = data.phones?.firstOrNull()?.carrier
            )

            callerCache[phoneNumber] = callerInfo
            Log.d(TAG, "Found: ${callerInfo.name} (spam: ${callerInfo.isSpam})")
            return@withContext callerInfo

        } catch (e: Exception) {
            Log.e(TAG, "Error looking up caller", e)
            return@withContext null
        }
    }

    suspend fun isSpamNumber(phoneNumber: String, countryCode: String = "US"): Boolean {
        val info = getCallerInfo(phoneNumber, countryCode)
        return info?.isSpam ?: false
    }

    fun clearCache() {
        callerCache.clear()
        Log.d(TAG, "Cache cleared")
    }

    fun getCachedCallerInfo(phoneNumber: String): CallerInfo? {
        return callerCache[phoneNumber]
    }
}
