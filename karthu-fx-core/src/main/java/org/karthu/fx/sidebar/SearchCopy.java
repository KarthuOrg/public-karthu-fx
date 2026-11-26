package org.karthu.fx.sidebar;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The copy of an application's tree that a navigation tree shows during a search, built anew for each change of the
 * search or of the tree: every hit, every folder above a hit, and for a hit with children all its descendants, so that
 * a folder whose own name matches keeps its whole content. Every folder of the copy is unfolded, and every item of it
 * carries the application's value, so that cells, selection and open handler receive the application's values. The
 * search only ever folds and unfolds the copy, never the application's tree. The copy knows the application's item of
 * each of its items, and back, by identity.
 *
 * @param <T> the type of the values of the application's tree items
 */
final class SearchCopy<T> {

    /** The root of the copy, carrying the application root's value; null without an application root. */
    private final TreeItem<T> root;
    /** The application's item of every item of the copy. */
    private final Map<TreeItem<T>, TreeItem<T>> originals = new IdentityHashMap<>();
    /** The item of the copy of every application's item the copy holds. */
    private final Map<TreeItem<T>, TreeItem<T>> copies = new IdentityHashMap<>();

    /**
     * Copies the application's tree for the hits of a search.
     *
     * @param applicationRoot the root of the application's tree, null for none
     * @param hits            the application's items whose search text matches
     */
    SearchCopy(TreeItem<T> applicationRoot, Collection<TreeItem<T>> hits) {
        if (applicationRoot == null) {
            root = null;
            return;
        }
        Set<TreeItem<T>> hitSet = Collections.newSetFromMap(new IdentityHashMap<>());
        hitSet.addAll(hits);
        root = new TreeItem<>(applicationRoot.getValue());
        root.setExpanded(true);
        for (TreeItem<T> child : applicationRoot.getChildren()) {
            TreeItem<T> copy = copy(child, hitSet, false);
            if (copy != null) {
                root.getChildren().add(copy);
            }
        }
    }

    /**
     * Returns the root of the copy, which the tree view shows instead of the application's root.
     *
     * @return the root, null without an application root
     */
    TreeItem<T> root() {
        return root;
    }

    /**
     * Returns whether the copy holds no entry: the search found none, or there is no application root.
     *
     * @return true when the copy shows no row
     */
    boolean isEmpty() {
        return root == null || root.getChildren().isEmpty();
    }

    /**
     * Returns the application's item of an item of the copy.
     *
     * @param copy an item of the copy
     * @return the application's item, null when the item is none of the copy's
     */
    TreeItem<T> originalOf(TreeItem<T> copy) {
        return originals.get(copy);
    }

    /**
     * Returns the item of the copy of an application's item.
     *
     * @param original an item of the application's tree
     * @return the item of the copy, null when the copy leaves the application's item out
     */
    TreeItem<T> copyOf(TreeItem<T> original) {
        return copies.get(original);
    }

    /**
     * The copy of an application's item, or null when the search leaves it out: a hit, an item below a hit, or a
     * folder with a hit below it is copied, with the copies of its children, unfolded.
     *
     * @param belowHit whether a folder above the item is a hit, which keeps all its entries
     */
    private TreeItem<T> copy(TreeItem<T> original, Set<TreeItem<T>> hitSet, boolean belowHit) {
        boolean hit = hitSet.contains(original);
        List<TreeItem<T>> children = new ArrayList<>();
        for (TreeItem<T> child : original.getChildren()) {
            TreeItem<T> copy = copy(child, hitSet, belowHit || hit);
            if (copy != null) {
                children.add(copy);
            }
        }
        if (!hit && !belowHit && children.isEmpty()) {
            return null;
        }
        TreeItem<T> copy = new TreeItem<>(original.getValue());
        copy.getChildren().setAll(children);
        copy.setExpanded(true);
        originals.put(copy, original);
        copies.put(original, copy);
        return copy;
    }
}
