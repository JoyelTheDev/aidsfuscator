package dev.lvstrng.aidsfuscator.analysis.call;

import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public record FieldCallNode(JClass callerClass, JMethod caller, JField field, AbstractInsnNode insn) {
    public boolean isGetter() {
        var op = insn.getOpcode();
        return op == Opcodes.GETFIELD || op == Opcodes.GETSTATIC;
    }

    public boolean isSetter() {
        var op = insn.getOpcode();
        return op == Opcodes.PUTFIELD || op == Opcodes.PUTSTATIC;
    }

    @Override
    public String toString() {
        return "{ " + caller + " -> " + field + " }";
    }
}
