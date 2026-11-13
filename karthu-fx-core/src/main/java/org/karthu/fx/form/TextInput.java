package org.karthu.fx.form;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

/**
 * A single line of text, such as a name or a title, in the form collection's shell: a {@link FieldFrame} around one
 * borderless text field, so it looks and behaves like every other input of the collection, with the {@code {}} icon,
 * the chip and the error look.
 *
 * <p><b>Value.</b> {@link #valueProperty()} is the text exactly as it stands in the field: what the user types stays as
 * typed (neither trimmed nor refused), a paste is inserted unchanged, and the empty text is no value. Checking the text
 * is the application's, which reports a problem through the input's {@linkplain #stateProperty() state}, usually with a
 * message of the {@link FormField} around it.
 *
 * <p><b>Prompt text.</b> {@link #promptTextProperty()} is the text the empty field shows, empty by default: a text
 * input asks for anything, so it has no English default of its own; the application names what it asks for (null
 * counts as empty).
 *
 * <p><b>States</b>, all the shell's (see {@link FieldShell}): the state's border ({@code :warning}, {@code :error}),
 * read-only ({@code :readonly}: the text can be selected and copied but not changed, the {@code {}} icon hides),
 * disabled (dimmed once, as a disabled button), and the binding: while bound the chip with the variable's name lies over
 * the hidden text, which is kept and shows again after unbinding, a missing variable draws the chip in the error colour.
 * A text input has no state of its own.
 *
 * <p><b>Keyboard</b>, in the text field, the input's {@linkplain #getFocusTarget() focus target}: the text field's own
 * keys, Ctrl+Space opens the binding list while the {@code {}} icon shows, Backspace or Delete unbind while bound. A text
 * input has no choice to open, so Alt+Down passes it unconsumed. The {@code {}} icon and the chip's <i>&times;</i> are
 * not focus traversable: Tab passes the input in one step.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The input carries {@code form-text-input} beside the shell's {@code form-frame}, and its text
 * field the frame's {@code form-frame-text} (see {@link FieldShell} and {@link FieldFrame} for their classes and
 * pseudo-classes). The input's own class is never {@code text-input}, which JavaFX gives every text control, its inner
 * text field included. A text input adds no pseudo-class.
 *
 * <p>Like every karthu-fx control, the input is used on the JavaFX Application Thread only.
 */
public class TextInput extends FieldFrame implements FormInput<String> {

    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "");

    /**
     * Creates an empty input without a prompt text.
     */
    public TextInput() {
        getStyleClass().add("form-text-input");
        value.bindBidirectional(getTextField().textProperty());
        getTextField().promptTextProperty().bind(Bindings.createStringBinding(this::shownPromptText, promptText));
    }

    // --- FormInput -----------------------------------------------------------------------------------

    /**
     * Returns the input itself, to place it in a layout.
     *
     * @return this input
     */
    @Override
    public Node getNode() {
        return this;
    }

    /**
     * The input's value: the text exactly as it stands in the field, the empty text when there is none. Setting it
     * sets the text, also while the input is bound or read-only.
     *
     * @return the value property
     */
    @Override
    public StringProperty valueProperty() {
        return value;
    }

    /**
     * Returns the input's text.
     *
     * @return the text in the field
     */
    public String getValue() {
        return value.get();
    }

    /**
     * Sets the input's text, as it is.
     *
     * @param text the new text
     */
    public void setValue(String text) {
        value.set(text);
    }

    // --- Texts ---------------------------------------------------------------------------------------

    /**
     * The text the empty field shows (default empty: none), such as <i>Customer name</i>. Null counts as empty.
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the prompt text.
     *
     * @return the prompt text, empty or null when there is none
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text.
     *
     * @param text the prompt text; null or empty for none
     */
    public void setPromptText(String text) {
        promptText.set(text);
    }

    /** The prompt text the text field shows: the property's, null as empty. */
    private String shownPromptText() {
        String text = promptText.get();
        return text == null ? "" : text;
    }
}
