package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.data.ConstantsFixTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.IntegerEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.StringEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.dynamic.ReferenceObfuscationTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowFlatteningTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowShufflingTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.optimize.TrimTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.SimpleClassSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LineNumberTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LocalVariableNameTransformer;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs/")
                .out("out.jar")
                .initialize();

        context.referenceManager().addMethodCandidate("*");
        context.referenceManager().addFieldCandidate("*");

        context.transform(
                new TrimTransformer(),

                new FieldRenameTransformer(),
                new MethodRenameTransformer(),
                new ClassRenameTransformer(),

                new LocalVariableNameTransformer(),
                new LineNumberTransformer(),
                new MethodSaltTransformer(),
                new SimpleClassSaltTransformer(),

                new ConstantsFixTransformer(),
                new IntegerEncryptTransformer(),
                new StringEncryptTransformer(),

                new ControlFlowFlatteningTransformer(),
                new ControlFlowShufflingTransformer(),
                new ReferenceObfuscationTransformer()
        ).exportJar();
    }
}
