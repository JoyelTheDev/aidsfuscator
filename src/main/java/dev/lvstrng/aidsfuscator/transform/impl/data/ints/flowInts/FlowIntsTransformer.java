package dev.lvstrng.aidsfuscator.transform.impl.data.ints.flowInts;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

/**
 * A transformer that adds an {@code int} variable to the code. This variable gets updated in the program's control flow.
 * Since these values are harder to track than regular variables (because they're being updated each block), aidsfuscator uses this variable as another layer of number obfuscation.
 */
public class FlowIntsTransformer extends Transformer {
    private final Setting<Boolean> obfuscateLongs = setting("obfuscateLongs", true);
    private final Setting<Boolean> separateBlocks = setting("separateBlocks", true);

    public  FlowIntsTransformer() {
        super("Flow Ints", "flowInts");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(Exclusions.FLOW_INTS.excluded(clazz))
                continue;

            for(var method : clazz.methods()) {
                if(Exclusions.FLOW_INTS.excluded(method))
                    continue;

                if(ASMUtils.codeSize(method) > 20000)
                    continue;

                modify(context, method);
                markChange();
            }
        }
    }

    private void modify(Context context, JMethod method) {
        var graph = method.createFlowGraph(context);
        if(graph.isEmpty())
            return;

        if(!graph.firstBlock().predecessors().isEmpty()) {
            method.insns().insert(new LabelNode());
            graph = method.createFlowGraph(context);
        }

        var variable = method.allocVar(Type.INT_TYPE);
        var parent = new HashMap<Block, Block>();

        for (var block : graph.blocks()) {
            parent.put(block, block);
        }

        var find = new Function<Block, Block>() {
            @Override
            public Block apply(Block block) {
                var p = parent.get(block);
                if (p != block) {
                    p = this.apply(p);
                    parent.put(block, p);
                }
                return p;
            }
        };

        for (var successor : graph.blocks()) {
            var preds = successor.predecessors();
            if (preds.size() < 2) {
                continue;
            }

            var first = preds.getFirst();
            var firstRoot = find.apply(first);

            for (var pred : preds) {
                var root = find.apply(pred);
                if (root != firstRoot) {
                    parent.put(root, firstRoot);
                }
            }
        }

        var rootValues = new HashMap<Block, Integer>();
        var blockValues = new HashMap<Block, Integer>();

        for (var block : graph.blocks()) {
            var root = find.apply(block);
            rootValues.putIfAbsent(root, random.nextInt());
            blockValues.put(block, rootValues.get(root));
        }

        for(var block : graph.blocks()) {
            var list = new InsnList();
            var predecessorValue = blockValues.get(block.predecessors().stream().findFirst().orElse(null));
            var value = blockValues.get(block);
            if(value == null)
                continue;

            if(!Objects.equals(value, predecessorValue)) {
                if (predecessorValue == null) {
                    if(method.hasSalt()) {
                        var masked = method.salt().value() | method.seed();
                        var xor = masked ^ value;

                        list.add(method.salt().load());
                        list.add(context.properties().add(ASMUtils.pushInt(method.seed()), Property.IGNORE_INTEGER));
                        list.add(new InsnNode(IOR));
                        list.add(ASMUtils.pushInt(xor));
                        list.add(new InsnNode(IXOR));
                    } else {
                        list.add(context.properties().add(ASMUtils.pushInt(value), Property.SENSITIVE_CONSTANT));
                    }
                    list.add(new VarInsnNode(ISTORE, variable));
                } else {
                    list.add(new VarInsnNode(ILOAD, variable));
                    list.add(context.properties().add(ASMUtils.pushInt(value ^ predecessorValue), Property.IGNORE_INTEGER));
                    list.add(new InsnNode(IXOR));
                    list.add(new VarInsnNode(ISTORE, variable));
                }
            }

            if(separateBlocks.value())
                list.add(new LabelNode());
            method.insns().insert(block.label(), list);

            for(var insn : block.insns()) {
                if(method.isUnsafe(insn))
                    continue;

                if(ASMUtils.isIntPush(insn)) {
                    if (ASMUtils.isIconst(insn))
                        continue;

                    if(context.properties().get(insn).has(Property.IGNORE_FLOW_INTS))
                        continue;

                    var num = ASMUtils.getInt(insn);
                    var masked = value | method.seed();

                    list = new InsnList();
                    list.add(new VarInsnNode(ILOAD, variable));
                    list.add(ASMUtils.pushInt(method.seed()));
                    list.add(new InsnNode(IOR));
                    list.add(ASMUtils.pushInt(masked ^ num));
                    list.add(new InsnNode(IXOR));

                    method.insns().insertBefore(insn, list);
                    method.insns().remove(insn);
                } else if(ASMUtils.isLongPush(insn) && obfuscateLongs.value()) {
                    if(ASMUtils.isLconst(insn))
                        continue;

                    if(context.properties().get(insn).has(Property.IGNORE_FLOW_INTS))
                        continue;

                    var num = ASMUtils.getLong(insn);
                    long obf = ((num << 32 >>> 32) ^ (value & 0xFFFFFFFFL)) | (((num >>> 32) ^ (value & 0xFFFFFFFFL)) << 32);
                    // long deobf = ((obf << 32 >>> 32) ^ (value & 0xFFFFFFFFL)) | (((obf >>> 32) ^ (value & 0xFFFFFFFFL)) << 32);
                    var builder = new InsnBuilder()
                            ._long(obf)
                            .dup2() // obf, obf
                            ._int(32) // obf, obf, 32
                            .lshl() // obf, obf << 32
                            ._int(32) // obf, obf << 32, 32
                            .lushr() // obf, obf << 32 >>> 32
                            ._var(ILOAD, variable) // obf, obf << 32 >>> 32, local
                            .i2l() // obf, obf << 32 >>> 32, (long) local
                            ._long(0xFFFFFFFFL) // long, long_2nd, long, long_2nd, local, local_2nd
                            .land() // long, long_2nd, long, long_2nd, local, local_2nd
                            .lxor() // long, long_2nd, firstPart, firstPart_2nd
                            .dup2_x2() // firstPart(2), long(2), firstPart(2)

                            .pop2()  // firstPart(2), long(2)
                            ._int(32) // firstPart(2), long(2), 32
                            .lushr() // firstPart(2), long(2)
                            ._var(ILOAD, variable) // firstPart(2), long(2), local
                            .i2l() // firstPart(2), long(2), local(2)
                            ._long(0xFFFFFFFFL) // firstPart(2), long(2), local(2), 0xFFFFFFFFL
                            .land() // firstPart(2), long(2), local(2)
                            .lxor() // firstPart(2), secondPart(2)
                            ._int(32) // firstPart(2), secondPart(2), 32
                            .lshl() // firstPart(2), secondPart(2)
                            .lor()
                            ;

                    method.insns().insertBefore(insn, builder.result());
                    method.insns().remove(insn);
                }
            }
        }

        method.reinitUnsafeInstructions();
    }
}
