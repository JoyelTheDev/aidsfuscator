package dev.lvstrng.aidsfuscator.transform.impl.data;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.CryptUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class StringEncryptTransformer extends Transformer {
    private final Setting<Integer> minLength = setting("minLength", 1);

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
            var decryptorName = context.dictionary().newMethodName(clazz, "(II)Ljava/lang/String;");
            var keys = CryptUtils.generateKeys(random, 32, 255);
            var idxXor = random.nextInt();

            for(var method : clazz.methods()) {
                if(Exclusions.STRING_ENCRYPTION.excluded(method))
                    continue;

                for(var insn : method.insns()) {
                    if(!(insn instanceof LdcInsnNode ldc && ldc.cst instanceof String str))
                        continue;

                    if(context.propertyContainer().get(insn).has(Property.IGNORE_STRING))
                        continue;

                    if(str.length() < minLength.value())
                        continue;

                    if(str.length() > Character.MAX_VALUE) {
                        Logger.warn("String constant in '%s' too big for string encryption.", method.fullOriginalName());
                        continue;
                    }

                    var key = method.hasSalt() ? method.salt().value() >> 16 : random.nextInt() >> 16;
                    var encryptedString = CryptUtils.xor(str, key, keys, keys[0]);
                    var idx = strings.size();
                    var idxVal = idx ^ idxXor;

                    var builder = new InsnBuilder().add(context.propertyContainer().add(ASMUtils.pushInt(idxVal), Property.IGNORE_INTEGER));
                    if(method.hasSalt()) {
                        builder.add(method.salt().load());
                    } else {
                        builder.add(context.propertyContainer().add(ASMUtils.pushInt(key << 16), Property.IGNORE_INTEGER));
                    }
                    builder.method(INVOKESTATIC, clazz.name(), decryptorName, "(II)Ljava/lang/String;");

                    method.insns().insertBefore(ldc, builder.result());
                    method.insns().remove(ldc);
                    strings.add(encryptedString);
                    markChange();
                }
            }

            if(strings.isEmpty())
                continue;


            int access = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;
            clazz.add(new FieldNode(access, fieldName, "[Ljava/lang/String;", null, null));
            generateClinit(context, clazz, fieldName, strings);
            generateDecryptor(context, clazz, fieldName, decryptorName, idxXor, keys);
        }
    }

    private void generateDecryptor(Context context, JClass clazz, String fieldName, String decryptorName, int idxXor, int[] keys) {
        var method = clazz.add(new MethodNode(ACC_PRIVATE | ACC_STATIC, decryptorName, "(II)Ljava/lang/String;", null, null));

        // ---- LOCALS ----
        var idxValVar = method.allocVar(Type.INT_TYPE); // param1
        var xorValueVar = method.allocVar(Type.INT_TYPE); // param2

        var charArrVar = method.allocVar();
        var iVar = method.allocVar(Type.INT_TYPE);
        var xorKey = method.allocVar(Type.INT_TYPE);

        // ---- CODE ----
        var loop = new LabelNode();

        var builder = new InsnBuilder()
                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")
                ._var(ILOAD, idxValVar)
                .add(context.propertyContainer().add(ASMUtils.pushInt(idxXor), Property.IGNORE_INTEGER))
                .ixor()
                .aaload()
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, charArrVar)

                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, iVar)

                .label(loop)
                ._var(ILOAD, iVar)
                .add(context.propertyContainer().add(ASMUtils.pushInt(keys.length), Property.IGNORE_INTEGER))
                .irem();

        var end = new LabelNode();
        var routeBuilder = new InsnBuilder();
        var lbls = new LabelNode[keys.length];

        for(int i = 0; i < keys.length; i++) {
            var route = new LabelNode();
            lbls[i] = route;

            routeBuilder
                    .label(route)
                    .add(context.propertyContainer().add(ASMUtils.pushInt(keys[i]), Property.IGNORE_INTEGER))
                    ._var(ISTORE, xorKey)
                    ._goto(end);
        }

        builder.tableswitch(lbls[0], 0, keys.length - 1, lbls)
                .add(routeBuilder)

                .label(end)
                ._var(ALOAD, charArrVar)
                ._var(ILOAD, iVar)

                ._var(ALOAD, charArrVar)
                ._var(ILOAD, iVar)
                .caload()
                ._var(ILOAD, xorKey)
                .ixor()
                ._var(ILOAD, xorValueVar)
                .add(context.propertyContainer().add(ASMUtils.pushInt(16), Property.IGNORE_INTEGER))
                .ishr()
                .ixor()
                .castore()

                .label(new LabelNode())
                .iinc(iVar, 1)

                .label(new LabelNode())
                ._var(ILOAD, iVar)
                ._var(ALOAD, charArrVar)
                .arraylength()
                .jump(IF_ICMPLT, loop)

                .label(new LabelNode())
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, charArrVar)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                .method(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;")
                ._areturn();

        method.insns().add(builder.result());
    }

    private void generateClinit(Context context, JClass clazz, String fieldName, List<String> strings) {
        // ---- INIT ----
        int key = random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
        var strBuilder = new StringBuilder();
        var lengthStr = new StringBuilder();

        for(var str : strings) {
            strBuilder.append(str);
            lengthStr.append((char) str.length());
        }

        var theStr = strBuilder.toString();
        var lenStr = lengthStr.toString();

        var method = clazz.findOrCreateClinit();

        // ---- LOCALS ----
        var keyVar = method.allocVar(Type.INT_TYPE);
        var strVar = method.allocVar();
        var lenArrVar = method.allocVar();
        var strArrVar = method.allocVar();
        var iVar = method.allocVar(Type.INT_TYPE);
        var offsetVar = method.allocVar(Type.INT_TYPE);
        var lenVar = method.allocVar(Type.INT_TYPE);
        var statusVar = method.allocVar(Type.INT_TYPE);

        // ---- CODE ----
        var loop = new LabelNode();
        var varLbl = new LabelNode();

        var builder = new InsnBuilder()
                .label(new LabelNode())
                ._int(key)
                ._var(ISTORE, keyVar)

                .label(new LabelNode())
                ._const(theStr)
                ._var(ASTORE, strVar)

                .label(new LabelNode())
                ._const(lenStr)
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, lenArrVar)

                .label(new LabelNode())
                ._var(ALOAD, lenArrVar)
                .arraylength()
                .anewarray("java/lang/String")
                ._var(ASTORE, strArrVar)

                .label(new LabelNode())
                ._int(-1)
                ._var(ISTORE, statusVar)
                ._goto(varLbl)

                // loop start
                .label(loop)
                ._var(ALOAD, lenArrVar)
                ._var(ILOAD, iVar)
                .caload()
                ._var(ISTORE, lenVar)

                .label(new LabelNode())
                ._var(ALOAD, strArrVar)
                ._var(ILOAD, iVar)

                ._var(ALOAD, strVar) // str
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, lenVar)
                .iadd()
                .method(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;") // target method
                .aastore()

                .label(new LabelNode())
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, lenVar)
                .iadd()
                ._var(ISTORE, offsetVar)

                .label(new LabelNode())
                .iinc(iVar, 1)

                .label(new LabelNode())
                ._var(ILOAD, iVar)
                ._var(ALOAD, lenArrVar)
                .arraylength()
                .jump(IF_ICMPLT, loop)

                // loop end
                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, statusVar)

                .label(varLbl)
                ._int(0)
                ._var(ISTORE, iVar)

                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, offsetVar)

                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, lenVar)

                .label(new LabelNode())
                ._var(ILOAD, statusVar)
                .jump(IFNE, loop)

                .label(new LabelNode())
                ._var(ALOAD, strArrVar)
                .field(PUTSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;");

        method.insns().insert(builder.result());
    }
}
