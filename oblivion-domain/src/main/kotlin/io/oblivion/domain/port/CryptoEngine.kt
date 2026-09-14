package io.oblivion.domain.port

import io.oblivion.domain.model.EncryptedPayload
import io.oblivion.domain.model.SecureBytes

/**
 * Domain port for cryptographic operations.
 */
interface CryptoEngine {
    fun encrypt(plaintext: ByteArray, secretKey: ByteArray, associatedData: ByteArray? = null): EncryptedPayload
    fun decrypt(payload: EncryptedPayload, secretKey: ByteArray, associatedData: ByteArray? = null): SecureBytes
    fun deriveKey(masterKey: ByteArray, salt: ByteArray, info: ByteArray, keyLengthBytes: Int = 32): ByteArray
}
