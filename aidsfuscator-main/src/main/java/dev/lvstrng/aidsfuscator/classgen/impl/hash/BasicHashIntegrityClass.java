package dev.lvstrng.aidsfuscator.classgen.impl.hash;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.invoke.StringConcatFactory;

import static org.objectweb.asm.Opcodes.*;

public class BasicHashIntegrityClass implements IHashIntegrityClass {
    private final Context context;
    private JClass clazz;
    private JMethod retrieverMethod, hashMethod;
    private JField mapField, stringField, valueField;
    private ByteArrayOutputStream hashFile;

    public BasicHashIntegrityClass(Context context) {
        this.context = context;
        this.hashFile = new ByteArrayOutputStream();
    }

    @Override
    public void init() {
        this.clazz = context.createClass("java/lang/Object", ACC_PUBLIC);
        this.mapField = clazz.createField(ACC_STATIC, context.dictionary().newFieldName(clazz, "Ljava/util/Map;"), "Ljava/util/Map;");
        this.stringField = clazz.createField(ACC_PRIVATE, context.dictionary().newFieldName(clazz, "Ljava/lang/String;"), "Ljava/lang/String;");
        this.valueField = clazz.createField(ACC_PRIVATE, context.dictionary().newFieldName(clazz, "I"), "I");

        this.createRetriever();
    }

    private void createRetriever() {
        var desc = "(Ljava/lang/Class;I)I";
        this.retrieverMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, context.dictionary().newMethodName(clazz, desc), desc);

        // ---- LOCALS ----
        var classVar = retrieverMethod.allocVar();
        var keyVar = retrieverMethod.allocVar(Type.INT_TYPE);
        var nameVar = retrieverMethod.allocVar();
        var loaderVar = retrieverMethod.allocVar();
        var inVar = retrieverMethod.allocVar();
        var instanceVar = retrieverMethod.allocVar();

        var loaderNull = new LabelNode();
        var loaderExit = new LabelNode();
        var inExists = new LabelNode();
        var list = new InsnBuilder(retrieverMethod.insns())
                .label()
                ._var(ALOAD, classVar)
                .method(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/Class;")
                ._int('.')
                ._int('/')
                .method(INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;")
                ._const(".class")
                .method(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;")
                ._var(ASTORE, nameVar)

                .label()
                ._var(ALOAD, classVar)
                .method(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
                ._var(ASTORE, loaderVar)

                .label()
                ._var(ALOAD, loaderVar)
                .jump(IFNULL, loaderNull)

                .label()
                ._var(ALOAD, loaderVar)
                ._var(ALOAD, nameVar)
                .method(INVOKEVIRTUAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;")
                ._var(ASTORE, inVar)

                .label()
                ._goto(loaderExit)

                .label(loaderNull)
                ._var(ALOAD, nameVar)
                .method(INVOKESTATIC, "java/lang/ClassLoader", "getSystemResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;")
                ._var(ASTORE, inVar)

                .label(loaderExit)
                ._var(ALOAD, inVar)
                .jump(IFNONNULL, inExists)

                .label()
                ._int(-1)
                ._ireturn()

                .label(inExists)
                .field(GETSTATIC, clazz.name(), mapField.name(), mapField.desc())
                ._var(ALOAD, inVar)
                .method(INVOKEVIRTUAL, "java/io/InputStream", "readAllBytes", "()[B")
                .method(INVOKESTATIC, clazz.name(), /*hashMethod.name()*/ "a", /*hashMethod.desc()*/ "([B)Ljava/lang/String;")
                .method(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;")
                .type(CHECKCAST, clazz.name())
                ._var(ASTORE, instanceVar)

                .label()
                ._var(ALOAD, instanceVar)
                .field(GETFIELD, clazz.name(), valueField.name(), valueField.desc())
                ._var(ALOAD, instanceVar)
                .field(GETFIELD, clazz.name(), stringField.name(), stringField.desc())
                .method(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I")
                ._var(ILOAD, keyVar)
                .ixor()
                .ixor()
                ._ireturn()
                ;


    }

    @Override
    public JClass get() {
        return clazz;
    }

    @Override
    public void add(JClass clazz) {

    }

    @Override
    public void obfuscate(JClass clazz) {

    }
}
