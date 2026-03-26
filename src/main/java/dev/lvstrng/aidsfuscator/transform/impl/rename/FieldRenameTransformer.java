package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;

public class FieldRenameTransformer extends Transformer {
    public FieldRenameTransformer() {
        super("Rename Fields", "renameFields");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            mapFields(context, clazz);
        }

        remap(context);
    }

    private void mapFields(Context context, JClass clazz) {
        for(var field : clazz.fields()) {
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
                newName = context.dictionary().newFieldName(clazz, field.desc());

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
