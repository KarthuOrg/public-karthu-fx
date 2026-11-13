package org.karthu.fx.form;

/**
 * The state of a form input, which colours its border: normal, a warning or an error.
 *
 * <p>An input shows its state as the pseudo-classes {@code :warning} and {@code :error} (none while normal). Inside a
 * {@link FormField} the row sets it from its messages; an input that stands alone gets it set directly by the
 * application.
 *
 * @see FormInput#stateProperty()
 */
public enum FieldState {

    /** Nothing to point out: the input's usual border. */
    NORMAL,

    /** A warning applies: the input shows {@code :warning}, which the theme draws with the warning border. */
    WARNING,

    /** An error applies: the input shows {@code :error}, which the theme draws with the error border. */
    ERROR
}
