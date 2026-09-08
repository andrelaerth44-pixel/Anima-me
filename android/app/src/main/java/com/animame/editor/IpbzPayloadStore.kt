package com.animame.editor

import android.content.Context
import android.util.Base64
import java.security.MessageDigest

/** Lossless storage for original ibisPaint IPBZ QR payloads. */
object IpbzPayloadStore {
    private const val PREFS = "animame_ipbz_payloads"
    private const val KEY_PREFIX = "payload_"

    fun isIpbz(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'B'.code.toByte() && bytes[3] == 'Z'.code.toByte()

    fun fingerprint(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun save(context: Context, bytes: ByteArray): String {
        require(isIpbz(bytes)) { "Payload não possui assinatura IPBZ." }
        val id = fingerprint(bytes)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PREFIX + id, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
        return id
    }

    fun load(context: Context, fingerprint: String): ByteArray? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + fingerprint, null) ?: return null
        return runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
    }

    fun all(context: Context): List<Pair<String, ByteArray>> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) return@mapNotNull null
            val id = key.removePrefix(KEY_PREFIX)
            val bytes = runCatching { Base64.decode(value, Base64.DEFAULT) }.getOrNull() ?: return@mapNotNull null
            id to bytes
        }
}
