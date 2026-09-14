package io.oblivion.domain

import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecureBytes
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecureBytesTest {

    @Test
    fun `useBytes allows reading content and close destroys memory`() {
        val original = byteArrayOf(1, 2, 3, 4, 5)
        val secureBytes = SecureBytes.wrap(original)

        assertEquals(5, secureBytes.size)

        var readSum = 0
        secureBytes.useBytes { bytes ->
            readSum = bytes.sum()
        }
        assertEquals(15, readSum)

        // Close and ensure zeroed
        secureBytes.close()

        // Original array should be zeroed
        assertTrue(original.all { it == 0.toByte() })

        // Accessing after close must fail
        assertFailsWith<IllegalStateException> {
            secureBytes.useBytes { }
        }
    }

    @Test
    fun `IntegrityVerdict hierarchy is correctly modeled`() {
        val clean: IntegrityVerdict = IntegrityVerdict.Clean(checksExecuted = setOf("root", "debug"))
        assertTrue(clean is IntegrityVerdict.Clean)

        val compromised: IntegrityVerdict = IntegrityVerdict.Compromised.DebuggerAttached()
        assertTrue(compromised is IntegrityVerdict.Compromised)
    }
}
