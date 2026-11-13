package org.karthu.fx.config;

import org.karthu.fx.services.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Service for managing application configuration persistence using Java Preferences API.
 */
public class ConfigService implements BaseService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private static final String PREF_LOCALE = "locale";
    private static final String PREF_WINDOW_X = "window.x";
    private static final String PREF_WINDOW_Y = "window.y";
    private static final String PREF_WINDOW_WIDTH = "window.width";
    private static final String PREF_WINDOW_HEIGHT = "window.height";
    private static final String PREF_WINDOW_MAXIMIZED = "window.maximized";

    private final Preferences prefs;
    private AppConfig config;

    /**
     * Creates the service on the user preferences node of this package. The configuration is
     * read in {@link #initialize()}.
     */
    public ConfigService() {
        this.prefs = Preferences.userNodeForPackage(ConfigService.class);
    }

    @Override
    public void initialize() {
        log.debug("Initializing ConfigService");
        load();
    }

    @Override
    public void shutdown() {
        log.debug("Shutting down ConfigService");
        save();
    }

    /**
     * Loads the configuration from preferences, or creates default if not found.
     */
    private void load() {
        String localeTag = prefs.get(PREF_LOCALE, Locale.getDefault().toLanguageTag());
        Locale locale = Locale.forLanguageTag(localeTag);

        double x = prefs.getDouble(PREF_WINDOW_X, WindowState.DEFAULT.x());
        double y = prefs.getDouble(PREF_WINDOW_Y, WindowState.DEFAULT.y());
        double width = prefs.getDouble(PREF_WINDOW_WIDTH, WindowState.DEFAULT.width());
        double height = prefs.getDouble(PREF_WINDOW_HEIGHT, WindowState.DEFAULT.height());
        boolean maximized = prefs.getBoolean(PREF_WINDOW_MAXIMIZED, WindowState.DEFAULT.maximized());

        WindowState windowState = new WindowState(x, y, width, height, maximized);
        config = new AppConfig(locale, windowState, Map.of());
        log.info("Configuration loaded successfully");
    }

    /**
     * Saves the current configuration to preferences.
     */
    public void save() {
        prefs.put(PREF_LOCALE, config.locale().toLanguageTag());
        prefs.putDouble(PREF_WINDOW_X, config.windowState().x());
        prefs.putDouble(PREF_WINDOW_Y, config.windowState().y());
        prefs.putDouble(PREF_WINDOW_WIDTH, config.windowState().width());
        prefs.putDouble(PREF_WINDOW_HEIGHT, config.windowState().height());
        prefs.putBoolean(PREF_WINDOW_MAXIMIZED, config.windowState().maximized());
        log.debug("Configuration saved");
    }

    /**
     * Gets the current configuration.
     *
     * @return the current AppConfig
     */
    public AppConfig getConfig() {
        return config;
    }

    /**
     * Sets the window state and saves the configuration.
     *
     * @param windowState the new window state
     */
    public void setWindowState(WindowState windowState) {
        config = config.withWindowState(windowState);
        save();
        log.debug("Window state updated");
    }
}
