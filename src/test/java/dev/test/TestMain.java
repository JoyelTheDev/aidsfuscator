package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.test.transform.MethodParameterObfuscationTransformer;

public class TestMain {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("out/artifacts/aidsfuscator_jar/aidsfuscator.jar")
                .libs("libs/")
                .out("out-out.jar")
                .initialize()
                .transform(
                        new MethodParameterObfuscationTransformer()
                )
                .exportJar();
    }
}
