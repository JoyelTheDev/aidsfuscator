package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.ref.nodes.ClassReference;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.stream.Collectors;

public class TestTransformer extends Transformer {
    public TestTransformer() {
        super("Test", "test");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();

        for(var clazz : context.classes()) {
            var callerClasses = graph.refs(clazz).stream()
                    .filter(ClassReference::initializesClass)
                    .map(ClassReference::callerClass)
                    .filter(e -> e != clazz)
                    .filter(e -> e.core().innerClasses.stream().noneMatch(d -> d.name.equals(clazz.name()))) // gay ass inner classes
                    .collect(Collectors.toSet());

            if(callerClasses.size() != 1)
                continue;

            var initer = callerClasses.stream().toList().getFirst();
            Logger.success("Automatically found (%s -> %s) class init order pair", initer, clazz);
            context.initOrder().add(initer.originalName(), clazz.originalName());
        }
    }
}
