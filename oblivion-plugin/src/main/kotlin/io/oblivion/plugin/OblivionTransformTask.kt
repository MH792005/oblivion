package io.oblivion.plugin

import io.oblivion.asm.OblivionClassTransformer
import io.oblivion.asm.OblivionMappingWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class OblivionTransformTask : DefaultTask() {

    @get:Input
    abstract val obfuscateStrings: Property<Boolean>

    @get:Input
    abstract val flattenControlFlow: Property<Boolean>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputFile
    abstract val mappingFile: RegularFileProperty

    @TaskAction
    fun transform() {
        val inDirectory = inputDir.get().asFile
        val outDirectory = outputDir.get().asFile
        val mapFile = mappingFile.get().asFile

        outDirectory.deleteRecursively()
        outDirectory.mkdirs()

        val mappingWriter = OblivionMappingWriter()
        val transformer = OblivionClassTransformer(
            obfuscateStrings.get(),
            flattenControlFlow.get(),
            mappingWriter
        )

        if (inDirectory.exists()) {
            inDirectory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(inDirectory).path
                    val outputFile = File(outDirectory, relativePath)
                    outputFile.parentFile.mkdirs()

                    if (file.extension == "class") {
                        val originalBytes = file.readBytes()
                        val transformedBytes = transformer.transform(originalBytes)
                        outputFile.writeBytes(transformedBytes)
                    } else {
                        file.copyTo(outputFile, overwrite = true)
                    }
                }
            }
        }

        mappingWriter.writeMappingFile(mapFile)
    }
}
