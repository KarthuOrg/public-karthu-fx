package org.karthu.fx.form;

import javafx.scene.control.TextField;

/**
 * The text a combo input shows in place of its combo box while the user may not choose: read-only in the combo input,
 * read-only or bound in the editable combo input. A text field that is never editable, style class
 * {@code form-combo-text}, with the value's text to select and copy, the combo box's prompt text without one, and one
 * accessible text with the combo box, so that a form row's label names whichever of the two shows.
 *
 * <p>It copies through the shell's guarded clipboard and never cuts or pastes: a cut only copies, as the text is the
 * value's, which neither read-only nor bound changes. While bound it hands nothing of the hidden value out: select-all
 * selects nothing, copy gives nothing.
 *
 * <p>Not exported: the combo inputs' implementation, shared by {@link ComboInput} and {@link EditableComboInput},
 * beside {@link ShellComboBox}.
 */
final class ShellComboText extends TextField {

    /** The input whose guarded clipboard the text uses. */
    private final FieldShell input;

    /**
     * Creates the text of a combo input.
     *
     * @param input the combo input
     * @param combo the combo box the text stands in for: its prompt text and its accessible text are the text's too
     */
    ShellComboText(FieldShell input, ShellComboBox<?> combo) {
        this.input = input;
        getStyleClass().add("form-combo-text");
        setEditable(false);
        promptTextProperty().bind(combo.promptTextProperty());
        // A row's label names whichever of the two is the focus target: it sets the accessible text on the one shown
        // when the row takes the input, and the other one follows (a bidirectional binding counts as not bound there).
        accessibleTextProperty().bindBidirectional(combo.accessibleTextProperty());
    }

    @Override
    public void copy() {
        input.guardedCopy(this);
    }

    @Override
    public void cut() {
        // Copies only: the text is the value's, which read-only and bound never change.
        input.guardedCopy(this);
    }

    @Override
    public void paste() {
        // Never editable: nothing is pasted.
    }

    @Override
    public void selectAll() {
        input.guardedSelectAll(this);
    }
}
