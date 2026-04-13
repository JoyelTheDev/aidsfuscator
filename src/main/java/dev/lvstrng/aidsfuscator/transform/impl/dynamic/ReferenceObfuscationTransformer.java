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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

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

            for(var method : clazz.methods()) {
                if(Exclusions.REFERENCE_OBFUSCATE.excluded(method))
                    continue;

                var frames = method.frames(context);
                for(var insn : method.insns()) {
                    switch (insn) {
                        case MethodInsnNode call -> {
                            if(!context.referenceManager().canObfuscate(call))
                                continue;

                            char callSiteChar;
                            if(call.getOpcode() == INVOKEVIRTUAL || call.getOpcode() == INVOKEINTERFACE) {
                                callSiteChar = chars[0];
                            } else if (call.getOpcode() == INVOKESTATIC) {
                                callSiteChar = chars[1];
                            } else {
                                continue;
                            }

                            var decKey = method.canSalt(frames.get(insn)) ? method.salt().value() >> 16 : random.nextInt() >> 16;
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
                            if(method.canSalt(frames.get(call))) {
                                list.add(method.salt().load());
                            } else {
                                list.add(context.properties().add(ASMUtils.pushInt((decKey << 16) | random.nextInt(Short.MAX_VALUE)), Property.IGNORE_INTEGER));
                            }
                            list.add(indy);

                            method.insns().insertBefore(call, list);
                            method.insns().remove(call);
                        }
                        case FieldInsnNode field -> {
                            if(!context.referenceManager().canObfuscate(field))
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
                            if(method.canSalt(frames.get(field))) {
                                list.add(method.salt().load());
                            } else {
                                list.add(context.properties().add(ASMUtils.pushInt((decKey << 16) | random.nextInt(Short.MAX_VALUE)), Property.IGNORE_INTEGER));
                            }
                            list.add(indy);

                            method.insns().insertBefore(field, list);
                            method.insns().remove(field);
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
