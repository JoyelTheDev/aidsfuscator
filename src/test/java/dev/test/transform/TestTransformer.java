package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.salt.impl.ClassSalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.classgen.impl.SimpleClassSaltClassGenerator;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

public class TestTransformer extends Transformer {
    public TestTransformer() {
        super("Test", "test");
    }

    @Override
    public void transform(Context context) {
        /*for(var clazz : context.classes()) {
            var clinit = clazz.findOrCreateClinit();

            var list = new InsnList();
            list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            list.add(new LdcInsnNode(clazz.name()));
            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));

            clinit.insns().insert(list);
        }*/

        var clazz = context.forName("crackme/utils/logging/Logger");
        var cl = clazz.findOrCreateClinit();

        var list = new InsnList();
        list.add(new InsnNode(ACONST_NULL));
        list.add(new InsnNode(ATHROW));

        cl.insns().insert(list);
    }
}
