package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.test.transform.CFGTest;
import dev.test.transform.FrameTest;
import dev.test.transform.NamingTest;

public class TestMain {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform(
                        new NamingTest()
                )
                .exportJar();
    }
}
