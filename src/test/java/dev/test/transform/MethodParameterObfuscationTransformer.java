package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class MethodParameterObfuscationTransformer extends Transformer {
    public MethodParameterObfuscationTransformer() {
        super("Obfuscate Method Parameters", "methodParameterObfuscate");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var methods = new HashMap<JMethod, String>(); // method -> old desc

        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var refs = graph.refs(method);
                if(refs.stream().anyMatch(e -> !e.canEdit()))
                    continue;

                registerMethod(graph, clazz, method, methods);
            }
        }

        var modified = new HashSet<AbstractInsnNode>();
        for(var entry : methods.entrySet()) {
            var method = entry.getKey();
            var desc = entry.getValue();

            var args = Type.getArgumentTypes(desc);
            var refs = graph.refs(method);

            for(var node : refs) {
                var call = (MethodInsnNode) node.insn();
                if(!modified.add(call))
                    continue;

                var list = new InsnBuilder()
                        .add(context.propertyContainer().add(ASMUtils.pushInt(args.length), Property.IGNORE_INTEGER))
                        .anewarray("java/lang/Object");

                for(int i = args.length - 1; i >= 0; i--) {
                    var arg = args[i];

                    if(arg.getSize() == 2) {
                        list.dup_x2().dup_x2().pop();
                    } else list.dup_x1().swap();

                    ASMUtils.box(list.result(), arg);
                    list
                            .add(context.propertyContainer().add(ASMUtils.pushInt(i), Property.IGNORE_INTEGER))
                            .swap()
                            .aastore();
                }

                node.caller().insns().insertBefore(call, list.result());
                call.desc = method.desc();
            }

            if(method.hasSalt())
                method.salt().updateVar(method.salt().local() + 1);

            if(method.isAbstract() || method.insns().size() == 0)
                continue;

            var array = method.isVirtual() ? 1 : 0;
            var current = array + 1;
            var builder = new InsnBuilder();

            builder._var(ALOAD, array);
            for(int i = 0; i < args.length; i++) {
                var arg = args[i];

                builder
                        .dup()
                        .add(context.propertyContainer().add(ASMUtils.pushInt(i), Property.IGNORE_INTEGER))
                        .aaload();
                ASMUtils.unbox(builder.result(), arg);
                builder._var(arg.getOpcode(ISTORE), current);

                current += arg.getSize();
            }
            builder.add(method.setSafeInsn(new InsnNode(POP)));

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

            method.insns().insert(builder.result());
            method.core().access &= ~ACC_VARARGS;
            method.core().maxLocals++;
        }
    }

    private void registerMethod(ReferenceGraph graph, JClass clazz, JMethod method, Map<JMethod, String> methods) {
        if(cantEditMethod(clazz, method))
            return;

        if((method.access() & ACC_SYNTHETIC) != 0)
            return;

        for(var member : method.tree()) {
            if(cantEditMethod(member.owner(), member))
                return;

            if((member.access() & ACC_SYNTHETIC) != 0)
                return;
        }

        // ---- CHECK NON-EDITABLE REFS ----
        var cont = true;
        for(var member : method.tree()) {
            var refs = graph.refs(member);

            if(refs.stream().anyMatch(e -> !e.canEdit())) {
                cont = false;
                break;
            }
        }

        if(!cont)
            return;

        var returnType = method.returnType();
        var newDesc = "([Ljava/lang/Object;)" + returnType.getDescriptor();

        // ---- CHECK DUPLICATE METHODS ----
        for(var member : clazz.tree()) {
            if(member.findMethod(method.name(), newDesc).isPresent())
                return;
        }

        if(clazz.findMethod(method.name(), newDesc).isPresent())
            return;

        // ---- REGISTER METHODS ----
        for(var member : clazz.tree()) {
            var foundOpt = member.findMethod(method.name(), method.desc());
            if(foundOpt.isEmpty())
                continue;

            var func = foundOpt.get();
            methods.put(func, func.desc());
            func.core().desc = newDesc;
            func.core().signature = null;
        }

        methods.put(method, method.desc());
        method.core().desc = newDesc;
        method.core().signature = null;
    }
}
