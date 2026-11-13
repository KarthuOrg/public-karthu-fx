package org.karthu.fx.settings;

import java.util.List;

/**
 * A node in the settings TreeView. Categories can contain setting groups
 * (rendered as the content panel) and/or child categories (rendered as
 * tree children).
 *
 * <p>If a category has no groups but has children, selecting it in the
 * TreeView will auto-select its first child.</p>
 *
 * @param key      unique identifier for this category
 * @param labelKey i18n ResourceBundle key for the display name in the tree
 * @param groups   setting groups shown when this category is selected (may be empty)
 * @param children subcategories (may be empty)
 */
public record SettingCategory(
        String key,
        String labelKey,
        List<SettingGroup> groups,
        List<SettingCategory> children
) {

    /**
     * Creates a setting category.
     */
    public SettingCategory {
        groups = List.copyOf(groups);
        children = List.copyOf(children);
    }
}
