package com.beconnect.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import com.beconnect.BleConstants

data class BeaconInfo(
    val address: String,
    val rssi: Int,
    val lastSeen: Long,
    val severityByte: Byte   // parsed from manufacturer data; 0 = unknown
)

/**
 * Scans for BeConnect gateway advertisements filtered by SERVICE_UUID.
 * Results are deduplicated by device address; [onBeaconsUpdated] fires on every update.
 *
 * Location services must be enabled on the device (even with neverForLocation flag on SDK 31+).
 */
class BleScanner(private val adapter: BluetoothAdapter) {

    private var scanCallback: ScanCallback? = null
    private val beacons = mutableMapOf<String, BeaconInfo>()

    var onBeaconsUpdated: ((List<BeaconInfo>) -> Unit)? = null

    fun start() {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val addr = result.device.address
                val mfr = result.scanRecord?.getManufacturerSpecificData(BleConstants.MANUFACTURER_ID)
                val sev = mfr?.getOrElse(0) { 0 } ?: 0
                beacons[addr] = BeaconInfo(addr, result.rssi, System.currentTimeMillis(), sev.toByte())
                onBeaconsUpdated?.invoke(beacons.values.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed: $errorCode")
            }
        }

        adapter.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback!!)
            ?: Log.e(TAG, "BluetoothLeScanner unavailable — Bluetooth may be off")
    }

    fun stop() {
        scanCallback?.let { adapter.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
    }

    companion object { private const val TAG = "BleScanner" }
}
