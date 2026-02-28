package com.beconnect.ble

/**
 * Frame format for GATT chunk transfer:
 *   [chunkIndex: 2 bytes big-endian][totalChunks: 2 bytes big-endian][payload: N bytes]
 *
 * Protocol:
 *   1. Receiver writes desired chunk index to CONTROL_CHAR.
 *   2. Receiver reads ALERT_CHAR → receives encoded frame for that chunk.
 *   3. Repeat until all chunks received.
 */
object ChunkUtils {

    fun chunk(data: ByteArray, chunkSize: Int): List<ByteArray> {
        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val end = minOf(offset + chunkSize, data.size)
            chunks.add(data.copyOfRange(offset, end))
            offset = end
        }
        return chunks
    }

    fun reassemble(chunks: List<ByteArray>): ByteArray {
        val result = ByteArray(chunks.sumOf { it.size })
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }

    fun encodeChunk(index: Int, total: Int, payload: ByteArray): ByteArray {
        val buf = ByteArray(4 + payload.size)
        buf[0] = (index shr 8).toByte()
        buf[1] = index.toByte()
        buf[2] = (total shr 8).toByte()
        buf[3] = total.toByte()
        payload.copyInto(buf, 4)
        return buf
    }

    fun decodeChunkIndex(frame: ByteArray): Int =
        ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)

    fun decodeTotalChunks(frame: ByteArray): Int =
        ((frame[2].toInt() and 0xFF) shl 8) or (frame[3].toInt() and 0xFF)

    fun decodePayload(frame: ByteArray): ByteArray =
        frame.copyOfRange(4, frame.size)
}
