package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LocalVariableNameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowFlatteningTransformer;

public class TestMain {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("out/artifacts/aidsfuscator_jar/aidsfuscator.jar")
                .libs("libs/")
                .out("out-out.jar")
                .initialize()
                .transform(
                        new ControlFlowFlatteningTransformer(),
                        new LocalVariableNameTransformer()
                )
                .exportJar();
    }
}
