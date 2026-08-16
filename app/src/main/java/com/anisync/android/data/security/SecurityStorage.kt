package com.anisync.android.data.security

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityStorage {

    private const val SALT_LENGTH = 16

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$saltHex:$password".toByteArray(Charsets.UTF_8)
        val hash = digest.digest(combined)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, saltHex: String?, expectedHashHex: String?): Boolean {
        if (saltHex.isNullOrEmpty() || expectedHashHex.isNullOrEmpty()) return false
        val computed = hashPassword(password, saltHex)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedHashHex.toByteArray(Charsets.UTF_8)
        )
    }
}
