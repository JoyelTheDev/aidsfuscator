package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleValue;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.analysis.Frame;

import java.awt.*;

public class FrameTest extends Transformer {
    public FrameTest() {
        super("Frame Test", "frameTest");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                Logger.info(method.fullName());
                var frames = method.frames(context);
                if(frames == null)
                    return;

                for(var insn : method.insns()) {
                    if(!(insn instanceof FrameNode node))
                        continue;

                    var frame = frames.get(insn);
                    if(frame == null)
                        continue;

                    System.out.println(frameString(frame));
                }
            }
        }
    }

    private String frameString(FrameNode frame) {
        var sb = new StringBuilder("{");
        if(frame.local != null) {
            for (var local : frame.local) {
                sb.append(local).append("; ");
            }
        }
        sb.append("} {");

        if(frame.stack != null) {
            for (var stack : frame.stack) {
                sb.append(stack).append("; ");
            }
        }

        return sb.append("}").toString();
    }

    private String frameString(Frame<SimpleValue> frame) {
        var sb = new StringBuilder("{");

        for(int i = 0; i < frame.getLocals(); i++) {
            var local = frame.getLocal(i);
            sb.append(local).append("; ");
        }

        sb.append("} {");
        for(int i = 0; i < frame.getStackSize(); i++) {
            var stack = frame.getStack(i);
            sb.append(stack).append("; ");
        }

        return sb.append("}").toString();
    }
}
