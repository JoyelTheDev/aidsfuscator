package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JField;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FieldRenameTransformer extends Transformer {
    private final Setting<String> prefix = setting("prefix", "");
    private final Setting<Boolean> shuffle = setting("shuffle", false);

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
        if(shuffle.value()) {
            Collections.shuffle(clazz.fields());
            Collections.shuffle(clazz.core().fields);
        }

        for(var field : clazz.fields()) {
            if(clazz.isLibField(field.name(), field.desc()))
                continue;

            var impactedClasses = impactedClasses(context, clazz, field);
            if(skipHierarchy(field, impactedClasses))
                continue;

            var name = findOrGenerateName(context, clazz, impactedClasses, field);
            for(var member : impactedClasses) {
                var oldId = MemberUtils.fullField(member, field);
                if(Mappings.FIELD.containsOld(oldId))
                    continue;

                var newId = MemberUtils.fullField(member.name(), name, field.desc());
                Mappings.FIELD.register(oldId, new Mapping(newId, name));

                markChange();
            }
        }
    }

    private String findOrGenerateName(Context context, JClass clazz, Set<JClass> impactedClasses, JField field) {
        for(var member : impactedClasses) {
            var id = MemberUtils.fullField(member, field);

            if(Mappings.FIELD.containsOld(id))
                return Mappings.FIELD.retrieve(id).value();
        }

        return context.dictionary().newFieldName(prefix.value(), clazz, field.desc());
    }

    private boolean skipHierarchy(JField field, Set<JClass> impactedClass) {
        for(var member : impactedClass) {
            if(Exclusions.RENAME_FIELD.excluded(member))
                return true;

            if(Exclusions.RENAME_FIELD.excluded(member, field))
                return true;
        }

        return false;
    }

    private Set<JClass> impactedClasses(Context context, JClass clazz, JField field) {
        var classes = new HashSet<>(clazz.children());
        classes.add(clazz);

        for(var parent : clazz.parents()) {
            if(!parent.hasFieldInTree(context, field))
                continue;

            classes.add(parent);
        }

        return classes;
    }
}
