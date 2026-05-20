package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.ref.MethodCallNode;
import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;


public class MethodParameterObfuscationTransformer extends Transformer {
    public MethodParameterObfuscationTransformer() {
        super("Obfuscate Method Parameters", "methodParameterObfuscate");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var obfuscatedMethods = new HashSet<JMethod>();

        for(var clazz : context.classes()) {
            registerClass(context, graph, clazz, obfuscatedMethods);
        }

        for(var method : obfuscatedMethods) {
            var args = method.args();
            if(method.isAbstract() || method.isNative() || method.insns().size() == 0) {
                method.core().desc = "([Ljava/lang/Object;)" + method.returnType().getDescriptor();
                continue;
            }
            unpackArgs(context, method, args);

            var refs = graph.refs(method);
            for(var ref : refs) {
                var caller = ref.caller();
                var insn = (MethodInsnNode) ref.insn();

                var list = new InsnBuilder()
                        ._int(args.length).addProps(context, Property.IGNORE_INTEGER)
                        .anewarray("java/lang/Object");

                for(int i = args.length - 1; i >= 0; i--) {
                    var arg = args[i];

                    if(arg.getSize() == 2) {
                        list.dup_x2().dup_x2().pop();
                    } else {
                        list.dup_x1().swap();
                    }

                    ASMUtils.box(list.result(), arg);
                    list.
                            _int(i).addProps(context, Property.IGNORE_INTEGER)
                            .swap()
                            .aastore();
                }

                caller.insns().insertBefore(insn, list.result());
                insn.desc = method.desc();
            }

            markChange();
        }
    }

    private void unpackArgs(Context context, JMethod method, Type[] args) {
        // ---- UNPACK ----
        var argIndex = method.isVirtual() ? 1 : 0;
        var varIndex = argIndex + 1;
        var list = new InsnList();
        list.add(new VarInsnNode(ALOAD, argIndex));
        for(int i = 0; i < args.length; i++) {
            var arg = args[i];

            list.add(new InsnNode(DUP));
            list.add(context.properties().add(ASMUtils.pushInt(i), Property.IGNORE_INTEGER));
            list.add(new InsnNode(AALOAD));
            ASMUtils.unbox(list, arg);
            list.add(new VarInsnNode(arg.getOpcode(ISTORE), varIndex));

            varIndex += arg.getSize();
        }
        list.add(new InsnNode(POP));

        // ---- OFFSET ALL VARS ----
        for(var insn : method.insns()) {
            switch (insn) {
                case VarInsnNode v -> {
                    if(method.isVirtual() && v.var == 0)
                        continue;

                    v.var++;
                }
                case IincInsnNode v -> v.var++;
                default -> {}
            }
        }

        // ---- FINISH ----
        method.insns().insert(list);
        method.allocVar();

        if(method.hasSalt())
            method.salt().updateVar(method.salt().local() + 1);
        method.core().desc = "([Ljava/lang/Object;)" + method.returnType().getDescriptor();
    }

    private void registerClass(Context context, ReferenceGraph graph, JClass clazz, Set<JMethod> obfuscatedMethods) {
        var toCheck = new HashSet<JMethod>();

        // ---- SET DANGER METHODS ----
        for(var method : clazz.methods()) {
            var impactedClasses = impactedClasses(context, clazz, method);
            if(!skipMethodAndTree(graph, method, impactedClasses))
                continue;

            toCheck.add(method);
            toCheck.addAll(method.tree());
        }

        // ---- REGISTER ----
        for(var method : clazz.methods()) {
            if(toCheck.contains(method))
                continue;

            var duplicateOpt = toCheck.stream()
                    .filter(e -> e != method)                    // filter this method
                    .filter(e -> e.name().equals(method.name())) // has same name
                    .filter(e -> e.desc().equals("([Ljava/lang/Object;)" + method.returnType().getDescriptor())) // has desired descriptor, causes collision if remapped
                    .findAny();

            if(duplicateOpt.isPresent()) // found duplicate, continue
                continue;

            registerMethodTree(method, obfuscatedMethods);
        }
    }

    private void registerMethodTree(JMethod method, Set<JMethod> obfuscatedMethods) {
        for(var member : method.tree()) {
            if(!obfuscatedMethods.add(member))
                continue;

            member.removeAccessFlags(ACC_VARARGS);
        }

        if(!obfuscatedMethods.add(method))
            return;

        method.removeAccessFlags(ACC_VARARGS);
    }

    private boolean skipMethodAndTree(ReferenceGraph graph, JMethod method, Set<JClass> impactedClasses) {
        for(var member : impactedClasses) {
            var opt = member.findMethod(method.name(), method.desc());
            if(opt.isPresent())
                method = opt.get();

            if(member.isLibMethod(method.name(), method.desc()))
                return true;

            if(Exclusions.PARAMETER_OBFUSCATE.excluded(member))
                return true;

            if(Exclusions.PARAMETER_OBFUSCATE.excluded(member, method))
                return true;

            if(cantEditMethod(member, method))
                return true;

            var refs = graph.refs(method);
            if (refs.stream().anyMatch(MethodCallNode::cantEdit))
                return true;
        }

        return false;
    }

    private Set<JClass> impactedClasses(Context context, JClass clazz, JMethod method) {
        var classes = new HashSet<>(clazz.children());
        classes.add(clazz);

        for(var parent : clazz.tree()) {
            if(!parent.hasMethodInTree(context, method))
                continue;

            classes.add(parent);
            classes.addAll(parent.children());
        }

        return classes;
    }
}