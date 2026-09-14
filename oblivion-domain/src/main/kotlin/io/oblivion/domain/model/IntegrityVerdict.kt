package io.oblivion.domain.model

/**
 * Exhaustive algebraic data type representing security integrity evaluation.
 * Replaces naive binary flags (`isProtected: Boolean`) with structured, unforgeable verdicts.
 */
sealed interface IntegrityVerdict {

    /**
     * The environment has been verified and meets all security constraints.
     */
    data class Clean(
        val timestampEpochMs: Long = System.currentTimeMillis(),
        val checksExecuted: Set<String> = emptySet()
    ) : IntegrityVerdict

    /**
     * One or more critical security violations were identified.
     */
    sealed interface Compromised : IntegrityVerdict {
        val detectedAt: Long
        val threatDescription: String

        data class DebuggerAttached(
            override val detectedAt: Long = System.currentTimeMillis(),
            val details: String = "Active debugger or ptrace hook detected"
        ) : Compromised {
            override val threatDescription: String get() = "DebuggerAttached: $details"
        }

        data class HookFrameworkDetected(
            val framework: String,
            override val detectedAt: Long = System.currentTimeMillis(),
            val trace: String = ""
        ) : Compromised {
            override val threatDescription: String get() = "HookFrameworkDetected: $framework ($trace)"
        }

        data class RootOrPrivilegeEscalation(
            val indicator: String,
            override val detectedAt: Long = System.currentTimeMillis()
        ) : Compromised {
            override val threatDescription: String get() = "RootDetected: $indicator"
        }

        data class SignatureMismatch(
            val expectedHash: String,
            val actualHash: String,
            override val detectedAt: Long = System.currentTimeMillis()
        ) : Compromised {
            override val threatDescription: String get() = "SignatureTampered: APK cert hash mismatch"
        }

        data class EmulatorDetected(
            val reason: String,
            override val detectedAt: Long = System.currentTimeMillis()
        ) : Compromised {
            override val threatDescription: String get() = "EmulatorDetected: $reason"
        }
    }
}
