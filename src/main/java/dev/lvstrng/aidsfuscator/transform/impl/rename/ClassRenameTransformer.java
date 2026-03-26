package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class ClassRenameTransformer extends Transformer {
    public ClassRenameTransformer() {
        super("Rename Classes", "renameClasses");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            var newClassName = context.dictionary().newClassName();

            Mappings.CLASS.register(clazz.name(), new Mapping(newClassName, newClassName));
            clazz.setSourceFile(newClassName + ".java");
            markChange();
        }

        remap(context);
    }
}
