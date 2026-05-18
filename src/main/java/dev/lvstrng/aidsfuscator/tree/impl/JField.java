package dev.lvstrng.aidsfuscator.tree.impl;

import dev.lvstrng.aidsfuscator.property.PropertyContainer;
import dev.lvstrng.aidsfuscator.tree.IHierarchical;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * A FieldNode wrapper for easier use.
 */
public class JField implements IHierarchical<JField> {
    private JClass owner;
    private FieldNode core;
    private final PropertyContainer properties;

    private final String originalName, originalDesc;
    private boolean library;

    private Set<JField> parents, children;

    public JField(FieldNode core) {
        this.properties = new PropertyContainer();
        this.library = false;
        this.originalName = core.name;
        this.originalDesc = core.desc;
        this.setCore(core);
    }

    public boolean isPublic() {
        return Modifier.isPublic(access());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(access());
    }

    public boolean isProtected() {
        return Modifier.isProtected(access());
    }

    public String originalName() {
        return originalName;
    }

    public String originalDesc() {
        return originalDesc;
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

        this.parents = new HashSet<>();
        this.children = new HashSet<>();
    }

    public void setOwner(JClass owner) {
        this.owner = owner;
    }

    public JClass owner() {
        return owner;
    }

    @Override
    public Set<JField> parents() {
        return parents;
    }

    @Override
    public Set<JField> children() {
        return children;
    }

    public boolean isVirtual() {
        return !Modifier.isStatic(access());
    }

    public boolean isFinal() {
        return Modifier.isFinal(access());
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

    public String simpleOriginalName() {
        return "%s %s".formatted(originalName, originalDesc);
    }

    public String fullOriginalName() {
        return "%s.%s".formatted(owner.originalName(), simpleOriginalName());
    }

    public String fullName() {
        return "%s.%s".formatted(owner, simpleName());
    }

    @Override
    public String toString() {
        return fullName();
    }
}
