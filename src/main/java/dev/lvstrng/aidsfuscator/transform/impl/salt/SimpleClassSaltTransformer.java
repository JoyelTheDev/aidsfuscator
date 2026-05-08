package dev.lvstrng.aidsfuscator.transform.impl.salt;

import dev.lvstrng.aidsfuscator.classgen.impl.SimpleClassSaltClassGenerator;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.salt.impl.ClassSalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Modifier;

public class SimpleClassSaltTransformer extends Transformer {
    private final Setting<Boolean> unifyAccess = setting("unifyAccess", true);

    public SimpleClassSaltTransformer() {
        super("Class Salting", "classSalting");
    }

    @Override
    public void transform(Context context) {
        // ---- ADD SALT FIELDS ----
        for(var clazz : context.classes()) {
            if((clazz.access() & ACC_MODULE) != 0)
                continue;

            if(Exclusions.CLASS_SALTING.excluded(clazz))
                continue;

            if(!Modifier.isPublic(clazz.access())) {
                if(!unifyAccess.value())
                    continue;

                if(Modifier.isPrivate(clazz.access()))
                    clazz.core().access &= ~(ACC_PRIVATE);
                if(Modifier.isProtected(clazz.access()))
                    clazz.core().access &= ~(ACC_PROTECTED);
                clazz.core().access |= ACC_PUBLIC;
            }

            var val = random.nextInt();
            var name = context.dictionary().newFieldName(clazz, "I");

            var field = clazz.createField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, name, "I");
            clazz.setSalt(new ClassSalt(clazz, field, val));

            for(var method : clazz.methods()) {
                if(Exclusions.CLASS_SALTING.excluded(method))
                    continue;

                if(method.hasSalt())
                    continue;

                if(method.isAbstract() || method.isNative() || method.insns().size() == 0)
                    continue;

                if(method.name().equals("<clinit>"))
                    continue;

                var saltLocal = method.allocVar(Type.INT_TYPE);
                var saltValue = random.nextInt();
                prepSalt(context, clazz, method, saltLocal, saltValue);
                obfuscateUnprotectedSalts(context, method, saltLocal, saltValue);
            }

            markChange();
        }

        // ---- ADD SALT DISPATCHER ----
        var gen = new SimpleClassSaltClassGenerator();
        var clazz = gen.create(context);
        context.addArtificial(clazz);
    }

    private void obfuscateUnprotectedSalts(Context context, JMethod method, int saltLocal, int saltValue) {
        var frames = method.frames(context);
        for(var insn : method.insns()) {
            if(!context.properties().get(insn).has(Property.UNPROTECTED_SALT))
                continue;

            if(frames.get(insn).getLocal(saltLocal).isUninitialized())
                continue;

            var n = ASMUtils.getInt(insn);
            var list = new InsnList();

            list.add(method.salt().load());
            list.add(context.properties().add(ASMUtils.pushInt(n ^ saltValue), Property.IGNORE_INTEGER));
            list.add(new InsnNode(IXOR));

            method.insns().insertBefore(insn, list);
            method.insns().remove(insn);
        }
    }

    private void prepSalt(Context context, JClass clazz, JMethod method, int saltLocal, int saltValue) {
        var list = new InsnBuilder();
        var mask = random.nextInt();
        var maskedSalt = clazz.salt().value() & mask;

        list
                .add(clazz.salt().load())
                ._int(mask).addProps(context, Property.IGNORE_INTEGER)
                .iand()
                ._int(maskedSalt ^ saltValue).addProps(context, Property.IGNORE_INTEGER)
                .ixor()
                ._var(ISTORE, saltLocal);

        method.makeSalt(saltValue, saltLocal);
        method.insns().insert(list.result());
    }
}
