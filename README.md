# Project Oblivion (`io.oblivion.security`)

[![Build & Security Verification](https://github.com/MH792005/oblivion/actions/workflows/ci.yml/badge.svg)](https://github.com/MH792005/oblivion/actions/workflows/ci.yml)
[![Gradle Plugin Portal](https://img.shields.io/badge/Gradle%20Plugin-io.oblivion.hardener-blue.svg)](https://plugins.gradle.org/plugin/io.oblivion.hardener)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Project Oblivion** is a polyglot, enterprise-grade security plugin and native runtime protection suite designed to protect JVM and Android applications against advanced reverse engineering, Frida dynamic instrumentation, Xposed hooks, Ghidra/Jadx decompilation, and symbolic execution.

The core philosophy of Project Oblivion is **Economic Denial**: shifting the cost of reverse engineering from a 10-minute automated script to weeks of grueling manual analysis, while delivering a **Zero-Config Developer Experience** where developers simply apply the Gradle plugin (`io.oblivion.hardener`) and click "Build."

---

## 🏛️ Monorepo Architecture

```
oblivion/
├── oblivion-annotations/  # [Kotlin JVM] Lightweight annotation (@Oblivion) & OblivionCore bridge
├── oblivion-crypto/       # [Rust / Cargo] White-box crypto, scrambled S-box tables & zeroize static lib
├── oblivion-runtime/      # [C++17 / CMake] Native RASP daemon (ARM64/x86_64 raw assembly syscalls)
├── oblivion-asm/          # [Java / OW2 ASM 9.x] Control Flow Flattening (CFF) & String Encryption
├── oblivion-plugin/       # [Kotlin DSL] AGP Variant API Hardener Plugin (io.oblivion.hardener)
└── app/                   # [Kotlin / Android] Integration sample application
```

---

## ⚡ Key Security Capabilities

### 1. Control Flow Flattening (CFF)
- Converts targeted method control flow graphs into state-machine driven `switch-case` dispatchers inside a `while(true)` loop.
- Uses opaque mathematical predicates to defeat static decompilation in Jadx, Ghidra, and IDA Pro.

### 2. Dynamic XOR String Obfuscation
- Extracts string constants from annotated methods, converts them to XOR-encrypted byte arrays, and replaces them with dynamic native decryption calls (`OblivionCore.decryptString`).

### 3. Native RASP (Runtime Application Self-Protection) Daemon
- **Direct Linux Assembly Syscalls**: Bypasses `libc.so` PLT symbol hooks entirely using raw assembly `svc #0` calls for `openat` (56), `read` (63), `close` (57), and `exit_group` (94) with multi-ABI portable fallback guards (`syscalls.h`).
- **Memory Scanner Daemon**: Spawns an independent detached thread on library load inspecting `/proc/self/maps` every 3 seconds for signature substrings (`"frida"`, `"xposed"`, `"gum-js"`, `"substrate"`). Triggers an immediate nuclear kernel exit (`exit_group(137)`) upon detection.

### 4. White-Box Cryptography & Hardware Binding
- Scrambled lookup tables for AES/custom block ciphers embedded directly into binary text sections.
- **Hardware Key Derivation**: `derive_hardware_key(salt, salt_len)` binds static salt with device-unique runtime attributes.
- **Zero-Memory Guarantee**: All intermediate cryptographic buffers automatically zero-out memory using Rust `zeroize` before deallocation.

### 5. Automatic Entry Point Splicing & Annotation Stripping
- Automatically splices `OblivionCore.init()` and `System.loadLibrary("oblivion_secure")` into entry points.
- Strips `@Oblivion` annotations post-mutation to leave zero static metadata traces in the compiled DEX.

---

## 🚀 Quick Start Guide

### Step 1: Apply the Hardener Plugin

Add `io.oblivion.hardener` to your application's `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.oblivion.hardener") version "1.0.0"
}
```

### Step 2: Configure the Plugin DSL

Configure the `oblivion` extension block in `build.gradle.kts`:

```kotlin
oblivion {
    enableInDebug = false       // Set to true to run mutations on Debug builds (default false)
    obfuscateStrings = true     // Enable dynamic XOR string obfuscation
    flattenControlFlow = true   // Enable Control Flow Flattening (CFF)
}
```

### Step 3: Annotate Target Functions or Classes

Apply the `@Oblivion` annotation to classes or sensitive functions:

```kotlin
package io.oblivion.sample

import android.app.Activity
import android.os.Bundle
import io.oblivion.annotations.Oblivion

@Oblivion(flatten = true, obfuscateStrings = true)
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val token = computeSensitiveToken("UserSession_2026_Enterprise")
        println("Generated Token: $token")
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
```

---

## 🛠️ De-obfuscation Stack Mapping File

During Release compilation, the plugin outputs a proprietary mapping file at:

```
build/outputs/oblivion/mapping.txt
```

This file correlates flattened state-machine block IDs back to original source code method names and line numbers for crash stack de-obfuscation.

---

## 🔧 Build & Publication Commands

### Build Full Monorepo
```bash
./gradlew assembleRelease --stacktrace
```

### Publish Plugin to Local Maven Repository (`~/.m2/repository`)
```bash
./gradlew :oblivion-plugin:publishToMavenLocal
```

### Validate Plugin Portal Publishing
```bash
./gradlew :oblivion-plugin:publishPlugins --validate-only
```

---

## 🔐 Publishing Security Best Practices

> [!IMPORTANT]
> **Never commit secret publishing keys or API tokens into version control.**

Always store your Gradle Plugin Portal credentials in your global user directory (`~/.gradle/gradle.properties`):

```properties
gradle.publish.key=YOUR_GRADLE_PUBLISH_KEY
gradle.publish.secret=YOUR_GRADLE_PUBLISH_SECRET
```

Or pass them via CI/CD environment variables:
- `GRADLE_PUBLISH_KEY`
- `GRADLE_PUBLISH_SECRET`

---

## 📄 License

Project Oblivion is licensed under the [Apache License 2.0](LICENSE).
