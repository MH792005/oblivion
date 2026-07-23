package io.oblivion.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Control Flow Flattening (CFF) transformer using OW2 ASM Tree API.
 * Converts method control flow graphs into state-machine driven switch-case dispatchers
 * executing inside a while(true) loop with opaque mathematical predicates.
 */
public class ControlFlowFlatteningVisitor {

    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final MethodNode methodNode;
    private final OblivionMappingWriter mappingWriter;

    public ControlFlowFlatteningVisitor(String className, String methodName, String methodDesc, MethodNode methodNode, OblivionMappingWriter mappingWriter) {
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.methodNode = methodNode;
        this.mappingWriter = mappingWriter;
    }

    public void transform() {
        if (methodNode.instructions == null || methodNode.instructions.size() == 0) {
            return;
        }

        // Avoid flattening abstract/native methods or constructors
        if ((methodNode.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || methodName.equals("<init>")) {
            return;
        }

        // Partition instructions into Basic Blocks
        List<List<AbstractInsnNode>> blocks = partitionBasicBlocks(methodNode.instructions);
        if (blocks.size() <= 1) {
            return; // Not enough blocks to flatten
        }

        int numBlocks = blocks.size();
        int[] stateIds = new int[numBlocks];
        Random random = new Random(className.hashCode() ^ methodName.hashCode());

        for (int i = 0; i < numBlocks; i++) {
            stateIds[i] = Math.abs(random.nextInt(0x7FFFFFFF - 1000)) + 1000 + i;
            mappingWriter.recordStateMapping(className, methodName, methodDesc, stateIds[i], i + 1);
        }

        MethodNode newMethod = new MethodNode(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions.toArray(new String[0]));

        Label entryLabel = new Label();
        Label loopHeaderLabel = new Label();
        Label loopExitLabel = new Label();

        // Find max local variable index to store state variable
        int stateVarSlot = methodNode.maxLocals;
        newMethod.maxLocals = stateVarSlot + 2;

        InsnList newInsns = newMethod.instructions;

        // Initialize state variable with first block state ID
        newInsns.add(new LabelNode(entryLabel));
        pushInt(newInsns, stateIds[0]);
        newInsns.add(new VarInsnNode(Opcodes.ISTORE, stateVarSlot));

        // Start while(true) loop
        newInsns.add(new LabelNode(loopHeaderLabel));

        // Add Opaque Predicate: if ((x * (x + 1)) % 2 != 0) goto loopExitLabel (never taken)
        newInsns.add(new VarInsnNode(Opcodes.ILOAD, stateVarSlot));
        newInsns.add(new VarInsnNode(Opcodes.ILOAD, stateVarSlot));
        newInsns.add(new InsnNode(Opcodes.ICONST_1));
        newInsns.add(new InsnNode(Opcodes.IADD));
        newInsns.add(new InsnNode(Opcodes.IMUL));
        newInsns.add(new InsnNode(Opcodes.ICONST_2));
        newInsns.add(new InsnNode(Opcodes.IREM));
        newInsns.add(new JumpInsnNode(Opcodes.IFNE, new LabelNode(loopExitLabel)));

        // Load state variable for switch dispatch
        newInsns.add(new VarInsnNode(Opcodes.ILOAD, stateVarSlot));

        LabelNode[] caseLabels = new LabelNode[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            caseLabels[i] = new LabelNode();
        }

        LabelNode defaultLabel = new LabelNode(loopExitLabel);

        // LookupSwitchInsnNode for state-machine dispatch
        newInsns.add(new LookupSwitchInsnNode(defaultLabel, stateIds, caseLabels));

        // Build switch cases
        for (int i = 0; i < numBlocks; i++) {
            newInsns.add(caseLabels[i]);

            List<AbstractInsnNode> blockInsns = blocks.get(i);
            for (AbstractInsnNode insn : blockInsns) {
                // If block ends in return or throw, pass through
                if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN || insn.getOpcode() == Opcodes.ATHROW) {
                    newInsns.add(insn.clone(null));
                } else if (!(insn instanceof JumpInsnNode)) {
                    newInsns.add(insn.clone(null));
                }
            }

            // Transition to next state or loop back
            if (i < numBlocks - 1) {
                pushInt(newInsns, stateIds[i + 1]);
                newInsns.add(new VarInsnNode(Opcodes.ISTORE, stateVarSlot));
                newInsns.add(new JumpInsnNode(Opcodes.GOTO, new LabelNode(loopHeaderLabel)));
            } else {
                newInsns.add(new JumpInsnNode(Opcodes.GOTO, new LabelNode(loopExitLabel)));
            }
        }

        // Loop Exit / Default Fallback
        newInsns.add(new LabelNode(loopExitLabel));
        newInsns.add(new InsnNode(Opcodes.RETURN));

        // Replace instructions in original method node
        methodNode.instructions = newInsns;
    }

    private List<List<AbstractInsnNode>> partitionBasicBlocks(InsnList insns) {
        List<List<AbstractInsnNode>> blocks = new ArrayList<>();
        List<AbstractInsnNode> currentBlock = new ArrayList<>();

        for (AbstractInsnNode node : insns.toArray()) {
            if (node instanceof LineNumberNode || node instanceof FrameNode) {
                continue;
            }
            currentBlock.add(node);

            if (node instanceof JumpInsnNode || (node.getOpcode() >= Opcodes.IRETURN && node.getOpcode() <= Opcodes.RETURN) || node.getOpcode() == Opcodes.ATHROW) {
                blocks.add(currentBlock);
                currentBlock = new ArrayList<>();
            }
        }

        if (!currentBlock.isEmpty()) {
            blocks.add(currentBlock);
        }

        return blocks;
    }

    private void pushInt(InsnList insns, int val) {
        if (val >= -1 && val <= 5) {
            insns.add(new InsnNode(Opcodes.ICONST_0 + val));
        } else if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
            insns.add(new IntInsnNode(Opcodes.BIPUSH, val));
        } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            insns.add(new IntInsnNode(Opcodes.SIPUSH, val));
        } else {
            insns.add(new LdcInsnNode(val));
        }
    }
}
