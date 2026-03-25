package dev.lvstrng.aidsfuscator.transform;

import dev.lvstrng.aidsfuscator.context.Context;

public abstract class Transformer {
    private final String name;
    private int changes;

    public Transformer(String name) {
        this.name = name;
    }

    public abstract void transform(Context context);

    public String name() {
        return name;
    }

    public void markChange() {
        changes++;
    }

    public int changes() {
        return changes;
    }
}
