package io.oblivion.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Enhanced CFF visitor with null-safe line number lookup to support release builds stripped of debug symbols.
 */
public class NullSafeControlFlowFlatteningVisitor extends ControlFlowFlatteningVisitor {

    public NullSafeControlFlowFlatteningVisitor(String className, String methodName, String methodDesc, MethodNode methodNode, OblivionMappingWriter mappingWriter) {
        super(className, methodName, methodDesc, methodNode, mappingWriter);
    }

    public static int extractFirstLineNumber(MethodNode methodNode) {
        if (methodNode == null || methodNode.instructions == null) return -1;
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn instanceof LineNumberNode) {
                return ((LineNumberNode) insn).line;
            }
        }
        return -1;
    }
}
