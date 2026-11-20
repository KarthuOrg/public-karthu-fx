package org.karthu.fx.showcase.viewmodel;

import org.kordamp.ikonli.Ikon;

/**
 * A showcase page in the navigation: its section, its group inside the section, or none when it sits directly under
 * the section, its name, its page file and its icon.
 *
 * @param section      the section the page belongs to
 * @param group        the group inside the section, {@code null} for a page directly under its section
 * @param displayName  the page's name in the navigation
 * @param fxmlFileName the page's FXML file below the showcase's page folder
 * @param icon         the page's icon in the navigation
 */
public record ShowcaseItem(
        ShowcaseSection section,
        String group,
        String displayName,
        String fxmlFileName,
        Ikon icon
) implements NavigationEntry {

    @Override
    public String text() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
