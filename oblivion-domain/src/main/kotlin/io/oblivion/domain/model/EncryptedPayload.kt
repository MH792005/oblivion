package io.oblivion.domain.model

/**
 * Immutable value object holding encrypted ciphertext alongside its IV/nonce and auth tag.
 */
data class EncryptedPayload(
    val initializationVector: ByteArray,
    val cipherText: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedPayload) return false
        if (!initializationVector.contentEquals(other.initializationVector)) return false
        if (!cipherText.contentEquals(other.cipherText)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = initializationVector.contentHashCode()
        result = 31 * result + cipherText.contentHashCode()
        return result
    }
}
