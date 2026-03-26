package dev.lvstrng.aidsfuscator.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.asm.RemapperImpl;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.HashMap;

public abstract class Transformer implements Opcodes {
    private final String name;
    protected final SecureRandom random;
    private int changes;

    public Transformer(String name) {
        this.name = name;
        this.random = new SecureRandom();
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

    public void remap(Context context) {
        var newClasses = new HashMap<String, JClass>();
        var newExcludedClasses = new HashMap<String, JClass>();

        for(var clazz : context.jarClasses()) {
            var remapped = new ClassNode();
            clazz.core().accept(new ClassRemapper(remapped, new RemapperImpl()));
            clazz.setCore(remapped);

            if(!clazz.isLibrary()) {
                newClasses.put(remapped.name, clazz);
                continue;
            }

            newExcludedClasses.put(remapped.name, clazz);
        }

        context.classMap().clear();
        context.classMap().putAll(newClasses);
        context.excluded().clear();
        context.excluded().putAll(newExcludedClasses);

        context.hierarchy().build();
    }

    protected boolean cantEditMethod(JClass node, JMethod method) {
        return cantEditMethod(node, method, false, false);
    }

    protected boolean cantEditMethod(JClass node, JMethod method, boolean ignoreInit) {
        return cantEditMethod(node, method, ignoreInit, false);
    }

    protected boolean cantEditMethod(JClass node, JMethod method, boolean ignoreInit, boolean ignoreSpecial) {
        if(method.name().equals("<clinit>")) return true;
        if(method.name().equals("<init>") && !ignoreInit) return true;
        if(method.name().contains("$") && !ignoreSpecial) return true;
        if(node.isAnnotatedBy("java/lang/FunctionalInterface")) return true;

        if(node.isEnum()) {
            if(method.name().equals("values") && method.desc().equals("()[L" + node.name() + ";")) return true;
            if(method.name().equals("valueOf") && method.desc().equals("(Ljava/lang/String;)L" + node.name() + ";")) return true;
        }

        if((node.access() & Opcodes.ACC_ANNOTATION) != 0) return true;
        if(Modifier.isNative(method.access())) return true;
        if(!method.name().equals("<init>")) {
            if (node.isLibMethod(method.name(), method.desc())) return true;
        }
        return method.name().equals("main") && method.desc().equals("([Ljava/lang/String;)V");
    }
}
