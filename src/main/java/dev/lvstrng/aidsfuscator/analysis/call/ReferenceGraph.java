package dev.lvstrng.aidsfuscator.analysis.call;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds a reference graph of every field and method in the obfuscatable (and excluded) classes.
 * @author lvstrng
 */
public class ReferenceGraph {
    private final Context context;
    private final Map<JMethod, Set<MethodCallNode>> methodReferences;
    private final Map<JField, Set<FieldCallNode>> fieldReferences;

    private final Map<JMethod, Set<MethodCallNode>> methodReferencesIn;
    private final Map<JMethod, Set<FieldCallNode>> fieldReferencesIn;

    public ReferenceGraph(Context context) {
        this.context = context;

        this.methodReferences = new ConcurrentHashMap<>();
        this.fieldReferences = new ConcurrentHashMap<>();

        this.methodReferencesIn = new ConcurrentHashMap<>();
        this.fieldReferencesIn = new ConcurrentHashMap<>();
    }

    public void build() {
        this.clear();

        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                buildInMethod(clazz, method);
            }
        }
    }

    public Set<MethodCallNode> refs(JMethod method) {
        return methodReferences.computeIfAbsent(method, _ -> new HashSet<>());
    }

    public Set<FieldCallNode> refs(JField field) {
        return fieldReferences.computeIfAbsent(field, _ -> new HashSet<>());
    }

    public Set<MethodCallNode> methodRefsIn(JMethod method) {
        return methodReferencesIn.computeIfAbsent(method, _ -> new HashSet<>());
    }

    public Set<FieldCallNode> fieldRefsIn(JMethod method) {
        return fieldReferencesIn.computeIfAbsent(method, _ -> new HashSet<>());
    }

    private void buildInMethod(JClass clazz, JMethod caller) {
        for(var insn : caller.insns()) {
            switch (insn) {
                case MethodInsnNode call -> {
                    var node = construct(clazz, caller, insn, call.owner, call.name, call.desc);
                    if(node == null)
                        break;

                    add(node);
                }
                case InvokeDynamicInsnNode indy -> {
                    handleHandle(clazz, caller, insn, indy.bsm);

                    for(var arg : indy.bsmArgs) {
                        if(!(arg instanceof Handle h))
                            continue;

                        handleHandle(clazz, caller, insn, h);
                    }
                }
                case LdcInsnNode ldc when ldc.cst instanceof ConstantDynamic condy -> {
                    handleHandle(clazz, caller, insn, condy.getBootstrapMethod());

                    for(int i = 0; i < condy.getBootstrapMethodArgumentCount(); i++) {
                        var arg = condy.getBootstrapMethodArgument(i);
                        if(!(arg instanceof Handle h))
                            continue;

                        handleHandle(clazz, caller, insn, h);
                    }
                }
                case FieldInsnNode field -> {
                    var node = constructField(clazz, caller, insn, field.owner, field.name, field.desc);
                    if(node == null)
                        break;

                    add(node);
                }
                default -> {}
            }
        }
    }

    private void handleHandle(JClass callerClass, JMethod caller, AbstractInsnNode insn, Handle handle) {
        var node = construct(callerClass, caller, insn, handle.getOwner(), handle.getName(), handle.getDesc());
        if(node == null)
            return;

        add(node);
    }

    private void add(FieldCallNode node) {
        fieldReferences.computeIfAbsent(node.field(), _ -> new HashSet<>()).add(node);
        fieldReferencesIn.computeIfAbsent(node.caller(), _ -> new HashSet<>()).add(node);
    }

    private void add(MethodCallNode node) {
        methodReferences.computeIfAbsent(node.method(), _ -> new HashSet<>()).add(node);
        methodReferencesIn.computeIfAbsent(node.caller(), _ -> new HashSet<>()).add(node);
    }

    public void clear() {
        fieldReferencesIn.clear();
        fieldReferences.clear();

        methodReferencesIn.clear();
        methodReferences.clear();
    }

    private FieldCallNode constructField(JClass callerClass, JMethod caller, AbstractInsnNode insn, String ownerName, String name, String desc) {
        var owner = context.forName(ownerName);
        if(owner == null)
            return null;

        var field = owner.findFieldFull(context, name, desc);
        if(field == null)
            return null;

        return new FieldCallNode(callerClass, caller, field, insn);
    }

    private MethodCallNode construct(JClass callerClass, JMethod caller, AbstractInsnNode insn, String ownerName, String name, String desc) {
        var owner = context.forName(ownerName);
        if (owner == null)
            return null;

        var method = owner.findMethodFull(context, name, desc);
        if (method == null)
            return null;

        return new MethodCallNode(callerClass, caller, method, insn);
    }
}
