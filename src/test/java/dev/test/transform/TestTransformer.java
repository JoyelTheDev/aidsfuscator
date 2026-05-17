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
        for(var clazz : context.classes()) {
            if(clazz.name().endsWith("Main"))
                continue;

            var clinit = clazz.findOrCreateClinit();

            var list = new InsnList();
            list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            list.add(new LdcInsnNode(clazz.name() + ", "));
            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V"));

            list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            list.add(new TypeInsnNode(NEW, "java/lang/Throwable"));
            list.add(new InsnNode(DUP));
            list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V"));
            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
            list.add(new InsnNode(ICONST_1));
            list.add(new InsnNode(AALOAD));
            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;"));
            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));

            clinit.insns().insert(list);
        }
    }
}
