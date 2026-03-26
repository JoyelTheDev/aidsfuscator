package dev.lvstrng.aidsfuscator.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class ASMUtils implements Opcodes {
    public static boolean isReturn(int opcode) {
        return opcode >= IRETURN && opcode <= RETURN;
    }

    public static boolean isReturn(AbstractInsnNode insn) {
        return isReturn(insn.getOpcode());
    }

    public static AbstractInsnNode pushInt(int n) {
        if(n >= -1 && n <= 5)
            return new InsnNode(ICONST_0 + n);

        if(n >= -128 && n <= 127)
            return new IntInsnNode(BIPUSH, n);
        if(n >= -32768 && n <= 32767)
            return new IntInsnNode(SIPUSH, n);

        return new LdcInsnNode(n);
    }

    public static int getInt(AbstractInsnNode insn) {
        var op = insn.getOpcode();
        if(op >= ICONST_M1 && op <= ICONST_5)
            return op - ICONST_0;

        if(op == BIPUSH || op == SIPUSH)
            return ((IntInsnNode) insn).operand;

        if(insn instanceof LdcInsnNode ldc)
            return (int)ldc.cst;

        throw new IllegalArgumentException("Not number insn: " + insn.getOpcode());
    }
}
