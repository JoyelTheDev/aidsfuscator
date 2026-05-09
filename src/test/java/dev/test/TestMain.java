package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.StringEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.SimpleClassSaltTransformer;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out-out.jar")
                .initialize();

        //context.referenceManager().addMethodCandidate("*");
        //context.referenceManager().addFieldCandidate("*");

        context.transform(
                new MethodSaltTransformer(),
                new SimpleClassSaltTransformer(),

                new StringEncryptTransformer()
        ).exportJar();
    }
}
