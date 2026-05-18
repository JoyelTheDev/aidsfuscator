package dev.lvstrng.aidsfuscator.classgen.impl;

import dev.lvstrng.aidsfuscator.classgen.IClassGen;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Random;

import static org.objectweb.asm.Opcodes.*;

public class SimpleClassSaltClassGenerator implements IClassGen {
    public JMethod retrieverMethod;

    @Override
    public JClass create(Context context) {
        var clazz = context.createClass("java/lang/Object", ACC_PUBLIC | ACC_SUPER);
        var fieldName = context.dictionary().newFieldName(clazz, "Ljava/util/Map;");
        var field = clazz.createField(ACC_STATIC, fieldName, "Ljava/util/Map;");

        generateRetrieverMethod(context, field, clazz);
        generateInitializersAndClinit(context, field, clazz);

        return clazz;
    }

    private void generateInitializersAndClinit(Context context, JField mapField, JClass clazz) {
        var saltyClasses = context.classes().stream().filter(JClass::hasSalt).toList();
        var count = (int) Math.ceil((double) saltyClasses.size() / 1000);

        var clinit = clazz.findOrCreateClinit();
        var clinitBuilder = new InsnBuilder()
                .type(NEW, "java/util/HashMap")
                .dup()
                .method(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V")
                .field(PUTSTATIC, clazz.name(), mapField.name(), mapField.desc());

        int j = 0;
        for(int i = 0; i < count; i++) {
            var name = context.dictionary().newMethodName(clazz, "()V");
            var method = clazz.createMethod(ACC_STATIC, name, "()V");

            var builder = new InsnBuilder(method.insns())
                    .field(GETSTATIC, clazz.name(), mapField.name(), mapField.desc());

            for(int n = Math.min(j + 1000, saltyClasses.size()); j < n; j++) {
                var saltClass = saltyClasses.get(j);
                var key = new Random().nextInt();

                builder.dup()
                        ._const(saltClass.type())
                        ._int(key ^ saltClass.salt().value())
                        .method(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
                        .method(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
                        .pop();

                var classClinit = saltClass.findOrCreateClinit();
                var list = new InsnList();
                list.add(new LdcInsnNode(saltClass.type()));

                if(saltClass.hasFirstInitializerClass() && saltClass.getFirstInitializerClass().hasSalt()) {
                    var otherSalt = saltClass.getFirstInitializerClass().salt();
                    list.add(context.properties().add(ASMUtils.pushInt(key ^ otherSalt.value()), Property.IGNORE_INTEGER));
                    list.add(otherSalt.load());
                    list.add(new InsnNode(IXOR));
                } else {
                    list.add(context.properties().add(ASMUtils.pushInt(key), Property.IGNORE_INTEGER));
                }

                list.add(new MethodInsnNode(INVOKESTATIC, clazz.name(), retrieverMethod.name(), retrieverMethod.desc()));
                list.add(saltClass.salt().store());

                classClinit.setSafeInsn(list.getLast());
                classClinit.insns().insert(list);
            }

            builder._return();
            clinitBuilder.method(INVOKESTATIC, clazz.name(), name, "()V");
        }

        clinit.insns().insert(clinitBuilder.result());
    }

    private void generateRetrieverMethod(Context context, JField mapField, JClass clazz) {
        var desc = "(Ljava/lang/Object;I)I";
        var name = context.dictionary().newMethodName(clazz, desc);
        retrieverMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, desc);

        // ---- LOCALS ----
        var objVar = retrieverMethod.allocVar();
        var keyVar = retrieverMethod.allocVar(Type.INT_TYPE);

        // ---- CODE ----
        var builder = new InsnBuilder(retrieverMethod.insns())
                .field(GETSTATIC, clazz.name(), mapField.name(), mapField.desc())
                ._var(ALOAD, objVar)
                .method(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;")
                .type(CHECKCAST, "java/lang/Integer")
                .method(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I")
                ._var(ILOAD, keyVar)
                .ixor()
                ._ireturn();
    }
}