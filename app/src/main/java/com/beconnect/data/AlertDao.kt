package com.beconnect.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY fetchedAt DESC LIMIT 20")
    fun getAllAlerts(): Flow<List<AlertPacket>>

    @Query("SELECT * FROM alerts WHERE alertId = :id")
    suspend fun getById(id: String): AlertPacket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertPacket)

    /** Keeps the 20 most recent alerts, deletes the rest */
    @Query("DELETE FROM alerts WHERE alertId NOT IN (SELECT alertId FROM alerts ORDER BY fetchedAt DESC LIMIT 20)")
    suspend fun pruneOldAlerts()
}
