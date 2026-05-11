package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.salt.impl.ClassSalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.classgen.impl.SimpleClassSaltClassGenerator;
import org.objectweb.asm.tree.*;

public class TestTransformer extends Transformer {
    public TestTransformer() {
        super("Test", "test");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                if(!method.hasSalt())
                    continue;

                var graph = method.createFlowGraph(context);
                if(graph.isEmpty())
                    continue;

                for(var block : graph.blocks()) {
                    if(!method.canSalt(block))
                        continue;

                    var target = new LabelNode();
                    var list = new InsnList();

                    list.add(new LabelNode());
                    list.add(method.salt().load());
                    list.add(new JumpInsnNode(IFNE, target));

                    list.add(method.salt().load());
                    list.add(new MethodInsnNode(INVOKESTATIC, "fuckyou", "java", "(I)V"));

                    list.add(target);
                    method.insns().insert(block.label(), list);
                }
            }
        }
    }
}
