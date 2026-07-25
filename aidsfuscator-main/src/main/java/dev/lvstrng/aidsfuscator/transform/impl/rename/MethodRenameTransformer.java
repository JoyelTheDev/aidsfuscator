package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;

import java.util.*;

public class MethodRenameTransformer extends Transformer {
    private final Setting<String> prefix = setting("prefix", "");
    private final Setting<Boolean> shuffle = setting("shuffle", false);

    public MethodRenameTransformer() {
        super("Rename Methods", "renameMethods");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            mapMethods(context, clazz);
        }

        remap(context);
        Mappings.METHOD.clearTemp();
    }

    private void mapMethods(Context context, JClass clazz) {
        if(shuffle.value()) {
            var seed = random.nextLong();

            Collections.shuffle(clazz.core().methods, new Random(seed));
            Collections.shuffle(clazz.methods(), new Random(seed));
        }

        for(var method : clazz.methods()) {
            var impactedClasses = impactedClasses(context, clazz, method);
            if(skipHierarchy(method, impactedClasses))
                continue;

            var identity = identityScope(context, method);
            var collision = new HashSet<>(identity);
            collision.addAll(clazz.tree()); // extra classes to avoid colliding with, not to propagate to

            var name = findOrGenerateName(context, clazz, identity, collision, method);
            clazz.methods().stream()
                    .filter(e -> e.mappedName().equals(name))
                    .filter(e -> e.desc().equals(method.desc()))
                    .filter(e -> e != method)
                    .findFirst().ifPresent(other -> Logger.error("[%s] %s (%s) -> %s (%s)", clazz.originalName(), method.simpleOriginalName(), name, other.simpleOriginalName(), other.mappedName()));

            // every member of identity actually shares this method (declared or inherited)
            for(var member : identity) {
                member.findMethod(method.name(), method.desc()).ifPresent(mth -> mth.setMappedName(name));

                var oldId = MemberUtils.fullMethod(member, method);
                var newId = MemberUtils.fullMethod(member.name(), name, method.desc());
                Mappings.METHOD.register(oldId, new Mapping(newId, name));

                markChange();
            }
        }
    }

    private String findOrGenerateName(Context context, JClass clazz, Set<JClass> identity, Set<JClass> collisionScope, JMethod method) {
        for(var member : identity) {
            var id = MemberUtils.fullMethod(member, method);
            if(Mappings.METHOD.containsOld(id))
                return Mappings.METHOD.retrieve(id).value();
        }

        var id = MemberUtils.fullMethod(clazz, method);
        if(Mappings.METHOD.containsOld(id))
            return Mappings.METHOD.retrieve(id).value();

        return context.dictionary().newMethodName(prefix.value(), clazz, method.desc(), collisionScope);
    }

    /**
     * Walks the full override closure for this method, not just clazz's own hierarchy.
     * Needed because a class implementing two unrelated interfaces with matching name+desc
     * bridges them, so a name picked from one interface's side can still collide on the other.
     * Used for propagation - every member here genuinely shares this method's identity.
     * @param context obfuscator context
     * @param method method to find the identity scope for
     * @author brownie
     */
    private Set<JClass> identityScope(Context context, JMethod method) {
        var visited = new HashSet<JClass>();
        var queue = new ArrayDeque<JClass>();

        visited.add(method.owner());
        queue.add(method.owner());

        while(!queue.isEmpty()) {
            var current = queue.poll();

            for(var child : current.children()) {
                if(visited.add(child))
                    queue.add(child);
            }

            for(var parent : current.parents()) {
                if(visited.contains(parent) || parent.isLibrary())
                    continue;

                if(parent.hasMethodInTree(context, method)) {
                    visited.add(parent);
                    queue.add(parent);
                }
            }
        }

        return visited;
    }

    /**
     * Exclusion and invalid method check
     * @param method method
     * @param impactedClasses all impacted classes
     * @return false if should continue, true if should skip
     */
    private boolean skipHierarchy(JMethod method, Set<JClass> impactedClasses) {
        if(method.owner().isLibMethod(method))
            return true;

        // ---- CLASS TREE CHECKS ----
        for(var member : impactedClasses) {
            var opt = member.findMethod(method.name(), method.desc());
            if(opt.isPresent())
                method = opt.get();

            if(Exclusions.RENAME_METHOD.excluded(member))
                return true;

            if(Exclusions.RENAME_METHOD.excluded(member, method))
                return true;

            if(cantEditMethod(member, method, false, true))
                return true;
        }

        return false;
    }

    private Set<JClass> impactedClasses(Context context, JClass clazz, JMethod method) {
        var classes = new HashSet<>(clazz.children()); // children will obviously have this method in their tree
        classes.add(clazz);

        for(var parent : clazz.parents()) {
            if(!parent.hasMethodInTree(context, method))
                continue;

            classes.add(parent);
            classes.addAll(parent.children());
        }

        return classes;
    }
}
