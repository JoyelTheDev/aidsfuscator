package dev.lvstrng.aidsfuscator.transform.impl.salt;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.analysis.ref.MethodCallNode;
import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.salt.ISalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.*;

/**
 * A transformer that adds an extra {@code int} parameter to methods where it's possible and strengthens other obfuscation transformers.
 * <br>
 * Rewritten on May 7, 2026
 * </br>
 * @author lvstrng
 */
public class MethodSaltTransformer extends Transformer {
    public MethodSaltTransformer() {
        super("Method Salting", "methodSalting");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var saltedMethods = new HashSet<JMethod>();

        // ---- REGISTER ALL METHODS ----
        for(var clazz : context.classes()) {
            registerClass(context, graph, clazz, saltedMethods);
        }

        // ---- MODIFY METHOD ----
        for(var method : saltedMethods) {
            method.salt().updateVar(method.allocParameter(Type.INT_TYPE));
            markChange();
        }

        var modified = new HashSet<AbstractInsnNode>();
        for(var method : saltedMethods) {
            var refs = graph.refs(method);
            var salt = method.salt();

            for(var ref : refs) {
                var call = (MethodInsnNode) ref.insn();
                if(!modified.add(call)) // if already visited, skip to avoid NoSuchMethodError
                    continue;

                var caller = ref.caller();
                var frames = caller.frames(context);

                salt(context, caller, salt, call, frames);
            }
        }
    }

    private void salt(Context context, JMethod caller, ISalt salt, MethodInsnNode call, Map<AbstractInsnNode, SimpleFrame> frames) {
        var list = new InsnList();

        if(!caller.canSalt(frames.get(call))) { // if unable to salt, use raw salt
            list.add(context.properties().add(ASMUtils.pushInt(salt.value()), Property.UNPROTECTED_SALT));
        } else {
            var mask = random.nextInt();
            var masked = caller.salt().value() & mask;

            list.add(caller.salt().load());
            list.add(context.properties().add(ASMUtils.pushInt(mask), Property.IGNORE_INTEGER));
            list.add(new InsnNode(IAND));
            list.add(context.properties().add(ASMUtils.pushInt(masked ^ salt.value()), Property.IGNORE_INTEGER));
            list.add(new InsnNode(IXOR));
        }

        caller.insns().insertBefore(call, list);
        call.desc = call.desc.replace(")", "I)");
    }

    private void registerClass(Context context, ReferenceGraph graph, JClass clazz, Set<JMethod> saltedMethods) {
        var toCheck = new HashSet<JMethod>();

        // ---- SET DANGER METHODS ----
        for(var method : clazz.methods()) {
            var impactedClasses = impactedClasses(context, clazz, method);

            if(skipMethodAndTree(graph, method, impactedClasses)) {
                toCheck.addAll(method.tree());
                toCheck.add(method);
            }
        }

        // ---- REGISTER METHODS ----
        for(var method : clazz.methods()) {
            if(toCheck.contains(method)) // if danger method, skip...
                continue;

            var duplicateOpt = toCheck.stream()
                    .filter(e -> e != method)                    // filter this method
                    .filter(e -> e.name().equals(method.name())) // has same name
                    .filter(e -> e.desc().equals(method.desc().replace(")", "I)"))) // has desired descriptor, causes collision if remapped
                    .findAny();

            if(duplicateOpt.isPresent()) // found duplicate, continue
                continue;

            registerMethodTree(context, clazz, method, saltedMethods);
        }
    }

    private void registerMethodTree(Context context, JClass clazz, JMethod method, Set<JMethod> saltedMethods) {
        var impacted = impactedClasses(context, clazz, method);
        var salt = findOrGenerateSalt(method, impacted);

        for(var member : method.tree()) {
            if(!saltedMethods.add(member))
                continue;

            member.removeAccessFlags(ACC_VARARGS);
            member.makeSalt(salt, -1);
        }

        if(!saltedMethods.add(method))
            return;

        method.removeAccessFlags(ACC_VARARGS);
        method.makeSalt(salt, -1);
    }

    private int findOrGenerateSalt(JMethod method, Set<JClass> impactedClasses) {
        for(var clazz : impactedClasses) {
            var foundOpt = clazz.findMethod(method.name(), method.desc());
            if(foundOpt.isEmpty())
                continue;

            var found = foundOpt.get();
            if(!found.hasSalt())
                continue;

            return found.salt().value();
        }

        return random.nextInt();
    }

    private boolean skipMethodAndTree(ReferenceGraph graph, JMethod method, Set<JClass> impactedClasses) {
        for(var member : impactedClasses) {
            var opt = member.findMethod(method.name(), method.desc());
            if(opt.isPresent())
                method = opt.get();

            if(member.isLibMethod(method.name(), method.desc()))
                return true;

            if(Exclusions.METHOD_SALTING.excluded(member))
                return true;

            if(Exclusions.METHOD_SALTING.excluded(member, method))
                return true;

            if(cantEditMethod(member, method, true))
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