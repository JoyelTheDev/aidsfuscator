package dev.lvstrng.aidsfuscator.context.hierarchy;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.exception.MissingMemberException;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builds a hierarchy based on the used classes in the JAR file.
 * @author lvstrng
 */
public class SimpleHierarchy implements IHierarchy {
    private final Context context;
    private final Predicate<JMethod> blacklistedMethod = (e -> e.name().equals("<init>") || !e.isVirtual() || e.isPrivate()); // these methods do not have hierarchy at all
    private final Predicate<JField> blacklistedField = (e -> !e.isVirtual()); // static fields do not have hierarchy

    public SimpleHierarchy(Context context) {
        this.context = context;
    }

    @Override
    public void build() {
        var classes = allNonLibraryClasses();
        clearHierarchy(classes);

        for(var clazz : classes) {
            build(clazz);
        }

        for(var clazz : classes) {
            for(var method : clazz.methods()) {
                build(method);
            }

            for(var field : clazz.fields()) {
                build(field);
            }
        }
    }

    @Override
    public void build(JClass clazz) {
        var parents = getTree(clazz).stream()
                .map(this::resolveClass)
                .filter(e -> e != null && e != clazz)
                .toList();

        for(var parent : parents) {
            if(!clazz.parents().contains(parent)) {
                clazz.parents().add(parent);
            }

            if(!parent.children().contains(clazz)) {
                parent.children().add(clazz);
            }
        }
    }

    @Override
    public void build(JMethod method) {
        var clazz = method.owner();
        if(blacklistedMethod.test(method))
            return;

        // ---- PARENT METHOD HIERARCHY ----
        for(var parent : clazz.parents()) {
            var opt = parent.findMethod(method.name(), method.desc());
            if(opt.isEmpty())
                continue;

            var parentMethod = opt.get();
            if(blacklistedMethod.test(parentMethod))
                continue;

            if(!method.parents().contains(parentMethod)) {
                method.parents().add(parentMethod);
            }
            if(!parentMethod.children().contains(method)) {
                parentMethod.children().add(method);
            }
        }

        // ---- CHILD METHOD HIERARCHY ----
        for(var child : clazz.children()) {
            var opt = child.findMethod(method.name(), method.desc());
            if(opt.isEmpty())
                continue;

            var childMethod = opt.get();
            if(blacklistedMethod.test(childMethod))
                continue;

            if(!method.children().contains(childMethod)) {
                method.children().add(childMethod);
            }
            if(!childMethod.parents().contains(method)) {
                childMethod.parents().add(method);
            }
        }
    }

    @Override
    public void build(JField field) {
        var clazz = field.owner();
        if(blacklistedField.test(field))
            return;

        // ---- PARENT FIELD HIERARCHY ----
        for(var parent : clazz.parents()) {
            var opt = parent.findField(field.name(), field.desc());
            if(opt.isEmpty())
                continue;

            var parentField = opt.get();
            if(blacklistedField.test(parentField))
                continue;

            if(!field.parents().contains(parentField)) {
                field.parents().add(parentField);
            }
            if(!parentField.children().contains(field)) {
                parentField.children().add(field);
            }
        }

        // ---- CHILD FIELD HIERARCHY ----
        for(var child : clazz.children()) {
            var opt = child.findField(field.name(), field.desc());
            if(opt.isEmpty())
                continue;

            var childField = opt.get();
            if(blacklistedField.test(childField))
                continue;

            if(!field.children().contains(childField)) {
                field.children().add(childField);
            }
            if(!childField.parents().contains(field)) {
                childField.parents().add(field);
            }
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

        var node = resolveClass(type1);
        var other = resolveClass(type2);
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

        var superClazz = resolveClass(clazz.superName());
        if(superClazz == null)
            return;

        getSuperClasses(superClazz, classes);
    }

    private List<String> getTree(JClass clazz) {
        var tree = new ArrayList<String>();
        traceSafe(clazz, tree);

        return tree;
    }

    private void clearHierarchy(List<JClass> classes) {
        for(var clazz : classes) {
            clazz.parents().clear();
            clazz.children().clear();

            for(var method : clazz.methods()) {
                method.parents().clear();
                method.children().clear();
            }

            for(var field : clazz.fields()) {
                field.parents().clear();
                field.children().clear();
            }
        }
    }

    private List<JClass> allNonLibraryClasses() {
        var all = new ArrayList<JClass>();
        all.addAll(context.jarClasses());
        all.addAll(context.artificials().values());

        var dedup = new HashSet<String>();
        var out = new ArrayList<JClass>();
        for(var clazz : all) {
            if(clazz.isLibrary())
                continue;

            if(!dedup.add(clazz.name()))
                continue;

            out.add(clazz);
        }

        return out;
    }

    private JClass resolveClass(String name) {
        try {
            return context.forName(name);
        } catch (MissingMemberException _) {
            return null;
        }
    }

    private void traceSafe(JClass clazz, List<String> used) {
        if(clazz == null)
            return;

        if(used.contains(clazz.name()))
            return;

        used.add(clazz.name());
        if(clazz.superName() != null && !clazz.isInterface()) {
            traceSafe(resolveClass(clazz.superName()), used);
        }

        for(var itf : clazz.interfaces()) {
            traceSafe(resolveClass(itf), used);
        }
    }
}