package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.test.transform.CFGTest;
import dev.test.transform.FrameTest;
import dev.test.transform.NamingTest;
import dev.test.transform.OldNameTest;

public class TestMain {
    public static void main(String[] args) {
        Exclusions.RENAME_CLASS.addClass("Main");

        Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform(
                        new FieldRenameTransformer(),
                        new MethodRenameTransformer(),
                        new ClassRenameTransformer(),
                        new MethodSaltTransformer(),
                        new OldNameTest()
                )
                .exportJar();
    }
}
