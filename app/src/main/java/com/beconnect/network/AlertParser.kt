package com.beconnect.network

import com.beconnect.data.AlertPacket
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant

object AlertParser {

    /** Parse NWS GeoJSON `/alerts/active` response into AlertPackets */
    fun parseGeoJson(json: String): List<AlertPacket> {
        val features = JSONObject(json).getJSONArray("features")
        val now = Instant.now().epochSecond
        return (0 until features.length()).mapNotNull { i ->
            runCatching {
                val props = features.getJSONObject(i).getJSONObject("properties")
                val headline = props.optString("headline", "Emergency Alert")
                val expiresStr = props.optString("expires", "")
                val expires = if (expiresStr.isNotEmpty()) Instant.parse(expiresStr).epochSecond else now + 3600
                AlertPacket(
                    alertId = sha1("$headline$expires").take(8),
                    severity = props.optString("severity", "Unknown"),
                    headline = headline,
                    expires = expires,
                    instructions = props.optString("instruction", "Follow official guidance."),
                    sourceUrl = props.optString("@id", "https://www.weather.gov"),
                    verified = true,
                    fetchedAt = now
                )
            }.getOrNull()
        }
    }

    private fun sha1(input: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
