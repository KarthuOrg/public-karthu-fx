package org.karthu.fx.form;

/**
 * The severity of a {@link FieldMessage}, which colours the message and decides the input's {@link FieldState}.
 *
 * <p>A message shows its severity as the pseudo-class {@code :error}, {@code :warning} or {@code :info} on
 * {@code form-field-message}.
 */
public enum MessageSeverity {

    /** An error: the message in the error colour, and the input's state {@link FieldState#ERROR}. */
    ERROR,

    /**
     * A warning: the message in the warning colour, and the input's state {@link FieldState#WARNING} unless an error
     * applies too.
     */
    WARNING,

    /** A note: the message in the secondary text colour; it does not change the input's state. */
    INFO
}
