package com.beconnect.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.beconnect.BleConstants
import com.beconnect.data.AlertPacket
import com.google.gson.Gson

/**
 * GATT server that serves the full alert packet to connecting receivers.
 *
 * Protocol (request/response per chunk):
 *   1. Receiver writes [chunkIndex: 2 bytes] to CONTROL_CHAR.
 *   2. Receiver reads ALERT_CHAR → server responds with encodeChunk(index, total, payload).
 *   3. Repeat until all chunks fetched.
 *
 * chunkSize defaults to DEFAULT_CHUNK_SIZE (safe before MTU negotiation). The GATT framework
 * will trim responses to the negotiated MTU automatically via sendResponse.
 */
class GattServer(private val context: Context) {

    private var server: BluetoothGattServer? = null
    private var chunks: List<ByteArray> = emptyList()
    private var requestedChunkIndex = 0

    fun start(alert: AlertPacket) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val raw = Gson().toJson(alert).toByteArray(Charsets.UTF_8)
        chunks = ChunkUtils.chunk(raw, BleConstants.DEFAULT_CHUNK_SIZE)
        Log.d(TAG, "Serving ${raw.size} bytes in ${chunks.size} chunks")

        val alertChar = BluetoothGattCharacteristic(
            BleConstants.ALERT_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val controlChar = BluetoothGattCharacteristic(
            BleConstants.CONTROL_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val service = BluetoothGattService(
            BleConstants.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).apply {
            addCharacteristic(alertChar)
            addCharacteristic(controlChar)
        }

        server = manager.openGattServer(context, object : BluetoothGattServerCallback() {

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice, requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean, responseNeeded: Boolean,
                offset: Int, value: ByteArray
            ) {
                if (characteristic.uuid == BleConstants.CONTROL_CHAR_UUID && value.size >= 2) {
                    requestedChunkIndex = ((value[0].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
                    Log.d(TAG, "Chunk request: $requestedChunkIndex / ${chunks.size}")
                }
                if (responseNeeded) {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice, requestId: Int, offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == BleConstants.ALERT_CHAR_UUID) {
                    val idx = requestedChunkIndex
                    val payload = chunks.getOrElse(idx) { ByteArray(0) }
                    val frame = ChunkUtils.encodeChunk(idx, chunks.size, payload)
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, frame)
                }
            }

            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                Log.d(TAG, "Client ${device.address} state → $newState")
            }
        })

        server?.addService(service)
    }

    fun stop() {
        server?.close()
        server = null
    }

    companion object { private const val TAG = "GattServer" }
}
