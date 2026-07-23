plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":oblivion-annotations"))
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
}
