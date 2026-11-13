package org.karthu.fx.form;

import java.util.Objects;

/**
 * A message a {@link FormField} shows under its input: a text, shown exactly as given, and its severity.
 *
 * <p>The application decides which messages apply and words them, and an input may report messages about its own value
 * ({@link FormInput#getInputMessages()}); the row neither validates nor rewrites them (no capitalisation, no
 * punctuation added).
 *
 * @param text     the text, shown as it is; never null
 * @param severity the severity, which colours the message and decides the input's state; never null
 */
public record FieldMessage(String text, MessageSeverity severity) {

    /**
     * Creates a message.
     *
     * @param text     the text, shown as it is
     * @param severity the severity
     * @throws NullPointerException if the text or the severity is null
     */
    public FieldMessage {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(severity, "severity");
    }

    /**
     * Creates an error message.
     *
     * @param text the text, shown as it is
     * @return a message of the severity {@link MessageSeverity#ERROR}
     */
    public static FieldMessage error(String text) {
        return new FieldMessage(text, MessageSeverity.ERROR);
    }

    /**
     * Creates a warning message.
     *
     * @param text the text, shown as it is
     * @return a message of the severity {@link MessageSeverity#WARNING}
     */
    public static FieldMessage warning(String text) {
        return new FieldMessage(text, MessageSeverity.WARNING);
    }

    /**
     * Creates an info message.
     *
     * @param text the text, shown as it is
     * @return a message of the severity {@link MessageSeverity#INFO}
     */
    public static FieldMessage info(String text) {
        return new FieldMessage(text, MessageSeverity.INFO);
    }
}
