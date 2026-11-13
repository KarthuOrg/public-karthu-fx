package org.karthu.fx.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * One row of a {@link KeyValueEditor}: a key and a value, a flag that switches the pair on or off, and a flag
 * that masks the value.
 *
 * <p>The four values are properties, so an application can observe or bind single fields; the editor's row binds
 * its fields to them both ways. An entry belongs to at most one editor and appears in its list at most once.
 * {@link #toSnapshot()} hands the content over as an immutable record.
 */
public class KeyValueEntry {

    private final BooleanProperty enabled = new SimpleBooleanProperty(this, "enabled", true);
    private final StringProperty key = new SimpleStringProperty(this, "key", "");
    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final BooleanProperty masked = new SimpleBooleanProperty(this, "masked", false);

    /**
     * Creates an active, unmasked entry with an empty key and an empty value, as the editor's add button does.
     */
    public KeyValueEntry() {
    }

    /**
     * Creates an active, unmasked entry.
     *
     * @param key   the key; null is taken as empty
     * @param value the value; null is taken as empty
     */
    public KeyValueEntry(String key, String value) {
        this(true, key, value, false);
    }

    /**
     * Creates an entry with all four values.
     *
     * @param enabled whether the pair is switched on
     * @param key     the key; null is taken as empty
     * @param value   the value; null is taken as empty
     * @param masked  whether the value is masked
     */
    public KeyValueEntry(boolean enabled, String key, String value, boolean masked) {
        this.enabled.set(enabled);
        this.key.set(key == null ? "" : key);
        this.value.set(value == null ? "" : value);
        this.masked.set(masked);
    }

    /**
     * Whether the pair is switched on (default true). The editor shows it as the row's CheckBox and dims an
     * inactive row, which stays editable.
     *
     * @return the enabled property
     */
    public BooleanProperty enabledProperty() {
        return enabled;
    }

    /**
     * Returns whether the pair is switched on.
     *
     * @return true if the pair is switched on
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Switches the pair on or off.
     *
     * @param value true to switch the pair on
     */
    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    /**
     * The key (default empty). The editor shows it in the row's key field.
     *
     * @return the key property
     */
    public StringProperty keyProperty() {
        return key;
    }

    /**
     * Returns the key.
     *
     * @return the key; null only if an application set null
     */
    public String getKey() {
        return key.get();
    }

    /**
     * Sets the key.
     *
     * @param value the key
     */
    public void setKey(String value) {
        key.set(value);
    }

    /**
     * The value (default empty). The editor shows it in the row's value field.
     *
     * @return the value property
     */
    public StringProperty valueProperty() {
        return value;
    }

    /**
     * Returns the value.
     *
     * @return the value; null only if an application set null
     */
    public String getValue() {
        return value.get();
    }

    /**
     * Sets the value.
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value.set(value);
    }

    /**
     * Whether the value is masked (default false).
     *
     * @return the masked property
     */
    public BooleanProperty maskedProperty() {
        return masked;
    }

    /**
     * Returns whether the value is masked.
     *
     * @return true if the value is masked
     */
    public boolean isMasked() {
        return masked.get();
    }

    /**
     * Masks or unmasks the value.
     *
     * @param value true to mask the value
     */
    public void setMasked(boolean value) {
        masked.set(value);
    }

    /**
     * Returns whether key and value are both empty (null counts as empty), the flags notwithstanding: such a row
     * is not data, and {@link KeyValueEditor#snapshot()} leaves it out.
     *
     * @return true if key and value are both empty
     */
    public boolean isBlank() {
        return isEmpty(key.get()) && isEmpty(value.get());
    }

    /**
     * Returns the entry's current values as an immutable record.
     *
     * @return the snapshot; null key or value become empty strings
     */
    public Snapshot toSnapshot() {
        return new Snapshot(enabled.get(), key.get(), value.get(), masked.get());
    }

    private static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * The immutable values of one {@link KeyValueEntry} at the time it was taken.
     *
     * @param enabled whether the pair is switched on
     * @param key     the key, never null
     * @param value   the value, never null
     * @param masked  whether the value is masked
     */
    public record Snapshot(boolean enabled, String key, String value, boolean masked) {

        /**
         * Creates a snapshot; null key or value become empty strings.
         *
         * @param enabled whether the pair is switched on
         * @param key     the key; null is taken as empty
         * @param value   the value; null is taken as empty
         * @param masked  whether the value is masked
         */
        public Snapshot {
            key = key == null ? "" : key;
            value = value == null ? "" : value;
        }
    }
}
