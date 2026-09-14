package io.oblivion.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import io.oblivion.android.Oblivion
import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecurityPolicy
import java.security.SecureRandom

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Evaluate runtime integrity (anti-debug, anti-Frida, anti-root)
        val verdict = Oblivion.verifyIntegrity(this, SecurityPolicy.STRICT)
        when (verdict) {
            is IntegrityVerdict.Clean -> {
                Log.i("Oblivion", "Environment verified! Executed: ${verdict.checksExecuted}")
            }
            is IntegrityVerdict.Compromised -> {
                Log.w("Oblivion", "Security Violation: ${verdict.threatDescription}")
            }
        }

        // 2. Hardware-accelerated authenticated encryption
        val random = SecureRandom()
        val sessionKey = ByteArray(32).also { random.nextBytes(it) }
        val secretData = "CONFIDENTIAL_FINANCIAL_RECORD_2026".toByteArray(Charsets.UTF_8)
        val contextAad = "USER_ID_98765".toByteArray(Charsets.UTF_8)

        val encrypted = Oblivion.encrypt(secretData, sessionKey, contextAad)
        Log.i("Oblivion", "Ciphertext length: ${encrypted.cipherText.size} bytes")

        // 3. Memory-safe decryption with automatic buffer destruction
        Oblivion.decrypt(encrypted, sessionKey, contextAad).use { secureBytes ->
            secureBytes.useBytes { decryptedBytes ->
                val resultString = String(decryptedBytes, Charsets.UTF_8)
                Log.i("Oblivion", "Decrypted successfully: $resultString")
            }
        }
        // At this point, decrypted memory has been actively zeroed out in RAM
    }
}
