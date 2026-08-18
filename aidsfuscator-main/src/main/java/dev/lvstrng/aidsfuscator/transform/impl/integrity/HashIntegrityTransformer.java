package dev.lvstrng.aidsfuscator.transform.impl.integrity;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class HashIntegrityTransformer extends Transformer {
    public HashIntegrityTransformer() {
        super("Hash Integrity Checks", "hashIntegrity");
    }

    @Override
    public void transform(Context context) {
        context.hashIntegrityClass().init();

        for(var clazz : context.classes()) {
            obfuscateClass(context, clazz);
        }

        if(changes() != 0) {
            context.addArtificial(context.hashIntegrityClass().get());
        }
    }

    private void obfuscateClass(Context context, JClass clazz) {
        var addedField = new AtomicReference<JField>(null);
        var desc = "I";
        var field = (Supplier<JField>) () -> {
            if(addedField.get() == null) {
                addedField.set(clazz.createField((clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL, context.dictionary().newFieldName(clazz, desc), desc));
                return addedField.get();
            }

            return addedField.get();
        };
        var value = context.hashIntegrityClass().classValue(clazz);
        if(Exclusions.HASH_INTEGRITY.excluded(clazz))
            return;

        for(var method : clazz.methods()) {
            if(Exclusions.HASH_INTEGRITY.excluded(method))
                continue;

            for(var insn : method.insns()) {
                if(!ASMUtils.isIntPush(insn))
                    continue;

                if(!context.properties().get(insn).has(Property.SENSITIVE_CONSTANT))
                    continue;

                var v = ASMUtils.getInt(insn);
                var hashField = field.get();

                var ls = new InsnBuilder()
                        .field(GETSTATIC, clazz.name(), hashField.name(), hashField.desc())
                        ._int(value ^ v).addProps(context, Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                        .ixor();

                method.insns().insertBefore(insn, ls.result());
                method.insns().remove(insn);
                markChange();
            }
        }

        if(addedField.get() != null) {
            var clinit = clazz.findOrCreateClinit();
            var integrity = context.hashIntegrityClass();

            var list = new InsnBuilder()
                    .label()
                    .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                    .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                    ._int(integrity.paramValue(clazz))
                    .method(INVOKESTATIC, integrity.get().name(), context.hashIntegrityClass().retriever().name(), context.hashIntegrityClass().retriever().desc())
                    .field(PUTSTATIC, clazz.name(), addedField.get().name(), addedField.get().desc());

            clinit.insns().insert(list.result());
        }
    }
}
