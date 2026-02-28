package com.beconnect.service

import android.app.*
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.beconnect.R
import com.beconnect.ble.BleAdvertiser
import com.beconnect.ble.GattServer
import com.beconnect.data.AlertPacket
import com.google.gson.Gson

/**
 * Keeps BLE advertising and GATT server alive in the background.
 * Must be started via [start]; stopped via [stop].
 */
class GatewayForegroundService : Service() {

    private lateinit var advertiser: BleAdvertiser
    private lateinit var gattServer: GattServer

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val json = intent.getStringExtra(EXTRA_ALERT_JSON)
        if (json == null) { stopSelf(); return START_NOT_STICKY }
        val alert = Gson().fromJson(json, AlertPacket::class.java)

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        advertiser = BleAdvertiser(manager.adapter)
        gattServer = GattServer(this)

        gattServer.start(alert)
        advertiser.start(alert) { err -> Log.e(TAG, "Advertise error: $err") }

        startForeground(NOTIF_ID, buildNotification(alert.headline))
        Log.d(TAG, "Gateway service started for: ${alert.headline}")
        return START_STICKY
    }

    override fun onDestroy() {
        advertiser.stop()
        gattServer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(headline: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Gateway Broadcasting", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BeConnect — Broadcasting")
            .setContentText(headline)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_ALERT_JSON = "alert_json"
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "beconnect_gateway"
        private const val TAG = "GatewayService"

        fun start(context: Context, alert: AlertPacket) {
            context.startForegroundService(
                Intent(context, GatewayForegroundService::class.java)
                    .putExtra(EXTRA_ALERT_JSON, Gson().toJson(alert))
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GatewayForegroundService::class.java))
        }
    }
}
