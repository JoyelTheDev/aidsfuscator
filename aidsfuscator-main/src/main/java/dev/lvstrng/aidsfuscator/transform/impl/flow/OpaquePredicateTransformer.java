package dev.lvstrng.aidsfuscator.transform.impl.flow;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public class OpaquePredicateTransformer extends Transformer {

    public OpaquePredicateTransformer() {
        super("Opaque Predicates", "opaquePredicates");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.OPAQUE_PREDICATES.excluded(clazz))
                continue;
            for (var method : clazz.methods()) {
                if (cantEditMethod(clazz, method))
                    continue;
                if (Exclusions.OPAQUE_PREDICATES.excluded(method))
                    continue;
                if (method.insns().size() < 4)
                    continue;

                var insns   = method.insns();
                var targets = new ArrayList<AbstractInsnNode>();

                for (var insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                    int op = insn.getOpcode();
                    if (op >= IRETURN && op <= RETURN) targets.add(insn);
                    else if (op == ATHROW)             targets.add(insn);
                    else if (insn instanceof MethodInsnNode) targets.add(insn);
                }

                if (targets.isEmpty())
                    continue;

                int inserted = 0;
                for (var target : targets) {
                    if (random.nextInt(3) == 0)
                        continue;

                    switch (random.nextInt(3)) {
                        case 0 -> { insns.insertBefore(target, buildAlwaysTrue(context, method));  inserted++; }
                        case 1 -> { insns.insertBefore(target, buildAlwaysFalse(context, method)); inserted++; }
                        case 2 -> { insns.insertBefore(target, buildMathBased(context, method));   inserted++; }
                    }
                }

                if (inserted > 0)
                    markChange();
            }
        }
    }

    private InsnList buildAlwaysTrue(Context ctx, JMethod method) {
        var list      = new InsnList();
        var skipLabel = new LabelNode();

        int n = random.nextInt(500) + 2;

        list.add(method.protectedIntPush(ctx, n));
        list.add(method.protectedIntPush(ctx, n));
        list.add(method.protectedIntPush(ctx, 1));
        list.add(new InsnNode(IADD));
        list.add(new InsnNode(IMUL));
        list.add(method.protectedIntPush(ctx, 2));
        list.add(new InsnNode(IREM));
        list.add(new JumpInsnNode(IFEQ, skipLabel));
        list.add(new InsnNode(ACONST_NULL));
        list.add(new InsnNode(ATHROW));
        list.add(skipLabel);
        return list;
    }

    private InsnList buildAlwaysFalse(Context ctx, JMethod method) {
        var list      = new InsnList();
        var realLabel = new LabelNode();

        int n = random.nextInt(Short.MAX_VALUE - 2) + 2;

        list.add(method.protectedIntPush(ctx, n));
        list.add(method.protectedIntPush(ctx, 1));
        list.add(new InsnNode(IOR));
        list.add(new JumpInsnNode(IFNE, realLabel));
        list.add(new InsnNode(ACONST_NULL));
        list.add(new InsnNode(ATHROW));
        list.add(realLabel);
        return list;
    }

    private InsnList buildMathBased(Context ctx, JMethod method) {
        var list    = new InsnList();
        var okLabel = new LabelNode();
        int a = random.nextInt(1000) + 1;
        int b = random.nextInt(1000) + 1;

        list.add(method.protectedIntPush(ctx, a * a));
        list.add(method.protectedIntPush(ctx, b * b));
        list.add(new InsnNode(IADD));
        list.add(new JumpInsnNode(IFGE, okLabel));
        list.add(new InsnNode(ACONST_NULL));
        list.add(new InsnNode(ATHROW));
        list.add(okLabel);
        return list;
    }
}