package io.oblivion.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Strips @Oblivion annotations from classes and methods post-transformation
 * to leave zero static metadata traces in the final compiled DEX binary.
 */
public class AnnotationStripperVisitor extends ClassVisitor {

    private static final String TARGET_ANNOTATION = "Lio/oblivion/annotations/Oblivion;";

    public AnnotationStripperVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (TARGET_ANNOTATION.equals(descriptor)) {
            return null; // Strip annotation
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(api, mv) {
            @Override
            public AnnotationVisitor visitAnnotation(String annDesc, boolean visible) {
                if (TARGET_ANNOTATION.equals(annDesc)) {
                    return null; // Strip annotation
                }
                return super.visitAnnotation(annDesc, visible);
            }
        };
    }
}
