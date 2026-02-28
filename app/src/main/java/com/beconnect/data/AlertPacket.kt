package com.beconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertPacket(
    @PrimaryKey val alertId: String,
    val severity: String,      // "Extreme" | "Severe" | "Moderate" | "Minor" | "Unknown"
    val headline: String,
    val expires: Long,         // Unix epoch seconds
    val instructions: String,
    val sourceUrl: String,
    val verified: Boolean,     // true = fetched from official NWS source
    val fetchedAt: Long        // Unix epoch seconds
)
