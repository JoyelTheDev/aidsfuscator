package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LineNumberTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LocalVariableNameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.trim.TrimTransformer;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out-out.jar")
                .initialize();

        context.transform(
                new TrimTransformer(),

                new LineNumberTransformer(),
                new LocalVariableNameTransformer()
        ).exportJar();
    }
}
