package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.data.IntegerEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.StringEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowShufflingTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.rename.MethodRenameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LocalVariableNameTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.ControlFlowFlatteningTransformer;

public class TestMain {
    public static void main(String[] args) {
        Context.newInstance()
                .computeFrames()
                .in("out/artifacts/aidsfuscator_jar/aidsfuscator.jar")
                .libs("libs/")
                .out("out-out.jar")
                .initialize()
                .transform(
                        new FieldRenameTransformer(),
                        new MethodRenameTransformer(),
                        new ClassRenameTransformer(),

                        new LocalVariableNameTransformer(),
                        new MethodSaltTransformer(),

                        new IntegerEncryptTransformer(),
                        new StringEncryptTransformer(),

                        new ControlFlowFlatteningTransformer(),
                        new ControlFlowShufflingTransformer()
                )
                .exportJar();
    }
}
