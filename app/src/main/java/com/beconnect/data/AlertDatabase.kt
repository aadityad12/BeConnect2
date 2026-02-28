package com.beconnect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlertPacket::class], version = 1, exportSchema = false)
abstract class AlertDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile private var INSTANCE: AlertDatabase? = null

        fun getInstance(context: Context): AlertDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlertDatabase::class.java,
                    "beconnect.db"
                ).build().also { INSTANCE = it }
            }
    }
}
