package dev.lvstrng.aidsfuscator.utils;

import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;

public class ASMUtils implements Opcodes {
    public static boolean isReturn(int opcode) {
        return opcode >= IRETURN && opcode <= RETURN;
    }

    public static boolean isReturn(AbstractInsnNode insn) {
        return isReturn(insn.getOpcode());
    }

    public static AbstractInsnNode pushInt(int n) {
        if(n >= -1 && n <= 5)
            return new InsnNode(ICONST_0 + n);

        if(n >= -128 && n <= 127)
            return new IntInsnNode(BIPUSH, n);
        if(n >= -32768 && n <= 32767)
            return new IntInsnNode(SIPUSH, n);

        return new LdcInsnNode(n);
    }

    public static AbstractInsnNode pushLong(long l) {
        if(l == 0 || l == 1)
            return new InsnNode((int) (LCONST_0 + l));

        return new LdcInsnNode(l);
    }

    public static AbstractInsnNode pushDouble(double d) {
        if(d == 0 || d == 1)
            return new InsnNode((int) (DCONST_0 + d));

        return new LdcInsnNode(d);
    }

    public static AbstractInsnNode pushFloat(float f) {
        if(f == 0 || f == 1 || f == 2)
            return new InsnNode((int) (FCONST_0 + f));

        return new LdcInsnNode(f);
    }

    public static int getInt(AbstractInsnNode insn) {
        var op = insn.getOpcode();
        if(isIconst(insn))
            return op - ICONST_0;

        if(op == BIPUSH || op == SIPUSH)
            return ((IntInsnNode) insn).operand;

        if(insn instanceof LdcInsnNode ldc)
            return (int)ldc.cst;

        throw new IllegalArgumentException("Not number insn: " + insn.getOpcode());
    }

    public static boolean isIntPush(AbstractInsnNode insn) {
        if(insn instanceof IntInsnNode node)
            return node.getOpcode() != NEWARRAY;

        if(isIconst(insn))
            return true;

        return insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer;
    }

    public static boolean isIconst(AbstractInsnNode insn) {
        return insn.getOpcode() >= ICONST_M1 && insn.getOpcode() <= ICONST_5;
    }

    /**
     * Translates invokedynamic string concat calls into string builder
     * @author reowya
     * @param method the method to translate the concats in
     */
    public static void translateConcatenation(JMethod method) {
        var STACK_ARG_CONSTANT = '\u0001';
        var BSM_ARG_CONSTANT = '\u0002';

        for(var insn : method.insns()) {
            if(!(insn instanceof InvokeDynamicInsnNode indy))
                continue;

            if(!indy.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory"))
                continue;

            if(!indy.bsm.getName().equals("makeConcatWithConstants"))
                continue;

            var pattern = (String) indy.bsmArgs[0];

            var stackArgs = Type.getArgumentTypes(indy.desc);
            var bsmArgs = Arrays.copyOfRange(indy.bsmArgs, 1, indy.bsmArgs.length);

            int stackArgsCount = 0;
            for(var c : pattern.toCharArray()) {
                if(c == STACK_ARG_CONSTANT)
                    stackArgsCount++;
            }

            int bsmArgsCount = 0;
            for (char c : pattern.toCharArray()) {
                if (c == BSM_ARG_CONSTANT)
                    bsmArgsCount++;
            }

            if(stackArgsCount != stackArgs.length)
                continue;

            if(bsmArgsCount != bsmArgs.length)
                continue;

            var v = method.allocVar(stackArgs[0]);
            var indices = new int[stackArgsCount];

            for(int i = 0; i < stackArgs.length; i++) {
                indices[i] = v;
                v += stackArgs[i].getSize();
            }

            for (int i = indices.length - 1; i >= 0; i--) {
                method.insns().insertBefore(indy, new VarInsnNode(stackArgs[i].getOpcode(ISTORE), indices[i]));
            }

            var list = new InsnList();
            var arr = pattern.toCharArray();

            int stackArgsIndex = 0;
            int bsmArgsIndex = 0;

            var builder = new StringBuilder();
            list.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
            list.add(new InsnNode(DUP));
            list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));

            for (char c : arr) {
                if (c == STACK_ARG_CONSTANT) {
                    if (!builder.isEmpty()) {
                        list.add(new LdcInsnNode(builder.toString()));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                        builder = new StringBuilder();
                    }

                    var stackArg = stackArgs[stackArgsIndex++];
                    var stackIndex = indices[stackArgsIndex - 1];

                    if (stackArg.getSort() == Type.OBJECT) {
                        list.add(new VarInsnNode(ALOAD, stackIndex));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                    } else if (stackArg.getSort() == Type.ARRAY) {
                        list.add(new VarInsnNode(ALOAD, stackIndex));
                        list.add(new MethodInsnNode(INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;"));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                    } else {
                        list.add(new VarInsnNode(stackArg.getOpcode(ILOAD), stackIndex));
                        var adaptedDescriptor = stackArg.getDescriptor();
                        if (adaptedDescriptor.equals("B") || adaptedDescriptor.equals("S"))
                            adaptedDescriptor = "I";

                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + adaptedDescriptor + ")Ljava/lang/StringBuilder;"));
                    }
                } else if (c == BSM_ARG_CONSTANT) {
                    list.add(new LdcInsnNode(bsmArgs[bsmArgsIndex++]));
                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                } else {
                    builder.append(c);
                }
            }

            if (!builder.isEmpty()) {
                list.add(new LdcInsnNode(builder.toString()));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
            }

            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));

            method.insns().insertBefore(indy, list);
            method.insns().remove(indy);
        }
    }
}
