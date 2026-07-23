package io.oblivion.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Core Bytecode Transformation Engine for Oblivion using OW2 ASM 9.x.
 */
public class OblivionClassTransformer {

    private static final String TARGET_ANNOTATION = "Lio/oblivion/annotations/Oblivion;";

    private final boolean obfuscateStrings;
    private final boolean flattenControlFlow;
    private final OblivionMappingWriter mappingWriter;

    public OblivionClassTransformer(boolean obfuscateStrings, boolean flattenControlFlow, OblivionMappingWriter mappingWriter) {
        this.obfuscateStrings = obfuscateStrings;
        this.flattenControlFlow = flattenControlFlow;
        this.mappingWriter = mappingWriter;
    }

    public byte[] transform(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        String className = cr.getClassName();

        // 3. Self-Referential Obfuscation Exclusion Filter: Never mutate internal framework or system classes
        if (className != null && (
                className.startsWith("io/oblivion/") ||
                className.startsWith("kotlin/") ||
                className.startsWith("java/") ||
                className.startsWith("android/") ||
                className.startsWith("androidx/")
        )) {
            return classBytes;
        }

        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        cr.accept(classNode, 0);

        boolean classHasAnnotation = hasOblivionAnnotation(classNode.invisibleAnnotations) ||
                                     hasOblivionAnnotation(classNode.visibleAnnotations);

        boolean shouldProcessClass = classHasAnnotation;

        for (MethodNode mn : classNode.methods) {
            boolean methodHasAnnotation = hasOblivionAnnotation(mn.invisibleAnnotations) ||
                                          hasOblivionAnnotation(mn.visibleAnnotations);

            if (classHasAnnotation || methodHasAnnotation) {
                shouldProcessClass = true;

                if (flattenControlFlow) {
                    ControlFlowFlatteningVisitor cff = new ControlFlowFlatteningVisitor(
                            classNode.name, mn.name, mn.desc, mn, mappingWriter
                    );
                    cff.transform();
                }
            }
        }

        if (!shouldProcessClass) {
            return classBytes;
        }

        // 4. Use AndroidSafeClassWriter to prevent ClassNotFoundException during Gradle Android compilation
        AndroidSafeClassWriter cw = new AndroidSafeClassWriter(cr, AndroidSafeClassWriter.COMPUTE_FRAMES | AndroidSafeClassWriter.COMPUTE_MAXS);
        
        AnnotationStripperVisitor stripper = new AnnotationStripperVisitor(Opcodes.ASM9, cw);
        BootstrapInjectorVisitor bootstrap = new BootstrapInjectorVisitor(Opcodes.ASM9, stripper, classNode.name);

        classNode.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, bootstrap) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                org.objectweb.asm.MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (obfuscateStrings && shouldProcessClass) {
                    return new StringObfuscationVisitor(api, mv, classNode.name, name);
                }
                return mv;
            }
        });

        return cw.toByteArray();
    }

    private boolean hasOblivionAnnotation(java.util.List<AnnotationNode> annotations) {
        if (annotations == null) return false;
        for (AnnotationNode an : annotations) {
            if (TARGET_ANNOTATION.equals(an.desc)) {
                return true;
            }
        }
        return false;
    }
}
