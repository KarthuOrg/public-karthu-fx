package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * The contract every input of the form collection implements, so that an application wires each kind of input the
 * same way: its node to place it in a layout, its value, its binding to a variable, read-only, its state and the
 * messages it reports itself.
 *
 * <p>An interface, not a base class, so that existing controls can implement it without changing their superclass.
 * Disabled is the node's own {@code disable}. Like every karthu-fx control, an input is used on the JavaFX Application
 * Thread only.
 *
 * <p>Every input can be bound to a variable through its {@linkplain #getBinding() binding}, known by name only.
 *
 * @param <T> the type of the input's value
 */
public interface FormInput<T> {

    /**
     * Returns the node to place in a layout; for a region that implements this interface, the region itself.
     *
     * @return the input's node, never null
     */
    Node getNode();

    /**
     * Returns the node that takes the focus in the input and that a label names as the input's accessible label
     * ({@link javafx.scene.control.Label#setLabelFor(Node) labelFor}), as a {@link FormField} does. By default the
     * input's node; an input whose node holds the focusable part returns that part, as a {@link FieldFrame} returns
     * its text field.
     *
     * @return the node that takes the focus, never null
     */
    default Node getFocusTarget() {
        return getNode();
    }

    /**
     * The input's value. An implementation may return a more specific property type, such as a
     * {@link javafx.beans.property.StringProperty}.
     *
     * @return the value property
     */
    Property<T> valueProperty();

    /**
     * The input's binding to a variable: one per input, the same object for the input's lifetime. Binding never
     * changes the value: while bound, the input keeps its value hidden and shows it again after unbinding.
     *
     * @return the input's binding, never null
     */
    FieldBinding getBinding();

    /**
     * Whether the input is read-only (default false): the user can select and copy its value but not change it, by
     * neither typing, choosing, dropping nor binding. The application can still set the value and the binding. A
     * read-only input shows the pseudo-class {@code :readonly}. Unlike a disabled input it is not greyed out and keeps
     * its place in the tab order.
     *
     * @return the read-only property
     */
    BooleanProperty readOnlyProperty();

    /**
     * The input's state (default {@link FieldState#NORMAL}), shown as the pseudo-classes {@code :warning} and
     * {@code :error}; null counts as normal. Set it directly when the input stands alone. Inside a {@link FormField}
     * the row sets it from its messages, the input's own ({@link #getInputMessages()}) and the application's.
     *
     * @return the state property
     */
    ObjectProperty<FieldState> stateProperty();

    /**
     * The messages the input reports itself, about its own value, such as what a text it checks means or why it is
     * wrong. A {@link FormField} shows them under the input above the application's
     * {@linkplain FormField#getMessages() messages}, in the same look, and sets the input's state from all of them.
     * The application does not change this list; it reads it, for example to show a message elsewhere.
     *
     * <p>An input returns the same list every time, so that a row can follow it. By default the input reports
     * nothing: an empty list that cannot be changed and never changes.
     *
     * @return the input's own messages, never null
     */
    default ObservableList<FieldMessage> getInputMessages() {
        return FXCollections.emptyObservableList();
    }
}
