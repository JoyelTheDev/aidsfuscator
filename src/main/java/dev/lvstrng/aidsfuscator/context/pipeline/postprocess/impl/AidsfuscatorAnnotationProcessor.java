package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.PostProcessor;

public class AidsfuscatorAnnotationProcessor implements PostProcessor {
    @Override
    public void run(Context context) {
        for(var clazz : context.classes()) {
            clazz.annotations().stream().filter(e -> e.desc.startsWith("Ldev/lvstrng/aidsfuscator/api/")).forEach(e -> {
                clazz.removeAnnotation(e.desc);
            });

            for(var field : clazz.fields()) {
                field.annotations().stream().filter(e -> e.desc.startsWith("Ldev/lvstrng/aidsfuscator/api/")).forEach(e -> {
                    field.removeAnnotation(e.desc);
                });
            }

            for(var method : clazz.methods()) {
                method.annotations().stream().filter(e -> e.desc.startsWith("Ldev/lvstrng/aidsfuscator/api/")).forEach(e -> {
                    method.removeAnnotation(e.desc);
                });
            }
        }
    }
}
