package org.karthu.fx.showcase.viewmodel;

/**
 * What a row of the showcase's navigation tree stands for: a page, or a section or group heading above pages. Only a
 * page opens something; a heading never does.
 */
public sealed interface NavigationEntry permits ShowcaseItem, SectionHeading, GroupHeading {

    /**
     * The text of the row.
     *
     * @return the page's, the section's or the group's name
     */
    String text();
}
