package dev.lvstrng.aidsfuscator.analysis.order;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.utils.Pair;

import java.util.HashSet;
import java.util.Set;

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

        pairs.add(new Pair<>(firstClass, secondClass));
    }

    public Set<Pair<JClass, JClass>> pairs() {
        return pairs;
    }
}
