package dev.lvstrng.aidsfuscator.classgen.impl.hash;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;

import java.util.UUID;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("all")
public class BasicHashIntegrityClass implements IHashIntegrityClass {
    private final Context context;
    private JClass clazz;
    private JMethod retrieverMethod, hashMethod;
    private JField mapField, stringField, valueField;
    private final String hashFileName;

    public BasicHashIntegrityClass(Context context) {
        this.context = context;
        this.hashFileName = UUID.randomUUID().toString() + ".bin";
    }

    @Override
    public void init() {
        this.clazz = context.createClass("java/lang/Object", ACC_PUBLIC);
        this.mapField = clazz.createField(ACC_STATIC, context.dictionary().newFieldName(clazz, "Ljava/util/Map;"), "Ljava/util/Map;");
        this.stringField = clazz.createField(ACC_PRIVATE, context.dictionary().newFieldName(clazz, "Ljava/lang/String;"), "Ljava/lang/String;");
        this.valueField = clazz.createField(ACC_PRIVATE, context.dictionary().newFieldName(clazz, "I"), "I");

        this.createStaticInitializer();
        this.createInitializer();
        this.createHasher();
        this.createRetriever();
    }

    private void createStaticInitializer() {
        var method = clazz.findOrCreateClinit();

        var hashFileVar = method.allocVar();
        var bytesVar = method.allocVar();
        var iVar = method.allocVar(Type.INT_TYPE);
        var hashBytesVar = method.allocVar();
        var keyBytesVar = method.allocVar();
        var hashStringVar = method.allocVar();

        var loopLabel = new LabelNode();
        var list = new InsnBuilder()
                .label()
                .type(NEW, "java/util/HashMap")
                .dup()
                .method(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V")
                .field(PUTSTATIC, clazz.name(), mapField.name(), mapField.desc())

                .label()
                ._const(Type.getObjectType(clazz.name()))
                ._const("/" + hashFileName)
                .method(INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;")
                ._var(ASTORE, hashFileVar)

                .label()
                ._var(ALOAD, hashFileVar)
                .method(INVOKEVIRTUAL, "java/io/InputStream", "readAllBytes", "()[B")
                ._var(ASTORE, bytesVar)

                .label()
                ._int(0)
                ._var(ISTORE, iVar)

                .label(loopLabel)
                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._var(ILOAD, iVar)
                ._int(32)
                .iadd()
                .method(INVOKESTATIC, "java/util/Arrays", "copyOfRange", "([BII)[B")
                ._var(ASTORE, hashBytesVar)

                .label()
                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._int(32)
                .iadd()
                ._var(ILOAD, iVar)
                ._int(36)
                .iadd()
                .method(INVOKESTATIC, "java/util/Arrays", "copyOfRange", "([BII)[B")
                ._var(ASTORE, keyBytesVar)

                .label()
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, hashBytesVar)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V")
                .method(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;")
                ._var(ASTORE, hashStringVar)

                .label()
                .field(GETSTATIC, clazz.name(), mapField.name(), mapField.desc())
                ._var(ALOAD, hashStringVar)
                .type(NEW, clazz.name())
                .dup()
                ._var(ALOAD, hashStringVar)

                ._var(ALOAD, keyBytesVar)
                ._int(0)
                .baload()
                ._int(0xff)
                .iand()
                ._int(24)
                .ishl()
                ._var(ALOAD, keyBytesVar)
                ._int(1)
                .baload()
                ._int(0xff)
                .iand()
                ._int(16)
                .ishl()
                .ior()
                ._var(ALOAD, keyBytesVar)
                ._int(2)
                .baload()
                ._int(0xff)
                .iand()
                ._int(8)
                .ishl()
                .ior()
                ._var(ALOAD, keyBytesVar)
                ._int(3)
                .baload()
                ._int(0xff)
                .iand()
                .ior()
                .method(INVOKESPECIAL, clazz.name(), "<init>", "(Ljava/lang/String;I)V")
                .method(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                .pop()

                .label()
                .iinc(iVar, 36)

                .label()
                ._var(ILOAD, iVar)
                ._var(ALOAD, bytesVar)
                .arraylength()
                .jump(IF_ICMPNE, loopLabel)

                .label()
                ._return()
                ;

        method.insns().insert(list.result());
    }

    private void createInitializer() {
        var name = "<init>";
        var desc = "(Ljava/lang/String;I)V";
        var method = clazz.createMethod(0, name, desc);

        var thisVar = method.allocVar();
        var hashVar = method.allocVar();
        var valueVar = method.allocVar();

        new InsnBuilder(method.insns())
                .label()
                ._var(ALOAD, thisVar)
                .method(INVOKESPECIAL, clazz.superName(), "<init>", "()V")

                .label()
                ._var(ALOAD, thisVar)
                ._var(ALOAD, hashVar)
                .field(PUTFIELD, clazz.name(), stringField.name(), stringField.desc())

                .label()
                ._var(ALOAD, thisVar)
                ._var(ILOAD, valueVar)
                .field(PUTFIELD, clazz.name(), valueField.name(), valueField.desc())

                .label()
                ._return()
        ;
    }

    private void createHasher() {
        var desc = "([B)Ljava/lang/String;";
        this.hashMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, context.dictionary().newMethodName(clazz, desc), desc);

        var byteArrVar = hashMethod.allocVar();
        var hashVar = hashMethod.allocVar();
        var sbVar = hashMethod.allocVar();
        var iVar = hashMethod.allocVar(Type.INT_TYPE);

        var loopLbl = new LabelNode();
        var list = new InsnBuilder(hashMethod.insns())
                .label()
                ._const("SHA-256")
                .method(INVOKESTATIC, "java/security/MessageDigest", "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;")
                ._var(ALOAD, byteArrVar)
                .method(INVOKEVIRTUAL, "java/security/MessageDigest", "digest", "([B)[B")
                ._var(ASTORE, hashVar)

                .label()
                .type(NEW, "java/lang/StringBuilder")
                .dup()
                .method(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V")
                ._var(ASTORE, sbVar)

                .label()
                ._int(0)
                ._var(ISTORE, iVar)

                .label(loopLbl)
                ._var(ALOAD, sbVar)
                ._const("%02x")
                ._int(1)
                .anewarray("java/lang/Object")
                .dup()
                ._int(0)
                ._var(ALOAD, hashVar)
                ._var(ILOAD, iVar)
                .baload()
                ._int(0xff)
                .iand()
                .i2b()
                .method(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;")
                .aastore()
                .method(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;")
                .method(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                .pop()

                .label()
                .iinc(iVar, 1)

                .label()
                ._var(ILOAD, iVar)
                ._var(ALOAD, hashVar)
                .arraylength()
                .jump(IF_ICMPLT, loopLbl)

                .label()
                ._var(ALOAD, sbVar)
                .method(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;")
                ._areturn()
                ;
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
                .method(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;")
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
                .method(INVOKESTATIC, clazz.name(), hashMethod.name() /*"a"*/, hashMethod.desc() /*"([B)Ljava/lang/String;"*/)
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
