package org.karthu.fx.form;

import java.util.Objects;

/**
 * A variable an input can be bound to, as the binding list offers it: its name and an optional detail, such as the
 * variable's type. karthu-fx knows the name only; the application maps it onto its own variable.
 *
 * @param name   the variable's name, which becomes the binding's bound name when the user chooses it
 * @param detail a text the list shows muted after the name, such as the variable's type; null or empty for none
 */
public record BindingCandidate(String name, String detail) {

    /**
     * Creates a candidate.
     *
     * @param name   the variable's name, not null
     * @param detail the detail text, or null for none
     * @throws NullPointerException if the name is null
     */
    public BindingCandidate {
        Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a candidate without a detail.
     *
     * @param name the variable's name, not null
     */
    public BindingCandidate(String name) {
        this(name, null);
    }
}
