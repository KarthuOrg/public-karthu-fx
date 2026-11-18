package org.karthu.fx.theme;

/**
 * The colour scheme an application chooses for the theme with {@link ThemeManager#setAppearance(Appearance)}: Nebula
 * Light, Nebula Dark, or the scheme the platform reports. Both schemes share every shape, size and spacing; only the
 * colours differ.
 *
 * @since 1.1.0-RC2
 */
public enum Appearance {

    /**
     * Nebula Light, whatever scheme the platform reports.
     */
    LIGHT,

    /**
     * Nebula Dark, whatever scheme the platform reports. The theme's choice until an application chooses another.
     */
    DARK,

    /**
     * The scheme the platform reports, followed while the application runs: Nebula Light while the platform reports
     * a light scheme, Nebula Dark otherwise, also when it reports none.
     */
    SYSTEM
}
