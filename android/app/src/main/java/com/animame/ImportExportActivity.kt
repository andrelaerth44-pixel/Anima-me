package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ImportExportActivity : Activity() {
    private val pickImages = 10
    private val pickVideo = 11
    private val pickQr = 12
    private val saveGif = 20
    private val saveMp4 = 21
    private val savePng = 22
    private val saveImage = 23
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 20, 30, 20)
            setBackgroundColor(ThemeColorStore.NAVY_950)
        }
        root.addView(TextView(this).apply {
            text = "Importação / Exportação"
            textSize = 27f
            setTextColor(ThemeColorStore.TEXT)
        })
        root.addView(TextView(this).apply {
            text = "QR • imagens • sequência • vídeo • GIF • MP4"
            textSize = 16f
            setTextColor(ThemeColorStore.MUTED)
            setPadding(0, 6, 0, 18)
        })
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(ThemeColorStore.TEXT)
            setPadding(0, 0, 0, 14)
        }
        root.addView(status)
        refresh()

        fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
            text = label
            setTextColor(ThemeColorStore.TEXT)
            setBackgroundColor(ThemeColorStore.NAVY_800)
            setOnClickListener { action() }
        }

        root.addView(actionButton("Importar imagens / sequência") { pickImages() })
        root.addView(actionButton("Importar vídeo → sequência de frames") { pickVideo() })
        root.addView(actionButton("Importar QR de uma imagem") { pickQr() })
        root.addView(actionButton("Exportar imagem PNG + QR") { ifReady { save(saveImage, "anima-me-frame.png", "image/png") } })
        root.addView(actionButton("Exportar sequência PNG (ZIP)") { ifReady { save(savePng, "anima-me-sequence.zip", "application/zip") } })
        root.addView(actionButton("Exportar GIF") { ifReady { save(saveGif, "anima-me.gif", "image/gif") } })
        root.addView(actionButton("Exportar vídeo MP4 (H.264)") { ifReady { save(saveMp4, "anima-me.mp4", "video/mp4") } })
        root.addView(actionButton("Limpar sequência") { MediaSequenceStore.clear(); refresh() })
        root.addView(actionButton("Voltar") { finish() })
        setContentView(root)
    }

    private fun refresh() {
        status.text = "Frames importados: ${MediaSequenceStore.count}"
    }

    private fun ifReady(action: () -> Unit) {
        if (MediaSequenceStore.count == 0) {
            Toast.makeText(this, "Importa primeiro uma imagem ou sequência.", Toast.LENGTH_SHORT).show()
        } else {
            action()
        }
    }

    private fun pickImages() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }, pickImages)
    }

    private fun pickVideo() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }, pickVideo)
    }

    private fun pickQr() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }, pickQr)
    }

    private fun save(code: Int, name: String, mime: String) {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = mime
            putExtra(Intent.EXTRA_TITLE, name)
            addCategory(Intent.CATEGORY_OPENABLE)
        }, code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            pickImages -> importImages(data)
            pickVideo -> importVideo(data.data)
            pickQr -> importQr(data.data)
            saveImage -> exportImage(data.data)
            savePng -> exportPngZip(data.data)
            saveGif -> exportGif(data.data)
            saveMp4 -> exportMp4(data.data)
        }
    }

    private fun importImages(data: Intent) {
        val uris = mutableListOf<Uri>()
        data.data?.let(uris::add)
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) uris += clip.getItemAt(i).uri
        }
        val ordered = uris.distinct().sortedBy(::displayName)
        val decoded = ordered.mapNotNull(::decode)
        if (decoded.isNotEmpty()) {
            MediaSequenceStore.replace(decoded)
            refresh()
            Toast.makeText(this, "${decoded.size} frame(s) importado(s)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importVideo(uri: Uri?) {
        if (uri == null) return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val fps = 24
            val step = 1_000_000L / fps
            var t = 0L
            val frames = mutableListOf<Bitmap>()
            while (t < duration * 1000L && frames.size < 240) {
                retriever.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST)?.let { frames += scale(it, 1280) }
                t += step
            }
            if (frames.isNotEmpty()) {
                MediaSequenceStore.replace(frames)
                refresh()
                Toast.makeText(this, "Vídeo importado: ${frames.size} frames a ${fps} FPS", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Não foi possível extrair frames.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Falha ao importar vídeo: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            retriever.release()
        }
    }

    private fun importQr(uri: Uri?) {
        if (uri == null) return
        val bitmap = decode(uri) ?: return
        val payload = ProjectQrCodec.decode(bitmap)
        bitmap.recycle()
        if (payload == null) {
            Toast.makeText(this, "QR não reconhecido.", Toast.LENGTH_SHORT).show()
            return
        }
        val obj = ProjectQrCodec.unpack(payload)
        Toast.makeText(
            this,
            if (obj != null) "QR Anima-me: ${obj.optString("name", "Projeto")} • ${obj.optInt("frames", obj.optInt("mediaFrames", 0))} frames"
            else "QR lido: ${payload.take(40)}…",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun exportImage(uri: Uri?) {
        if (uri == null) return
        val source = MediaSequenceStore.all().first()
        val bitmap = Bitmap.createBitmap(source)
        val payload = ProjectQrCodec.payload(com.animame.editor.AnimationDocument(duration = MediaSequenceStore.count), MediaSequenceStore.count)
        val qr = try { ProjectQrCodec.qrBitmap(payload, 360) } catch (_: Throwable) { null }
        qr?.let {
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(it, (bitmap.width - it.width - 24).toFloat(), (bitmap.height - it.height - 24).toFloat(), paint)
            it.recycle()
        }
        contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        Toast.makeText(this, "PNG exportado com QR.", Toast.LENGTH_SHORT).show()
    }

    private fun exportPngZip(uri: Uri?) {
        if (uri == null) return
        contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                MediaSequenceStore.all().forEachIndexed { index, bitmap ->
                    zip.putNextEntry(ZipEntry("frame_${(index + 1).toString().padStart(5, '0')}.png"))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, zip)
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry("project.json"))
                val json = ProjectQrCodec.payload(com.animame.editor.AnimationDocument(duration = MediaSequenceStore.count), MediaSequenceStore.count)
                zip.write(json.toByteArray())
                zip.closeEntry()
            }
        }
        Toast.makeText(this, "Sequência PNG exportada.", Toast.LENGTH_SHORT).show()
    }

    private fun exportGif(uri: Uri?) {
        if (uri == null) return
        contentResolver.openOutputStream(uri)?.use { GifSequenceEncoder.encode(MediaSequenceStore.all(), it, 42) }
        Toast.makeText(this, "GIF exportado.", Toast.LENGTH_SHORT).show()
    }

    private fun exportMp4(uri: Uri?) {
        if (uri == null) return
        val temp = java.io.File(cacheDir, "animame-export.mp4")
        try {
            VideoSequenceEncoder.encode(MediaSequenceStore.all(), temp, 24)
            contentResolver.openOutputStream(uri)?.use { destination ->
                temp.inputStream().use { source -> source.copyTo(destination) }
            }
            Toast.makeText(this, "MP4 H.264 exportado.", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Toast.makeText(this, "MP4 não suportado neste dispositivo: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            temp.delete()
        }
    }

    private fun decode(uri: Uri): Bitmap? = try {
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let { scale(it, 1280) }
    } catch (_: Throwable) {
        null
    }

    private fun scale(bitmap: Bitmap, max: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= max) return bitmap
        val scale = max.toFloat() / largest
        val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.toString()
    }
}
