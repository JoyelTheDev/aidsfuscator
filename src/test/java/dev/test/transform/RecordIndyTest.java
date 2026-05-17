package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.util.Arrays;

public class RecordIndyTest extends Transformer {
    public RecordIndyTest() {
        super("Record Indy Test", "recordIndyTest");
    }

    @Override
    public void transform(Context context) {

    }
}
