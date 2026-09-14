package io.oblivion.domain.port

import java.security.Key

/**
 * Domain port for hardware-backed key store operations.
 */
interface KeyStoreProvider {
    val isHardwareBacked: Boolean
    fun getOrCreateSecretKey(alias: String): Key
    fun deleteKey(alias: String)
}
