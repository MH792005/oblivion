package io.oblivion.integrity

import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecurityPolicy
import io.oblivion.integrity.detector.SignatureValidator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OblivionIntegrityEngineTest {

    private val engine = OblivionIntegrityEngine()

    @Test
    fun `evaluating permissive policy returns Clean verdict`() {
        val verdict = engine.evaluate(SecurityPolicy.PERMISSIVE_DEV)
        assertTrue(verdict is IntegrityVerdict.Clean)
    }

    @Test
    fun `SignatureValidator computes correct SHA-256 fingerprint`() {
        val dummyCert = "TEST_CERTIFICATE_CONTENT".toByteArray(Charsets.UTF_8)
        val expectedFingerprint = SignatureValidator.computeSha256Fingerprint(dummyCert)

        assertTrue(SignatureValidator.verify(dummyCert, expectedFingerprint))
        assertFalse(SignatureValidator.verify(dummyCert, "0000000000000000000000000000000000000000000000000000000000000000"))
    }
}
