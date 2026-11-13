package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of a form: the label above the input, an asterisk after the label while the field is required, the
 * description as a tooltip on the label, and the messages under the input. It takes any {@link FormInput}, so every
 * field of a form looks the same.
 *
 * <p><b>Display only.</b> The row does not validate. The application decides whether the field is
 * {@linkplain #requiredProperty() required} and which {@linkplain #getMessages() messages} apply, and words them: the
 * row shows each message exactly as given (no capitalisation, nothing added) and puts no marker beside the label. The
 * label always stands above the input.
 *
 * <p><b>Label line.</b> The {@linkplain #labelProperty() label}, then, while the field is required, an asterisk.
 * While the label is empty and the field is not required, there is no label line and the input stands at the top.
 * The {@linkplain #descriptionProperty() description} is the label's tooltip, and there is none while the description
 * is empty. The label names the input: it is the {@linkplain Node#accessibleTextProperty() accessible text} of the
 * input's {@linkplain FormInput#getFocusTarget() focus target}, for an input built on a {@link FieldFrame} its text
 * field, unless the application gave that node an accessible text of its own.
 *
 * <p><b>Messages.</b> Under the input, first the messages the input reports itself
 * ({@link FormInput#getInputMessages()}, such as what a text it checks means), then the application's
 * ({@link #getMessages()}), each list in its order: one line each, wrapped at the row's width, coloured by severity,
 * never cut off or shortened, the input's own in the same look as the application's. Each message's tooltip holds its
 * whole text. The messages never make the row wider: it is as wide as its label line and its input. While both lists
 * are empty, nothing is shown under the input.
 *
 * <p><b>Selecting and copying.</b> The label and every message are text the user can select with the mouse and copy
 * (the shortcut key plus C, or the context menu's <i>Copy</i>), but not change: an error message can be copied into a
 * search or a report. They are not in the focus traversal, so Tab passes from input to input.
 *
 * <p><b>State.</b> The row sets its input's {@linkplain FormInput#stateProperty() state} from all messages, the
 * input's own and the application's together: {@link FieldState#ERROR} if any message is an error, else
 * {@link FieldState#WARNING} if any is a warning, else {@link FieldState#NORMAL}; info does not colour the input. It
 * sets the state whenever a message or the input changes, so a state set directly on an input inside a row lasts until
 * the next change. Replacing the input moves the row to the new input's own messages and the state to the new input,
 * and sets the old one back to normal.
 *
 * <p><b>Disabled.</b> While its input's node is {@linkplain Node#disabledProperty() disabled}, by its own
 * {@code disable} or through a parent, the row shows the pseudo-class {@code :input-disabled}, and the theme dims the
 * label, the asterisk and every message as it dims the input, so the whole row reads as taking no input now. The row
 * does not disable its texts: while only the input is disabled they stay selectable and copyable. A row disabled
 * through a parent of its own has its texts disabled with it, as JavaFX disables every node below.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * <ul>
 *   <li>{@code form-field}: the row, with the pseudo-class {@code :input-disabled} while its input is disabled</li>
 *   <li>{@code form-field-label}: the label, a selectable, read-only text (a {@link TextArea} that looks like a label,
 *       also with the class {@code form-selectable-text})</li>
 *   <li>{@code form-field-required}: the asterisk, a {@link Label}, shown while the field is required</li>
 *   <li>{@code form-field-messages}: the box of the messages, shown while there is one</li>
 *   <li>{@code form-field-message}: each message, a selectable, read-only, wrapping text (a {@link TextArea}, also
 *       with the class {@code form-selectable-text}) with its whole text as tooltip and the pseudo-class of its
 *       severity: {@code :error}, {@code :warning} or {@code :info}</li>
 * </ul>
 *
 * <p>Like every karthu-fx control, the row is used on the JavaFX Application Thread only.
 */
public class FormField extends VBox {

    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
    private static final PseudoClass WARNING_PSEUDO_CLASS = PseudoClass.getPseudoClass("warning");
    private static final PseudoClass INFO_PSEUDO_CLASS = PseudoClass.getPseudoClass("info");
    private static final PseudoClass INPUT_DISABLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("input-disabled");

    private final StringProperty label = new SimpleStringProperty(this, "label", "");
    private final BooleanProperty required = new SimpleBooleanProperty(this, "required", false);
    private final StringProperty description = new SimpleStringProperty(this, "description", "");
    private final ObjectProperty<FormInput<?>> input = new SimpleObjectProperty<>(this, "input");
    private final ObservableList<FieldMessage> messages = FXCollections.observableArrayList();

    /** The widest a message's tooltip grows before it wraps. */
    private static final double MESSAGE_TOOLTIP_WIDTH = 480;

    private final SelectableText labelNode = new SelectableText();
    private final Label asterisk = new Label("*");
    private final HBox labelRow = new HBox(labelNode, asterisk);
    private final Tooltip descriptionTooltip = new Tooltip();
    private final VBox messagesBox = new VBox();

    /** The node the label names, and the accessible text the row gave it (both null when it gave none). */
    private Node labelledNode;
    private String labelledText;

    /**
     * The input's own messages as the row follows them: the list it listens to, kept so that the row stops listening
     * to exactly that list when the input leaves (empty while there is no input).
     */
    private ObservableList<FieldMessage> inputMessages = FXCollections.emptyObservableList();
    private final ListChangeListener<FieldMessage> inputMessagesListener = change -> {
        updateMessages();
        updateInputState();
    };

    /** The input's node whose disabled state the row follows (null while there is no input). */
    private Node disabledSource;
    private final ChangeListener<Boolean> disabledListener =
            (observable, wasDisabled, disabled) -> updateInputDisabled();

    /**
     * Creates a row without a label and without an input, not required, without a description and messages.
     */
    public FormField() {
        getStyleClass().add("form-field");

        labelNode.getStyleClass().add("form-field-label");
        labelNode.textProperty().bind(label);
        label.addListener((observable, oldLabel, newLabel) -> updateLabelTarget());
        asterisk.getStyleClass().add("form-field-required");
        asterisk.visibleProperty().bind(required);
        asterisk.managedProperty().bind(required);
        labelRow.setAlignment(Pos.BASELINE_LEFT);
        // No label line at all for a row without a label that is not required, rather than an empty line.
        labelRow.visibleProperty().bind(label.isNotEmpty().or(required));
        labelRow.managedProperty().bind(labelRow.visibleProperty());
        descriptionTooltip.textProperty().bind(description);
        description.addListener((observable, oldText, newText) -> updateTooltip());
        updateTooltip();

        messagesBox.getStyleClass().add("form-field-messages");
        messages.addListener((ListChangeListener<FieldMessage>) change -> {
            updateMessages();
            updateInputState();
        });
        updateMessages();

        input.addListener((observable, oldInput, newInput) -> {
            // The messages are the row's: an input that leaves the row leaves their state behind.
            if (oldInput != null) {
                oldInput.stateProperty().set(FieldState.NORMAL);
            }
            followInputMessages(newInput);
            followInputDisabled(newInput);
            updateChildren();
            updateLabelTarget();
            updateMessages();
            updateInputState();
            updateInputDisabled();
        });
        updateChildren();
    }

    /**
     * Creates a row with a label and an input, not required, without a description and messages.
     *
     * @param label the label above the input
     * @param input the input, or null
     */
    public FormField(String label, FormInput<?> input) {
        this();
        setLabel(label);
        setInput(input);
    }

    // --- Properties ----------------------------------------------------------------------------------

    /**
     * The label above the input (default empty). While it is empty and the field is not required, the row shows no
     * label line.
     *
     * @return the label property
     */
    public StringProperty labelProperty() {
        return label;
    }

    /**
     * Returns the label.
     *
     * @return the label
     */
    public String getLabel() {
        return label.get();
    }

    /**
     * Sets the label.
     *
     * @param value the label
     */
    public void setLabel(String value) {
        label.set(value);
    }

    /**
     * Whether the field is required (default false), shown as an asterisk after the label. The row only shows it;
     * checking that a required field has a value is the application's.
     *
     * @return the required property
     */
    public BooleanProperty requiredProperty() {
        return required;
    }

    /**
     * Returns whether the field is required.
     *
     * @return true when required
     */
    public boolean isRequired() {
        return required.get();
    }

    /**
     * Sets whether the field is required.
     *
     * @param value true when required
     */
    public void setRequired(boolean value) {
        required.set(value);
    }

    /**
     * The description, shown as the label's tooltip (default empty: no tooltip).
     *
     * @return the description property
     */
    public StringProperty descriptionProperty() {
        return description;
    }

    /**
     * Returns the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description.get();
    }

    /**
     * Sets the description.
     *
     * @param value the description; empty or null for no tooltip
     */
    public void setDescription(String value) {
        description.set(value);
    }

    /**
     * The row's input (default null: none), any {@link FormInput}, shown between the label line and the messages.
     * The row shows the input's own messages above the application's, sets the input's state from all of them and shows
     * {@code :input-disabled} while the input is disabled; replacing the input moves all of this to the new one and
     * sets the old input's state back to normal.
     *
     * @return the input property
     */
    public ObjectProperty<FormInput<?>> inputProperty() {
        return input;
    }

    /**
     * Returns the input.
     *
     * @return the input, or null
     */
    public FormInput<?> getInput() {
        return input.get();
    }

    /**
     * Sets the input.
     *
     * @param value the input, or null
     */
    public void setInput(FormInput<?> value) {
        input.set(value);
    }

    /**
     * The application's messages under the input, in display order, after the messages the input reports itself
     * ({@link FormInput#getInputMessages()}): one wrapped line each, coloured by severity, shown exactly as given,
     * selectable and copyable, with the whole text as tooltip. While this list and the input's own are empty, nothing
     * is shown under the input. Every change sets the input's state anew, from both lists. Null elements are ignored,
     * in both lists.
     *
     * @return the live list of messages
     */
    public ObservableList<FieldMessage> getMessages() {
        return messages;
    }

    // --- Children ------------------------------------------------------------------------------------

    /** Puts the children in their order: the label line, the input, the messages. */
    private void updateChildren() {
        List<Node> children = new ArrayList<>();
        children.add(labelRow);
        FormInput<?> current = input.get();
        if (current != null) {
            children.add(current.getNode());
        }
        children.add(messagesBox);
        getChildren().setAll(children);
    }

    /**
     * The label names the input's focus target as its accessible text. The row only ever replaces or removes an
     * accessible text it set itself, so an application's own stays.
     */
    private void updateLabelTarget() {
        if (labelledNode != null && labelledText != null && labelledText.equals(labelledNode.getAccessibleText())
                && !labelledNode.accessibleTextProperty().isBound()) {
            labelledNode.setAccessibleText(null);
        }
        labelledNode = null;
        labelledText = null;
        FormInput<?> current = input.get();
        Node target = current == null ? null : current.getFocusTarget();
        if (target == null || target.accessibleTextProperty().isBound()) {
            return;
        }
        String existing = target.getAccessibleText();
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        String text = label.get();
        if (text != null && !text.isEmpty()) {
            target.setAccessibleText(text);
            labelledNode = target;
            labelledText = text;
        }
    }

    // --- Layout --------------------------------------------------------------------------------------

    /**
     * The row is as wide as its label line and its input: the messages wrap at that width and never widen it, so a
     * long message does not stretch the form.
     */
    @Override
    protected double computePrefWidth(double height) {
        double width = 0;
        for (Node child : getManagedChildren()) {
            if (child != messagesBox) {
                width = Math.max(width, child.prefWidth(-1));
            }
        }
        return snappedLeftInset() + snapSizeX(width) + snappedRightInset();
    }

    // --- Messages and state --------------------------------------------------------------------------

    /** Listens to the new input's own messages instead of the old input's. */
    private void followInputMessages(FormInput<?> newInput) {
        inputMessages.removeListener(inputMessagesListener);
        inputMessages = newInput == null ? FXCollections.emptyObservableList() : newInput.getInputMessages();
        inputMessages.addListener(inputMessagesListener);
    }

    /**
     * Listens to the new input's node for its disabled state instead of the old one's. The node's {@code disabled}
     * (not {@code disable}) also turns true through a parent, the row or a form around it.
     */
    private void followInputDisabled(FormInput<?> newInput) {
        if (disabledSource != null) {
            disabledSource.disabledProperty().removeListener(disabledListener);
        }
        disabledSource = newInput == null ? null : newInput.getNode();
        if (disabledSource != null) {
            disabledSource.disabledProperty().addListener(disabledListener);
        }
    }

    /** Shows {@code :input-disabled} while the input's node is disabled, so the theme dims the row's texts. */
    private void updateInputDisabled() {
        pseudoClassStateChanged(INPUT_DISABLED_PSEUDO_CLASS, disabledSource != null && disabledSource.isDisabled());
    }

    /** All messages in display order: the input's own first, then the application's. */
    private List<FieldMessage> allMessages() {
        List<FieldMessage> all = new ArrayList<>(inputMessages);
        all.addAll(messages);
        return all;
    }

    /** Shows one selectable text per message, in display order; hides the box while there is none. */
    private void updateMessages() {
        List<Node> texts = new ArrayList<>();
        for (FieldMessage message : allMessages()) {
            if (message != null) {
                texts.add(messageText(message));
            }
        }
        messagesBox.getChildren().setAll(texts);
        messagesBox.setVisible(!texts.isEmpty());
        messagesBox.setManaged(!texts.isEmpty());
    }

    /**
     * Sets the input's state from all messages, its own and the application's: error before warning; info does not
     * colour the input.
     */
    private void updateInputState() {
        FormInput<?> current = input.get();
        if (current != null) {
            current.stateProperty().set(stateOf(allMessages()));
        }
    }

    private static FieldState stateOf(List<FieldMessage> messages) {
        FieldState state = FieldState.NORMAL;
        for (FieldMessage message : messages) {
            if (message == null) {
                continue;
            }
            if (message.severity() == MessageSeverity.ERROR) {
                return FieldState.ERROR;
            }
            if (message.severity() == MessageSeverity.WARNING) {
                state = FieldState.WARNING;
            }
        }
        return state;
    }

    private static SelectableText messageText(FieldMessage message) {
        SelectableText text = new SelectableText();
        text.setText(message.text());
        text.getStyleClass().add("form-field-message");
        text.setMaxWidth(Double.MAX_VALUE);
        Tooltip tooltip = new Tooltip(message.text());
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(MESSAGE_TOOLTIP_WIDTH);
        text.setTooltip(tooltip);
        text.pseudoClassStateChanged(ERROR_PSEUDO_CLASS, message.severity() == MessageSeverity.ERROR);
        text.pseudoClassStateChanged(WARNING_PSEUDO_CLASS, message.severity() == MessageSeverity.WARNING);
        text.pseudoClassStateChanged(INFO_PSEUDO_CLASS, message.severity() == MessageSeverity.INFO);
        return text;
    }

    private void updateTooltip() {
        String text = description.get();
        labelNode.setTooltip(text == null || text.isEmpty() ? null : descriptionTooltip);
    }
}
