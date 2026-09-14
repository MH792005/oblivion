package io.oblivion.android.keystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.oblivion.domain.exception.OblivionException
import io.oblivion.domain.port.KeyStoreProvider
import java.security.Key
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Android Hardware-backed KeyStore manager.
 * Stores AES-256 keys inside the device's hardware Secure Element / StrongBox or TEE.
 * Degrades gracefully across API levels from Android 8.0 (API 26) through Android 15/16+ (API 35+).
 */
class HardwareKeyStore : KeyStoreProvider {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        try {
            load(null)
        } catch (e: Exception) {
            throw OblivionException.KeyStoreException("Failed to initialize AndroidKeyStore: ${e.message}", e)
        }
    }

    override val isHardwareBacked: Boolean = true

    @Synchronized
    override fun getOrCreateSecretKey(alias: String): Key {
        try {
            if (keyStore.containsAlias(alias)) {
                return keyStore.getKey(alias, null)
                    ?: throw OblivionException.KeyStoreException("Key alias '$alias' exists but cannot be retrieved.")
            }
            return generateHardwareKey(alias)
        } catch (e: OblivionException) {
            throw e
        } catch (e: Exception) {
            throw OblivionException.KeyStoreException("Failed to get or create hardware key for alias '$alias': ${e.message}", e)
        }
    }

    @Synchronized
    override fun deleteKey(alias: String) {
        try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (e: Exception) {
            throw OblivionException.KeyStoreException("Failed to delete key alias '$alias': ${e.message}", e)
        }
    }

    private fun generateHardwareKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

        // 1. Attempt StrongBox Keymaster on Android 9+ (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val strongBoxSpec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setIsStrongBoxBacked(true)
                    .build()

                keyGenerator.init(strongBoxSpec)
                return keyGenerator.generateKey()
            } catch (_: Throwable) {
                // Device does not have physical StrongBox HSM; gracefully fallback to TEE hardware backing below
            }
        }

        // 2. Standard TEE Hardware-backed generation (API 26+)
        val teeSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(teeSpec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}
