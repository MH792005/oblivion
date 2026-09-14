package io.oblivion.integrity.detector

import io.oblivion.crypto.ConstantTime
import java.security.MessageDigest

/**
 * Validates APK signing certificate to prevent repacking, cracking, and unauthorized redistribution.
 */
object SignatureValidator {

    /**
     * Computes the SHA-256 fingerprint of the raw certificate bytes in uppercase hex.
     */
    fun computeSha256Fingerprint(certificateBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certificateBytes)
        return hash.joinToString("") { "%02X".format(it) }
    }

    /**
     * Validates whether actual certificate matches the expected hash using constant-time evaluation.
     */
    fun verify(actualCertificateBytes: ByteArray, expectedSha256Hex: String): Boolean {
        val actualFingerprint = computeSha256Fingerprint(actualCertificateBytes)
        val normalizedExpected = expectedSha256Hex.replace(":", "").uppercase()
        return ConstantTime.equals(actualFingerprint, normalizedExpected)
    }
}
