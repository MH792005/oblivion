package io.oblivion.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class OblivionHardenerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("oblivion", OblivionExtension::class.java)

        // 1. Zero-Config Dependency Injection
        project.afterEvaluate {
            if (project.configurations.findByName("implementation") != null) {
                val hasAnnotations = project.configurations.getByName("implementation").dependencies.any {
                    it.name.contains("oblivion-annotations")
                }
                if (!hasAnnotations) {
                    project.dependencies.add("implementation", project.files("${project.rootDir}/oblivion-annotations"))
                }
            }
        }

        // 2. AGP Variant Awareness & Task Registration
        project.plugins.withId("com.android.application") {
            val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                val isRelease = variant.name.equals("release", ignoreCase = true)
                val shouldTransform = isRelease || extension.enableInDebug

                if (shouldTransform) {
                    val mappingFileLocation = project.layout.buildDirectory.file("outputs/oblivion/mapping.txt")

                    val transformTask = project.tasks.register(
                        "oblivionTransform${variant.name.capitalize()}",
                        OblivionTransformTask::class.java
                    ) { task ->
                        task.obfuscateStrings.set(extension.obfuscateStrings)
                        task.flattenControlFlow.set(extension.flattenControlFlow)
                        task.mappingFile.set(mappingFileLocation)
                        task.inputDir.set(project.layout.buildDirectory.dir("intermediates/javac/${variant.name}/classes"))
                        task.outputDir.set(project.layout.buildDirectory.dir("outputs/oblivion/transformed-classes/${variant.name}"))
                    }

                    project.tasks.findByName("assemble${variant.name.capitalize()}")?.dependsOn(transformTask)
                }
            }
        }
    }
}
