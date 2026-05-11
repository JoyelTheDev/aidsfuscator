package dev.lvstrng.aidsfuscator.transform.impl.salt;

import dev.lvstrng.aidsfuscator.analysis.ref.MethodCallNode;
import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.salt.ISalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;
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
    private final Setting<Boolean> advancedSalting = setting("advancedSalting", true); // adds a random number to AND gate the caller salt with another number, so salt values can not be traced backwards if callers are known

    public MethodSaltTransformer() {
        super("Method Salting", "methodSalting");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();

        for(var clazz : context.classes()) {
            if(Exclusions.METHOD_SALTING.excluded(clazz))
                continue;

            for(var method : clazz.methods()) {
                if(Exclusions.METHOD_SALTING.excluded(method))
                    continue;

                if(clazz.tree().stream().anyMatch(e -> Exclusions.METHOD_SALTING.excluded(e) && e.hasMethodInTree(context, method)))
                    continue;

                if(clazz.tree().stream().anyMatch(e -> Exclusions.METHOD_SALTING.excluded(e, method) && e.hasMethodInTree(context, method)))
                    continue;

                if(cantEditMethod(clazz, method, true))
                    continue;

                // ---- CHECK FOR INVALID CALLS ----
                var refs = graph.refs(method);
                if(refs.stream().anyMatch(MethodCallNode::cantEdit))
                    continue;

                var foundInvalid = false;
                for(var other : method.tree()) {
                    var otherRefs = graph.refs(other);
                    if(otherRefs.stream().noneMatch(MethodCallNode::cantEdit))
                        continue;

                    foundInvalid = true;
                    break;
                }

                if(foundInvalid)
                    continue;

                // ---- REGISTER SALTS FOR METHODS ----
                register(method);
            }
        }

        // ---- ADD PARAMETER TO METHODS ----
        var saltedMethods = new HashSet<JMethod>();
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                if(!method.hasSalt())
                    continue;

                method.salt().updateVar(method.allocParameter(Type.INT_TYPE));
                saltedMethods.add(method);
                markChange();
            }
        }

        // ---- OBFUSCATE RAW SALTS ----
        for(var method : saltedMethods) {
            var refs = graph.refs(method);
            var salt = method.salt();

            for(var ref : refs) {
                var caller = ref.caller();
                var insn = (MethodInsnNode) ref.insn();

                var list = new InsnBuilder();
                if(caller.hasSalt()) {
                    var callerSalt = caller.salt();

                    if(callerSalt.value() != salt.value()) {
                        saltTheSalt(context, salt, callerSalt, list);
                    } else {
                        list.add(callerSalt.load());
                    }
                } else {
                    list._int(salt.value()).addProps(context, Property.UNPROTECTED_SALT);
                }

                caller.insns().insertBefore(insn, list.result());
                insn.desc = insn.desc.replace(")", "I)");
            }
        }
    }

    private void saltTheSalt(Context context, ISalt salt, ISalt callerSalt, InsnBuilder list) {
        if(advancedSalting.value()) {
            var mask = random.nextInt();
            var maskedSalt = callerSalt.value() & mask;

            list
                    .add(callerSalt.load())
                    ._int(mask).addProps(context, Property.IGNORE_INTEGER)
                    .iand()
                    ._int(maskedSalt ^ salt.value()).addProps(context, Property.IGNORE_INTEGER)
                    .ixor();
        } else {
            list
                    .add(callerSalt.load())
                    ._int(callerSalt.value() ^ salt.value()).addProps(context, Property.IGNORE_INTEGER)
                    .ixor();
        }
    }

    private void register(JMethod method) {
        // find maybe existing salts
        int salt = -1;
        for(var member : method.tree()) {
            if(!member.hasSalt())
                continue;

            salt = member.salt().value();
            break;
        }

        if(method.hasSalt())
            salt = method.salt().value();

        // if no salt found, generate a new one
        if(salt == -1)
            salt = random.nextInt();

        // make salts
        for(var member : method.tree()) {
            member.makeSalt(salt, -1);
            if((member.access() & ACC_VARARGS) != 0)
                member.core().access &= ~ACC_VARARGS;
        }
        method.makeSalt(salt, -1);
        if((method.access() & ACC_VARARGS) != 0)
            method.core().access &= ~ACC_VARARGS;
    }
}