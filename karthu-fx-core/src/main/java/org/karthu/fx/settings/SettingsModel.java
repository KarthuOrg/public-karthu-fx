package org.karthu.fx.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Root container for the settings data model. Holds all categories and their
 * entries, provides snapshot/restore for cancel support, and resolves i18n keys.
 *
 * <p>The {@link ResourceBundle} is provided by the calling app module. If
 * {@code null}, raw keys are returned as display text.</p>
 */
public class SettingsModel {

    private final List<SettingCategory> categories;
    private final ResourceBundle resourceBundle;

    /**
     * Creates a settings model.
     *
     * @param categories     the top-level setting categories
     * @param resourceBundle the i18n bundle for resolving label keys (may be null)
     */
    public SettingsModel(List<SettingCategory> categories, ResourceBundle resourceBundle) {
        this.categories = List.copyOf(categories);
        this.resourceBundle = resourceBundle;
    }

    /**
     * {@return the top-level categories}
     */
    public List<SettingCategory> getCategories() {
        return categories;
    }

    /**
     * {@return the resource bundle, or {@code null} if none was provided}
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Takes a snapshot of all current entry values. Used to support
     * cancel (restore) and dirty detection.
     *
     * @return a map of entry key to current value
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> snap = new LinkedHashMap<>();
        for (SettingEntry<?> entry : allEntries()) {
            snap.put(entry.getKey(), entry.getValue());
        }
        return snap;
    }

    /**
     * Restores all entry values from a previously taken snapshot.
     *
     * @param snap the snapshot to restore
     */
    @SuppressWarnings("unchecked")
    public void restore(Map<String, Object> snap) {
        for (SettingEntry<?> entry : allEntries()) {
            Object savedValue = snap.get(entry.getKey());
            if (savedValue != null) {
                ((SettingEntry<Object>) entry).setValue(savedValue);
            }
        }
    }

    /**
     * Checks whether any entry value differs from the given snapshot.
     *
     * @param snap the snapshot to compare against
     * @return {@code true} if at least one entry has changed
     */
    public boolean isDirty(Map<String, Object> snap) {
        for (SettingEntry<?> entry : allEntries()) {
            Object current = entry.getValue();
            Object saved = snap.get(entry.getKey());
            if (!Objects.equals(current, saved)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a setting entry by its unique key.
     *
     * @param key the entry key
     * @return the entry, or empty if not found
     */
    public Optional<SettingEntry<?>> findEntry(String key) {
        for (SettingEntry<?> entry : allEntries()) {
            if (entry.getKey().equals(key)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * {@return a flat list of all setting entries across all categories and groups}
     */
    public List<SettingEntry<?>> allEntries() {
        List<SettingEntry<?>> result = new ArrayList<>();
        for (SettingCategory category : categories) {
            collectEntries(category, result);
        }
        return result;
    }

    /**
     * Resolves an i18n key through the resource bundle. Falls back to the raw
     * key string if the bundle is null or the key is missing.
     *
     * @param key the ResourceBundle key
     * @return the resolved string, or the raw key as fallback
     */
    public String resolve(String key) {
        if (key == null) {
            return "";
        }
        if (resourceBundle == null) {
            return key;
        }
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    private void collectEntries(SettingCategory category, List<SettingEntry<?>> result) {
        for (SettingGroup group : category.groups()) {
            result.addAll(group.entries());
        }
        for (SettingCategory child : category.children()) {
            collectEntries(child, result);
        }
    }
}
