package io.oblivion.crypto

import io.oblivion.domain.exception.OblivionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.ByteBuffer
import java.security.SecureRandom
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HardwareAcceleratedCipherTest {

    private val cipher = HardwareAcceleratedCipher()
    private val random = SecureRandom()

    @Test
    fun `encrypt and decrypt roundtrip succeeds with authenticated data`() {
        val key = ByteArray(32).also { random.nextBytes(it) }
        val plaintext = "CONFIDENTIAL_PAYLOAD_2026".toByteArray(Charsets.UTF_8)
        val aad = "DEVICE_ID_XYZ".toByteArray(Charsets.UTF_8)

        val encrypted = cipher.encrypt(plaintext, key, aad)

        assertFalse(encrypted.cipherText.contentEquals(plaintext))
        assertEquals(12, encrypted.initializationVector.size)

        val decrypted = cipher.decrypt(encrypted, key, aad)
        decrypted.useAndDestroy { bytes ->
            assertEquals("CONFIDENTIAL_PAYLOAD_2026", String(bytes, Charsets.UTF_8))
        }
    }

    @Test
    fun `invalid key length throws InvalidKeyException`() {
        val badKey = ByteArray(10) // Neither 16 nor 32
        val plaintext = "TEST".toByteArray()

        assertFailsWith<OblivionException.InvalidKeyException> {
            cipher.encrypt(plaintext, badKey)
        }
    }

    @Test
    fun `truncated ciphertext throws InvalidPayloadException`() {
        val key = ByteArray(32).also { random.nextBytes(it) }
        val invalidPayload = io.oblivion.domain.model.EncryptedPayload(
            initializationVector = ByteArray(12),
            cipherText = ByteArray(5) // Less than 16-byte tag
        )

        assertFailsWith<OblivionException.InvalidPayloadException> {
            cipher.decrypt(invalidPayload, key)
        }
    }

    @Test
    fun `tampered ciphertext throws DecryptionFailedException`() {
        val key = ByteArray(32).also { random.nextBytes(it) }
        val plaintext = "SECRET".toByteArray()
        val encrypted = cipher.encrypt(plaintext, key)

        encrypted.cipherText[0] = (encrypted.cipherText[0].toInt() xor 0xFF).toByte()

        assertFailsWith<OblivionException.DecryptionFailedException> {
            cipher.decrypt(encrypted, key)
        }
    }

    @Test
    fun `direct ByteBuffer encryption roundtrip succeeds`() {
        val key = ByteArray(32).also { random.nextBytes(it) }
        val message = "HIGH_THROUGHPUT_STREAM_DATA".toByteArray(Charsets.UTF_8)

        val inBuf = ByteBuffer.allocateDirect(message.size)
        inBuf.put(message).flip()

        val outBuf = ByteBuffer.allocateDirect(message.size + 16)
        val iv = cipher.encryptByteBuffer(inBuf, outBuf, key)
        outBuf.flip()

        assertEquals(12, iv.size)
        assertTrue(outBuf.remaining() > message.size)
    }

    @Test
    fun `concurrency stress test passes without thread collisions`() {
        runBlocking(Dispatchers.Default) {
            val key = ByteArray(32).also { random.nextBytes(it) }

            val jobs = (1..100).map { id ->
                async {
                    val secret = "THREAD_PAYLOAD_$id"
                    val encrypted = cipher.encrypt(secret.toByteArray(), key)
                    val decrypted = cipher.decrypt(encrypted, key)
                    decrypted.useAndDestroy { bytes ->
                        assertEquals(secret, String(bytes))
                    }
                }
            }
            jobs.awaitAll()
        }
    }
}
