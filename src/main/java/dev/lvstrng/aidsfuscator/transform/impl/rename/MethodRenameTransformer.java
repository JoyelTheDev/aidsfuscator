package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;

import java.util.Collections;

public class MethodRenameTransformer extends Transformer {
    private final Setting<String> prefix = setting("prefix", "");
    private final Setting<Boolean> shuffle = setting("shuffle", false);

    public MethodRenameTransformer() {
        super("Rename Methods", "renameMethods");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(Exclusions.RENAME_METHOD.excluded(clazz))
                continue;

            if(clazz.tree().stream().anyMatch(Exclusions.RENAME_METHOD::excluded))
                continue;

            mapMethods(context, clazz);
        }

        remap(context);
    }

    private void mapMethods(Context context, JClass clazz) {
        // ---- SHUFFLING ----
        var methods = clazz.methods();
        if(shuffle.value()) {
            Collections.shuffle(methods);
            Collections.shuffle(clazz.core().methods);
        }

        // ---- REMAPPING ENTIRE TREE ----
        for(var method : methods) {
            if(Exclusions.RENAME_METHOD.excluded(method))
                continue;

            if(clazz.tree().stream().anyMatch(e -> Exclusions.RENAME_METHOD.excluded(e, method)))
                continue;

            var selfOld = method.fullName();
            if(Mappings.METHOD.containsOld(selfOld))
                continue;

            if(cantEditMethod(clazz, method))
                continue;

            var newName = "";
            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var id = String.format("%s.%s", member.name(), method.simpleName());
                if(Mappings.METHOD.containsOld(id)) {
                    newName = Mappings.METHOD.retrieve(id).value();
                    break;
                }
            }

            if(newName.isEmpty())
                newName = context.dictionary().newMethodName(prefix.value(), clazz, method.desc());

            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var oldId = String.format("%s.%s", member.name(), method.simpleName());
                var newId = String.format("%s.%s%s", member.name(), newName, method.desc());
                Mappings.METHOD.register(oldId, new Mapping(newId, newName));
            }

            var selfNew = String.format("%s.%s%s", clazz.name(), newName, method.desc());
            Mappings.METHOD.register(selfOld, new Mapping(selfNew, newName));
            markChange();
        }
    }
}
