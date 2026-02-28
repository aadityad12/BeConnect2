package com.beconnect.demo

import com.beconnect.data.AlertPacket
import java.time.Instant

/** Hardcoded fallback alerts used when the NWS API is unreachable (demo mode). */
object DemoAlerts {

    val alerts: List<AlertPacket> = listOf(
        AlertPacket(
            alertId = "demo_001",
            severity = "Extreme",
            headline = "Tornado Warning issued for Central County until 8:45 PM",
            expires = Instant.now().epochSecond + 3600,
            instructions = "Take shelter immediately in a sturdy building. Go to the lowest " +
                "floor, interior room. Stay away from windows. Do not attempt to drive away.",
            sourceUrl = "https://www.weather.gov",
            verified = false,
            fetchedAt = Instant.now().epochSecond
        ),
        AlertPacket(
            alertId = "demo_002",
            severity = "Severe",
            headline = "Flash Flood Watch in effect through tomorrow morning",
            expires = Instant.now().epochSecond + 14400,
            instructions = "Do not drive through flooded roads. Turn Around, Don't Drown. " +
                "Move to higher ground if you are near streams or low-lying areas.",
            sourceUrl = "https://www.weather.gov",
            verified = false,
            fetchedAt = Instant.now().epochSecond
        )
    )
}
