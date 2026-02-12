package com.anisync.android.data.serializer

import androidx.datastore.core.Serializer
import com.anisync.android.data.crypto.CryptoManager
import com.anisync.android.data.proto.AuthToken
import com.google.protobuf.InvalidProtocolBufferException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializer for AuthToken Proto DataStore with encryption.
 *
 * This serializer intercepts DataStore read/write operations and applies
 * encryption/decryption using Google Tink. The data is encrypted before
 * being written to disk and decrypted when read back.
 */
@Singleton
class AuthTokenSerializer @Inject constructor(
    private val cryptoManager: CryptoManager
) : Serializer<AuthToken> {

    override val defaultValue: AuthToken = AuthToken.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AuthToken {
        return try {
            // Decrypt the input stream to get plaintext bytes
            val decryptedOutputStream = ByteArrayOutputStream()
            cryptoManager.decrypt(input, decryptedOutputStream)
            val plaintextBytes = decryptedOutputStream.toByteArray()

            // Parse the protobuf message from decrypted bytes
            AuthToken.parseFrom(plaintextBytes)
        } catch (e: InvalidProtocolBufferException) {
            // Return default value if parsing fails (corrupted or invalid data)
            defaultValue
        } catch (e: Exception) {
            // Return default value for any other decryption errors
            defaultValue
        }
    }

    override suspend fun writeTo(t: AuthToken, output: OutputStream) {
        // Serialize the protobuf message to bytes
        val plaintextBytes = t.toByteArray()
        val inputStream = ByteArrayInputStream(plaintextBytes)

        // Encrypt and write to output stream
        cryptoManager.encrypt(inputStream, output)
    }
}
