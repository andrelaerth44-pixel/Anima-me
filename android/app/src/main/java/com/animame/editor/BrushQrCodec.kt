package com.animame.editor

import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream

/** Independent IPBZ-compatible framing reader. Unknown records are preserved so imports can be lossless. */
object BrushQrCodec {
    data class Record(val raw: ByteArray, val header: String?, val inflated: ByteArray?)
    data class ImportResult(val records: List<Record>, val magic: String?, val version: Int?, val payload: ByteArray?)

    fun decode(data: ByteArray): ImportResult {
        require(data.isNotEmpty()) { "Empty brush QR payload" }
        var offset=0; var magic:String?=null; var version:Int?=null; var payload:ByteArray?=null; val records=mutableListOf<Record>()
        while(offset+8<=data.size) {
            val len=readLongBE(data,offset); offset+=8
            if(len<0 || len>data.size-offset) break
            val raw=data.copyOfRange(offset,offset+len.toInt()); offset+=len.toInt()
            val header=if(raw.size>=4) String(raw,0,4,Charsets.UTF_8) else null
            if(magic==null && header!=null && header.all{it.code in 32..126}) magic=header
            if(version==null && raw.size>=8 && header!=null && header.equals("IPBZ",true)) version=readIntBE(raw,4)
            var inflated:ByteArray?=null
            try { inflated=InflaterInputStream(ByteArrayInputStream(raw)).readBytes(); if(inflated.isNotEmpty()) payload=inflated } catch(_:Throwable) { }
            records += Record(raw,header,inflated)
        }
        return ImportResult(records,magic,version,payload)
    }

    private fun readIntBE(b:ByteArray,o:Int)=((b[o].toInt() and 255) shl 24) or ((b[o+1].toInt() and 255) shl 16) or ((b[o+2].toInt() and 255) shl 8) or (b[o+3].toInt() and 255)
    private fun readLongBE(b:ByteArray,o:Int):Long { var v=0L; repeat(8){v=(v shl 8) or (b[o+it].toLong() and 255)}; return v }
}
