package dev.lvstrng.aidsfuscator.salt;

import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClassSalt {
    private final JClass clazz;
    private final JField field;
    private final int value;

    public ClassSalt(JClass clazz, JField field, int value) {
        this.clazz = clazz;
        this.field = field;
        this.value = value;
    }

    public int value() {
        return value;
    }

    public AbstractInsnNode load() {
        return new FieldInsnNode(Opcodes.GETSTATIC, clazz.name(), field.name(), "I");
    }

    public AbstractInsnNode store() {
        return new FieldInsnNode(Opcodes.PUTSTATIC, clazz.name(), field.name(), "I");
    }
}
