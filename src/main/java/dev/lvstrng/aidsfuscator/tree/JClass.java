package dev.lvstrng.aidsfuscator.tree;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.PropertyContainer;
import dev.lvstrng.aidsfuscator.salt.impl.ClassSalt;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
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

    private final String originalName;
    private boolean library;

    private List<JClass> parents, children;
    private final List<JField> fields;
    private final List<JMethod> methods;

    private ClassSalt salt;
    private JClass initializerClass; // class that initializes

    public JClass(ClassNode core) {
        this.properties = new PropertyContainer();
        this.library = false;
        this.originalName = core.name;

        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();

        this.setCore(core);

        core.methods.forEach(this::add);
        core.fields.forEach(this::add);
    }

    public void setFirstInitializerClass(JClass clazz) {
        this.initializerClass = clazz;
    }

    public JClass getFirstInitializerClass() {
        return initializerClass;
    }

    public boolean hasFirstInitializerClass() {
        return initializerClass != null;
    }

    public boolean hasSalt() {
        return salt != null;
    }

    public ClassSalt salt() {
        return salt;
    }

    public void setSalt(ClassSalt salt) {
        this.salt = salt;
    }

    public int version() {
        return core.version;
    }

    public String originalName() {
        return originalName;
    }

    public boolean isAnnotatedBy(String annotation) {
        return MemberUtils.hasAnnotation(core.visibleAnnotations, annotation) ||
                MemberUtils.hasAnnotation(core.invisibleAnnotations, annotation);
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

    public boolean isEnum() {
        return core.superName != null && core.superName.equals("java/lang/Enum");
    }

    public boolean isRecord() {
        return core.superName != null && core.superName.equals("java/lang/Record");
    }

    public PropertyContainer properties() {
        return properties;
    }

    public boolean isAssignableFrom(JClass clazz) {
        if(this == clazz)
            return true;
        return clazz.parents.contains(this);
    }

    public boolean isLibMethod(String name, String desc) {
        for(var parent : parents()) {
            if(!parent.isLibrary())
                continue;

            if(parent.methods.stream().anyMatch(e -> e.name().equals(name) && e.desc().equals(desc)))
                return true;
        }

        return false;
    }

    public boolean isLibField(String name, String desc) {
        for(var parent : parents()) {
            if(!parent.isLibrary())
                continue;

            if(parent.fields.stream().anyMatch(e -> e.name().equals(name) && e.desc().equals(desc)))
                return true;
        }

        return false;
    }

    public boolean hasFieldInTree(String name, String desc) {
        for(var member : tree()) {
            if(member.fields.stream().anyMatch(e -> e.name().equals(name) && e.desc().equals(desc)))
                return true;
        }

        return false;
    }

    public boolean hasMethodInTree(String name, String desc) {
        for(var member : tree()) {
            if(member.methods.stream().anyMatch(e -> e.name().equals(name) && e.desc().equals(desc)))
                return true;
        }

        return false;
    }

    public JMethod findOrCreateClinit() {
        var opt = findMethod("<clinit>", "()V");
        if(opt.isPresent())
            return opt.get();

        var node = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        node.instructions.add(new InsnNode(Opcodes.RETURN));
        return add(node);
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
    }

    public void accept(ClassVisitor visitor, ClassNode remapped) {
        core.accept(visitor);

        // ---- refresh member cores ----
        for(int i = 0; i < methods.size(); i++) {
            methods.get(i).setCore(remapped.methods.get(i));
        }

        for(int i = 0; i < fields.size(); i++) {
            fields.get(i).setCore(remapped.fields.get(i));
        }
    }

    public void remove(JMethod method) {
        methods.remove(method);
        core.methods.remove(method.core());
    }

    public void remove(JField field) {
        fields.remove(field);
        core.fields.remove(field.core());
    }

    public JMethod add(MethodNode method) {
        return add(new JMethod(method));
    }

    public JField add(FieldNode field) {
        return add(new JField(field));
    }

    public JMethod add(JMethod method) {
        methods.add(method);
        if(!core.methods.contains(method.core()))
            core.methods.add(method.core());

        method.setOwner(this);
        return method;
    }

    public JField add(JField field) {
        fields.add(field);
        if(!core.fields.contains(field.core()))
            core.fields.add(field.core());

        field.setOwner(this);
        return field;
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

    public Type type() {
        return Type.getObjectType(name());
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
