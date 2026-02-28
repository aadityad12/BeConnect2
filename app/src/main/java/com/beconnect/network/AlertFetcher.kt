package com.beconnect.network

import com.beconnect.data.AlertPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object AlertFetcher {

    private const val NWS_URL =
        "https://api.weather.gov/alerts/active?status=actual&message_type=alert&limit=5"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatest(): Result<List<AlertPacket>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(NWS_URL)
                // NWS requires a descriptive User-Agent
                .header("User-Agent", "BeConnect/1.0 (hackathon@beconnect.app)")
                .header("Accept", "application/geo+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) error("HTTP ${response.code}: ${response.message}")
            val body = response.body?.string() ?: error("Empty response body")
            AlertParser.parseGeoJson(body)
        }
    }
}
