package dev.lvstrng.aidsfuscator.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class ASMUtils implements Opcodes {
    public static boolean isReturn(int opcode) {
        return opcode >= IRETURN && opcode <= RETURN;
    }

    public static boolean isReturn(AbstractInsnNode insn) {
        return isReturn(insn.getOpcode());
    }
}
