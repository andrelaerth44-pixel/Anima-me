package com.animame

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/** Native Android H.264 encoder. Uses MediaCodec/MediaMuxer, no FFmpeg dependency. */
object VideoSequenceEncoder {
    fun encode(frames: List<Bitmap>, output: File, fps: Int = 24) {
        require(frames.isNotEmpty()) { "No frames" }
        val source = frames.first()
        val width = source.width and -2
        val height = source.height and -2
        require(width >= 2 && height >= 2) { "Video dimensions must be at least 2x2" }
        val safeFps = fps.coerceIn(1, 60)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, (width.toLong() * height.toLong() * safeFps / 4L).coerceIn(500_000L, 20_000_000L).toInt())
            setInteger(MediaFormat.KEY_FRAME_RATE, safeFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        var muxer: MediaMuxer? = null
        var track = -1
        var muxerStarted = false
        val info = MediaCodec.BufferInfo()
        val frameUs = 1_000_000L / safeFps
        var pts = 0L

        fun startMuxer(outputFormat: MediaFormat) {
            if (muxerStarted) return
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            track = muxer!!.addTrack(outputFormat)
            muxer!!.start()
            muxerStarted = true
        }

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            fun drain(endOfStream: Boolean = false): Boolean {
                var eos = false
                while (true) {
                    when (val index = codec.dequeueOutputBuffer(info, 10_000)) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> startMuxer(codec.outputFormat)
                        else -> if (index >= 0) {
                            val data = codec.getOutputBuffer(index)
                            if (data != null && info.size > 0 && muxerStarted) {
                                data.position(info.offset)
                                data.limit(info.offset + info.size)
                                muxer!!.writeSampleData(track, data, info)
                            }
                            eos = eos || (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                            codec.releaseOutputBuffer(index, false)
                            if (eos) break
                        }
                    }
                }
                return eos
            }

            frames.forEach { bitmap ->
                var queued = false
                while (!queued) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex) ?: error("Encoder input buffer unavailable")
                        input.clear()
                        putI420(bitmap, width, height, input)
                        codec.queueInputBuffer(inputIndex, 0, input.position(), pts, 0)
                        pts += frameUs
                        queued = true
                    }
                    drain()
                }
            }

            var eosQueued = false
            while (!eosQueued) {
                val inputIndex = codec.dequeueInputBuffer(100_000)
                if (inputIndex >= 0) {
                    codec.queueInputBuffer(inputIndex, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    eosQueued = true
                }
                drain()
            }
            while (!drain()) {
                // Keep draining until MediaCodec reports EOS.
            }
        } finally {
            try { codec.stop() } catch (_: Throwable) {}
            codec.release()
            if (muxerStarted) {
                try { muxer?.stop() } catch (_: Throwable) {}
            }
            muxer?.release()
        }
    }

    private fun putI420(src: Bitmap, width: Int, height: Int, buffer: ByteBuffer) {
        val pixels = IntArray(width * height)
        val source = if (src.width == width && src.height == height) src else Bitmap.createScaledBitmap(src, width, height, true)
        try {
            source.getPixels(pixels, 0, width, 0, 0, width, height)
            val y = ByteArray(width * height)
            val u = ByteArray(width * height / 4)
            val v = ByteArray(width * height / 4)
            for (j in 0 until height) {
                for (i in 0 until width) {
                    val c = pixels[j * width + i]
                    val r = (c ushr 16) and 255
                    val g = (c ushr 8) and 255
                    val b = c and 255
                    y[j * width + i] = ((66 * r + 129 * g + 25 * b + 128) shr 8).plus(16).coerceIn(0, 255).toByte()
                }
            }
            for (j in 0 until height step 2) {
                for (i in 0 until width step 2) {
                    var sr = 0
                    var sg = 0
                    var sb = 0
                    for (dy in 0..1) for (dx in 0..1) {
                        val c = pixels[(j + dy) * width + i + dx]
                        sr += (c ushr 16) and 255
                        sg += (c ushr 8) and 255
                        sb += c and 255
                    }
                    val r = sr / 4
                    val g = sg / 4
                    val b = sb / 4
                    val index = (j / 2) * (width / 2) + i / 2
                    u[index] = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte()
                    v[index] = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte()
                }
            }
            buffer.put(y)
            buffer.put(u)
            buffer.put(v)
        } finally {
            if (source !== src) source.recycle()
        }
    }
}
