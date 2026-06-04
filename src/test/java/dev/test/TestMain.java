package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.test.transform.MethodInlineTransformer;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("out/artifacts/aidsfuscator_jar/aidsfuscator.jar")
                .libs("libs")
                .out("out.jar")
                .setAggressiveOverload(true);

        //context.referenceManager().addMethodCandidate("*");
        //context.referenceManager().addFieldCandidate("*");
        //context.referenceManager().addMethodCandidate("*");

        Exclusions.GLOBAL.addClass("dev/lvstrng/aidsfuscator/api");
        context.run(
                new MethodInlineTransformer()
        );
    }
}
