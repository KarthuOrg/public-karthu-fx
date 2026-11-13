package org.karthu.fx.config;

import java.util.Locale;
import java.util.Map;

/**
 * Application configuration record.
 *
 * @param locale          the application's locale
 * @param windowState     the main window's position and size
 * @param userPreferences further user preferences, by key
 */
public record AppConfig(
        Locale locale,
        WindowState windowState,
        Map<String, Object> userPreferences
) {

    /**
     * Creates a default configuration.
     *
     * @return default AppConfig with default locale and window state
     */
    public static AppConfig defaults() {
        return new AppConfig(
                Locale.getDefault(),
                WindowState.DEFAULT,
                Map.of()
        );
    }

    /**
     * Creates a copy of this config with a different window state.
     *
     * @param newWindowState the new window state
     * @return a new AppConfig with the specified window state
     */
    public AppConfig withWindowState(WindowState newWindowState) {
        return new AppConfig(locale, newWindowState, userPreferences);
    }

    /**
     * Creates a copy of this config with a different locale.
     *
     * @param newLocale the new locale
     * @return a new AppConfig with the specified locale
     */
    public AppConfig withLocale(Locale newLocale) {
        return new AppConfig(newLocale, windowState, userPreferences);
    }
}
