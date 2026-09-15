package com.animame.editor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream

/** Independent reader for the observed ibisPaint IPBZ framing. */
object IpbzPayloadReader {
    data class Record(
        val raw: ByteArray,
        val header: String?,
        val version: Int?,
        val inflated: ByteArray?
    )

    data class Chunk(
        val id: Long,
        val raw: ByteArray,
        val known: Boolean
    )

    data class Result(
        val magic: String?,
        val version: Int?,
        val records: List<Record>,
        val chunks: List<Chunk>,
        val unknownChunks: List<Chunk>,
        val inflatedPayload: ByteArray?
    )

    private const val BRUSH_CHUNK_ID = 0x01000202L

    fun read(data: ByteArray): Result {
        require(data.isNotEmpty()) { "Empty IPBZ payload" }
        val records = mutableListOf<Record>()
        var offset = 0
        var magic: String? = null
        var version: Int? = null
        var bestInflated: ByteArray? = null

        while (offset + 8 <= data.size) {
            val length = readLong(data, offset)
            offset += 8
            if (length <= 0 || length > data.size - offset) break
            val end = offset + length.toInt()
            val raw = data.copyOfRange(offset, end)
            offset = end
            val header = raw.takeIf { it.size >= 4 }?.let { String(it, 0, 4, Charsets.UTF_8) }
            val localVersion = if (header.equals("IPBZ", true) && raw.size >= 8) readInt(raw, 4) else null
            if (magic == null && header.equals("IPBZ", true)) magic = header
            if (version == null && localVersion != null) version = localVersion
            val inflated = inflate(raw)
            if (inflated != null && inflated.isNotEmpty()) bestInflated = inflated
            records += Record(raw, header, localVersion, inflated)
        }

        if (records.isEmpty()) {
            val header = if (data.size >= 4) String(data, 0, 4, Charsets.UTF_8) else null
            val localVersion = if (header.equals("IPBZ", true) && data.size >= 8) readInt(data, 4) else null
            val inflated = inflate(data)
            records += Record(data.copyOf(), header, localVersion, inflated)
            magic = header
            version = localVersion
            bestInflated = inflated ?: bestInflated
        }

        val chunks = parseChunks(bestInflated ?: ByteArray(0))
        val unknown = chunks.filterNot { it.known }
        return Result(magic, version, records, chunks, unknown, bestInflated)
    }

    private fun inflate(data: ByteArray): ByteArray? = try {
        InflaterInputStream(ByteArrayInputStream(data)).use { input ->
            val output = ByteArrayOutputStream()
            input.copyTo(output)
            output.toByteArray()
        }
    } catch (_: Throwable) {
        null
    }

    private fun parseChunks(data: ByteArray): List<Chunk> {
        if (data.isEmpty()) return emptyList()
        val result = mutableListOf<Chunk>()
        var offset = 0
        while (offset + 8 <= data.size) {
            val id = readInt(data, offset).toLong() and 0xFFFFFFFFL
            val length = readInt(data, offset + 4).toLong() and 0xFFFFFFFFL
            offset += 8
            if (length > data.size - offset) break
            val payload = data.copyOfRange(offset, offset + length.toInt())
            offset += length.toInt()
            result += Chunk(id, payload, id == BRUSH_CHUNK_ID)
        }
        return result
    }

    private fun readInt(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)

    private fun readLong(data: ByteArray, offset: Int): Long {
        var value = 0L
        repeat(8) { value = (value shl 8) or (data[offset + it].toLong() and 0xFF) }
        return value
    }
}
