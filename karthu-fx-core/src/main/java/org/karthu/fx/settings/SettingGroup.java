package org.karthu.fx.settings;

import java.util.List;

/**
 * A visual section within a settings category page.
 *
 * <p>Groups provide titled separators inside a category, similar to IntelliJ's
 * "Accessibility" and "UI Options" sections within the "Appearance" page.</p>
 *
 * @param labelKey i18n ResourceBundle key for the group title
 * @param entries  the setting entries displayed in this group
 */
public record SettingGroup(
        String labelKey,
        List<SettingEntry<?>> entries
) {

    /**
     * Creates a setting group.
     *
     * @param labelKey i18n key for the group title
     * @param entries  the entries in this group
     */
    public SettingGroup {
        entries = List.copyOf(entries);
    }
}
