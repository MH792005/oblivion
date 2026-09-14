package io.oblivion.domain.exception

import io.oblivion.domain.model.IntegrityVerdict

/**
 * Root exception hierarchy for all Oblivion operations.
 */
sealed class OblivionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    class InvalidKeyException(message: String) : OblivionException(message)

    class InvalidPayloadException(message: String) : OblivionException(message)

    class DecryptionFailedException(message: String, cause: Throwable? = null) : OblivionException(message, cause)

    class IntegrityCompromisedException(
        val verdict: IntegrityVerdict.Compromised
    ) : OblivionException("Integrity check failed: ${verdict.threatDescription}")

    class KeyStoreException(message: String, cause: Throwable? = null) : OblivionException(message, cause)
}
