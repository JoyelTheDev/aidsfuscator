package dev.lvstrng.aidsfuscator.context.order;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.utils.Pair;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles class initialization order used further in obfuscation by class salting.
 * @see dev.lvstrng.aidsfuscator.transform.impl.salt.SimpleClassSaltTransformer
 * @author lvstrng
 */
public class ClassInitOrderHandler {
    private final Context context;
    private final Set<Pair<JClass, JClass>> pairs; // before -> after

    public ClassInitOrderHandler(Context context) {
        this.context = context;
        this.pairs = new HashSet<>();
    }

    public void add(String before, String after) {
        before = before.replace('.', '/');
        after = after.replace('.', '/');

        var firstClass = context.forName(before);
        var secondClass = context.forName(after);

        secondClass.setFirstInitializerClass(firstClass);
        firstClass.initializes().add(secondClass);
        pairs.add(new Pair<>(firstClass, secondClass));
    }

    public Set<Pair<JClass, JClass>> pairs() {
        return pairs;
    }
}
