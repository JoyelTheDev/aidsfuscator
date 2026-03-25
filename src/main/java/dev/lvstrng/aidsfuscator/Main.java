package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;

public class Main {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform()
                .exportJar();
    }
}
