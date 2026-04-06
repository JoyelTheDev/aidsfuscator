package dev.lvstrng.aidsfuscator.salt;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MethodSalt {
    private int value;
    private int local;

    public MethodSalt(int value, int local) {
        this.value = value;
        this.local = local;
    }

    public int value() {
        return value;
    }

    public int local() {
        return local;
    }

    public void updateValue(int value) {
        this.value = value;
    }

    public void updateVar(int local) {
        this.local = local;
    }

    public AbstractInsnNode load() {
        return new VarInsnNode(Opcodes.ILOAD, local);
    }
}
