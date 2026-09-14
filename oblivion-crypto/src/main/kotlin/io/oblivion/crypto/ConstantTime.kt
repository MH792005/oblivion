package io.oblivion.crypto

/**
 * Constant-time comparison primitives.
 * Prevents side-channel timing attacks that infer key/hash prefixes by observing early exits.
 */
object ConstantTime {

    /**
     * Compares two byte arrays in constant time.
     * Always evaluates all bytes regardless of mismatches.
     */
    @JvmStatic
    fun equals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) {
            return false
        }
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    /**
     * Compares two strings in constant time.
     */
    @JvmStatic
    fun equals(a: String, b: String): Boolean {
        return equals(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }
}
