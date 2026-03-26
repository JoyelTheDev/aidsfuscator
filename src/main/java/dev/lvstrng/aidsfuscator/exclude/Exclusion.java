package dev.lvstrng.aidsfuscator.exclude;

import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;

public class Exclusion {
    private final StringFilter filter;

    public Exclusion(String pattern) {
        this.filter = new StringFilter(pattern);
    }

    public boolean matchesClass(JClass clazz) {
        return filter.test(clazz.originalName());
    }

    public boolean matchesMethod(JMethod method) {
        return matchesMethod(method.owner(), method);
    }

    public boolean matchesField(JField field) {
        return matchesField(field.owner(), field);
    }

    public boolean matchesMethod(JClass clazz, JMethod method) {
        return filter.test(clazz.originalName() + "." + method.simpleOriginalName());
    }

    public boolean matchesField(JClass clazz, JField field) {
        return filter.test(clazz.originalName() + "." + field.simpleOriginalName());
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
        return filter.string();
    }
}
