package dev.lvstrng.aidsfuscator.transform.impl.flow;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ControlFlowShufflingTransformer extends Transformer {
    public ControlFlowShufflingTransformer() {
        super("Control Flow Shuffling", "controlFlowShuffle");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(Exclusions.FLOW_SHUFFLE.excluded(clazz))
                continue;

            for(var method : clazz.methods()) {
                if(Exclusions.FLOW_SHUFFLE.excluded(method))
                    continue;

                var graph = method.createFlowGraph(context);
                if(graph.blocks().size() <= 3)
                    continue;

                // ---- PREP ----
                method.localVariables().clear();
                var firstBlock = graph.firstBlock();
                var blocks = graph.blocks();

                Collections.shuffle(blocks, random);
                blocks.remove(firstBlock);
                blocks.addFirst(firstBlock);

                // ---- REBUILD ----
                var oldTcbs = new ArrayList<>(method.traps());
                var rebuilt = new InsnList();

                for(var block : blocks) {
                    for(var insn : block.insns()) {
                        method.insns().remove(insn); // an insn node can only be child to ONE insn list, so remove it from the methods instructions, since we're rebuilding it anyway
                        if(insn instanceof FrameNode)
                            continue;

                        rebuilt.add(insn);
                    }

                    if(block.inTrap()) {
                        var end = new LabelNode();
                        rebuilt.add(end);
                        rebuilt.add(new JumpInsnNode(GOTO, block.defaultBlock().label()));

                        for(var trap : block.traps()) {
                            var handler = new LabelNode();
                            rebuilt.add(handler);
                            rebuilt.add(new JumpInsnNode(GOTO, trap.handler));
                            method.traps().add(new TryCatchBlockNode(block.label(), end, handler, trap.type));
                        }
                    } else {
                        if(block.deadEnd() || block.ends() || isNextBlock(blocks, block))
                            continue;

                        rebuilt.add(new JumpInsnNode(GOTO, block.defaultBlock().label()));
                    }
                }

                method.insns().clear();
                method.insns().add(rebuilt);
                method.traps().removeAll(oldTcbs);
                markChange();
            }
        }
    }

    private boolean isNextBlock(List<Block> blocks, Block block) {
        var dfltIdx = blocks.indexOf(block.defaultBlock());
        var currIdx = blocks.indexOf(block);
        // if block is next one, skip adding a GOTO
        return (dfltIdx - currIdx) == 1;
    }
}
