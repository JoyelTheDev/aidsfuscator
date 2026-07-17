package dev.lvstrng.aidsfuscator.transform.impl.flow;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.analysis.flow.graph.ControlFlowGraph;
import dev.lvstrng.aidsfuscator.analysis.interpreter.FrameString;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import dev.lvstrng.aidsfuscator.utils.SwitchUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Flattens a methods Control Flow Graph (CFG), making analysis by control flow graph a little bit harder. (much harder when shuffling is applied).
 * It can also use a methods salt (if present) to lightly obfuscate the switch values a little.
 */
public class ControlFlowFlatteningTransformer extends Transformer {
    private final Setting<Boolean> obfuscateValues = setting("obfuscateValues", true);

    private final BiFunction<Integer, Integer, Integer> or = (v1, v2) -> v1 | v2;
    private final BiFunction<Integer, Integer, Integer> and = (v1, v2) -> v1 & v2;

    private final BiPredicate<ControlFlowGraph, Block> goodBlock = (graph, block) -> {
        if(block.inTrapHandler())   return false;
        if(block.inTrapEnd())       return false;
        if(block.expectsValue())    return false;

        return block.start().isInitThis();
    };

    public ControlFlowFlatteningTransformer() {
        super("Control Flow Flattening", "controlFlowFlatten");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(Exclusions.FLOW_FLATTEN.excluded(clazz))
                continue;

            for(var method : clazz.methods()) {
                if(method.properties().has(Property.STRING_DECRYPTOR, Property.INTEGER_DECRYPTOR))
                    continue;

                if(Exclusions.FLOW_FLATTEN.excluded(method))
                    continue;

                var graph = method.createFlowGraph(context);
                if(graph.isEmpty())
                    continue;

                fixLocals(method);
                var grouped = grouped(graph);
                int flattenerLocal = -1;

                for(var group : grouped) {
                    group.removeIf(e -> !goodBlock.test(graph, e));
                    if(group.size() < 3)
                        continue;

                    if(flattenerLocal == -1) {
                        flattenerLocal = method.allocVar(Type.INT_TYPE);

                        var list = new InsnList();
                        list.add(ASMUtils.pushInt(0));
                        list.add(new VarInsnNode(ISTORE, flattenerLocal));

                        method.insns().insert(list);
                    }

                    var dispatcher = new LabelNode();
                    var cases = new HashMap<LabelNode, Integer>();

                    for(var block : group) {
                        var list = new InsnList();
                        var lbl = new LabelNode();
                        var useOr = random.nextBoolean();
                        var func = useOr ? or : and;
                        var key = uniqueInt(cases, method.hasSalt() ? method.salt().value() : Integer.MAX_VALUE, func);

                        if (method.hasSalt()) {
                            list.add(method.salt().load());
                            list.add(context.properties().add(ASMUtils.pushInt(key), Property.IGNORE_INTEGER));
                            list.add(new InsnNode(useOr ? IOR : IAND));
                            key = func.apply(method.salt().value(), key);
                        } else {
                            list.add(context.properties().add(ASMUtils.pushInt(key), Property.IGNORE_INTEGER));
                        }

                        list.add(new VarInsnNode(ISTORE, flattenerLocal));
                        list.add(new JumpInsnNode(GOTO, dispatcher));
                        list.add(lbl);

                        cases.put(lbl, key);
                        method.insns().insert(block.label(), list);

                        if(!obfuscateValues.value())
                            continue;

                        for(var insn : block.insns()) {
                            if(!ASMUtils.isIntPush(insn))
                                continue;

                            if(ASMUtils.isIconst(insn))
                                continue;

                            if(method.isUnsafe(insn))
                                continue;

                            var num = ASMUtils.getInt(insn);
                            method.insns().insertBefore(insn, new InsnBuilder()
                                    ._var(ILOAD, flattenerLocal)
                                    ._int(num ^ key)
                                    .ixor()
                                    .result()
                            );
                            method.insns().remove(insn);
                        }
                    }

                    var list = new InsnList();
                    list.add(dispatcher);
                    list.add(new VarInsnNode(ILOAD, flattenerLocal));
                    list.add(SwitchUtils.createLookupInvert(dispatcher, cases));

                    method.insns().add(list);
                }

                markChange();
                method.reinitUnsafeInstructions();
            }
        }
    }

    private int uniqueInt(Map<LabelNode, Integer> cases, int mask, BiFunction<Integer, Integer, Integer> func) {
        int res;
        do {
            res = random.nextInt();
        } while (cases.containsValue(func.apply(res, mask)));

        return res;
    }

    private List<List<Block>> grouped(ControlFlowGraph graph) {
        var map = new HashMap<String, List<Block>>();

        for(var block : graph.blocks()) {
            if(block.start() == null)
                continue;

            map.computeIfAbsent(FrameString.generate(block.start()), _ -> new ArrayList<>()).add(block);
        }

        return new ArrayList<>(map.values());
    }

    private void fixLocals(JMethod method) {
        int maxUsed = 0;
        for(var insn : method.insns()) {
            if(insn instanceof VarInsnNode v) {
                var isCategoryTwo = v.getOpcode() == LLOAD || v.getOpcode() == LSTORE
                        || v.getOpcode() == DLOAD || v.getOpcode() == DSTORE;

                var slots = isCategoryTwo ? 2 : 1;
                maxUsed = Math.max(maxUsed, v.var + slots);
            } else if(insn instanceof IincInsnNode v) {
                maxUsed = Math.max(maxUsed, v.var + 1);
            }
        }

        if(maxUsed > method.maxLocals())
            method.setMaxLocals(maxUsed);
    }
}
