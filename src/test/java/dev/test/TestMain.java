package dev.test;

import dev.lvstrng.aidsfuscator.config.initOrder.ClassInitOrderLoader;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.data.IntegerEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.StringEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowFlatteningTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowShufflingTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.test.transform.ClassSaltTransformer;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("eval.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize();

        new ClassInitOrderLoader(context, "initOrder.json").load();

        context.transform(
                new MethodSaltTransformer(),
                new ControlFlowFlatteningTransformer(),
                new ClassSaltTransformer(),

                new IntegerEncryptTransformer(),
                new StringEncryptTransformer()
        ).exportJar();
    }
}
