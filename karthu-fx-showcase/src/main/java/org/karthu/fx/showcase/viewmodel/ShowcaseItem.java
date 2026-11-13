package org.karthu.fx.showcase.viewmodel;

import org.kordamp.ikonli.Ikon;

/**
 * Represents a single component showcase page in the sidebar navigation.
 */
public record ShowcaseItem(
        String displayName,
        String fxmlFileName,
        String category,
        Ikon icon
) {
    @Override
    public String toString() {
        return displayName;
    }
}
