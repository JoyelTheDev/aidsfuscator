package dev.lvstrng.aidsfuscator.transform.impl.optimize;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class TrimTransformer extends Transformer {
    private final Setting<Boolean> classes = setting("classes", true);
    private final Setting<Boolean> fields = setting("fields", true);
    private final Setting<Boolean> methods = setting("methods", true);

    private final Predicate<JClass> canTrimClass = e -> e.methods().isEmpty() && e.fields().isEmpty();

    public TrimTransformer() {
        super("Trim Transformer", "trim");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var toRemoveClasses = new HashSet<JClass>();

        for(var clazz : context.classes()) {
            if(Exclusions.TRIM.excluded(clazz))
                continue;

            var toRemoveMethods = new HashSet<JMethod>();
            var toRemoveFields = new HashSet<JField>();

            if(fields.value()) {
                for(var field : clazz.fields()) {
                    if(!canTrimField(graph, field))
                        continue;

                    if(Exclusions.TRIM.excluded(field))
                        continue;

                    toRemoveFields.add(field);
                    markChange();
                }
            }

            if(methods.value()) {
                for(var method : clazz.methods()) {
                    if(!canTrimMethod(graph, method))
                        continue;

                    if(Exclusions.TRIM.excluded(method))
                        continue;

                    toRemoveMethods.add(method);
                    markChange();
                }
            }

            toRemoveFields.forEach(clazz::remove);
            toRemoveMethods.forEach(clazz::remove);
            registerClass(toRemoveClasses, clazz);
        }

        toRemoveClasses.forEach(e -> {
            context.classMap().remove(e.name());
            markChange();
        });
    }

    private boolean canTrimMethod(ReferenceGraph graph, JMethod method) {
        if(cantEditMethod(method.owner(), method, true, false))
            return false;

        if(!method.tree().isEmpty())
            return false;

        var refs = graph.refs(method).stream().filter(node -> node.caller() != node.method()).toList();
        if(refs.isEmpty())
            return true;

        for(var ref : refs) {
            var otherRefs = graph.refs(ref.caller()).stream().filter(node -> node.caller() != node.method()).toList();
            if(!otherRefs.isEmpty())
                return false;
        }

        return false;
    }

    private boolean canTrimField(ReferenceGraph graph, JField field) {
        var refs = graph.refs(field);
        if(refs.isEmpty())
            return true;

        for(var ref : refs) {
            var otherRefs = graph.refs(ref.caller()).stream().filter(node -> node.caller() != node.method()).toList();
            if(!otherRefs.isEmpty())
                return false;
        }

        return false;
    }

    private void registerClass(Set<JClass> toRemoveClasses, JClass clazz) {
        if(Exclusions.TRIM.excluded(clazz))
            return;

        if(classes.value() && canTrimClass.test(clazz))
            toRemoveClasses.add(clazz);
    }
}
