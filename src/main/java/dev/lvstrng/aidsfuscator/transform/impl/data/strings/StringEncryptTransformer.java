package dev.lvstrng.aidsfuscator.transform.impl.data.strings;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors.DefaultStringDecryptor;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors.PolymorphicStringDecryptor;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.initializers.DefaultStringInitializer;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.initializers.XorStringInitializer;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StringEncryptTransformer extends Transformer {
    private final Setting<Boolean> translateConcat = setting("translateConcat", true);
    private final Setting<Integer> minLength = setting("minLength", 1);

    private static final List<Supplier<IStringInitializer>> initializers = List.of(
            DefaultStringInitializer::new,
            XorStringInitializer::new
    );

    private static final List<Supplier<IStringDecryptor>> decryptors = List.of(
            DefaultStringDecryptor::new,
            PolymorphicStringDecryptor::new
    );

    public StringEncryptTransformer() {
        super("Encrypt String Constants", "encryptStrings");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(clazz.isInterface() && clazz.version() < V1_8)
                continue;

            if(Exclusions.STRING_ENCRYPTION.excluded(clazz))
                continue;

            var strings = new ArrayList<String>();
            var fieldName = context.dictionary().newFieldName(clazz, "[Ljava/lang/String;");

            var initializer = initializers.get(random.nextInt(initializers.size())).get(); // random initializer
            var decryptor = decryptors.get(random.nextInt(decryptors.size())).get(); // random decryptor
            decryptor.setName(context.dictionary().newMethodName(clazz, decryptor.getDescriptor()));

            for(var method : clazz.methods()) {
                if(Exclusions.STRING_ENCRYPTION.excluded(method))
                    continue;

                if(translateConcat.value())
                    ASMUtils.translateConcatenation(method);

                var frames = method.frames(context);
                for(var insn : method.insns()) {
                    if(!(insn instanceof LdcInsnNode ldc && ldc.cst instanceof String str))
                        continue;

                    if(context.properties().get(insn).has(Property.IGNORE_STRING))
                        continue;

                    if(str.length() < minLength.value())
                        continue;

                    if(str.length() > Character.MAX_VALUE) {
                        Logger.warn("String constant in '%s' too big for string encryption.", method.fullOriginalName());
                        continue;
                    }

                    method.insns().insertBefore(ldc, decryptor.addAndCall(context, method, ldc, frames, strings, str));
                    method.insns().remove(ldc);
                    markChange();
                }
            }

            if(strings.isEmpty()) {
                context.dictionary().revertField();
                context.dictionary().revertMethod();
                continue;
            }

            int access = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;
            clazz.createField(access, fieldName, "[Ljava/lang/String;");

            var cacheName = context.dictionary().newFieldName(clazz, "[Ljava/lang/Object;");
            clazz.createField(access, cacheName, "[Ljava/lang/Object;");

            initializer.generate(context, clazz, fieldName, cacheName, strings);
            decryptor.generate(context, clazz, fieldName, cacheName);
        }
    }
}
