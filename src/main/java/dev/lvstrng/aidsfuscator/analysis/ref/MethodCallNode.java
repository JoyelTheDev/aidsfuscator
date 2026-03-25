package dev.lvstrng.aidsfuscator.analysis.ref;

import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public record MethodCallNode(JClass callerClass, JMethod caller, JMethod method, AbstractInsnNode insn) {
    public boolean isDynamic() {
        var isCondy = insn instanceof LdcInsnNode ldc && ldc.cst instanceof ConstantDynamic;
        var isIndy = insn instanceof InvokeDynamicInsnNode;
        return isCondy || isIndy;
    }

    public boolean canEdit() {
        return !isDynamic() && !method.isLibrary();
    }

    @Override
    public String toString() {
        return "{ " + caller + " -> " + method + " }";
    }
}
