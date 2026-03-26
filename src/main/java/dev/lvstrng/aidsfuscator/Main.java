package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;

public class Main {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("eval.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform(
                        new FieldRenameTransformer(),
                        new MethodRenameTransformer(),
                        new ClassRenameTransformer()
                )
                .exportJar();
    }
}
