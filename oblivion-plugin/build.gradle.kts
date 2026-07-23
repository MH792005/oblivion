plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "2.1.1"
    id("maven-publish")
}

group = "io.oblivion.security"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":oblivion-asm"))
    implementation(project(":oblivion-annotations"))
    compileOnly("com.android.tools.build:gradle:8.5.0")
}

gradlePlugin {
    website.set("https://github.com/MH792005/oblivion")
    vcsUrl.set("https://github.com/MH792005/oblivion.git")

    plugins {
        create("oblivionHardener") {
            id = "io.oblivion.hardener"
            displayName = "Project Oblivion Security Hardener"
            description = "Enterprise-grade polyglot security framework for Android/JVM"
            tags.set(listOf("android", "security", "obfuscation", "rasp", "bytecode"))
            implementationClass = "io.oblivion.plugin.OblivionHardenerPlugin"
        }
    }
}
