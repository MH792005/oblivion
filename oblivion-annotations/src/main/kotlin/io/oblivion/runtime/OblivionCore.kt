package io.oblivion.runtime

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runtime bridge and security control layer for Oblivion.
 * Exposes native initialization hooks, status flags, and decryption helpers.
 */
object OblivionCore {
    private val isInitialized = AtomicBoolean(false)
    private var nativeLibraryLoaded = false

    @JvmStatic
    val isProtected: Boolean
        get() = isInitialized.get() && nativeLibraryLoaded

    /**
     * Initializes native security components and loads the native library.
     */
    @JvmStatic
    fun init() {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                System.loadLibrary("oblivion_secure")
                nativeLibraryLoaded = true
                nativeStartSecurityDaemons()
            } catch (e: UnsatisfiedLinkError) {
                // In non-Android or fallback JVM environments where native library is absent
                nativeLibraryLoaded = false
            }
        }
    }

    /**
     * JNI Native entry point to start the background memory scanner & syscall daemon.
     */
    @JvmStatic
    external fun nativeStartSecurityDaemons(): Boolean

    /**
     * Dynamic string decryption hook invoked by bytecode transformed methods.
     */
    @JvmStatic
    fun decryptString(encryptedBytes: ByteArray, key: Byte): String {
        val result = ByteArray(encryptedBytes.size)
        for (i in encryptedBytes.indices) {
            result[i] = (encryptedBytes[i].toInt() xor key.toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }
}
