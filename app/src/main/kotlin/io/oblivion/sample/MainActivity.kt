package io.oblivion.sample

import android.app.Activity
import android.os.Bundle
import io.oblivion.annotations.Oblivion
import io.oblivion.runtime.OblivionCore

@Oblivion(flatten = true, obfuscateStrings = true)
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OblivionCore initializes native library and RASP security daemons automatically
        OblivionCore.init()

        val secretKey = computeSensitiveToken("UserSession_2026_Enterprise")
        val status = if (OblivionCore.isProtected) "SECURE" else "UNPROTECTED"
        println("Oblivion Security Status: $status | Token: $secretKey")
    }

    @Oblivion
    fun computeSensitiveToken(salt: String): String {
        var token = "HEADER_"
        val data = salt.toByteArray()
        for (i in data.indices) {
            token += (data[i].toInt() xor 0x7A).toChar()
        }
        return token + "_FOOTER"
    }
}
