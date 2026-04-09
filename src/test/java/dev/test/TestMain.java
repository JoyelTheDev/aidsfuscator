package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.data.StringEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowFlatteningTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowShufflingTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.classSalting.SimpleClassSaltTransformer;
import dev.test.transform.ClassSaltTransformer;
import dev.test.transform.FrameTest;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("out/artifacts/aidsfuscator_jar/aidsfuscator.jar")
                .libs("libs/")
                .out("out-out.jar")
                .initialize();

        context.transform(
                new StringEncryptTransformer()
        ).exportJar();
    }
}
