package dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringDecryptor;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.List;
import java.util.Map;

public class PolymorphicStringDecryptor implements IStringDecryptor {
    private String name;
    private final int idxXor, traceXor;

    public PolymorphicStringDecryptor() {
        this.idxXor = random.nextInt(Character.MAX_VALUE);
        this.traceXor = random.nextInt(Character.MAX_VALUE);
    }

    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName) {
        var method = clazz.createMethod(ACC_PRIVATE | ACC_STATIC, name, getDescriptor());

        // ---- LOCALS ----
        var idxValVar = method.allocVar(Type.INT_TYPE); // param1
        var xorValueVar = method.allocVar(Type.INT_TYPE); // param2
        var keyParamVar = method.allocVar(Type.INT_TYPE); // param2

        var idxVar = method.allocVar(Type.INT_TYPE);
        var cachedTraceVar = method.allocVar();
        var charArrVar = method.allocVar();
        var iVar = method.allocVar(Type.INT_TYPE);

        // ---- CODE ----
        var body = new InsnBuilder()
                .label(new LabelNode())
                ._var(ILOAD, idxValVar)
                .add(context.properties().add(ASMUtils.pushInt(idxXor), Property.IGNORE_INTEGER))
                .ixor()
                ._var(ISTORE, idxVar);

    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<String> strings, String str) {
        return null;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescriptor() {
        return "(III)Ljava/lang/String;";
    }
}
