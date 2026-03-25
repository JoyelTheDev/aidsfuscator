package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.test.transform.FrameTest;

public class TestMain {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform(new FrameTest())
                .exportJar();
    }
}
