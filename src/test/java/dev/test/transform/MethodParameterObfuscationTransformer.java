package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.ref.MethodCallNode;
import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;


public class MethodParameterObfuscationTransformer extends Transformer {
    public MethodParameterObfuscationTransformer() {
        super("Obfuscate Method Parameters", "methodParameterObfuscate");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var methods = new HashSet<JMethod>();

        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                if(cantEditMethod(clazz, method))
                    continue;

                var refs = graph.refs(method);
                if(refs.stream().anyMatch(MethodCallNode::cantEdit))
                    continue;

                var foundInvalid = false;
                for(var other : method.tree()) {
                    var otherRefs = graph.refs(other);
                    if(otherRefs.stream().noneMatch(MethodCallNode::cantEdit) && !cantEditMethod(other.owner(), other))
                        continue;

                    foundInvalid = true;
                    break;
                }

                if(foundInvalid)
                    continue;

                register(method, methods);
            }
        }

        for(var method : methods) {
            var args = method.args();

            changeRefs(context, method, graph, args);
            changeBody(context, method, args);
        }
    }

    private void changeBody(Context context, JMethod method, Type[] args) {
        markChange();

        method.core().desc = "([Ljava/lang/Object;)" + method.returnType().getDescriptor();
        if(method.isAbstract() || method.insns().size() == 0)
            return;

        int arrayIndex = method.isVirtual() ? 1 : 0;
        var currentVar = arrayIndex + 1;
        var builder = new InsnBuilder();

        builder._var(ALOAD, arrayIndex);
        for(int i = 0; i < args.length; i++) {
            var arg = args[i];

            builder
                    .dup()
                    ._int(i).addProps(context, Property.IGNORE_INTEGER)
                    .aaload();
            ASMUtils.unbox(builder.result(), arg);
            builder._var(arg.getOpcode(ISTORE), currentVar);

            currentVar += arg.getSize();
        }
        builder.pop();

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
        method.allocVar();
        method.core().access &= ~ACC_VARARGS;
        if(method.hasSalt())
            method.salt().updateVar(method.salt().local() + 1);
    }

    private void changeRefs(Context context, JMethod method, ReferenceGraph graph, Type[] args) {
        var refs = graph.refs(method);

        for(var ref : refs) {
            var insn = (MethodInsnNode) ref.insn();

            var list = new InsnBuilder()
                    ._int(args.length).addProps(context, Property.IGNORE_INTEGER)
                    .anewarray("java/lang/Object");

            for(int i = args.length - 1; i >= 0; i--) {
                var arg = args[i];

                if(arg.getSize() == 2) {
                    list.dup_x2().dup_x2().pop();
                } else list.dup_x1().swap();

                ASMUtils.box(list.result(), arg);
                list.
                        _int(i).addProps(context, Property.IGNORE_INTEGER)
                        .swap()
                        .aastore();
            }

            ref.caller().insns().insertBefore(insn, list.result());
            insn.desc = "([Ljava/lang/Object;)" + method.returnType().getDescriptor();
        }
    }

    private void register(JMethod method, Set<JMethod> methods) {
        for(var member : method.tree()) {
            methods.add(member);
            if((member.access() & ACC_VARARGS) != 0)
                member.core().access &= ~ACC_VARARGS;
        }

        methods.add(method);
        if((method.access() & ACC_VARARGS) != 0)
            method.core().access &= ~ACC_VARARGS;
    }
}