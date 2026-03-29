package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.data.IntegerEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.StringEncryptTransformer;

public class TestMain {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform(
                        new IntegerEncryptTransformer(),
                        new StringEncryptTransformer()
                )
                .exportJar();
    }
}
