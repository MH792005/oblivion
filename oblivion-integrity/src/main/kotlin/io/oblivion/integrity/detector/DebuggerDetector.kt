package io.oblivion.integrity.detector

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Detects active debugging, tracing, and ptrace attachment.
 */
object DebuggerDetector {

    /**
     * Checks whether TracerPid in /proc/self/status is non-zero.
     * When GDB, LLDB, or Frida attach via ptrace, Linux sets TracerPid to the debugger's PID.
     */
    fun isTracerAttached(): Boolean {
        val statusFile = File("/proc/self/status")
        if (!statusFile.exists() || !statusFile.canRead()) {
            return false
        }

        try {
            BufferedReader(FileReader(statusFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("TracerPid:")) {
                        val pidStr = line!!.substring("TracerPid:".length).trim()
                        val pid = pidStr.toIntOrNull() ?: 0
                        return pid > 0
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore filesystem access restrictions
        }
        return false
    }

    /**
     * Checks JVM / Android debug flags via reflection.
     */
    fun isDebuggerConnected(): Boolean {
        return try {
            val debugClass = Class.forName("android.os.Debug")
            val isDebuggerConnectedMethod = debugClass.getMethod("isDebuggerConnected")
            isDebuggerConnectedMethod.invoke(null) as? Boolean ?: false
        } catch (_: Throwable) {
            // Non-Android JVM fallback using reflection
            try {
                val mfClass = Class.forName("java.lang.management.ManagementFactory")
                val getRuntimeMXBean = mfClass.getMethod("getRuntimeMXBean")
                val runtimeMXBean = getRuntimeMXBean.invoke(null)
                val getInputArguments = runtimeMXBean.javaClass.getMethod("getInputArguments")
                val args = getInputArguments.invoke(runtimeMXBean) as? List<*> ?: emptyList<Any>()
                args.any { it.toString().contains("-agentlib:jdwp") || it.toString().contains("-Xrunjdwp") }
            } catch (_: Throwable) {
                false
            }
        }
    }
}
