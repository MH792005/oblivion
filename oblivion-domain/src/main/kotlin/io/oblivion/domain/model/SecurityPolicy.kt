package io.oblivion.domain.model

/**
 * Immutable security policy specifying which environmental checks must pass.
 */
data class SecurityPolicy(
    val checkDebugger: Boolean = true,
    val checkFridaAndHooks: Boolean = true,
    val checkRoot: Boolean = true,
    val checkSignature: Boolean = true,
    val expectedSignatureSha256: String? = null,
    val allowEmulators: Boolean = false
) {
    companion object {
        val STRICT = SecurityPolicy(
            checkDebugger = true,
            checkFridaAndHooks = true,
            checkRoot = true,
            checkSignature = true,
            allowEmulators = false
        )

        val PERMISSIVE_DEV = SecurityPolicy(
            checkDebugger = false,
            checkFridaAndHooks = false,
            checkRoot = false,
            checkSignature = false,
            allowEmulators = true
        )
    }
}
