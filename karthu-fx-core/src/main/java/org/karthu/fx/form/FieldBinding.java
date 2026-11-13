package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * The binding of a form input to a variable: whether the input can be bound, the name of the variable it is bound to,
 * the variables that fit, and whether the bound variable still exists. Every {@link FormInput} has exactly one.
 *
 * <p><b>Names only.</b> karthu-fx never knows what a variable is. The application fills the
 * {@linkplain #getCandidates() candidates} with the variables that fit the input, sets the
 * {@linkplain #boundNameProperty() bound name} to show an existing binding, and listens to the bound name to apply what
 * the user does: the input sets it when the user binds, switches to another variable or unbinds. The application sets
 * {@linkplain #missingProperty() missing} when the bound variable does not exist any more.
 *
 * <p><b>The value stays.</b> Binding never changes the input's value. While bound, the input keeps its value hidden
 * and shows it again after unbinding, so binding never costs the user the configuration entered before.
 *
 * <p><b>Texts</b>, properties with English defaults that every renderer of a binding uses, so an application sets
 * them once per input: the {@code {}} icon's tooltip <i>Bind to a variable (Ctrl+Space)</i>, the list entry
 * <i>Remove binding</i> (also the tooltip of the chip's remove button), the list entry <i>No variable fits</i> and the
 * missing chip's tooltip <i>This variable does not exist</i>.
 *
 * <p>How an input shows its binding is the input's: a {@link FieldFrame} shows the {@code {}} icon as its last trailing
 * icon and the bound variable as a chip in place of its text. Like every karthu-fx control, the binding is used on the
 * JavaFX Application Thread only.
 */
public final class FieldBinding {

    private final BooleanProperty bindable = new SimpleBooleanProperty(this, "bindable", false);
    private final StringProperty boundName = new SimpleStringProperty(this, "boundName");
    private final ObservableList<BindingCandidate> candidates = FXCollections.observableArrayList();
    private final BooleanProperty missing = new SimpleBooleanProperty(this, "missing", false);
    private final StringProperty bindTooltipText =
            new SimpleStringProperty(this, "bindTooltipText", "Bind to a variable (Ctrl+Space)");
    private final StringProperty removeText = new SimpleStringProperty(this, "removeText", "Remove binding");
    private final StringProperty noCandidatesText =
            new SimpleStringProperty(this, "noCandidatesText", "No variable fits");
    private final StringProperty missingTooltipText =
            new SimpleStringProperty(this, "missingTooltipText", "This variable does not exist");

    /**
     * Creates a binding that is unbound, not bindable and without candidates, with English texts.
     */
    public FieldBinding() {
    }

    // --- State ---------------------------------------------------------------------------------------

    /**
     * Whether the input can be bound (default false). While false and unbound, the input offers no binding: it shows
     * no {@code {}} icon. A binding the application sets shows even while false, but once unbound such an input shows
     * no {@code {}} icon any more: an input the user may bind again after removing a binding must be bindable. A
     * {@linkplain FormInput#readOnlyProperty() read-only} input offers no binding either, and shows a binding without
     * the means to remove it.
     *
     * @return the bindable property
     */
    public BooleanProperty bindableProperty() {
        return bindable;
    }

    /**
     * Returns whether the input can be bound.
     *
     * @return true when the input can be bound
     */
    public boolean isBindable() {
        return bindable.get();
    }

    /**
     * Sets whether the input can be bound.
     *
     * @param value true to offer the binding
     */
    public void setBindable(boolean value) {
        bindable.set(value);
    }

    /**
     * The name of the variable the input is bound to, null while it is unbound (the default). The input sets it when
     * the user binds, switches or unbinds; the application listens to it to apply the change, and sets it to show an
     * existing binding.
     *
     * @return the bound name property
     */
    public StringProperty boundNameProperty() {
        return boundName;
    }

    /**
     * Returns the name of the bound variable.
     *
     * @return the bound name, or null while unbound
     */
    public String getBoundName() {
        return boundName.get();
    }

    /**
     * Binds the input to the named variable, or unbinds it.
     *
     * @param value the variable's name, or null to unbind
     */
    public void setBoundName(String value) {
        boundName.set(value);
    }

    /**
     * Returns whether the input is bound: whether the bound name is not null.
     *
     * @return true while bound
     */
    public boolean isBound() {
        return boundName.get() != null;
    }

    /**
     * The variables that fit the input, in the order the binding list shows them. The application fills it; an empty
     * list makes the binding list say that no variable fits.
     *
     * @return the live list of candidates
     */
    public ObservableList<BindingCandidate> getCandidates() {
        return candidates;
    }

    /**
     * Whether the bound variable does not exist any more (default false). The application sets it; the input shows a
     * missing binding in the error colour with the {@linkplain #missingTooltipTextProperty() missing tooltip}. When
     * the user binds to a candidate or unbinds, the input clears it before it sets the bound name, so the
     * application's listener on the bound name still has the last word.
     *
     * @return the missing property
     */
    public BooleanProperty missingProperty() {
        return missing;
    }

    /**
     * Returns whether the bound variable does not exist any more.
     *
     * @return true when the bound variable is missing
     */
    public boolean isMissing() {
        return missing.get();
    }

    /**
     * Sets whether the bound variable does not exist any more.
     *
     * @param value true when the bound variable is missing
     */
    public void setMissing(boolean value) {
        missing.set(value);
    }

    // --- Texts ---------------------------------------------------------------------------------------

    /**
     * The {@code {}} icon's tooltip and accessible text, its shortcut included (default <i>Bind to a variable
     * (Ctrl+Space)</i>). An application that translates it names the shortcut in its own words.
     *
     * @return the bind tooltip text property
     */
    public StringProperty bindTooltipTextProperty() {
        return bindTooltipText;
    }

    /**
     * Returns the {@code {}} icon's tooltip.
     *
     * @return the {@code {}} icon's tooltip
     */
    public String getBindTooltipText() {
        return bindTooltipText.get();
    }

    /**
     * Sets the {@code {}} icon's tooltip.
     *
     * @param value the {@code {}} icon's tooltip
     */
    public void setBindTooltipText(String value) {
        bindTooltipText.set(value);
    }

    /**
     * The binding list's entry that unbinds, also the tooltip and accessible text of the chip's remove button
     * (default <i>Remove binding</i>).
     *
     * @return the remove text property
     */
    public StringProperty removeTextProperty() {
        return removeText;
    }

    /**
     * Returns the text of the entry that unbinds.
     *
     * @return the remove text
     */
    public String getRemoveText() {
        return removeText.get();
    }

    /**
     * Sets the text of the entry that unbinds.
     *
     * @param value the remove text
     */
    public void setRemoveText(String value) {
        removeText.set(value);
    }

    /**
     * The binding list's disabled entry while there are no candidates (default <i>No variable fits</i>).
     *
     * @return the no-candidates text property
     */
    public StringProperty noCandidatesTextProperty() {
        return noCandidatesText;
    }

    /**
     * Returns the text of the entry shown without candidates.
     *
     * @return the no-candidates text
     */
    public String getNoCandidatesText() {
        return noCandidatesText.get();
    }

    /**
     * Sets the text of the entry shown without candidates.
     *
     * @param value the no-candidates text
     */
    public void setNoCandidatesText(String value) {
        noCandidatesText.set(value);
    }

    /**
     * The tooltip of a binding whose variable is missing (default <i>This variable does not exist</i>).
     *
     * @return the missing tooltip text property
     */
    public StringProperty missingTooltipTextProperty() {
        return missingTooltipText;
    }

    /**
     * Returns the tooltip of a missing binding.
     *
     * @return the missing tooltip text
     */
    public String getMissingTooltipText() {
        return missingTooltipText.get();
    }

    /**
     * Sets the tooltip of a missing binding.
     *
     * @param value the missing tooltip text
     */
    public void setMissingTooltipText(String value) {
        missingTooltipText.set(value);
    }
}
