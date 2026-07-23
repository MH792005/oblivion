package io.oblivion.annotations

/**
 * Annotation used to target functions or classes for Oblivion anti-reverse engineering transformations
 * (Control Flow Flattening, String Obfuscation, and Native RASP protection).
 *
 * Retention is BINARY so bytecode transformers can process it before DEX generation,
 * after which the annotation is stripped from the final bytecode.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Oblivion(
    val flatten: Boolean = true,
    val obfuscateStrings: Boolean = true
)
