package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.StringEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.dynamic.ReferenceObfuscationTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.ClassSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LineNumberTransformer;
import dev.test.transform.MethodParameterObfuscationTransformer;
import dev.test.transform.NewMethodRenameTransformer;
import dev.test.transform.TestTransformer;
import org.objectweb.asm.util.Textifier;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("crackme.jar")
                .libs("libs")
                .out("out.jar")
                .initialize();

        //context.referenceManager().addMethodCandidate("*");
        //context.referenceManager().addFieldCandidate("*");
        context.referenceManager().addMethodCandidate("*");

        context.transform(
                new ClassRenameTransformer(),
                new FieldRenameTransformer(),
                new MethodRenameTransformer(),

                new ClassSaltTransformer(),
                new ReferenceObfuscationTransformer()
        ).exportJar();
    }
}
