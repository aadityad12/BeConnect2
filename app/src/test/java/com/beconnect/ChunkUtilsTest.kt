package com.beconnect

import com.beconnect.ble.ChunkUtils
import org.junit.Assert.*
import org.junit.Test

class ChunkUtilsTest {

    @Test
    fun `roundtrip small payload`() {
        val original = "Hello, BeConnect!".toByteArray()
        val chunks = ChunkUtils.chunk(original, 5)
        val reassembled = ChunkUtils.reassemble(chunks)
        assertArrayEquals(original, reassembled)
    }

    @Test
    fun `roundtrip large payload`() {
        val original = ByteArray(1500) { it.toByte() }
        val chunks = ChunkUtils.chunk(original, 17)
        val reassembled = ChunkUtils.reassemble(chunks)
        assertArrayEquals(original, reassembled)
    }

    @Test
    fun `encode then decode frame`() {
        val payload = "test".toByteArray()
        val frame = ChunkUtils.encodeChunk(3, 10, payload)
        assertEquals(3, ChunkUtils.decodeChunkIndex(frame))
        assertEquals(10, ChunkUtils.decodeTotalChunks(frame))
        assertArrayEquals(payload, ChunkUtils.decodePayload(frame))
    }

    @Test
    fun `chunk count is correct`() {
        val data = ByteArray(50)
        val chunks = ChunkUtils.chunk(data, 17)
        assertEquals(3, chunks.size) // ceil(50/17) = 3
    }

    @Test
    fun `single byte payload`() {
        val original = byteArrayOf(42)
        val chunks = ChunkUtils.chunk(original, 17)
        assertEquals(1, chunks.size)
        assertArrayEquals(original, ChunkUtils.reassemble(chunks))
    }
}
