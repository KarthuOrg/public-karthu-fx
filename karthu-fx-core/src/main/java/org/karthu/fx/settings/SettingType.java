package org.karthu.fx.settings;

/**
 * Defines the data type of a setting entry, which determines the UI control
 * rendered in the settings dialog.
 */
public enum SettingType {

    /** Plain text input — rendered as {@code TextField}. */
    STRING,

    /** On/off toggle — rendered as {@code CheckBox}. */
    BOOLEAN,

    /** Numeric input with bounds — rendered as {@code Spinner<Integer>}. */
    INTEGER,

    /** Single selection from a fixed list — rendered as {@code ComboBox<String>}. */
    CHOICE,

    /** Masked text input — rendered as {@code PasswordField}. */
    SECRET,

    /** Date input — rendered as {@code DatePicker}. */
    DATETIME
}
