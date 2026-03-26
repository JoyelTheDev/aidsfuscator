package dev.lvstrng.aidsfuscator.exclude;

import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.tree.JMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Exclusions {
    GLOBAL("global", true, false, false),

    RENAME_CLASS("renameClass", true, true, true),
    RENAME_FIELD("renameField", true, true, false),
    RENAME_METHOD("renameMethod", true, false, true),

    LOCAL_NAMES("localNames", true, false, true),
    METHOD_SALTING("methodSalting", true, false, true),

    ;

    private final boolean excludesClass, excludesField, excludesMethod;
    private final String key;

    private final Set<Exclusion> classExclusions, fieldExclusions, methodExclusions;

    Exclusions(String key, boolean excludesClass, boolean excludesField, boolean excludesMethod) {
        this.key = key;

        this.excludesClass = excludesClass;
        this.excludesField = excludesField;
        this.excludesMethod = excludesMethod;

        this.classExclusions = new HashSet<>();
        this.fieldExclusions = new HashSet<>();
        this.methodExclusions = new HashSet<>();
    }

    public boolean excluded(JClass clazz) {
        var match = classExclusions.stream().anyMatch(e -> e.matchesClass(clazz));

        if(this == GLOBAL) return match;
        else return match && GLOBAL.excluded(clazz);
    }

    public boolean excluded(JField field) {
        return fieldExclusions.stream().anyMatch(e -> e.matchesField(field));
    }

    public boolean excluded(JClass clazz, JField field) {
        return fieldExclusions.stream().anyMatch(e -> e.matchesField(clazz, field));
    }

    public boolean excluded(JMethod method) {
        return methodExclusions.stream().anyMatch(e -> e.matchesMethod(method));
    }

    public boolean excluded(JClass clazz, JMethod method) {
        return methodExclusions.stream().anyMatch(e -> e.matchesMethod(clazz, method));
    }

    public String key() {
        return key;
    }

    public boolean excludesClass() {
        return excludesClass;
    }

    public boolean excludesField() {
        return excludesField;
    }

    public boolean excludesMethod() {
        return excludesMethod;
    }

    public void addMethod(String pattern) {
        methodExclusions.add(new Exclusion(pattern));
    }

    public void addField(String pattern) {
        fieldExclusions.add(new Exclusion(pattern));
    }

    public void addClass(String pattern) {
        methodExclusions.add(new Exclusion(pattern));
    }

    public Set<Exclusion> classExclusions() {
        return classExclusions;
    }

    public Set<Exclusion> fieldExclusions() {
        return fieldExclusions;
    }

    public Set<Exclusion> methodExclusions() {
        return methodExclusions;
    }
}
