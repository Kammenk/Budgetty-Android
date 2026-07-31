package com.budgetty.app.data.settings

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 hashing for the app-lock PIN — the PIN itself is never stored, only a per-install
 * salted digest ("saltHex:hashHex"). Enough to keep the 4-digit PIN out of plaintext prefs; the lock
 * is a convenience gate, not the app's data-at-rest protection.
 */
object PinHash {

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return salt.toHex() + ":" + digest(salt, pin).toHex()
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].fromHex() ?: return false
        return constantTimeEquals(digest(salt, pin).toHex(), parts[1])
    }

    private fun digest(salt: ByteArray, pin: String): ByteArray =
        MessageDigest.getInstance("SHA-256").apply { update(salt) }.digest(pin.toByteArray(Charsets.UTF_8))

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray? = runCatching {
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }.getOrNull()

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
