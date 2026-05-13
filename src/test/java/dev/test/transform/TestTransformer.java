package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.salt.impl.ClassSalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.classgen.impl.SimpleClassSaltClassGenerator;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

public class TestTransformer extends Transformer {
    public TestTransformer() {
        super("Test", "test");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var impacted = impactedClasses(context, clazz, method);
                System.out.println(method.simpleName());

                for(var c : impacted)
                    System.out.println("\t" + c.name());
            }
        }
    }

    private Set<JClass> impactedClasses(Context context, JClass clazz, JMethod method) {
        var classes = new HashSet<>(clazz.children());
        classes.add(clazz);

        for(var parent : clazz.tree()) {
            if(!parent.hasMethodInTree(context, method))
                continue;

            classes.add(parent);
            classes.addAll(parent.children());
        }

        return classes;
    }
}
