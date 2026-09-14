package io.oblivion.android

import android.content.Context
import io.oblivion.android.keystore.HardwareKeyStore
import io.oblivion.crypto.HardwareAcceleratedCipher
import io.oblivion.domain.model.EncryptedPayload
import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecureBytes
import io.oblivion.domain.model.SecurityPolicy
import io.oblivion.domain.port.CryptoEngine
import io.oblivion.domain.port.IntegrityScanner
import io.oblivion.domain.port.KeyStoreProvider
import io.oblivion.integrity.IntegrityMonitor
import io.oblivion.integrity.OblivionIntegrityEngine
import io.oblivion.integrity.detector.SignatureValidator
import kotlinx.coroutines.flow.Flow
import java.security.Key
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Oblivion Security Engine.
 * High-performance, pure Kotlin/Java defense-in-depth security framework for Android applications.
 */
object Oblivion {

    private val isInitialized = AtomicBoolean(false)
    private val cryptoEngine: CryptoEngine = HardwareAcceleratedCipher()
    private val integrityScanner: IntegrityScanner = OblivionIntegrityEngine()
    private val integrityMonitor: IntegrityMonitor by lazy { IntegrityMonitor(integrityScanner) }
    private val hardwareKeyStore: KeyStoreProvider by lazy { HardwareKeyStore() }

    /**
     * Initializes the security engine. Automatically invoked by [OblivionInitProvider].
     */
    fun init(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                hardwareKeyStore.isHardwareBacked
            } catch (_: Throwable) {
                // Keystore prewarm non-fatal
            }
        }
    }

    /**
     * Evaluates runtime integrity (anti-debug, anti-Frida, anti-root, signature checks).
     *
     * @param context Application context
     * @param policy Security policies to enforce
     * @return [IntegrityVerdict] representing the unforgeable verdict
     */
    fun verifyIntegrity(
        context: Context,
        policy: SecurityPolicy = SecurityPolicy.STRICT
    ): IntegrityVerdict {
        // 1. Evaluate runtime signals
        val verdict = integrityScanner.evaluate(policy)
        if (verdict !is IntegrityVerdict.Clean) {
            return verdict
        }

        // 2. Validate APK signature if required by policy
        val expectedSignature = policy.expectedSignatureSha256
        if (policy.checkSignature && expectedSignature != null) {
            val certBytes = getPackageSigningCert(context)
            if (certBytes == null || !SignatureValidator.verify(certBytes, expectedSignature)) {
                return IntegrityVerdict.Compromised.SignatureMismatch(
                    expectedHash = expectedSignature,
                    actualHash = certBytes?.let { SignatureValidator.computeSha256Fingerprint(it) } ?: "NULL"
                )
            }
        }

        return verdict
    }

    /**
     * Continuous background environment integrity monitoring.
     * Emits periodic [IntegrityVerdict] updates so the app can react if an attacker attaches tools dynamically.
     */
    fun monitorIntegrity(
        policy: SecurityPolicy = SecurityPolicy.STRICT,
        intervalMs: Long = 5000L
    ): Flow<IntegrityVerdict> {
        return integrityMonitor.monitor(policy, intervalMs)
    }

    /**
     * Encrypts plaintext bytes using hardware-accelerated AES-GCM (128-bit authentication tag).
     */
    fun encrypt(
        plaintext: ByteArray,
        secretKey: ByteArray,
        associatedData: ByteArray? = null
    ): EncryptedPayload {
        return cryptoEngine.encrypt(plaintext, secretKey, associatedData)
    }

    /**
     * Decrypts ciphertext bytes into a memory-safe [SecureBytes] container.
     */
    fun decrypt(
        payload: EncryptedPayload,
        secretKey: ByteArray,
        associatedData: ByteArray? = null
    ): SecureBytes {
        return cryptoEngine.decrypt(payload, secretKey, associatedData)
    }

    /**
     * Derives a cryptographically strong session token or key bound to context info using HKDF-SHA256.
     */
    fun deriveSessionToken(
        masterKey: ByteArray,
        salt: ByteArray,
        contextInfo: ByteArray,
        lengthBytes: Int = 32
    ): ByteArray {
        return cryptoEngine.deriveKey(masterKey, salt, contextInfo, lengthBytes)
    }

    /**
     * Retrieves or generates an AES-256 key stored inside the hardware StrongBox / TEE chip.
     */
    fun getOrCreateHardwareKey(alias: String): Key {
        return hardwareKeyStore.getOrCreateSecretKey(alias)
    }

    @Suppress("DEPRECATION")
    private fun getPackageSigningCert(context: Context): ByteArray? {
        return try {
            val pm = context.packageManager
            val packageName = context.packageName
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val signingInfo = pm.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners.firstOrNull()?.toByteArray()
                    } else {
                        signingInfo.signingCertificateHistory.firstOrNull()?.toByteArray()
                    }
                } else null
            } else {
                val packageInfo = pm.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
                packageInfo.signatures.firstOrNull()?.toByteArray()
            }
        } catch (_: Throwable) {
            null
        }
    }
}
