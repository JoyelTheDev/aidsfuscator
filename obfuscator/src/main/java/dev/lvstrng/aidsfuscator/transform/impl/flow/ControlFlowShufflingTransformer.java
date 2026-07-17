package dev.lvstrng.aidsfuscator.transform.impl.flow;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Shuffles control flow blocks into a random order after control flow flattening to actually make control flow flattening strong.
 * @author lvstrng
 */
public class ControlFlowShufflingTransformer extends Transformer {
    public ControlFlowShufflingTransformer() {
        super("Control Flow Shuffling", "controlFlowShuffle");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.FLOW_SHUFFLE.excluded(clazz))
                continue;

            for (var method : clazz.methods()) {
                if (Exclusions.FLOW_SHUFFLE.excluded(method))
                    continue;

                var graph = method.createFlowGraph(context);
                if (graph.blocks().size() <= 3)
                    continue;

                // ---- PREP ----
                method.localVariables().clear();
                var firstBlock = graph.firstBlock();
                var blocks = graph.blocks();
                var oldTraps = new ArrayList<>(method.traps());

                var blockTraps = new HashMap<Block, List<TryCatchBlockNode>>();
                for (var block : blocks) {
                    blockTraps.put(block, oldTraps.stream()
                            .filter(e -> method.idx(e.start) <= method.idx(block.label()))
                            .filter(e -> method.idx(e.end) > method.idx(block.label()))
                            .toList());
                }

                Collections.shuffle(blocks, random);
                blocks.remove(firstBlock);
                blocks.addFirst(firstBlock);

                // ---- REBUILD ----
                var rebuilt = new InsnList();

                for (var block : blocks) {
                    for (var insn : block.insns()) {
                        method.insns().remove(insn);
                        if (insn instanceof FrameNode)
                            continue;

                        rebuilt.add(insn);
                    }

                    var traps = blockTraps.get(block);

                    if (traps.isEmpty()) {
                        if (block.deadEnd() || block.ends() || isNextBlock(blocks, block))
                            continue;

                        rebuilt.add(new JumpInsnNode(GOTO, block.defaultBlock().label()));
                    } else {
                        var endLabel = new LabelNode();
                        rebuilt.add(endLabel);

                        if (!block.deadEnd())
                            rebuilt.add(new JumpInsnNode(GOTO, block.defaultBlock().label()));

                        for (var trap : traps) {
                            var handler = new LabelNode();
                            rebuilt.add(handler);
                            rebuilt.add(new JumpInsnNode(GOTO, trap.handler));

                            method.traps().add(new TryCatchBlockNode(block.label(), endLabel, handler, trap.type));
                        }
                    }
                }

                method.insns().clear();
                method.insns().add(rebuilt);
                method.traps().removeAll(oldTraps);
                markChange();
            }
        }
    }

    private boolean isNextBlock(List<Block> blocks, Block block) {
        var dfltIdx = blocks.indexOf(block.defaultBlock());
        var currIdx = blocks.indexOf(block);
        return (dfltIdx - currIdx) == 1;
    }
}
