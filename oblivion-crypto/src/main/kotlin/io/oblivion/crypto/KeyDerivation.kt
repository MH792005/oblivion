package io.oblivion.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

/**
 * High-performance pure JVM Key Derivation Functions.
 * Implements RFC 5869 HMAC-based Extract-and-Expand Key Derivation Function (HKDF) using SHA-256.
 */
object KeyDerivation {

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val HASH_LEN = 32 // SHA-256 output length in bytes

    /**
     * Extracts and expands pseudo-random keys using HKDF-SHA256.
     */
    fun hkdfSha256(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        outputLengthBytes: Int
    ): ByteArray {
        val prk = extract(salt, inputKeyMaterial)
        return expand(prk, info, outputLengthBytes)
    }

    private fun extract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val actualSalt = if (salt.isEmpty()) ByteArray(HASH_LEN) else salt
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(actualSalt, HMAC_ALGORITHM))
        return mac.doFinal(ikm)
    }

    private fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length <= 255 * HASH_LEN) { "Requested output length too large for HKDF-SHA256." }

        val n = ceil(length.toDouble() / HASH_LEN).toInt()
        val result = ByteArray(length)
        var t = ByteArray(0)
        var bytesWritten = 0

        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(prk, HMAC_ALGORITHM))

        for (i in 1..n) {
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()

            val toCopy = minOf(HASH_LEN, length - bytesWritten)
            System.arraycopy(t, 0, result, bytesWritten, toCopy)
            bytesWritten += toCopy
        }

        return result
    }
}
