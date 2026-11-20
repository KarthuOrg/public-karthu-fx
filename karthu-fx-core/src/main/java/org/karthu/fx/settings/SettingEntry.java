package org.karthu.fx.settings;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.karthu.fx.form.PathFilter;
import org.karthu.fx.form.PathMode;

/**
 * A single setting definition with a mutable value.
 *
 * <p>The generic type {@code T} ensures compile-time safety when reading/writing values.
 * Use the static factory methods to create entries — they enforce the correct
 * {@link SettingType} for each value class.</p>
 *
 * @param <T> the value type (String, Boolean, Integer, LocalDate)
 */
public class SettingEntry<T> {

    private final String key;
    private final String labelKey;
    private final String descriptionKey;
    private final SettingType type;
    private final Class<T> valueClass;
    private final T defaultValue;
    private final List<T> choices;
    private final int min;
    private final int max;
    private final int step;
    private final PathMode pathMode;
    private final List<PathFilter> pathFilters;
    private final ObjectProperty<T> value;

    private SettingEntry(String key, String labelKey, String descriptionKey,
                         SettingType type, Class<T> valueClass, T defaultValue,
                         List<T> choices, int min, int max, int step) {
        this(key, labelKey, descriptionKey, type, valueClass, defaultValue, choices, min, max, step, null, List.of());
    }

    private SettingEntry(String key, String labelKey, String descriptionKey,
                         SettingType type, Class<T> valueClass, T defaultValue,
                         List<T> choices, int min, int max, int step,
                         PathMode pathMode, List<PathFilter> pathFilters) {
        this.key = key;
        this.labelKey = labelKey;
        this.descriptionKey = descriptionKey;
        this.type = type;
        this.valueClass = valueClass;
        this.defaultValue = defaultValue;
        this.choices = choices != null ? Collections.unmodifiableList(choices) : List.of();
        this.min = min;
        this.max = max;
        this.step = step;
        this.pathMode = pathMode;
        this.pathFilters = pathFilters;
        this.value = new SimpleObjectProperty<>(defaultValue);
    }

    // --- Static factory methods (without description) ---

    /**
     * Creates a string setting rendered as a {@code TextField}.
     *
     * @param key          the unique key, which names the setting
     * @param labelKey     the i18n key of the label
     * @param defaultValue the initial value
     * @return a {@link SettingType#STRING} entry
     */
    public static SettingEntry<String> ofString(String key, String labelKey, String defaultValue) {
        return new SettingEntry<>(key, labelKey, null, SettingType.STRING,
                String.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates a boolean setting rendered as a {@code CheckBox}.
     *
     * @param key          the unique key, which names the setting
     * @param labelKey     the i18n key of the label
     * @param defaultValue the initial value
     * @return a {@link SettingType#BOOLEAN} entry
     */
    public static SettingEntry<Boolean> ofBoolean(String key, String labelKey, boolean defaultValue) {
        return new SettingEntry<>(key, labelKey, null, SettingType.BOOLEAN,
                Boolean.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates an integer setting rendered as a {@code Spinner}.
     *
     * @param key          the unique key, which names the setting
     * @param labelKey     the i18n key of the label
     * @param defaultValue the initial value
     * @param min          the smallest value
     * @param max          the largest value
     * @param step         the spinner's step
     * @return a {@link SettingType#INTEGER} entry
     */
    public static SettingEntry<Integer> ofInteger(String key, String labelKey,
                                                  int defaultValue, int min, int max, int step) {
        return new SettingEntry<>(key, labelKey, null, SettingType.INTEGER,
                Integer.class, defaultValue, null, min, max, step);
    }

    /**
     * Creates a single-selection setting rendered as a {@code ComboBox}.
     *
     * @param key          the unique key, which names the setting
     * @param labelKey     the i18n key of the label
     * @param defaultValue the initially selected value
     * @param choices      the values to choose from
     * @return a {@link SettingType#CHOICE} entry
     */
    public static SettingEntry<String> ofChoice(String key, String labelKey,
                                                String defaultValue, List<String> choices) {
        return new SettingEntry<>(key, labelKey, null, SettingType.CHOICE,
                String.class, defaultValue, choices, 0, 0, 0);
    }

    /**
     * Creates a secret setting rendered as a {@code PasswordField}.
     *
     * @param key          the unique key, which names the setting
     * @param labelKey     the i18n key of the label
     * @param defaultValue the initial value
     * @return a {@link SettingType#SECRET} entry
     */
    public static SettingEntry<String> ofSecret(String key, String labelKey, String defaultValue) {
        return new SettingEntry<>(key, labelKey, null, SettingType.SECRET,
                String.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates a date setting rendered as a {@code DatePicker}.
     *
     * @param key          the unique key, which names the setting
     * @param labelKey     the i18n key of the label
     * @param defaultValue the initial value
     * @return a {@link SettingType#DATETIME} entry
     */
    public static SettingEntry<LocalDate> ofDateTime(String key, String labelKey, LocalDate defaultValue) {
        return new SettingEntry<>(key, labelKey, null, SettingType.DATETIME,
                LocalDate.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates a path setting rendered as a {@link org.karthu.fx.form.PathInput}, a text field with a <i>Browse</i>
     * icon that opens the file or folder dialog of the mode. The value is the path's text as the field holds it;
     * the empty text is no path.
     *
     * @param key          the unique key, which names the setting
     * @param labelKey     the i18n key of the label
     * @param defaultValue the initial path's text; null counts as the empty text
     * @param mode         what the field chooses: a file to open, a file to save or a directory
     * @param filters      the file filters of the open and save dialogs, in their order; their descriptions are i18n
     *                     keys like the label's; copied
     * @return a {@link SettingType#PATH} entry
     * @throws NullPointerException if the mode or the filters are null, or a filter is null
     * @since 1.1.0-RC2
     */
    public static SettingEntry<String> ofPath(String key, String labelKey, String defaultValue,
                                              PathMode mode, List<PathFilter> filters) {
        return ofPath(key, labelKey, null, defaultValue, mode, filters);
    }

    // --- Static factory methods (with description) ---

    /**
     * Creates a string setting with a description hint.
     *
     * @param key            the unique key, which names the setting
     * @param labelKey       the i18n key of the label
     * @param descriptionKey the i18n key of the hint shown below the control
     * @param defaultValue   the initial value
     * @return a {@link SettingType#STRING} entry
     */
    public static SettingEntry<String> ofString(String key, String labelKey,
                                                String descriptionKey, String defaultValue) {
        return new SettingEntry<>(key, labelKey, descriptionKey, SettingType.STRING,
                String.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates a boolean setting with a description hint.
     *
     * @param key            the unique key, which names the setting
     * @param labelKey       the i18n key of the label
     * @param descriptionKey the i18n key of the hint shown below the control
     * @param defaultValue   the initial value
     * @return a {@link SettingType#BOOLEAN} entry
     */
    public static SettingEntry<Boolean> ofBoolean(String key, String labelKey,
                                                  String descriptionKey, boolean defaultValue) {
        return new SettingEntry<>(key, labelKey, descriptionKey, SettingType.BOOLEAN,
                Boolean.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates an integer setting with a description hint.
     *
     * @param key            the unique key, which names the setting
     * @param labelKey       the i18n key of the label
     * @param descriptionKey the i18n key of the hint shown below the control
     * @param defaultValue   the initial value
     * @param min            the smallest value
     * @param max            the largest value
     * @param step           the spinner's step
     * @return a {@link SettingType#INTEGER} entry
     */
    public static SettingEntry<Integer> ofInteger(String key, String labelKey,
                                                  String descriptionKey,
                                                  int defaultValue, int min, int max, int step) {
        return new SettingEntry<>(key, labelKey, descriptionKey, SettingType.INTEGER,
                Integer.class, defaultValue, null, min, max, step);
    }

    /**
     * Creates a single-selection setting with a description hint.
     *
     * @param key            the unique key, which names the setting
     * @param labelKey       the i18n key of the label
     * @param descriptionKey the i18n key of the hint shown below the control
     * @param defaultValue   the initially selected value
     * @param choices        the values to choose from
     * @return a {@link SettingType#CHOICE} entry
     */
    public static SettingEntry<String> ofChoice(String key, String labelKey,
                                                String descriptionKey,
                                                String defaultValue, List<String> choices) {
        return new SettingEntry<>(key, labelKey, descriptionKey, SettingType.CHOICE,
                String.class, defaultValue, choices, 0, 0, 0);
    }

    /**
     * Creates a secret setting with a description hint.
     *
     * @param key            the unique key, which names the setting
     * @param labelKey       the i18n key of the label
     * @param descriptionKey the i18n key of the hint shown below the control
     * @param defaultValue   the initial value
     * @return a {@link SettingType#SECRET} entry
     */
    public static SettingEntry<String> ofSecret(String key, String labelKey,
                                                String descriptionKey, String defaultValue) {
        return new SettingEntry<>(key, labelKey, descriptionKey, SettingType.SECRET,
                String.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates a date setting with a description hint.
     *
     * @param key            the unique key, which names the setting
     * @param labelKey       the i18n key of the label
     * @param descriptionKey the i18n key of the hint shown below the control
     * @param defaultValue   the initial value
     * @return a {@link SettingType#DATETIME} entry
     */
    public static SettingEntry<LocalDate> ofDateTime(String key, String labelKey,
                                                     String descriptionKey, LocalDate defaultValue) {
        return new SettingEntry<>(key, labelKey, descriptionKey, SettingType.DATETIME,
                LocalDate.class, defaultValue, null, 0, 0, 0);
    }

    /**
     * Creates a path setting with a description hint, rendered as a {@link org.karthu.fx.form.PathInput}.
     *
     * @param key            the unique key, which names the setting
     * @param labelKey       the i18n key of the label
     * @param descriptionKey the i18n key of the hint shown below the control
     * @param defaultValue   the initial path's text; null counts as the empty text
     * @param mode           what the field chooses: a file to open, a file to save or a directory
     * @param filters        the file filters of the open and save dialogs, in their order; their descriptions are
     *                       i18n keys like the label's; copied
     * @return a {@link SettingType#PATH} entry
     * @throws NullPointerException if the mode or the filters are null, or a filter is null
     * @since 1.1.0-RC2
     */
    public static SettingEntry<String> ofPath(String key, String labelKey, String descriptionKey,
                                              String defaultValue, PathMode mode, List<PathFilter> filters) {
        Objects.requireNonNull(mode, "mode must not be null");
        List<PathFilter> copied = List.copyOf(Objects.requireNonNull(filters, "filters must not be null"));
        String initial = defaultValue != null ? defaultValue : "";
        return new SettingEntry<>(key, labelKey, descriptionKey, SettingType.PATH,
                String.class, initial, null, 0, 0, 0, mode, copied);
    }

    // --- Accessors ---

    /** {@return the unique key, which names the setting} */
    public String getKey() {
        return key;
    }

    /** {@return the i18n key of the label} */
    public String getLabelKey() {
        return labelKey;
    }

    /** {@return the i18n key of the hint shown below the control, or null for none} */
    public String getDescriptionKey() {
        return descriptionKey;
    }

    /** {@return the setting type, which determines the control} */
    public SettingType getType() {
        return type;
    }

    /** {@return the class of the values} */
    public Class<T> getValueClass() {
        return valueClass;
    }

    /** {@return the default value, the value the entry starts with} */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * {@return the values to choose from, unmodifiable; empty unless the type is
     * {@link SettingType#CHOICE}}
     */
    public List<T> getChoices() {
        return choices;
    }

    /** {@return the smallest value of an {@link SettingType#INTEGER} setting; 0 for other types} */
    public int getMin() {
        return min;
    }

    /** {@return the largest value of an {@link SettingType#INTEGER} setting; 0 for other types} */
    public int getMax() {
        return max;
    }

    /** {@return the spinner's step of an {@link SettingType#INTEGER} setting; 0 for other types} */
    public int getStep() {
        return step;
    }

    /**
     * {@return what a {@link SettingType#PATH} setting chooses: a file to open, a file to save or a directory; null
     * for other types}
     *
     * @since 1.1.0-RC2
     */
    public PathMode getPathMode() {
        return pathMode;
    }

    /**
     * {@return the file filters of a {@link SettingType#PATH} setting, unmodifiable, in their order; empty for other
     * types}
     *
     * @since 1.1.0-RC2
     */
    public List<PathFilter> getPathFilters() {
        return pathFilters;
    }

    public T getValue() {
        return value.get();
    }

    public void setValue(T newValue) {
        value.set(newValue);
    }

    /**
     * The setting's current value. It starts as the default value.
     *
     * @return the value property
     */
    public ObjectProperty<T> valueProperty() {
        return value;
    }
}
