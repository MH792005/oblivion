package io.oblivion.integrity

import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecurityPolicy
import io.oblivion.domain.port.IntegrityScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Continuous background integrity monitor.
 * Emits periodic [IntegrityVerdict] updates so applications can react if an attacker
 * attaches Frida, GDB, or drops a root payload while the app is actively running.
 */
class IntegrityMonitor(
    private val scanner: IntegrityScanner = OblivionIntegrityEngine()
) {

    /**
     * Polls the environment at a specified [intervalMs] and emits verdicts.
     */
    fun monitor(
        policy: SecurityPolicy = SecurityPolicy.STRICT,
        intervalMs: Long = 5000L
    ): Flow<IntegrityVerdict> = flow {
        while (true) {
            val verdict = scanner.evaluate(policy)
            emit(verdict)
            delay(intervalMs)
        }
    }
}
