package io.oblivion.integrity

import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecurityPolicy
import io.oblivion.domain.port.IntegrityScanner
import io.oblivion.integrity.detector.DebuggerDetector
import io.oblivion.integrity.detector.FridaDetector
import io.oblivion.integrity.detector.RootDetector

/**
 * Domain implementation of [IntegrityScanner].
 * Orchestrates multi-vector defense checks and returns unforgeable [IntegrityVerdict] models.
 */
class OblivionIntegrityEngine : IntegrityScanner {

    override fun evaluate(policy: SecurityPolicy): IntegrityVerdict {
        val executedChecks = mutableSetOf<String>()

        // 1. Debugger & Tracer Check
        if (policy.checkDebugger) {
            executedChecks.add("debugger")
            if (DebuggerDetector.isTracerAttached()) {
                return IntegrityVerdict.Compromised.DebuggerAttached(details = "TracerPid is non-zero")
            }
            if (DebuggerDetector.isDebuggerConnected()) {
                return IntegrityVerdict.Compromised.DebuggerAttached(details = "Debugger actively attached")
            }
        }

        // 2. Frida & Dynamic Hooking Frameworks
        if (policy.checkFridaAndHooks) {
            executedChecks.add("hooking")
            val injected = FridaDetector.findInjectedLibraries()
            if (injected != null) {
                return IntegrityVerdict.Compromised.HookFrameworkDetected(framework = injected)
            }
            if (FridaDetector.isFridaServerListening()) {
                return IntegrityVerdict.Compromised.HookFrameworkDetected(framework = "Frida Server Port Open")
            }
            val suspiciousThread = FridaDetector.hasSuspiciousThreads()
            if (suspiciousThread != null) {
                return IntegrityVerdict.Compromised.HookFrameworkDetected(
                    framework = "Frida Thread: $suspiciousThread"
                )
            }
        }

        // 3. Root / Privilege Escalation
        if (policy.checkRoot) {
            executedChecks.add("root")
            val suPath = RootDetector.findSuBinary()
            if (suPath != null) {
                return IntegrityVerdict.Compromised.RootOrPrivilegeEscalation(indicator = "Binary at $suPath")
            }
            if (RootDetector.hasTestKeys()) {
                return IntegrityVerdict.Compromised.RootOrPrivilegeEscalation(indicator = "Build with test-keys")
            }
        }

        return IntegrityVerdict.Clean(
            checksExecuted = executedChecks
        )
    }
}
