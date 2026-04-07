package dev.lvstrng.aidsfuscator.transform.impl.salt;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodSaltTransformer extends Transformer {
    private final Map<JMethod, List<AbstractInsnNode>> seedInsns;
    private final Setting<Boolean> seedUselessMethods = setting("seedUselessMethods", true);

    public MethodSaltTransformer() {
        super("Method Salting", "methodSalting");
        this.seedInsns = new HashMap<>();
    }

    @Override
    public void transform(Context context) {
        var salts = new HashMap<String, Integer>();
        var methods = new HashMap<String, JMethod>();

        var graph = context.referenceGraph().build();

        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var refs = graph.refs(method);
                if(!seedUselessMethods.value()) {
                    var methodsIn = graph.methodRefsIn(method);
                    var fieldsIn = graph.fieldRefsIn(method);
                    if(refs.isEmpty()) {
                        if(methodsIn.stream().allMatch(e -> e.method().owner().isLibrary()) && fieldsIn.stream().allMatch(e -> e.field().owner().isLibrary()))
                            continue;
                    }
                }

                if(refs.stream().anyMatch(e -> !e.canEdit()))
                    continue;

                registerTree(graph, clazz, method, salts, methods);
            }
        }

        for(var id : methods.keySet()) {
            var seed = salts.get(id);
            var method = methods.get(id);
            var nodes = graph.refs(method);

            for(var node : nodes) {
                var insn = (MethodInsnNode) node.insn();

                insn.desc = insn.desc.replace(")", "I)");
                node.caller().insns().insertBefore(
                        insn,
                        add(context, node.caller(), ASMUtils.pushInt(seed))
                );
            }

            method.salt().updateVar(method.allocParameter(Type.INT_TYPE));
            markChange();
        }

        for(var method : seedInsns.keySet()) {
            if(!method.hasSalt())
                continue;

            for(var insn : seedInsns.get(method)) {
                var num = ASMUtils.getInt(insn);
                var list = new InsnList();

                if(num != method.salt().value()) {
                    list.add(method.salt().load());
                    list.add(context.properties().add(
                            ASMUtils.pushInt(method.salt().value() ^ num),
                            Property.IGNORE_INTEGER
                    ));
                    list.add(new InsnNode(IXOR));
                } else {
                    list.add(method.salt().load());
                }

                method.insns().insertBefore(insn, list);
                method.insns().remove(insn);
            }
        }
    }

    private AbstractInsnNode add(Context context, JMethod caller, AbstractInsnNode insn) {
        seedInsns.computeIfAbsent(caller, _ -> new ArrayList<>()).add(
                context.properties().add(insn, Property.UNPROTECTED_SALT)
        );
        return insn;
    }

    /**
     * Registers a seed for the entire method's tree
     * @param graph built reference graph
     * @param clazz owner class
     * @param method the method to start from
     * @param salts registered salts
     * @param methods registered methods
     */
    private void registerTree(ReferenceGraph graph, JClass clazz, JMethod method, Map<String, Integer> salts, Map<String, JMethod> methods) {
        var self = method.fullName();
        if(cantEditMethod(clazz, method, true))
            return;

        var canContinue = true;
        for(var member : method.tree()) {
            var calls = graph.refs(member);

            if(calls.stream().anyMatch(e -> !e.canEdit())) {
                canContinue = false;
                break;
            }
        }

        if(!canContinue)
            return;

        Integer salt = null;
        for(var member : clazz.tree()) {
            var id = MemberUtils.fullMethod(member, method);
            if(!salts.containsKey(id))
                continue;

            salt = salts.get(id);
            break;
        }

        if(salts.containsKey(self) && salt == null)
            salt = salts.get(self);

        if(salt == null)
            salt = random.nextInt();

        for(var member : clazz.tree()) {
            if(member.isLibrary())
                continue;

            var id = MemberUtils.fullMethod(member, method);
            salts.put(id, salt);

            var result = member.findMethod(method.name(), method.desc());
            if(result.isEmpty())
                continue;

            var res = result.get();
            methods.put(id, res);
            res.makeSalt(salt, -1);
        }

        salts.put(self, salt);
        methods.put(self, method);
        method.makeSalt(salt, -1);
    }
}
