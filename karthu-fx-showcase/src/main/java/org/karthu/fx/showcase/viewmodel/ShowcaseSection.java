package org.karthu.fx.showcase.viewmodel;

/**
 * A section of the showcase's navigation, the top level of its tree, in display order.
 */
public enum ShowcaseSection {

    /** JavaFX's standard controls in the theme. */
    JAVAFX("JavaFX"),

    /** karthu-fx's own components. */
    KARTHU_FX("karthu-fx"),

    /** Sample applications built from the components. */
    EXAMPLES("Examples");

    private final String displayName;

    ShowcaseSection(String displayName) {
        this.displayName = displayName;
    }

    /**
     * The section's name as the navigation shows it.
     *
     * @return the name
     */
    public String displayName() {
        return displayName;
    }
}
