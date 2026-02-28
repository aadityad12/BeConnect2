package com.beconnect.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import android.util.Log
import com.beconnect.BleConstants
import com.beconnect.data.AlertPacket
import java.nio.ByteBuffer

/**
 * Starts BLE advertising so receivers can discover this gateway.
 *
 * Advertisement payload (≤ 9 bytes of manufacturer data):
 *   [severity: 1 byte][alertId hash: 4 bytes][fetchedAt truncated: 4 bytes]
 *
 * Check [isSupported] before calling [start] — not all Android devices support peripheral mode.
 * Emulators do NOT support BLE advertising; physical devices required.
 */
class BleAdvertiser(private val adapter: BluetoothAdapter) {

    private var callback: AdvertiseCallback? = null

    val isSupported: Boolean get() = adapter.isMultipleAdvertisementSupported

    fun start(alert: AlertPacket, onError: (String) -> Unit) {
        if (!isSupported) {
            onError("BLE peripheral advertising not supported on this device")
            return
        }

        val meta = ByteBuffer.allocate(9).apply {
            put(severityByte(alert.severity))
            val hash = alert.alertId.hashCode()
            put(hash.toByte())
            put((hash shr 8).toByte())
            put((hash shr 16).toByte())
            put((hash shr 24).toByte())
            putInt(alert.fetchedAt.toInt())
        }.array()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .addManufacturerData(BleConstants.MANUFACTURER_ID, meta)
            .setIncludeDeviceName(false)
            .build()

        callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d(TAG, "Advertising started successfully")
            }
            override fun onStartFailure(errorCode: Int) {
                onError("BLE advertise failed with error code $errorCode")
            }
        }

        adapter.bluetoothLeAdvertiser?.startAdvertising(settings, data, callback!!)
            ?: onError("BluetoothLeAdvertiser unavailable")
    }

    fun stop() {
        callback?.let { adapter.bluetoothLeAdvertiser?.stopAdvertising(it) }
        callback = null
    }

    private fun severityByte(severity: String): Byte = when (severity) {
        "Extreme" -> 4; "Severe" -> 3; "Moderate" -> 2; "Minor" -> 1; else -> 0
    }.toByte()

    companion object { private const val TAG = "BleAdvertiser" }
}
