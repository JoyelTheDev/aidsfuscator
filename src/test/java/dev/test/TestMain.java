package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LineNumberTransformer;
import dev.test.transform.NewMethodRenameTransformer;
import dev.test.transform.TestTransformer;
import org.objectweb.asm.util.Textifier;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("out/artifacts/aidsfuscator_jar/wave/wave.jar")
                .libs("out/artifacts/aidsfuscator_jar/libs")
                .out("out.jar")
                .initialize();

        //context.referenceManager().addMethodCandidate("*");
        //context.referenceManager().addFieldCandidate("*");

        context.transform(
                new MethodRenameTransformer(),
                new MethodSaltTransformer()
        ).exportJar();
    }
}
