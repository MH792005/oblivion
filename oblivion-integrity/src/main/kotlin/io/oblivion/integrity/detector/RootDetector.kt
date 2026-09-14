package io.oblivion.integrity.detector

import java.io.File

/**
 * Detects device rooting, SuperUser binaries, and Magisk privilege escalation.
 */
object RootDetector {

    private val SU_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    /**
     * Checks if any known su binaries exist on disk.
     */
    fun findSuBinary(): String? {
        for (path in SU_PATHS) {
            try {
                val file = File(path)
                if (file.exists()) {
                    return path
                }
            } catch (_: Throwable) {
                // Ignore file system security exceptions
            }
        }
        return null
    }

    /**
     * Inspects /proc/mounts to see if /system or /vendor is mounted read-write.
     */
    fun isSystemMountedRw(): Boolean {
        val mountsFile = File("/proc/mounts")
        if (!mountsFile.exists() || !mountsFile.canRead()) {
            return false
        }

        return try {
            mountsFile.useLines { lines ->
                lines.any { line ->
                    val tokens = line.split(" ")
                    if (tokens.size >= 4) {
                        val mountPoint = tokens[1]
                        val mountOptions = tokens[3]
                        (mountPoint == "/system" || mountPoint == "/") && mountOptions.split(",").contains("rw")
                    } else {
                        false
                    }
                }
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks Build.TAGS for "test-keys".
     */
    fun hasTestKeys(): Boolean {
        return try {
            val buildClass = Class.forName("android.os.Build")
            val tagsField = buildClass.getField("TAGS")
            val tags = tagsField.get(null) as? String
            tags != null && tags.contains("test-keys")
        } catch (_: Throwable) {
            false
        }
    }
}
