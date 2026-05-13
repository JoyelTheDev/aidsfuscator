package dev.lvstrng.aidsfuscator.exclude;

import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.tree.JMethod;

public class Exclusion {
    private final StringFilter filter;
    private final boolean inclusion;

    public Exclusion(String pattern) {
        if(pattern.startsWith("!")) {
            inclusion = true;
            pattern = pattern.substring(1);
        } else {
            inclusion = false;
        }

        this.filter = new StringFilter(pattern);
    }

    public boolean matchesClass(JClass clazz) {
        var match = filter.test(clazz.originalName());
        return inclusion != match;
    }

    public boolean matchesMethod(JMethod method) {
        return matchesMethod(method.owner(), method);
    }

    public boolean matchesField(JField field) {
        return matchesField(field.owner(), field);
    }

    public boolean matchesMethod(JClass clazz, JMethod method) {
        var match = filter.test(clazz.originalName() + "." + method.simpleOriginalName());
        return inclusion != match;
    }

    public boolean matchesField(JClass clazz, JField field) {
        var match = filter.test(clazz.originalName() + "." + field.simpleOriginalName());
        return inclusion != match;
    }

    @Override
    public int hashCode() {
        return filter.string().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Exclusion e))
            return false;

        return e.filter.string().equals(filter.string());
    }

    @Override
    public String toString() {
        return (inclusion ? "!" : "") + filter.string();
    }
}