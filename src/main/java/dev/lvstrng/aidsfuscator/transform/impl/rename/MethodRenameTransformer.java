package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.JClass;

public class MethodRenameTransformer extends Transformer {
    public MethodRenameTransformer() {
        super("Rename Methods", "renameMethods");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            mapMethods(context, clazz);
        }

        remap(context);
    }

    private void mapMethods(Context context, JClass clazz) {
        for(var method : clazz.methods()) {
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
                newName = context.dictionary().newMethodName(clazz, method.desc());

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
