package dev.lvstrng.aidsfuscator;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LocalVariableNameTransformer;

public class Main {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("eval.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform(
                        new LocalVariableNameTransformer(),

                        new FieldRenameTransformer(),
                        new MethodRenameTransformer(),
                        new ClassRenameTransformer()
                )
                .exportJar();
    }
}
