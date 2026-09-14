# Oblivion (`io.oblivion.security`)

[![Build & Test](https://github.com/MH792005/oblivion/actions/workflows/ci.yml/badge.svg)](https://github.com/MH792005/oblivion/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Oblivion** is a lightweight, high-performance application security and anti-tamper framework for Android and the JVM, written entirely in pure Kotlin and Java.

Rather than relying on fragile bytecode mutation plugins that break with compiler and build-tool updates, Oblivion is built around **Domain-Driven Design (DDD)**, **hardware-backed isolation (Android StrongBox / TEE)**, **zero-allocation memory safety**, and **multi-vector RASP**.

---

## Architecture

Oblivion is split into clean, decoupled modules with strict dependency boundaries:

```
oblivion/
├── oblivion-domain/     # Pure Kotlin: value objects, sealed verdicts, domain ports
├── oblivion-crypto/     # Pure JVM: AES-GCM, constant-time comparisons, HKDF-SHA256
├── oblivion-integrity/  # Multi-vector RASP (anti-debug, anti-Frida, anti-root, signature)
├── oblivion-android/    # Android adapters: StrongBox/TEE KeyStore, auto-init provider
└── app/                 # Sample application with R8 minification enabled
```

### Key Modules

* **`:oblivion-domain`**: The foundation. Defines typed value objects, domain exceptions (`OblivionException`), and the sealed `IntegrityVerdict` hierarchy. Contains zero external dependencies.
* **`:oblivion-crypto`**: High-throughput cryptography utilizing CPU hardware intrinsics (AES-NI on x86, ARMv8 Crypto Extensions). Includes constant-time comparison primitives to defeat side-channel timing attacks, direct `ByteBuffer` support, and RFC 5869 HKDF-SHA256 key derivation.
* **`:oblivion-integrity`**: Runtime Application Self-Protection (RASP). Inspects `/proc/self/status` for `TracerPid`, scans `/proc/self/maps` for injection agents (Frida, Xposed, Substrate), checks loopback debug ports (27042/27043), and verifies APK signing certificates against expected SHA-256 hashes. Includes `IntegrityMonitor` for continuous coroutine-based background polling.
* **`:oblivion-android`**: Integrates with Android hardware. Manages AES-256 keys inside dedicated hardware chips (StrongBox HSM on Android 9+, with automatic fallback to TEE), and exposes an auto-bootstrapping `OblivionInitProvider`.

---

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // For Android applications
    implementation("io.oblivion.security:oblivion-android:2.0.0")

    // Or for standalone JVM / backend projects
    implementation("io.oblivion.security:oblivion-crypto:2.0.0")
    implementation("io.oblivion.security:oblivion-integrity:2.0.0")
}
```

---

## Quickstart

### 1. Evaluate Environment Integrity (RASP)

Evaluate whether the current device is rooted, actively debugged, or injected with hooking tools:

```kotlin
import io.oblivion.android.Oblivion
import io.oblivion.domain.model.IntegrityVerdict
import io.oblivion.domain.model.SecurityPolicy

val verdict = Oblivion.verifyIntegrity(context, SecurityPolicy.STRICT)

when (verdict) {
    is IntegrityVerdict.Clean -> {
        // Environment is secure. Executed checks: verdict.checksExecuted
        proceedWithSecureFlow()
    }
    is IntegrityVerdict.Compromised -> {
        // Security violation detected (e.g. debugger, Frida, root)
        println("Security Violation: ${verdict.threatDescription}")
        terminateSession()
    }
}
```

### 2. Continuous Background Monitoring

You can also monitor the runtime environment continuously via a Kotlin Coroutine `Flow` to catch tools attached after startup:

```kotlin
lifecycleScope.launch {
    Oblivion.monitorIntegrity(SecurityPolicy.STRICT, intervalMs = 5000L)
        .collect { verdict ->
            if (verdict is IntegrityVerdict.Compromised) {
                // Attacker attached a debugger or hooked a function during runtime
                terminateApp()
            }
        }
}
```

### 3. Hardware-Accelerated Authenticated Encryption

Encrypt sensitive data with AES-GCM (128-bit authentication tag) and decrypt into memory-safe containers:

```kotlin
val secretKey = ByteArray(32) // 256-bit AES key
val sensitiveData = "CONFIDENTIAL_USER_SESSION".toByteArray()
val contextAad = "USER_ID_10492".toByteArray()

// Encrypt
val encrypted = Oblivion.encrypt(sensitiveData, secretKey, associatedData = contextAad)

// Decrypt into memory-safe container
Oblivion.decrypt(encrypted, secretKey, associatedData = contextAad).useAndDestroy { rawBytes ->
    // Use sensitive bytes safely here
    transmitPayload(rawBytes)
}
// Memory buffer is actively zeroed out in RAM upon exiting the block
```

### 4. Hardware KeyStore (StrongBox / TEE)

Generate and retrieve keys stored directly inside the physical hardware security chip:

```kotlin
val hardwareKey = Oblivion.getOrCreateHardwareKey("payment_signing_key")
```

---

## Building from Source

### Run All Unit & Concurrency Tests
```bash
./gradlew test
```

### Build Sample Release APK (with R8 Minification)
```bash
./gradlew :app:assembleRelease
```

### Publish to Local Maven Repository (`~/.m2/repository`)
```bash
./gradlew publishToMavenLocal
```

---

## License

Oblivion is open-source software licensed under the [Apache License 2.0](LICENSE).
