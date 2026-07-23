package io.oblivion.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that extracts String constants in targeted methods,
 * converts them to XOR-encrypted byte arrays, and splices dynamic native decryption invocations.
 */
public class StringObfuscationVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;

    public StringObfuscationVisitor(int api, MethodVisitor methodVisitor, String className, String methodName) {
        super(api, methodVisitor);
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof String) {
            String originalStr = (String) value;
            if (!originalStr.isEmpty()) {
                byte key = (byte) (0x5A ^ (originalStr.hashCode() & 0xFF));
                byte[] rawBytes = originalStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] encryptedBytes = new byte[rawBytes.length];
                for (int i = 0; i < rawBytes.length; i++) {
                    encryptedBytes[i] = (byte) (rawBytes[i] ^ key);
                }

                // Push encrypted byte array size to stack
                pushInt(encryptedBytes.length);
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);

                // Populate byte array element by element
                for (int i = 0; i < encryptedBytes.length; i++) {
                    mv.visitInsn(Opcodes.DUP);
                    pushInt(i);
                    pushInt(encryptedBytes[i]);
                    mv.visitInsn(Opcodes.BASTORE);
                }

                // Push XOR Key
                pushInt(key);

                // Call OblivionCore.decryptString(byte[], byte) -> String
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "io/oblivion/runtime/OblivionCore",
                        "decryptString",
                        "([BB)Ljava/lang/String;",
                        false
                );
                return;
            }
        }
        super.visitLdcInsn(value);
    }

    private void pushInt(int val) {
        if (val >= -1 && val <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + val);
        } else if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, val);
        } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, val);
        } else {
            mv.visitLdcInsn(val);
        }
    }
}
