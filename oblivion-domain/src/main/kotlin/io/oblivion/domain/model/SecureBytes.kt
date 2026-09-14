package io.oblivion.domain.model

import java.nio.ByteBuffer
import java.util.Arrays

/**
 * Memory-defensive byte container implementing [AutoCloseable].
 * Actively destroys and zeroes sensitive memory buffers to prevent RAM dumps and cold-boot attacks.
 */
class SecureBytes(private var internalBytes: ByteArray) : AutoCloseable {

    @Volatile
    private var isDestroyed = false

    val size: Int
        get() {
            checkNotDestroyed()
            return internalBytes.size
        }

    /**
     * Executes an operation with the underlying byte array and keeps it alive during execution.
     */
    fun <R> useBytes(block: (ByteArray) -> R): R {
        checkNotDestroyed()
        return block(internalBytes)
    }

    /**
     * Executes an operation and guarantees memory zeroing upon completion,
     * even if [block] throws an exception.
     */
    fun <R> useAndDestroy(block: (ByteArray) -> R): R {
        try {
            return useBytes(block)
        } finally {
            close()
        }
    }

    /**
     * Copies content into a [ByteBuffer] with bounds verification.
     */
    fun copyInto(byteBuffer: ByteBuffer) {
        checkNotDestroyed()
        byteBuffer.put(internalBytes)
    }

    /**
     * Copies the content into a destination array.
     */
    fun copyInto(destination: ByteArray, destinationOffset: Int = 0) {
        checkNotDestroyed()
        System.arraycopy(internalBytes, 0, destination, destinationOffset, internalBytes.size)
    }

    /**
     * Actively overwrites the internal byte array with zeros.
     */
    @Synchronized
    override fun close() {
        if (!isDestroyed) {
            Arrays.fill(internalBytes, 0.toByte())
            isDestroyed = true
        }
    }

    private fun checkNotDestroyed() {
        check(!isDestroyed) { "SecureBytes has already been destroyed and zeroed out." }
    }

    companion object {
        fun of(bytes: ByteArray): SecureBytes = SecureBytes(bytes.copyOf())
        fun wrap(bytes: ByteArray): SecureBytes = SecureBytes(bytes)
    }
}
