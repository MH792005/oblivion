plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":oblivion-domain"))
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "io.oblivion.security"
            artifactId = "oblivion-crypto"
            version = "2.0.0"
        }
    }
}
