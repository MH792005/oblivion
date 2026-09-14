package io.oblivion.crypto

import io.oblivion.domain.exception.OblivionException
import io.oblivion.domain.model.EncryptedPayload
import io.oblivion.domain.model.SecureBytes
import io.oblivion.domain.port.CryptoEngine
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Ultra-high-performance pure JVM cryptographic engine.
 * Leverages native AES-NI and ARMv8 Crypto Extensions via JVM/ART intrinsics.
 * Fully thread-safe, memory-defensive, and authenticated via AES-GCM (128-bit tag).
 */
class HardwareAcceleratedCipher(
    private val secureRandom: SecureRandom = SecureRandom()
) : CryptoEngine {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_TAG_LENGTH_BYTES = 16
        const val GCM_IV_LENGTH_BYTES = 12 // 96-bit standard nonce for GCM
    }

    override fun encrypt(
        plaintext: ByteArray,
        secretKey: ByteArray,
        associatedData: ByteArray?
    ): EncryptedPayload {
        validateKey(secretKey)

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec: SecretKey = SecretKeySpec(secretKey, KEY_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            if (associatedData != null && associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }

            val ciphertext = cipher.doFinal(plaintext)
            return EncryptedPayload(
                initializationVector = iv,
                cipherText = ciphertext
            )
        } catch (e: Exception) {
            throw OblivionException.DecryptionFailedException("Encryption failed: ${e.message}", e)
        }
    }

    override fun decrypt(
        payload: EncryptedPayload,
        secretKey: ByteArray,
        associatedData: ByteArray?
    ): SecureBytes {
        validateKey(secretKey)
        validatePayload(payload)

        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec: SecretKey = SecretKeySpec(secretKey, KEY_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.initializationVector)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            if (associatedData != null && associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }

            val decrypted = cipher.doFinal(payload.cipherText)
            return SecureBytes.wrap(decrypted)
        } catch (e: AEADBadTagException) {
            throw OblivionException.DecryptionFailedException("Tampered ciphertext or invalid authentication tag.", e)
        } catch (e: Exception) {
            throw OblivionException.DecryptionFailedException("Decryption failed: ${e.message}", e)
        }
    }

    /**
     * Zero-allocation direct ByteBuffer encryption.
     */
    fun encryptByteBuffer(
        input: ByteBuffer,
        output: ByteBuffer,
        secretKey: ByteArray,
        associatedData: ByteBuffer? = null
    ): ByteArray {
        validateKey(secretKey)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec: SecretKey = SecretKeySpec(secretKey, KEY_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        if (associatedData != null && associatedData.hasRemaining()) {
            cipher.updateAAD(associatedData)
        }

        cipher.doFinal(input, output)
        return iv
    }

    override fun deriveKey(
        masterKey: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        keyLengthBytes: Int
    ): ByteArray {
        validateKeyMaterial(masterKey, keyLengthBytes)
        return KeyDerivation.hkdfSha256(masterKey, salt, info, keyLengthBytes)
    }

    private fun validateKey(key: ByteArray) {
        if (key.size != 16 && key.size != 32) {
            throw OblivionException.InvalidKeyException(
                "Invalid AES key length: ${key.size} bytes. Key must be exactly 16 bytes (128-bit) or 32 bytes (256-bit)."
            )
        }
    }

    private fun validatePayload(payload: EncryptedPayload) {
        if (payload.initializationVector.size != GCM_IV_LENGTH_BYTES) {
            throw OblivionException.InvalidPayloadException(
                "Invalid IV length: ${payload.initializationVector.size} bytes. Must be $GCM_IV_LENGTH_BYTES bytes."
            )
        }
        if (payload.cipherText.size < GCM_TAG_LENGTH_BYTES) {
            throw OblivionException.InvalidPayloadException(
                "Ciphertext too short: ${payload.cipherText.size} bytes. Must include at least $GCM_TAG_LENGTH_BYTES bytes for the auth tag."
            )
        }
    }

    private fun validateKeyMaterial(key: ByteArray, length: Int) {
        if (key.isEmpty()) {
            throw OblivionException.InvalidKeyException("Master key material cannot be empty.")
        }
        if (length <= 0 || length > 1024) {
            throw OblivionException.InvalidKeyException("Derived key length must be between 1 and 1024 bytes.")
        }
    }
}
