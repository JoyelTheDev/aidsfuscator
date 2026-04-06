package dev.test;

import dev.lvstrng.aidsfuscator.config.initOrder.ClassInitOrderLoader;
import dev.lvstrng.aidsfuscator.context.Context;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out-out.jar")
                .initialize();

        new ClassInitOrderLoader(context, "initOrder.json").load();

        context.transform(
                new ClassSaltTransformer()
        ).exportJar();
    }
}
