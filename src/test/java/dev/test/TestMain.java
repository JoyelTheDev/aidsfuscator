package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.test.transform.TestTransformer;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("eval.jar")
                .libs("libs")
                .out("out.jar")
                .initialize();

        //context.referenceManager().addMethodCandidate("*");
        //context.referenceManager().addFieldCandidate("*");
        //context.referenceManager().addMethodCandidate("*");

        context.transform(
                new MethodSaltTransformer()
        ).exportJar();
    }
}
