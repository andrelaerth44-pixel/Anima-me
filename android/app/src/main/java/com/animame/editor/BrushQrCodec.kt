package com.animame.editor

import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream

/** Lossless framing reader for brush QR payloads; unknown bytes are always preserved. */
object BrushQrCodec {
    data class Record(val raw: ByteArray, val header: String?, val inflated: ByteArray?)
    data class ImportResult(val records: List<Record>, val magic: String?, val version: Int?, val payload: ByteArray?)

    fun decode(data: ByteArray): ImportResult {
        require(data.isNotEmpty()) { "Empty brush QR payload" }
        val directMagic = data.size >= 4 && data[0] == 'I'.code.toByte() && data[1] == 'P'.code.toByte() && data[2] == 'B'.code.toByte() && data[3] == 'Z'.code.toByte()
        if (directMagic) {
            val version = if (data.size >= 8) readIntBE(data, 4) else null
            val inflated = inflate(data.copyOfRange(8.coerceAtMost(data.size), data.size))
            return ImportResult(listOf(Record(data.copyOf(), "IPBZ", inflated)), "IPBZ", version, inflated)
        }

        var offset = 0
        var magic: String? = null
        var version: Int? = null
        var payload: ByteArray? = null
        val records = mutableListOf<Record>()
        while (offset + 8 <= data.size) {
            val len = readLongBE(data, offset)
            offset += 8
            if (len < 0 || len > data.size - offset) break
            val raw = data.copyOfRange(offset, offset + len.toInt())
            offset += len.toInt()
            val header = if (raw.size >= 4) String(raw, 0, 4, Charsets.ISO_8859_1) else null
            if (magic == null && header != null && header.all { it.code in 32..126 }) magic = header
            if (version == null && header.equals("IPBZ", true) && raw.size >= 8) version = readIntBE(raw, 4)
            val inflated = inflate(raw)
            if (inflated != null && inflated.isNotEmpty()) payload = inflated
            records += Record(raw, header, inflated)
        }
        return ImportResult(records, magic, version, payload)
    }

    private fun inflate(data: ByteArray): ByteArray? = runCatching {
        InflaterInputStream(ByteArrayInputStream(data)).readBytes()
    }.getOrNull()

    private fun readIntBE(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 255) shl 24) or
            ((b[o + 1].toInt() and 255) shl 16) or
            ((b[o + 2].toInt() and 255) shl 8) or
            (b[o + 3].toInt() and 255)

    private fun readLongBE(b: ByteArray, o: Int): Long {
        var value = 0L
        repeat(8) { value = (value shl 8) or (b[o + it].toLong() and 255) }
        return value
    }
}
