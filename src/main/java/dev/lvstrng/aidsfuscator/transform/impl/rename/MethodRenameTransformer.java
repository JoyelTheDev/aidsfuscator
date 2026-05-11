package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.Exclusions;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.settings.Setting;
import dev.lvstrng.aidsfuscator.tree.JClass;
import dev.lvstrng.aidsfuscator.tree.JMethod;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

            mapMethods(context, clazz);
        }

        remap(context);
    }

    private void mapMethods(Context context, JClass clazz) {
        // ---- SHUFFLE IF NEEDED ----
        var methods = clazz.methods();
        if(this.shuffle.value()) {
            Collections.shuffle(methods);
            Collections.shuffle(clazz.core().methods);
        }

        for(var method : methods) {
            // ---- EXCLUSIONS
            if(clazz.tree().stream().anyMatch(e -> Exclusions.RENAME_METHOD.excluded(e) && e.hasMethodInTree(context, method)))
                continue;

            if(Exclusions.RENAME_METHOD.excluded(method))
                continue;

            var hierarchy = this.collectHierarchy(method);
            if(this.shouldSkipHierarchy(hierarchy))
                continue;

            if(Mappings.METHOD.containsOld(method.fullName()))
                continue;

            if(this.cantEditMethod(clazz, method))
                continue;

            var impactedClasses = this.collectImpactedClasses(context, hierarchy);
            var newName = this.findExistingName(hierarchy);
            if(newName.isEmpty())
                newName = this.nextMethodName(context, impactedClasses, hierarchy, method.desc());

            for(var member : impactedClasses) {
                var oldId = MemberUtils.fullMethod(member.name(), method.name(), method.desc());
                var newId = MemberUtils.fullMethod(member.name(), newName, method.desc());

                Mappings.METHOD.register(oldId, new Mapping(newId, newName));
            }

            markChange();
        }
    }

    private Set<JMethod> collectHierarchy(JMethod method) {
        var hierarchy = new LinkedHashSet<JMethod>();

        hierarchy.add(method);
        hierarchy.addAll(method.tree());

        return hierarchy;
    }

    private boolean shouldSkipHierarchy(Set<JMethod> hierarchy) {
        for(var member : hierarchy) {
            if(member.isLibrary())
                continue;

            if(Exclusions.RENAME_METHOD.excluded(member))
                return true;

            if(Exclusions.RENAME_METHOD.excluded(member.owner(), member))
                return true;

            if(this.cantEditMethod(member.owner(), member))
                return true;
        }

        return false;
    }

    private Set<JClass> collectImpactedClasses(Context context, Set<JMethod> hierarchy) {
        var impactedClasses = new LinkedHashSet<JClass>();
        for(var member : hierarchy) {
            var owner = member.owner();
            if(owner.isLibrary())
                continue;

            impactedClasses.add(owner);
            for(var related : owner.tree()) {
                if(related.isLibrary())
                    continue;

                if(!related.hasMethodInTree(context, member))
                    continue;

                impactedClasses.add(related);
            }
        }

        return impactedClasses;
    }

    private String findExistingName(Set<JMethod> hierarchy) {
        for(var member : hierarchy) {
            if(member.isLibrary())
                continue;

            var id = member.fullName();
            if(Mappings.METHOD.containsOld(id))
                return Mappings.METHOD.retrieve(id).value();
        }

        return "";
    }

    private String nextMethodName(Context context, Set<JClass> impactedClasses, Set<JMethod> hierarchy, String desc) {
        var counter = 0;
        while(true) {
            var nextName = this.prefix.value() + context.dictionary().newName(counter++);
            if(!this.hasCollision(impactedClasses, hierarchy, nextName, desc))
                return nextName;
        }
    }

    private boolean hasCollision(Set<JClass> impactedClasses, Set<JMethod> hierarchy, String name, String desc) {
        var simpleName = name/*MemberUtils.methodDesc(name, desc)*/;
        for(var clazz : impactedClasses) {
            var id = MemberUtils.fullMethod(clazz.name(), name, desc);
            if(Mappings.METHOD.containsNew(id))
                return true;

            if(this.hasCollisionInClass(clazz, hierarchy, simpleName))
                return true;

            for(var related : clazz.tree()) {
                if(related.isLibrary())
                    continue;

                if(this.hasCollisionInClass(related, hierarchy, simpleName))
                    return true;
            }
        }

        return false;
    }

    private boolean hasCollisionInClass(JClass clazz, Set<JMethod> hierarchy, String simpleName) {
        for(var method : clazz.methods()) {
            if(hierarchy.contains(method))
                continue;

            if(method.name().equals(simpleName))
                return true;
        }

        return false;
    }
}