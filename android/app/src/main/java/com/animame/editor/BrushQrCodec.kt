package com.animame.editor

import java.util.zip.Inflater

/**
 * Lossless IPBZ/QR framing reader. It deliberately does not guess unknown brush fields.
 * The decompressed payload is exposed for the dedicated IPBZ BrushParameter parser.
 */
object BrushQrCodec {
    private const val MAX_INPUT_BYTES = 256 * 1024
    private const val MAX_INFLATED_BYTES = 4 * 1024 * 1024
    private const val BRUSH_PARAMETER_SUBCHUNK = 0x202

    data class Record(
        val raw: ByteArray,
        val header: String?,
        val inflated: ByteArray?,
        val compression: Compression
    )

    enum class Compression { NONE, ZLIB, DEFLATE_RAW }

    data class ImportResult(
        val records: List<Record>,
        val magic: String?,
        val version: Int?,
        val payload: ByteArray?,
        val brushParameterSubChunkSeen: Boolean
    )

    fun decode(data: ByteArray): ImportResult {
        require(data.isNotEmpty()) { "Empty brush QR payload" }
        require(data.size <= MAX_INPUT_BYTES) { "Brush QR payload too large" }

        if (hasMagic(data, "IPBZ")) {
            val version = if (data.size >= 8) readIntBE(data, 4) else null
            val body = data.copyOfRange(8.coerceAtMost(data.size), data.size)
            val candidates = inflateCandidates(body)
            val best = candidates.maxByOrNull { it.second?.size ?: 0 }
            val payload = best?.second
            return ImportResult(
                records = listOf(Record(data.copyOf(), "IPBZ", payload, best?.first ?: Compression.NONE)),
                magic = "IPBZ",
                version = version,
                payload = payload,
                brushParameterSubChunkSeen = payload?.containsInt(BRUSH_PARAMETER_SUBCHUNK) == true
            )
        }

        var offset = 0
        var magic: String? = null
        var version: Int? = null
        var payload: ByteArray? = null
        var compression = Compression.NONE
        var brushChunk = false
        val records = mutableListOf<Record>()

        while (offset + 8 <= data.size) {
            val len = readLongBE(data, offset)
            offset += 8
            if (len < 0 || len > data.size - offset) break
            val size = len.toInt()
            val raw = data.copyOfRange(offset, offset + size)
            offset += size
            val header = if (raw.size >= 4) String(raw, 0, 4, Charsets.ISO_8859_1) else null
            if (magic == null && header.equals("IPBZ", true)) magic = "IPBZ"
            if (version == null && header.equals("IPBZ", true) && raw.size >= 8) version = readIntBE(raw, 4)

            val candidates = inflateCandidates(raw)
            val best = candidates.maxByOrNull { it.second?.size ?: 0 }
            val inflated = best?.second
            if (inflated != null && inflated.isNotEmpty() && (payload == null || inflated.size > payload!!.size)) {
                payload = inflated
                compression = best.first
            }
            brushChunk = brushChunk || raw.containsInt(BRUSH_PARAMETER_SUBCHUNK) || inflated?.containsInt(BRUSH_PARAMETER_SUBCHUNK) == true
            records += Record(raw, header, inflated, best?.first ?: Compression.NONE)
        }

        return ImportResult(records, magic, version, payload, brushChunk)
    }

    private fun inflateCandidates(data: ByteArray): List<Pair<Compression, ByteArray?>> {
        if (data.isEmpty()) return listOf(Compression.NONE to null)
        val zlib = inflate(data, nowrap = false)
        val raw = inflate(data, nowrap = true)
        return listOf(
            Compression.ZLIB to zlib,
            Compression.DEFLATE_RAW to raw,
            Compression.NONE to if (zlib == null && raw == null) data.copyOf() else null
        )
    }

    private fun inflate(data: ByteArray, nowrap: Boolean): ByteArray? = runCatching {
        val inflater = Inflater(nowrap)
        inflater.setInput(data)
        val output = LimitedBuffer(MAX_INFLATED_BYTES)
        val buffer = ByteArray(16 * 1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count > 0) {
                output.write(buffer, count)
                continue
            }
            if (inflater.needsDictionary() || inflater.needsInput()) return@runCatching null
        }
        inflater.end()
        output.toByteArray()
    }.getOrNull()

    private fun hasMagic(data: ByteArray, magic: String): Boolean =
        data.size >= 4 && data[0] == magic[0].code.toByte() && data[1] == magic[1].code.toByte() &&
            data[2] == magic[2].code.toByte() && data[3] == magic[3].code.toByte()

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

    private fun ByteArray.containsInt(value: Int): Boolean {
        val be = byteArrayOf(
            (value ushr 24).toByte(), (value ushr 16).toByte(),
            (value ushr 8).toByte(), value.toByte()
        )
        val le = byteArrayOf(value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte())
        for (i in 0..size - 4) {
            if (this[i] == be[0] && this[i + 1] == be[1] && this[i + 2] == be[2] && this[i + 3] == be[3]) return true
            if (this[i] == le[0] && this[i + 1] == le[1] && this[i + 2] == le[2] && this[i + 3] == le[3]) return true
        }
        return false
    }

    private class LimitedBuffer(private val limit: Int) {
        private var data = ByteArray(16 * 1024)
        private var size = 0

        fun write(bytes: ByteArray, length: Int) {
            require(length >= 0 && size + length <= limit) { "Inflated brush QR payload exceeds limit" }
            ensure(size + length)
            bytes.copyInto(data, size, 0, length)
            size += length
        }

        private fun ensure(required: Int) {
            if (required <= data.size) return
            var next = data.size
            while (next < required) next = (next * 2).coerceAtMost(limit)
            data = data.copyOf(next)
        }

        fun toByteArray(): ByteArray = data.copyOf(size)
    }
}
