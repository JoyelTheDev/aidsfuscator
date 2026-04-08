package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.salt.impl.ClassSalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.impl.salt.classSalting.SimpleClassSaltClassGenerator;
import org.objectweb.asm.tree.FieldNode;

public class TestTransformer extends Transformer {
    public TestTransformer() {
        super("Test", "test");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            var val = random.nextInt();
            var name = context.dictionary().newFieldName(clazz, "I");

            var field = clazz.add(new FieldNode(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, name, "I", null, null));
            clazz.setSalt(new ClassSalt(clazz, field, val));
        }

        var gen = new SimpleClassSaltClassGenerator();
        var clazz = gen.create(context);

        context.addArtificial(clazz);
    }
}
