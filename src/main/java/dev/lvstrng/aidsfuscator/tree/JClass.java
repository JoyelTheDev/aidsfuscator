package dev.lvstrng.aidsfuscator.tree;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.PropertyContainer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A ClassNode wrapper for easier use.
 */
public class JClass {
    private ClassNode core;
    private final PropertyContainer properties;
    private boolean library;

    private List<JClass> parents, children;
    private List<JField> fields;
    private List<JMethod> methods;

    public JClass(ClassNode core) {
        this.properties = new PropertyContainer();
        this.library = false;
        this.setCore(core);
    }

    public void setLibrary() {
        this.library = true;
    }

    public boolean isLibrary() {
        return library;
    }

    public boolean isVirtual() {
        return !Modifier.isStatic(access());
    }

    public boolean isInterface() {
        return Modifier.isInterface(access());
    }

    public PropertyContainer properties() {
        return properties;
    }

    public boolean isAssignableFrom(JClass clazz) {
        if(this == clazz)
            return true;
        return clazz.parents.contains(this);
    }

    public JMethod findMethodFull(Context context, String name, String desc) {
        var method = findMethod(name, desc).orElse(null);
        if(method != null)
            return method;

        if(parents.isEmpty())
            context.hierarchy().build(this);

        for(var parent : parents) {
            method = parent.findMethod(name, desc).orElse(null);
            if(method != null) break;
        }

        return method;
    }

    public JField findFieldFull(Context context, String name, String desc) {
        var field = findField(name, desc).orElse(null);
        if(field != null)
            return field;

        if(parents.isEmpty())
            context.hierarchy().build(this);

        for(var parent : parents) {
            field = parent.findField(name, desc).orElse(null);
            if(field != null) break;
        }

        return field;
    }

    public Optional<JMethod> findMethod(String name, String desc) {
        return methods.stream()
                .filter(e -> e.name().equals(name))
                .filter(e -> e.desc().equals(desc))
                .findAny();
    }

    public Optional<JField> findField(String name, String desc) {
        return fields.stream()
                .filter(e -> e.name().equals(name))
                .filter(e -> e.desc().equals(desc))
                .findAny();
    }

    public void setCore(ClassNode core) {
        this.core = core;

        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();

        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();

        core.methods.forEach(this::add);
        core.fields.forEach(this::add);
    }

    public void remove(JMethod method) {
        methods.remove(method);
        core.methods.remove(method.core());
    }

    public void remove(JField field) {
        fields.remove(field);
        core.fields.remove(field.core());
    }

    public void add(MethodNode method) {
        add(new JMethod(method));
    }

    public void add(FieldNode field) {
        add(new JField(field));
    }

    public void add(JMethod method) {
        methods.add(method);
        if(!core.methods.contains(method.core()))
            core.methods.add(method.core());

        method.setOwner(this);
    }

    public void add(JField field) {
        fields.add(field);
        if(!core.fields.contains(field.core()))
            core.fields.add(field.core());

        field.setOwner(this);
    }

    public List<JClass> parents() {
        return parents;
    }

    public List<JClass> children() {
        return children;
    }

    public List<JClass> tree() {
        var list = new ArrayList<>(parents);
        list.addAll(children);
        return list;
    }

    public List<JMethod> methods() {
        return methods;
    }

    public List<JField> fields() {
        return fields;
    }

    public ClassNode core() {
        return core;
    }

    public int access() {
        return core.access;
    }

    public String name() {
        return core.name;
    }

    public String signature() {
        return core.signature;
    }

    public String sourceFile() {
        return core.sourceFile;
    }

    public String sourceDebug() {
        return core.sourceDebug;
    }

    public void setSourceFile(String sourceFile) {
        core.sourceFile = sourceFile;
    }

    public void setSourceDebug(String sourceDebug) {
        core.sourceDebug = sourceDebug;
    }

    public String superName() {
        return core.superName;
    }

    public List<String> interfaces() {
        if(core.interfaces == null)
            core.interfaces = new ArrayList<>();

        return core.interfaces;
    }

    @Override
    public String toString() {
        return name();
    }
}
