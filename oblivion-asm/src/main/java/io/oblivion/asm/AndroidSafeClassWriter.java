package io.oblivion.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Android-safe extension of OW2 ASM ClassWriter.
 * Overrides getCommonSuperClass() to prevent ClassNotFoundException during Gradle builds
 * when resolving Android framework classes (e.g. android.app.Activity) on host JVM classloaders.
 */
public class AndroidSafeClassWriter extends ClassWriter {

    public AndroidSafeClassWriter(int flags) {
        super(flags);
    }

    public AndroidSafeClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Exception e) {
            // Fall back to java/lang/Object if hierarchy resolution cannot be determined on host JVM
            return "java/lang/Object";
        }
    }
}
