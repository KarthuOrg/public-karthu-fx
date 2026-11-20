package org.karthu.fx.showcase.viewmodel;

/**
 * The heading row of a group of pages inside a section of the showcase's navigation tree.
 *
 * @param section the section the group belongs to
 * @param name    the group's name
 */
public record GroupHeading(ShowcaseSection section, String name) implements NavigationEntry {

    @Override
    public String text() {
        return name;
    }
}
