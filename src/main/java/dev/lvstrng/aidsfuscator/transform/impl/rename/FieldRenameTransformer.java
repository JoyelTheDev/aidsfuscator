package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;

import java.util.Collections;

public class FieldRenameTransformer extends Transformer {
    private final Setting<String> prefix = setting("prefix", "");
    private final Setting<Boolean> shuffle = setting("shuffle", false);

    public FieldRenameTransformer() {
        super("Rename Fields", "renameFields");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(Exclusions.RENAME_FIELD.excluded(clazz))
                continue;

            if(clazz.tree().stream().anyMatch(Exclusions.RENAME_CLASS::excluded))
                continue;

            if(clazz.isRecord())
                clazz.core().recordComponents.clear();

            mapFields(context, clazz);
        }

        remap(context);
    }

    private void mapFields(Context context, JClass clazz) {
        // ---- SHUFFLING ----
        var fields = clazz.fields();
        if(shuffle.value()) {
            Collections.shuffle(fields);
            Collections.shuffle(clazz.core().fields);
        }

        // ---- REMAPPING ENTIRE TREE ----
        for(var field : fields) {
            if(Exclusions.RENAME_FIELD.excluded(field))
                continue;

            if(clazz.tree().stream().anyMatch(e -> Exclusions.RENAME_FIELD.excluded(e, field)))
                continue;

            var selfOld = field.fullName();
            if(Mappings.FIELD.containsOld(selfOld))
                continue;

            var newName = "";
            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var id = String.format("%s.%s", member.name(), field.simpleName());
                if(Mappings.FIELD.containsOld(id)) {
                    newName = Mappings.FIELD.retrieve(id).value();
                    break;
                }
            }

            if(newName.isEmpty())
                newName = context.dictionary().newFieldName(prefix.value(), clazz, field.desc());

            for(var member : clazz.tree()) {
                if(member.isLibrary())
                    continue;

                var oldId = String.format("%s.%s", member.name(), field.simpleName());
                var newId = String.format("%s.%s %s", member.name(), newName, field.desc());
                Mappings.FIELD.register(oldId, new Mapping(newId, newName));
            }

            var selfNew = String.format("%s.%s %s", clazz.name(), newName, field.desc());
            Mappings.FIELD.register(selfOld, new Mapping(selfNew, newName));
            markChange();
        }
    }
}
