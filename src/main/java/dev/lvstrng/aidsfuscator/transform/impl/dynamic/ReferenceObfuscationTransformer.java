package dev.lvstrng.aidsfuscator.transform.impl.dynamic;

import dev.lvstrng.aidsfuscator.classgen.impl.ReferenceObfuscationClassGenerator;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.CryptUtils;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ReferenceObfuscationTransformer extends Transformer {
    private final List<String> references = new ArrayList<>();

    public ReferenceObfuscationTransformer() {
        super("Reference Obfuscation", "referenceObfuscate");
    }

    @Override
    public void transform(Context context) {
        var chars = getChars(); // {v, s, vg, sg, vs, ss}
        var indexXor = random.nextInt();

        var gen = new ReferenceObfuscationClassGenerator(chars, indexXor);
        var refClass = gen.create(context);

        for(var clazz : context.classes()) {
            if(Exclusions.REFERENCE_OBFUSCATE.excluded(clazz))
                continue;

            if(clazz.isEnum())
                continue;

            if(clazz.version() < Opcodes.V1_7)
                continue;

            for(var method : clazz.methods()) {
                if(Exclusions.REFERENCE_OBFUSCATE.excluded(method))
                    continue;

                if(method.properties().has(Property.STRING_DECRYPTOR, Property.INTEGER_DECRYPTOR))
                    continue;

                var frames = method.frames(context);
                for(var insn : method.insns()) {
                    var frame = frames.get(insn);
                    if(frame == null)
                        continue;

                    if(!frame.isInitThis())
                        continue;

                    switch (insn) {
                        case MethodInsnNode call -> {
                            if(call.owner.startsWith("["))
                                continue;

                            if(!context.referenceManager().canObfuscate(call))
                                continue;

                            if(context.properties().get(call).has(Property.IGNORE_REF_OBFUSCATION))
                                continue;

                            char callSiteChar;
                            if(call.getOpcode() == INVOKEVIRTUAL || call.getOpcode() == INVOKEINTERFACE) {
                                callSiteChar = chars[0];
                            } else if (call.getOpcode() == INVOKESTATIC) {
                                callSiteChar = chars[1];
                            } else {
                                continue;
                            }

                            var decKey = random.nextInt() >> 16;
                            var idx = add(call.owner, call.name, call.desc, decKey);

                            var handle = new Handle(H_INVOKESTATIC, refClass.name(), gen.outerInvoker.name(), gen.outerInvoker.desc(), false);
                            var desc = call.getOpcode() == INVOKESTATIC ? call.desc : call.desc.replace("(", "(Ljava/lang/Object;");

                            var list = new InsnList();
                            var indy = new InvokeDynamicInsnNode(
                                    Character.toString(callSiteChar),
                                    desc.replace(")", "II)"),
                                    handle
                            );

                            int xorIndex = idx ^ indexXor;
                            list.add(context.properties().add(ASMUtils.pushInt(xorIndex), Property.IGNORE_INTEGER));
                            if(method.canSalt(frame)) {
                                var mask = random.nextInt();
                                var masked = method.salt().value() & mask;

                                list.add(method.salt().load());
                                list.add(context.properties().add(ASMUtils.pushInt(mask), Property.IGNORE_INTEGER));
                                list.add(new InsnNode(IAND));
                                list.add(context.properties().add(ASMUtils.pushInt(masked ^ (decKey << 16)), Property.IGNORE_INTEGER));
                                list.add(new InsnNode(IXOR));
                            } else {
                                list.add(context.properties().add(ASMUtils.pushInt((decKey << 16) | random.nextInt(Short.MAX_VALUE)), Property.IGNORE_INTEGER));
                            }
                            list.add(indy);

                            method.insns().insertBefore(call, list);
                            method.insns().remove(call);
                            markChange();
                        }
                        case FieldInsnNode field -> {
                            if(!context.referenceManager().canObfuscate(field))
                                continue;

                            if(context.properties().get(field).has(Property.IGNORE_REF_OBFUSCATION))
                                continue;

                            var getter = field.getOpcode() == GETSTATIC || field.getOpcode() == GETFIELD;
                            var virtual = field.getOpcode() == GETFIELD || field.getOpcode() == PUTFIELD;

                            if(!getter) {
                                var owner = context.forName(field.owner);
                                var fieldRef = owner.findFieldFull(context, field.name, field.desc);
                                if(fieldRef == null)
                                    continue;

                                if(owner.isLibrary(fieldRef) || owner.isInterface())
                                    continue;

                                if(fieldRef.isFinal())
                                    fieldRef.core().access &= ~(ACC_FINAL);
                            }

                            char callSiteChar;
                            if(virtual) {
                                if(getter) {
                                    callSiteChar = chars[2];
                                } else callSiteChar = chars[4];
                            } else {
                                if(getter) {
                                    callSiteChar = chars[3];
                                } else callSiteChar = chars[5];
                            }

                            var bsmDesc = new StringBuilder("(");
                            if(virtual) bsmDesc.append("Ljava/lang/Object;");
                            if(getter) {
                                bsmDesc.append("II)").append(field.desc);
                            } else {
                                bsmDesc.append(field.desc).append("II)V");
                            }

                            var decKey = method.canSalt(frames.get(insn)) ? method.salt().value() >> 16 : random.nextInt() >> 16;
                            var idx = add(field.owner, field.name, "()" + field.desc, decKey);
                            var handle = new Handle(H_INVOKESTATIC, refClass.name(), gen.outerInvoker.name(), gen.outerInvoker.desc(), false);

                            var list = new InsnList();
                            var indy = new InvokeDynamicInsnNode(
                                    Character.toString(callSiteChar),
                                    bsmDesc.toString(),
                                    handle
                            );

                            int idxXor = idx ^ indexXor;
                            list.add(context.properties().add(ASMUtils.pushInt(idxXor), Property.IGNORE_INTEGER));
                            if(method.canSalt(frame)) {
                                var mask = random.nextInt();
                                var masked = method.salt().value() & mask;

                                list.add(method.salt().load());
                                list.add(context.properties().add(ASMUtils.pushInt(mask), Property.IGNORE_INTEGER));
                                list.add(new InsnNode(IAND));
                                list.add(context.properties().add(ASMUtils.pushInt(masked ^ (decKey << 16)), Property.IGNORE_INTEGER));
                                list.add(new InsnNode(IXOR));
                            } else {
                                list.add(context.properties().add(ASMUtils.pushInt((decKey << 16) | random.nextInt(Short.MAX_VALUE)), Property.IGNORE_INTEGER));
                            }
                            list.add(indy);

                            method.insns().insertBefore(field, list);
                            method.insns().remove(field);
                            markChange();
                        }
                        default -> {}
                    }
                }
            }
        }

        if(references.isEmpty()) {
            context.artificials().remove(refClass.name());
            return;
        }

        gen.generateClinit(context, refClass, references);
        context.saltDispatcher().setIndyField(refClass);
    }

    /**
     * @param owner owner class of member
     * @param name name of member
     * @param desc descriptor
     * @return index of string in reference array
     */
    private int add(String owner, String name, String desc, int decKey) {
        owner = owner.replace('/', '.');
        var token = String.format("%s:%s:%s", owner, name, desc);
        token = CryptUtils.xor(token, decKey, 0);

        if(references.contains(token)) {
            return references.indexOf(token);
        } else {
            var idx = references.size();
            references.add(token);
            return idx;
        }
    }

    private char[] getChars() {
        var chars = new ArrayList<Character>();
        for(char c = 0; c < (char) 0xffff; c++) {
            chars.add(c);
        }

        var out = new char[6];
        Collections.shuffle(chars);
        for(int i = 0; i < out.length; i++) {
            out[i] = chars.get(i);
        }

        return out;
    }
}
