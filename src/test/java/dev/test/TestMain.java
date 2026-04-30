package dev.test;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.polymorph.IntPolymorphStack;
import dev.lvstrng.aidsfuscator.polymorph.impl.*;
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
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.NamedOpcodes;
import dev.test.transform.CFGTest;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;

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
                new SimpleClassSaltTransformer()
        ).exportJar();
    }
}
