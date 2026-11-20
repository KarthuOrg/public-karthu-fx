package org.karthu.fx.showcase.viewmodel;

/**
 * The heading row of a section in the showcase's navigation tree.
 *
 * @param section the section
 */
public record SectionHeading(ShowcaseSection section) implements NavigationEntry {

    @Override
    public String text() {
        return section.displayName();
    }
}
