package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowFlatteningTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.optimize.DeadCodeCleanTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.optimize.TrimTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LineNumberTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LocalVariableNameTransformer;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("eval.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize();

        context.transform(
                new TrimTransformer(),
                new FieldRenameTransformer(),
                new MethodRenameTransformer(),
                new ClassRenameTransformer(),
                new LocalVariableNameTransformer(),
                new LineNumberTransformer(),
                new ControlFlowFlatteningTransformer(),
                new DeadCodeCleanTransformer()
        ).exportJar();
    }
}
