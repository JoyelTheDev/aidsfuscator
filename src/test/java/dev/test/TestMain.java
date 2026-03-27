package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.transform.impl.data.IntegerEncryptTransformer;
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
        Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize()
                .transform(
                        new MethodSaltTransformer(),
                        new IntegerEncryptTransformer()
                )
                .exportJar();
    }
}
