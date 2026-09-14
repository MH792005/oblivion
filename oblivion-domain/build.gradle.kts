plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "io.oblivion.security"
            artifactId = "oblivion-domain"
            version = "2.0.0"
        }
    }
}
