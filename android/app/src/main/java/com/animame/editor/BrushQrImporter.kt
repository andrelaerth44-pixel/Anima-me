package com.animame.editor

/** Import facade used by the UI. QR decoding itself can be supplied by ZXing/ML Kit; this class keeps the IPBZ parser independent. */
object BrushQrImporter {
    data class Result(val settings: BrushSettings?, val codec: BrushQrCodec.ImportResult, val warning: String? = null)
    fun importBytes(data: ByteArray, suggestedName: String = "Imported Brush"): Result {
        val decoded=BrushQrCodec.decode(data)
        val id="imported_${decoded.magic ?: "brush"}_${data.contentHashCode()}"
        val base=BrushDefaults.forPreset(id).copy(id=id,name=suggestedName,category="Imported")
        val warning=if(decoded.magic?.equals("IPBZ",true)==true) null else "QR payload read, but no validated IPBZ magic was found"
        return Result(base,decoded,warning)
    }
}
