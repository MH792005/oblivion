pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "oblivion"

include(":oblivion-annotations")
include(":oblivion-crypto")
include(":oblivion-runtime")
include(":oblivion-asm")
include(":oblivion-plugin")
include(":app")
