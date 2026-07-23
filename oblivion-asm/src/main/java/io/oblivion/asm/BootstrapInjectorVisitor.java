package io.oblivion.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM Visitor that splices System.loadLibrary("oblivion_secure") and OblivionCore.init()
 * at the start of Application.onCreate() or main entry point method.
 */
public class BootstrapInjectorVisitor extends ClassVisitor {

    private final String className;

    public BootstrapInjectorVisitor(int api, ClassVisitor classVisitor, String className) {
        super(api, classVisitor);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals("onCreate") || name.equals("main")) {
            return new MethodVisitor(api, mv) {
                @Override
                public void visitCode() {
                    super.visitCode();
                    // Splice System.loadLibrary("oblivion_secure") & OblivionCore.init()
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "io/oblivion/runtime/OblivionCore",
                            "init",
                            "()V",
                            false
                    );
                }
            };
        }

        return mv;
    }
}
