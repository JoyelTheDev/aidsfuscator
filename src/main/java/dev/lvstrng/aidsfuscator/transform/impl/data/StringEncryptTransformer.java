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
    private final Setting<Boolean> translateConcat = setting("translateConcat", true);
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
            var traceXor = random.nextInt() >> 16;

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

                    // ---- stack trace element stuff ----
                    var callerClass = clazz.name().replace('/', '.');
                    var callerMethod = method.name();
                    var traceKey = ((callerClass.hashCode() ^ callerMethod.hashCode()) >> 16) ^ traceXor;

                    // ---- enc ----
                    var key = method.hasSalt() ? method.salt().value() >> 16 : random.nextInt() >> 16;
                    var encryptedString = CryptUtils.xor(str, key ^ traceKey, keys, keys[0]);
                    var idx = strings.size();
                    var idxVal = idx ^ idxXor;

                    var builder = new InsnBuilder().add(context.properties().add(ASMUtils.pushInt(idxVal), Property.IGNORE_INTEGER));
                    if(method.canSalt(frames.get(ldc))) {
                        builder.add(method.salt().load());
                    } else {
                        //                                                                            add useless bits on purpose
                        builder.add(context.properties().add(ASMUtils.pushInt((key << 16) | random.nextInt(Short.MAX_VALUE)), Property.IGNORE_INTEGER));
                    }
                    builder.add(context.properties().add(new MethodInsnNode(INVOKESTATIC, clazz.name(), decryptorName, "(II)Ljava/lang/String;"), Property.IGNORE_REF_OBFUSCATION));

                    method.insns().insertBefore(ldc, builder.result());
                    method.insns().remove(ldc);
                    strings.add(encryptedString);
                    markChange();
                }
            }

            if(strings.isEmpty())
                continue;

            int access = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;
            clazz.createField(access, fieldName, "[Ljava/lang/String;");

            var cacheName = context.dictionary().newFieldName(clazz, "[Ljava/lang/Object;");
            clazz.createField(access, cacheName, "[Ljava/lang/Object;");

            generateClinit(context, clazz, fieldName, cacheName, strings);
            generateDecryptor(context, clazz, fieldName, cacheName, decryptorName, idxXor, traceXor, keys);
        }
    }

    private void generateDecryptor(Context context, JClass clazz, String fieldName, String cacheName, String decryptorName, int idxXor, int traceXor, int[] keys) {
        var method = clazz.createMethod(ACC_PRIVATE | ACC_STATIC, decryptorName, "(II)Ljava/lang/String;");

        // ---- LOCALS ----
        var idxValVar = method.allocVar(Type.INT_TYPE); // param1
        var xorValueVar = method.allocVar(Type.INT_TYPE); // param2

        var cachedTraceVar = method.allocVar(Type.INT_TYPE);
        var idxVar = method.allocVar(Type.INT_TYPE);
        var charArrVar = method.allocVar();
        var stackElementsVar = method.allocVar();
        var elementVar = method.allocVar();
        var hashVar = method.allocVar(Type.INT_TYPE);
        var iVar = method.allocVar(Type.INT_TYPE);
        var xorKey = method.allocVar(Type.INT_TYPE);

        // ---- CODE ----
        var loop = new LabelNode();
        var newTraceLabel = new LabelNode();
        var exitLabel = new LabelNode();

        var builder = new InsnBuilder()
                .label(new LabelNode())
                ._var(ILOAD, idxValVar)
                .add(context.properties().add(ASMUtils.pushInt(idxXor), Property.IGNORE_INTEGER))
                .ixor()
                ._var(ISTORE, idxVar)

                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")
                ._var(ILOAD, idxVar)
                .aaload()
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, charArrVar)

                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
                ._var(ILOAD, idxVar)
                .aaload()
                .type(CHECKCAST, "[Ljava/lang/StackTraceElement;")
                ._var(ASTORE, cachedTraceVar)

                .label(new LabelNode())
                ._var(ALOAD, cachedTraceVar)
                .jump(IFNULL, newTraceLabel)

                .label(new LabelNode())
                ._var(ALOAD, cachedTraceVar)
                ._var(ASTORE, stackElementsVar)
                ._goto(exitLabel)

                .label(newTraceLabel)
                .type(NEW, "java/lang/Throwable")
                .dup()
                .method(INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V")
                .method(INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;")
                ._var(ASTORE, stackElementsVar)

                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
                ._var(ILOAD, idxVar)
                ._var(ALOAD, stackElementsVar)
                .aastore()

                .label(exitLabel)
                ._var(ALOAD, stackElementsVar)
                ._const(1)
                .aaload()
                ._var(ASTORE, elementVar)

                .label(new LabelNode())
                ._var(ALOAD, elementVar)
                .method(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;")
                .method(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I")
                ._var(ALOAD, elementVar)
                .method(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;")
                .method(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I")
                .ixor()
                ._const(16)
                .ishr()
                ._const(traceXor)
                .ixor()
                ._var(ISTORE, hashVar)

                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, iVar)

                .label(loop)
                ._var(ILOAD, iVar)
                .add(context.properties().add(ASMUtils.pushInt(keys.length - 1), Property.IGNORE_INTEGER))
                .iand(); // do this instead of IREM, since `i` isn't supposed to be negative anyway

        var end = new LabelNode();
        var routeBuilder = new InsnBuilder();
        var lbls = new LabelNode[keys.length];

        for(int i = 0; i < keys.length; i++) {
            var route = new LabelNode();
            lbls[i] = route;

            routeBuilder
                    .label(route)
                    .add(context.properties().add(ASMUtils.pushInt(keys[i]), Property.IGNORE_INTEGER))
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
                .add(context.properties().add(ASMUtils.pushInt(16), Property.IGNORE_INTEGER))
                .ishr()
                .ixor()
                ._var(ILOAD, hashVar)
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
        method.properties().add(Property.STRING_DECRYPTOR);
    }

    private void generateClinit(Context context, JClass clazz, String fieldName, String cacheName, List<String> strings) {
        // ---- INIT ----
        int key = random.nextInt(Short.MAX_VALUE);
        var strBuilder = new StringBuilder();
        var lengthStr = new StringBuilder();

        for(var str : strings) {
            strBuilder.append(str);
            lengthStr.append((char) (str.length() ^ key));
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
                .label(new LabelNode());
        if(clazz.hasSalt()) {
            builder.add(context.properties().add(ASMUtils.pushInt(key ^ clazz.salt().value()), Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER))
                    .add(clazz.salt().load())
                    .ixor();
        } else {
            builder.add(context.properties().add(ASMUtils.pushInt(key), Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER));
        }

        builder._var(ISTORE, keyVar)

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
                ._var(ILOAD, keyVar)
                .ixor()
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
                .field(PUTSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")

                .label(new LabelNode())
                ._var(ALOAD, strArrVar)
                .arraylength()
                .anewarray("java/lang/Object")
                .field(PUTSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;");

        method.insertSafe(builder.result());
    }
}
