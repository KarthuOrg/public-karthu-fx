package org.karthu.fx.showcase.viewmodel;

/**
 * The heading row of a group of pages inside a section of the showcase's navigation tree. It names its section, so
 * that two groups of the same name in different sections are not equal.
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
