package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.salt.impl.ClassSalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Uses the ClassInitOrder functionality to give classes salts
 * @author lvstrng
 */
public class ClassSaltTransformer extends Transformer {
    @SuppressWarnings("all")
    private final int fieldAccess = ACC_PUBLIC | ACC_STATIC;

    public ClassSaltTransformer() {
        super("Class Salting", "classSalt");
    }

    @Override
    public void transform(Context context) {
        if(context.initOrder().pairs().isEmpty())
            return;

        // ---- INIT ----
        var allSalts = new HashMap<JClass, Integer>();
        var silentSalts = new HashMap<JClass, Integer>();
        initializeSalts(context, allSalts, silentSalts);

        // ---- DO METHOD SEEDS ----
        for(var clazz : allSalts.keySet()) {
            if(silentSalts.containsKey(clazz))
                continue;

            var salt = clazz.salt();
            for(var method : clazz.methods()) {
                if(method.hasSalt())
                    continue;

                if(method.name().equals("<clinit>"))
                    continue;

                // ---- INIT SALT VAR ----
                var methodSaltVal = random.nextInt();
                var seedVar = method.allocVar(Type.INT_TYPE);
                method.makeSalt(methodSaltVal, seedVar);
                var storeSalt = new VarInsnNode(ISTORE, seedVar);

                var list = new InsnBuilder()
                        .add(salt.load())
                        .add(context.properties().add(ASMUtils.pushInt(methodSaltVal ^ salt.value()), Property.IGNORE_INTEGER))
                        .ixor()
                        .add(storeSalt);

                method.insns().insert(list.result());
                method.setSafeInsn(storeSalt);

                // ---- OBFUSCATE UNPROTECTED SALTS ----
                for(var insn : method.insns()) {
                    if(!context.properties().get(insn).has(Property.UNPROTECTED_SALT))
                        continue;

                    var num = ASMUtils.getInt(insn);
                    var ls = new InsnList();
                    ls.add(method.salt().load());
                    ls.add(context.properties().add(ASMUtils.pushInt(num ^ methodSaltVal), Property.IGNORE_INTEGER));
                    ls.add(new InsnNode(IXOR));

                    method.insns().insertBefore(insn, ls);
                    method.insns().remove(insn);
                }
            }
        }
    }

    private void initializeSalts(Context context, Map<JClass, Integer> allSalts, Map<JClass, Integer> silentSalts) {
        // ---- PREPARE FIELDS + CLASS ----
        populateSaltFields(context, allSalts, silentSalts);
        var gen = new ClassInitOrderClassGen(context);
        var saltClass = gen.create();

        // ---- INIT SILENT SEEDS ----
        for(var clazz : silentSalts.keySet()) {
            var salt = silentSalts.get(clazz);
            var clinit = clazz.findOrCreateClinit();

            var list = new InsnBuilder()
                    ._const(clazz.type())
                    ._int(salt)
                    .method(INVOKESTATIC, saltClass.name(), gen.set.name(), gen.set.desc());

            clinit.insns().insert(list.result());
        }

        // ---- INIT REAL SEEDS ----
        for(var pair : context.initOrder().pairs()) {
            var initializer = pair.first;
            var clazz = pair.second;

            var initSalt = allSalts.get(initializer);
            var builder = new InsnBuilder()
                    ._const(initializer.type())
                    .method(INVOKESTATIC, saltClass.name(), gen.get.name(), gen.get.desc())
                    .add(context.properties().add(ASMUtils.pushInt(clazz.salt().value() ^ initSalt), Property.IGNORE_INTEGER))
                    .ixor()
                    .add(clazz.salt().store())

                    ._const(clazz.type())
                    .add(clazz.salt().load())
                    .method(INVOKESTATIC, saltClass.name(), gen.set.name(), gen.set.desc());

            var clinit = clazz.findOrCreateClinit();
            var safe = builder.result().getLast();

            clinit.insns().insert(builder.result());
            clinit.setSafeInsn(safe);
        }

        context.addArtificial(saltClass);
    }

    // `first` gets initialized before `second`
    private void populateSaltFields(Context context, Map<JClass, Integer> salts, Map<JClass, Integer> silentSalts) {
        var added = new HashSet<JClass>();

        for(var pair : context.initOrder().pairs()) {
            var second = pair.second;
            if(!added.add(second))
                continue;

            // ---- MAKE SALT ----
            var fieldName = context.dictionary().newFieldName(second, "I");
            var saltField = second.add(new FieldNode(fieldAccess, fieldName, "I", null, null));
            var saltValue = random.nextInt();

            second.setSalt(new ClassSalt(second, saltField, saltValue));
            salts.put(second, saltValue);
        }

        // ---- SILENT SALTS ----
        for(var pair : context.initOrder().pairs()) {
            var first = pair.first;
            if(silentSalts.containsKey(first))
                continue;

            if(first.hasSalt())
                continue;

            var salt = random.nextInt();
            silentSalts.put(first, salt);
            salts.put(first, salt);
        }
    }
}
