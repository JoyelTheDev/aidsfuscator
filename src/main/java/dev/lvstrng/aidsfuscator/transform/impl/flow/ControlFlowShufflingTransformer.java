package dev.lvstrng.aidsfuscator.transform.impl.flow;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;

import java.util.Collections;

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

                if(!method.traps().isEmpty())
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
                var rebuilt = new InsnList();
                for(var block : blocks) {
                    for(var insn : block.insns()) {
                        method.insns().remove(insn); // an insn node can only be child to ONE insn list, so remove it from the methods instructions, since we're rebuilding it anyway
                        if(insn instanceof FrameNode)
                            continue;

                        rebuilt.add(insn);
                    }

                    if(block.deadEnd() || block.ends())
                        continue;

                    var dfltIdx = blocks.indexOf(block.defaultBlock());
                    var currIdx = blocks.indexOf(block);
                    if((dfltIdx - currIdx) == 1) // if block is next one, skip adding a GOTO
                        continue;

                    rebuilt.add(new JumpInsnNode(GOTO, block.defaultBlock().label()));
                }

                method.insns().clear();
                method.insns().add(rebuilt);
                markChange();
            }
        }
    }
}
