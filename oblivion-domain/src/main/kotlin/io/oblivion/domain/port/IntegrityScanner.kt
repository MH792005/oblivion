package io.oblivion.domain.port

import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecurityPolicy

/**
 * Domain port for environment and integrity evaluation.
 */
interface IntegrityScanner {
    fun evaluate(policy: SecurityPolicy): IntegrityVerdict
}
