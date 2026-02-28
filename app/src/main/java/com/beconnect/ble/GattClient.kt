package com.beconnect.ble

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.beconnect.BleConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.UUID

/**
 * GATT client that connects to a gateway and downloads the full alert packet.
 *
 * Steps:
 *   1. connectGatt → wait for STATE_CONNECTED
 *   2. requestMtu(512) → wait for onMtuChanged
 *   3. discoverServices → wait for onServicesDiscovered
 *   4. For each chunk index: write index to CONTROL_CHAR, then read ALERT_CHAR
 *   5. Reassemble and return raw bytes
 *
 * GATT_ERROR 133 on first connect is common — retry once after 600ms (handled by caller).
 */
class GattClient(private val context: Context) {

    private var gatt: BluetoothGatt? = null
    private val readChannel = Channel<ByteArray?>(Channel.BUFFERED)
    private val connectionReady = CompletableDeferred<Unit>()
    private var writeAck = CompletableDeferred<Boolean>()

    suspend fun downloadAlert(device: BluetoothDevice): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                connect(device)

                val alertChar = requireChar(BleConstants.ALERT_CHAR_UUID)
                val controlChar = requireChar(BleConstants.CONTROL_CHAR_UUID)

                val chunks = mutableMapOf<Int, ByteArray>()
                var totalChunks = -1
                var index = 0

                while (true) {
                    requestChunk(controlChar, alertChar, index)
                    val frame = readChannel.receive() ?: error("Null read at chunk $index")
                    val chunkIdx = ChunkUtils.decodeChunkIndex(frame)
                    totalChunks = ChunkUtils.decodeTotalChunks(frame)
                    chunks[chunkIdx] = ChunkUtils.decodePayload(frame)
                    Log.d(TAG, "Received chunk $chunkIdx / $totalChunks")
                    index++
                    if (chunks.size == totalChunks) break
                }

                val ordered = (0 until totalChunks).map { chunks[it] ?: error("Missing chunk $it") }
                ChunkUtils.reassemble(ordered)
            }.also { disconnect() }
        }

    private suspend fun connect(device: BluetoothDevice) {
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    g.requestMtu(512)
                } else if (!connectionReady.isCompleted) {
                    connectionReady.completeExceptionally(Exception("GATT connect failed: status=$status"))
                }
            }

            override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                Log.d(TAG, "MTU negotiated: $mtu")
                g.discoverServices()
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) connectionReady.complete(Unit)
                else connectionReady.completeExceptionally(Exception("Service discovery failed: $status"))
            }

            override fun onCharacteristicRead(
                g: BluetoothGatt, char: BluetoothGattCharacteristic, value: ByteArray, status: Int
            ) {
                readChannel.trySend(if (status == BluetoothGatt.GATT_SUCCESS) value else null)
            }

            override fun onCharacteristicWrite(
                g: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int
            ) {
                writeAck.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }

        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        connectionReady.await()
    }

    private suspend fun requestChunk(
        controlChar: BluetoothGattCharacteristic,
        alertChar: BluetoothGattCharacteristic,
        index: Int
    ) {
        writeAck = CompletableDeferred()
        val req = byteArrayOf((index shr 8).toByte(), index.toByte())
        @Suppress("DEPRECATION")
        controlChar.value = req
        gatt?.writeCharacteristic(controlChar, req, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        writeAck.await()
        gatt?.readCharacteristic(alertChar)
    }

    private fun requireChar(uuid: UUID): BluetoothGattCharacteristic =
        gatt?.getService(BleConstants.SERVICE_UUID)?.getCharacteristic(uuid)
            ?: error("Characteristic $uuid not found")

    private fun disconnect() {
        gatt?.close()
        gatt = null
    }

    companion object { private const val TAG = "GattClient" }
}
