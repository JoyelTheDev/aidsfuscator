package dev.lvstrng.aidsfuscator.tree;

import java.util.HashSet;
import java.util.Set;

/**
 * Makes a member hierarchical, giving it parents and children.
 * @param <T> type of hierarchical components
 */
public interface IHierarchical<T> {
    /**
     * @return all parents of this member
     */
    Set<T> parents();

    /**
     * @return all children of this member
     */
    Set<T> children();

    /**
     * @return both children and parents of this member
     */
    default Set<T> tree() {
        var set = new HashSet<>(parents());
        set.addAll(children());

        return set;
    }

    default boolean hasParent(T member) {
        return parents().contains(member);
    }

    default boolean hasChild(T member) {
        return children().contains(member);
    }

    default boolean isNonHierarchical() {
        return false;
    }

    /**
     * Clears the hierarchy for this member
     */
    default void clear() {
        parents().clear();
        children().clear();
    }
}
