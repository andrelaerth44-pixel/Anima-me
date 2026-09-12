package com.animame.editor

import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream

/** Independent IPBZ-compatible framing reader. Unknown records are preserved for imports. */
object BrushQrCodec {
    data class Record(val raw: ByteArray, val header: String?, val inflated: ByteArray?)
    data class ImportResult(
        val records: List<Record>,
        val magic: String?,
        val version: Int?,
        val payload: ByteArray?,
        val raw: ByteArray
    )

    fun decode(data: ByteArray): ImportResult {
        require(data.isNotEmpty()) { "Empty brush QR payload" }
        val records = mutableListOf<Record>()
        var magic: String? = null
        var version: Int? = null
        var payload: ByteArray? = null

        // Some QR readers expose the IPBZ stream directly, while the native parser
        // can also receive the outer length-prefixed framing. Accept both forms.
        if (data.size >= 8 && ascii(data, 0, 4).equals("IPBZ", true)) {
            magic = ascii(data, 0, 4)
            version = readIntBE(data, 4)
            val body = data.copyOfRange(8, data.size)
            val inflated = inflate(body)
            payload = inflated ?: body.takeIf { it.isNotEmpty() }
            records += Record(data.copyOf(), magic, inflated)
            return ImportResult(records, magic, version, payload, data.copyOf())
        }

        var offset = 0
        while (offset + 8 <= data.size) {
            val len = readLongBE(data, offset)
            offset += 8
            if (len < 0 || len > data.size - offset) break
            val raw = data.copyOfRange(offset, offset + len.toInt())
            offset += len.toInt()
            val header = if (raw.size >= 4) ascii(raw, 0, 4) else null
            if (magic == null && header != null && header.all { it.code in 32..126 }) magic = header
            if (version == null && raw.size >= 8 && header.equals("IPBZ", true)) version = readIntBE(raw, 4)
            val inflated = inflate(raw)
            if (inflated != null && inflated.isNotEmpty()) payload = inflated
            records += Record(raw, header, inflated)
        }
        return ImportResult(records, magic, version, payload, data.copyOf())
    }

    private fun inflate(data: ByteArray): ByteArray? = try {
        InflaterInputStream(ByteArrayInputStream(data)).readBytes().takeIf { it.isNotEmpty() }
    } catch (_: Throwable) {
        null
    }

    private fun ascii(data: ByteArray, offset: Int, length: Int): String =
        data.copyOfRange(offset, offset + length).toString(Charsets.US_ASCII)

    private fun readIntBE(b: ByteArray, o: Int) =
        ((b[o].toInt() and 255) shl 24) or
            ((b[o + 1].toInt() and 255) shl 16) or
            ((b[o + 2].toInt() and 255) shl 8) or
            (b[o + 3].toInt() and 255)

    private fun readLongBE(b: ByteArray, o: Int): Long {
        var v = 0L
        repeat(8) { v = (v shl 8) or (b[o + it].toLong() and 255) }
        return v
    }
}
