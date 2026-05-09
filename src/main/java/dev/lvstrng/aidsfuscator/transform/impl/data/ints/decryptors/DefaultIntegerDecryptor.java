package dev.lvstrng.aidsfuscator.transform.impl.data.ints.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.ints.IIntegerDecryptor;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;
import java.util.Map;

public class DefaultIntegerDecryptor implements IIntegerDecryptor {
    private String name;
    private final int idxXor;

    public DefaultIntegerDecryptor() {
        this.idxXor = random.nextInt();
    }

    @Override
    public void generate(JClass clazz, String fieldName) {
        int access = (clazz.isInterface())
                ? ACC_PUBLIC | ACC_STATIC
                : ACC_PRIVATE | ACC_STATIC;
        var method = clazz.createMethod(access, name, getDescriptor());

        // ---- LOCALS ----
        var idxVal = method.allocVar(Type.INT_TYPE);
        var key = method.allocVar(Type.INT_TYPE);

        new InsnBuilder(method.insns())
                .label(new LabelNode())

                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), fieldName, "[I")
                ._var(ILOAD, idxVal)
                ._int(idxXor)
                .ixor()
                .iaload()
                ._var(ILOAD, key)
                .ixor()
                ._var(ILOAD, idxVal)
                .ixor()
                ._ireturn()
        ;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescriptor() {
        return "(II)I";
    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<Integer> numbers, int num) {
        // ---- PREPARE KEYS -----
        var key = method.hasSalt() ? method.salt().value() : random.nextInt();
        int idxValue = numbers.size() ^ idxXor;
        num = ASMUtils.getInt(callSite) ^ key ^ idxValue;
        numbers.add(num);

        // ---- INSTRUCTIONS ----
        var builder = new InsnBuilder()._int(idxValue);
        if(method.canSalt(frames.get(callSite))) {
            builder.add(method.salt().load());
        } else {
            builder._int(key);
        }
        builder.add(context.properties().add(
                new MethodInsnNode(INVOKESTATIC, method.owner().name(), name, getDescriptor(), method.owner().isInterface()), Property.IGNORE_REF_OBFUSCATION
        ));
        return builder.result();
    }
}
