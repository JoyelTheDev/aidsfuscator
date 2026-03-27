package dev.lvstrng.aidsfuscator.transform.impl.data;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple number encryption transformer using XOR.
 */
public class IntegerEncryptTransformer extends Transformer {
    public IntegerEncryptTransformer() {
        super("Encrypt Integer Constants", "integerEncrypt");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(clazz.isInterface() && clazz.version() < V1_8)
                continue;

            if(Exclusions.INTEGER_ENCRYPTION.excluded(clazz))
                continue;

            var numbers = new ArrayList<Integer>();
            var decryptorName = context.dictionary().newMethodName(clazz, "(II)I");
            var fieldName = context.dictionary().newFieldName(clazz, "[I");
            int idxXor = random.nextInt();

            for(var method : clazz.methods()) {
                if(Exclusions.INTEGER_ENCRYPTION.excluded(method))
                    continue;

                for(var insn : method.insns()) {
                    if(!ASMUtils.isIntPush(insn))
                        continue;

                    if(ASMUtils.isIconst(insn))
                        continue;

                    if(context.propertyContainer().get(insn).has(Property.IGNORE_INTEGER))
                        continue;

                    // ---- PREPARE KEYS -----
                    var key = method.hasSalt() ? method.salt().value() : random.nextInt();
                    var num = ASMUtils.getInt(insn) ^ key;
                    int idxValue = numbers.size() ^ idxXor;
                    numbers.add(num);

                    // ---- INSTRUCTIONS ----
                    var builder = new InsnBuilder()._int(idxValue);
                    if(method.hasSalt()) {
                        builder.add(method.salt().load());
                    } else {
                        builder._int(key);
                    }
                    builder.method(INVOKESTATIC, clazz.name(), decryptorName, "(II)I");
                    method.insns().insertBefore(insn, builder.result());
                    method.insns().remove(insn);
                    markChange();
                }
            }

            if(numbers.isEmpty())
                continue;

            int access = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;
            clazz.add(new FieldNode(access, fieldName, "[I", null, null));
            generateDecryptor(clazz, fieldName, decryptorName, idxXor);
            generateClinit(context, clazz, fieldName, numbers);
        }
    }

    private void generateDecryptor(JClass clazz, String fieldName, String decryptorName, int idxXor) {
        int access = (clazz.isInterface())
                ? ACC_PUBLIC | ACC_STATIC
                : ACC_PRIVATE | ACC_STATIC;
        var method = clazz.add(new MethodNode(access, decryptorName, "(II)I", null, null));
        // ---- LOCALS ----
        if(clazz.isInterface()) method.allocVar();

        var idxVal = method.allocVar(Type.INT_TYPE);
        var key = method.allocVar(Type.INT_TYPE);

        new InsnBuilder(method.insns())
                .label(new LabelNode())
                ._var(ILOAD, idxVal)
                ._int(idxXor)
                .ixor()
                ._var(ISTORE, idxVal)

                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), fieldName, "[I")
                ._var(ILOAD, idxVal)
                .iaload()
                ._var(ILOAD, key)
                .ixor()
                ._ireturn()
        ;
    }

    private void generateClinit(Context context, JClass clazz, String fieldName, List<Integer> numbers) {
        var clinit = clazz.findOrCreateClinit();
        var key = random.nextInt();
        var theStr = new StringBuilder();

        for(var num : numbers) {
            theStr.append(new String(intToBytes(num ^ key), StandardCharsets.ISO_8859_1));
        }

        // ---- LOCALS ----
        var bytesVar = clinit.allocVar();
        var lenVar = clinit.allocVar(Type.INT_TYPE);
        var keyVar = clinit.allocVar(Type.INT_TYPE);
        var iVar = clinit.allocVar(Type.INT_TYPE);
        var iVarReal = clinit.allocVar(Type.INT_TYPE);
        var valVar = clinit.allocVar(Type.INT_TYPE);

        // ---- INSNS ----
        var loop = new LabelNode();

        var builder = new InsnBuilder();
        builder.label(new LabelNode())
                .add(context.propertyContainer().add(ASMUtils.pushInt(key), Property.SENSITIVE_CONSTANT))
                ._var(ISTORE, keyVar)

                .label(new LabelNode())
                ._const(theStr.toString())
                ._const("ISO-8859-1")
                .method(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B")
                ._var(ASTORE, bytesVar)

                .label(new LabelNode())
                ._var(ALOAD, bytesVar)
                .arraylength()
                ._int(4)
                .idiv()
                ._var(ISTORE, lenVar)

                .label(new LabelNode())
                ._var(ILOAD, lenVar)
                .newarray(T_INT)
                .field(PUTSTATIC, clazz.name(), fieldName, "[I")

                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, iVar)

                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, iVarReal)

                .label(loop)

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                .baload()
                ._int(255)
                .iand()
                ._int(24)
                .ishl()

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._int(1)
                .iadd()
                .baload()
                ._int(255)
                .iand()
                ._int(16)
                .ishl()
                .ior()

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._int(2)
                .iadd()
                .baload()
                ._int(255)
                .iand()
                ._int(8)
                .ishl()
                .ior()

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._int(3)
                .iadd()
                .baload()
                ._int(255)
                .iand()
                .ior()
                ._var(ISTORE, valVar)

                .label(new LabelNode())
                ._var(ILOAD, valVar)
                ._var(ILOAD, keyVar)
                .ixor()
                ._var(ISTORE, valVar)

                .field(GETSTATIC, clazz.name(), fieldName, "[I")
                ._var(ILOAD, iVarReal)
                ._var(ILOAD, valVar)
                .iastore()

                .label(new LabelNode())
                .iinc(iVar, 4)
                .iinc(iVarReal, 1)
                ._var(ILOAD, iVarReal)
                ._var(ILOAD, lenVar)
                .jump(IF_ICMPLT, loop);

        clinit.insns().insert(builder.result());
    }

    private static byte[] intToBytes(int i) {
        return new byte[] {
                (byte) (i >> 24), (byte) (i >> 16),
                (byte) (i >> 8), (byte) (i)
        };
    }
}
