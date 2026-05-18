package dev.lvstrng.aidsfuscator.context.hierarchy;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builds a strict hierarchy based on the used classes in the JAR file.
 * @author lvstrng
 */
public class SimpleHierarchy implements IHierarchy {
    private final Context context;
    private final Predicate<JMethod> blacklistedMethod = (e -> e.name().equals("<init>") || !e.isVirtual() || e.isPrivate()); // these methods do not have hierarchy at all
    private final Predicate<JField> blacklistedField = (e -> !e.isVirtual() || e.isPrivate()); // static fields do not have hierarchy

    public SimpleHierarchy(Context context) {
        this.context = context;
    }

    @Override
    public void build() {
        var classes = context.jarClasses();
        this.clearHierarchy(classes);

        classes.forEach(this::build);
        classes.forEach(clazz -> {
            clazz.methods().forEach(this::build);
            clazz.fields().forEach(this::build);
        });
    }

    @Override
    public void build(JClass clazz) {
        var parents = getTree(clazz).stream()
                .map(context::forName)
                .filter(e -> e != clazz)
                .toList();

        clazz.parents().addAll(parents);
        for(var parent : parents) {
            parent.children().add(clazz);
        }
    }

    @Override
    public void build(JMethod method) {
        var owner = method.owner();
        if(blacklistedMethod.test(method))
            return;

        // parent methods
        for(var parent : owner.parents()) {
            var opt = parent.findMethod(method.name(), method.desc());
            if(opt.isEmpty())
                continue;

            var parentMethod = opt.get();
            if(parentMethod.isVirtual() != method.isVirtual())
                continue;

            if(blacklistedMethod.test(parentMethod))
                continue;

            method.parents().add(parentMethod);
            parentMethod.children().add(method);
        }

        // child methods
        for(var child : owner.children()) {
            var opt = child.findMethod(method.name(), method.desc());
            if(opt.isEmpty())
                continue;

            var childMethod = opt.get();
            if(childMethod.isVirtual() != method.isVirtual())
                continue;

            if(blacklistedMethod.test(childMethod))
                continue;

            method.children().add(childMethod);
            childMethod.parents().add(method);
        }
    }

    @Override
    public void build(JField field) {
        var owner = field.owner();
        if(blacklistedField.test(field))
            return;

        // parent fields
        for(var parent : owner.parents()) {
            var opt = parent.findField(field.name(), field.desc());
            if(opt.isEmpty())
                continue;

            var parentField = opt.get();
            if(parentField.isVirtual() != field.isVirtual())
                continue;

            if(blacklistedField.test(parentField))
                continue;

            field.parents().add(parentField);
            parentField.children().add(field);
        }

        // child fields
        for(var child : owner.children()) {
            var opt = child.findField(field.name(), field.desc());
            if(opt.isEmpty())
                continue;

            var childField = opt.get();
            if(childField.isVirtual() != field.isVirtual())
                continue;

            if(blacklistedField.test(childField))
                continue;

            field.children().add(childField);
            childField.parents().add(field);
        }
    }

    @Override
    public void buildIfEmpty(JClass clazz) {
        if(!clazz.tree().isEmpty())
            return;

        build(clazz);
    }

    @Override
    public void buildIfEmpty(JMethod method) {
        if(!method.tree().isEmpty())
            return;

        build(method);
    }

    @Override
    public void buildIfEmpty(JField field) {
        if(!field.tree().isEmpty())
            return;

        build(field);
    }

    @Override
    public String commonSuperClass(String type1, String type2) {
        if(type1.startsWith("[") || type2.startsWith("[")) {
            if(!type1.startsWith("[") || !type2.startsWith("["))
                return object;

            return commonArray(type1, type2);
        }

        var node = context.forName(type1);
        var other = context.forName(type2);
        if(node == null || other == null)
            return object;

        // interfaces can only have super class "java/lang/Object", so don't waste time.
        if(node.isInterface() || other.isInterface())
            return object;

        if (node.name().equals(other.name()))
            return node.name();

        // ensure both classes have their hierarchy built
        if(node.parents().isEmpty())
            build(node);
        if(other.parents().isEmpty())
            build(other);

        var nodeClasses = new LinkedHashSet<JClass>();
        var otherClasses = new LinkedHashSet<JClass>();
        getSuperClasses(node, nodeClasses);
        getSuperClasses(other, otherClasses);

        if(otherClasses.contains(node))
            return node.name();
        if(nodeClasses.contains(other))
            return other.name();

        for(var clazz : nodeClasses) {
            if(otherClasses.contains(clazz))
                return clazz.name();
        }

        return object; // fallback if no common superclass is found
    }

    private String commonArray(String type1, String type2) {
        var t1 = Type.getObjectType(type1);
        var t2 = Type.getObjectType(type2);
        if(t1.getDimensions() != t2.getDimensions())
            return "[".repeat(Math.min(t1.getDimensions(), t2.getDimensions()) - 1) + "Ljava/lang/Object;";

        var e1 = t1;
        do {
            e1 = e1.getElementType();
        } while (e1.getSort() == Type.ARRAY);

        var e2 = t2;
        do {
            e2 = e2.getElementType();
        } while (e2.getSort() == Type.ARRAY);

        if(e1.getSort() != e2.getSort())
            return "[".repeat(Math.min(t1.getDimensions(), t2.getDimensions()) - 1) + "Ljava/lang/Object;";

        if(e1.getSort() == Type.OBJECT) {
            var common = commonSuperClass(e1.getInternalName(), e2.getInternalName());
            return "[".repeat(t1.getDimensions()) + "L" + common + ";";
        }

        return "[".repeat(t1.getDimensions()) + e2.getInternalName();
    }

    private void getSuperClasses(JClass clazz, Set<JClass> classes) {
        if(!classes.add(clazz))
            return;

        if(clazz.superName() == null)
            return;

        var superClazz = context.forName(clazz.superName());
        if(superClazz == null)
            return;

        getSuperClasses(superClazz, classes);
    }

    private List<String> getTree(JClass clazz) {
        var tree = new ArrayList<String>();
        trace(context, clazz, tree);
        return tree;
    }

    private void clearHierarchy(List<JClass> classes) {
        classes.forEach(clazz -> {
            clazz.clear();

            clazz.methods().forEach(JMethod::clear);
            clazz.fields().forEach(JField::clear);
        });
    }
}