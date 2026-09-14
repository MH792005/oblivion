package io.oblivion.integrity.detector

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetAddress
import java.net.Socket

/**
 * Multi-vector detection for Frida, Xposed, and dynamic instrumentation agents.
 */
object FridaDetector {

    private val SUSPICIOUS_LIBRARIES = arrayOf(
        "frida",
        "gadget",
        "xposed",
        "substrate",
        "cydiasubstrate",
        "sandhook",
        "epic"
    )

    private val SUSPICIOUS_THREADS = arrayOf(
        "gum-js-loop",
        "gmain",
        "frida"
    )

    private val FRIDA_DEFAULT_PORTS = intArrayOf(27042, 27043)

    /**
     * Inspects /proc/self/maps to detect injected shared objects and hooked libraries.
     */
    fun findInjectedLibraries(): String? {
        val mapsFile = File("/proc/self/maps")
        if (!mapsFile.exists() || !mapsFile.canRead()) {
            return null
        }

        try {
            BufferedReader(FileReader(mapsFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val lower = line!!.lowercase()
                    for (suspicious in SUSPICIOUS_LIBRARIES) {
                        if (lower.contains(suspicious)) {
                            return suspicious
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore access errors
        }
        return null
    }

    /**
     * Checks if default Frida server ports are actively listening on loopback.
     */
    fun isFridaServerListening(): Boolean {
        for (port in FRIDA_DEFAULT_PORTS) {
            try {
                Socket(InetAddress.getByName("127.0.0.1"), port).use {
                    return true
                }
            } catch (_: Throwable) {
                // Connection failed = port closed / no server
            }
        }
        return false
    }

    /**
     * Checks active threads for known Frida runtime worker names.
     */
    fun hasSuspiciousThreads(): String? {
        val taskDir = File("/proc/self/task")
        if (!taskDir.exists() || !taskDir.isDirectory) {
            return null
        }

        try {
            taskDir.listFiles()?.forEach { task ->
                val commFile = File(task, "comm")
                if (commFile.exists() && commFile.canRead()) {
                    val commName = commFile.readText().trim().lowercase()
                    for (suspicious in SUSPICIOUS_THREADS) {
                        if (commName.contains(suspicious)) {
                            return suspicious
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore access errors
        }
        return null
    }
}
