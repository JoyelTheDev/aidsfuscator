package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.interpreter.FrameString;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.SwitchUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ControlFlowFlatteningTransformer extends Transformer {
    public ControlFlowFlatteningTransformer() {
        super("Control Flow Flattening", "controlFlowFlatten");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                int maxUsed = 0;
                for(var insn : method.insns()) {
                    if(insn instanceof VarInsnNode v)
                        maxUsed = Math.max(maxUsed, v.var + 1);
                    else if(insn instanceof IincInsnNode v)
                        maxUsed = Math.max(maxUsed, v.var + 1);
                }
                if(maxUsed > method.maxLocals()) {
                    method.setMaxLocals(maxUsed);
                }

                var graph = method.createFlowGraph(context);
                if(graph.isEmpty())
                    continue;

                var groups = new HashMap<String, List<LabelNode>>();
                for(var insn : method.insns()) {
                    var frame = graph.frameAt(insn);
                    if(frame == null)
                        continue;

                    if(frame.getStackSize() != 0)
                        continue;

                    var block = graph.blockContaining(insn);
                    if(block == graph.blocks().getFirst())
                        continue;

                    if(block == null || block.inTrapHandler() || block.inTrapEnd())
                        continue;

                    var lbl = new LabelNode();
                    method.insns().insertBefore(insn, lbl);
                    groups.computeIfAbsent(
                            FrameString.generate(frame),
                            _ -> new ArrayList<>()).add(lbl);
                }

                var entries = new ArrayList<>(groups.entrySet());
                Collections.shuffle(entries);

                int flattenerLocal = -1;
                for(var group : entries) {
                    var labels = group.getValue();
                    if(labels.size() < 3)
                        continue;

                    if(flattenerLocal == -1) {
                        flattenerLocal = method.allocVar(Type.INT_TYPE);
                        var initList = new InsnList();
                        initList.add(ASMUtils.pushInt(0));
                        initList.add(new VarInsnNode(ISTORE, flattenerLocal));
                        method.insns().insert(initList);
                    }

                    var local = flattenerLocal;
                    var dispatcher = new LabelNode();
                    var cases = new HashMap<LabelNode, Integer>();

                    labels.forEach(e -> {
                        var key = random.nextInt();
                        var list = new InsnList();
                        cases.put(e, key);

                        list.add(ASMUtils.pushInt(cases.get(e)));
                        list.add(new VarInsnNode(ISTORE, local));
                        list.add(new JumpInsnNode(GOTO, dispatcher));

                        method.insns().insertBefore(e, list);
                    });

                    var lookup = SwitchUtils.createLookupInvert(
                            labels.getFirst(),
                            cases
                    );

                    var list = new InsnList();
                    list.add(dispatcher);
                    list.add(new VarInsnNode(ILOAD, local));
                    list.add(lookup);

                    method.insns().add(list);
                }
            }
        }
    }
}
