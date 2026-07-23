plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":oblivion-asm"))
    implementation(project(":oblivion-annotations"))
    compileOnly("com.android.tools.build:gradle:8.2.2")
}

gradlePlugin {
    plugins {
        create("oblivionHardener") {
            id = "io.oblivion.hardener"
            implementationClass = "io.oblivion.plugin.OblivionHardenerPlugin"
            displayName = "Oblivion Security Hardener"
            description = "Zero-Config JVM/Android Security & Bytecode Obfuscation Engine"
        }
    }
}
