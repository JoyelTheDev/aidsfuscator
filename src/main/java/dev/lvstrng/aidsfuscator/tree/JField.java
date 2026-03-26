package dev.lvstrng.aidsfuscator.tree;

import dev.lvstrng.aidsfuscator.property.PropertyContainer;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A FieldNode wrapper for easier use.
 */
public class JField {
    private JClass owner;
    private FieldNode core;
    private final PropertyContainer properties;
    private boolean library;

    private List<JField> parents, children;

    public JField(FieldNode core) {
        this.properties = new PropertyContainer();
        this.library = false;
        this.setCore(core);
    }

    public PropertyContainer properties() {
        return properties;
    }

    public void setLibrary() {
        this.library = true;
    }

    public boolean isLibrary() {
        return library;
    }

    public void setCore(FieldNode core) {
        this.core = core;

        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public void setOwner(JClass owner) {
        this.owner = owner;
    }

    public JClass owner() {
        return owner;
    }

    public List<JField> parents() {
        return parents;
    }

    public List<JField> children() {
        return children;
    }

    public List<JField> tree() {
        var list = new ArrayList<>(parents);
        list.addAll(children);
        return list;
    }

    public boolean isVirtual() {
        return !Modifier.isStatic(access());
    }

    public FieldNode core() {
        return core;
    }

    public int access() {
        return core.access;
    }

    public String name() {
        return core.name;
    }

    public String desc() {
        return core.desc;
    }

    public String signature() {
        return core.signature;
    }

    public Object value() {
        return core.value;
    }

    public void setValue(Object value) {
        core.value = value;
    }

    public String simpleName() {
        return "%s %s".formatted(name(), desc());
    }

    public String fullName() {
        return "%s.%s".formatted(owner, simpleName());
    }

    @Override
    public String toString() {
        return fullName();
    }
}
