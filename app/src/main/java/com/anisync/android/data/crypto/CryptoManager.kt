package com.anisync.android.data.crypto

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encryption and decryption operations using Google Tink.
 *
 * This class provides encryption using AES-256-GCM, which is ideal for
 * encrypting Proto DataStore files. The encryption keys are stored securely
 * in the Android Keystore.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val aead: Aead

    init {
        // Register Tink AEAD configuration
        AeadConfig.register()

        // Build or retrieve the keyset handle from Android Keystore
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        // Get the Aead primitive for encryption using the non-deprecated API
        aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    /**
     * Encrypts data from an input stream and writes to an output stream.
     *
     * @param inputStream The input stream to read plaintext from
     * @param outputStream The output stream to write ciphertext to
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream) {
        val plaintext = inputStream.readBytes()
        val ciphertext = aead.encrypt(plaintext, ByteArray(0))
        outputStream.write(ciphertext)
    }

    /**
     * Decrypts data from an input stream and writes to an output stream.
     *
     * @param inputStream The input stream to read ciphertext from
     * @param outputStream The output stream to write plaintext to
     */
    fun decrypt(inputStream: InputStream, outputStream: OutputStream) {
        val ciphertext = inputStream.readBytes()
        val plaintext = aead.decrypt(ciphertext, ByteArray(0))
        outputStream.write(plaintext)
    }

    companion object {
        private const val KEYSET_NAME = "auth_token_keyset"
        private const val PREFERENCE_FILE = "auth_key_preferences"
        private const val MASTER_KEY_URI = "android-keystore://auth_master_key"
    }
}
