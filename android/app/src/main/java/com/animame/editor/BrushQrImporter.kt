package com.animame.editor

/** Import facade used by the UI. Binary records remain available for lossless future decoding. */
object BrushQrImporter {
    data class Result(
        val settings: BrushSettings?,
        val codec: BrushQrCodec.ImportResult,
        val ipbz: IpbzPayloadReader.Result,
        val rawPayload: ByteArray,
        val warning: String? = null
    )

    fun importBytes(data: ByteArray, suggestedName: String = "Imported Brush"): Result {
        val decoded = BrushQrCodec.decode(data)
        val ipbz = IpbzPayloadReader.read(data)
        val id = "imported_${ipbz.magic ?: "brush"}_${data.contentHashCode()}"
        val base = BrushDefaults.forPreset(id).copy(id = id, name = suggestedName, category = "Imported")
        val warning = when {
            !ipbz.magic.equals("IPBZ", true) -> "QR lido, mas o cabeçalho IPBZ não foi validado"
            ipbz.version == null -> "IPBZ detectado, mas a versão não foi encontrada"
            ipbz.inflatedPayload == null -> "IPBZ detectado, mas a carga zlib não pôde ser descomprimida"
            ipbz.chunks.none { it.known } -> "IPBZ descomprimido, mas o chunk de parâmetros 0x01000202 não foi encontrado"
            else -> null
        }
        return Result(base, decoded, ipbz, data.copyOf(), warning)
    }
}
