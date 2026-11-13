package org.karthu.fx.form;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import org.karthu.fx.font.MonospaceFont;
import org.karthu.fx.form.cron.CronCheck;
import org.karthu.fx.form.cron.CronConversion;
import org.karthu.fx.form.cron.CronDescription;
import org.karthu.fx.form.cron.CronRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A schedule as a cron expression, Unix or Quartz: a {@link FieldFrame} whose text field shows the expression in a
 * monospace font, followed by a small format selector (<i>Unix</i> or <i>Quartz</i>) and the {@code {}} icon. Under
 * the field, the input reports what the expression means in plain English (<i>At 09:00, Monday to Friday</i>) or, once
 * the error is due, the one message of the first rule it breaks (<i>Hour must be 0 to 23, found 24</i>), never both.
 * The input checks the expression itself, with rules of its own and no library.
 *
 * <p><b>Value.</b> {@link #valueProperty()} is the expression exactly as entered: what the user types stays as typed,
 * spaces and lower case included, and the empty text is no value (null counts as the empty text). A paste turns each
 * tab and line break into a space, which JavaFX's text field would drop, joining two fields of an expression copied
 * with tabs; a value set with tabs stays as set, and the field shows them as spaces.
 *
 * <p><b>Format.</b> {@link #formatProperty()} is the syntax the expression is checked against, {@link CronFormat#QUARTZ}
 * by default (the format of the Java schedulers); it is never null, as null counts as Quartz. While
 * {@linkplain #formatSelectableProperty() the format is selectable} (default true) the user chooses it in the format
 * selector; while not, the application has fixed it, and the format's name shows as plain text in the selector's place,
 * without chevron and hover, and nothing opens. The user's choice of a format converts a valid expression of the old
 * format into the new one when that loses nothing: the result means the same schedule, is written with single spaces
 * and its weekdays as names ({@code 0 9 * * 1-5} becomes {@code 0 0 9 ? * MON-FRI}), and switching back converts it
 * back ({@code 0 9 * * MON-FRI}). Anything else stays as it is and is checked against the new format: an invalid or
 * empty text, and an expression the other format cannot say without loss, such as Unix's <i>day 1 or Monday</i>, a
 * step in the weekday, or Quartz's seconds, year, {@code L}, {@code W} and {@code #}. Nothing is converted while the
 * input is bound or the application has bound {@link #valueProperty()}. A format the application sets (also through
 * a binding) never converts: the text stays as it is and is checked against the new format, its error due at once,
 * as for a value the application sets. So an application that loads a saved expression and its format shows the same
 * in either order, and never changes the stored expression behind its back.
 *
 * <p><b>Reading the result.</b> {@link #problemProperty()} is the message of the first broken rule, and
 * {@link #descriptionProperty()} the plain text of a valid expression; each is empty when the other applies, and both
 * are empty while the text is empty (whitespace only counts as empty) and while the input is bound. Both follow the
 * text and the format at once, whatever the info line shows, so an application can block saving or show the text
 * elsewhere, also without a form row and while the error is not yet shown.
 *
 * <p><b>Placeholder.</b> The empty field shows the field names of the current format:
 * {@link #unixPromptTextProperty()} (default <i>min hour day month weekday</i>) or
 * {@link #quartzPromptTextProperty()} (default <i>sec min hour day month weekday [year]</i>); null counts as empty.
 *
 * <p><b>The info line.</b> The input reports at most one message in {@link #getInputMessages()}, which a
 * {@link FormField} shows under the input above the application's own messages, colouring the frame by it:
 * <ul>
 *   <li>a valid expression: its description as an info message, at once, also while the user types;</li>
 *   <li>an empty text: nothing;</li>
 *   <li>an invalid expression: its problem as an error message once the error is <i>due</i>, nothing before. It becomes
 *       due when another node takes the focus from the text field, when the user presses Enter in it, when the format
 *       changes (the user's choice or the application's), and when the value is set other than by the user's typing
 *       (by the application, a binding, loading), so that a broken schedule saved earlier shows when the form opens,
 *       whether the application sets the value or the format first. Once due, the error follows every
 *       change of the text until the expression is valid or empty again; after that a new error waits for the next due
 *       point;</li>
 *   <li>bound: nothing.</li>
 * </ul>
 * Read-only and disabled, the input reports as it does unbound. The input never sets its own
 * {@linkplain #stateProperty() state}: inside a row the row sets it from all messages, so a due error draws the error
 * border; an input that stands alone gets its state from the application, which may read the problem.
 *
 * <p><b>Format selector.</b> A small non-editable combo box between the text and {@code {}}, without a divider, with a
 * background of its own, its own corner radius and a chevron, centred in the frame and exactly as wide as its widest
 * entry, so the field does not change when the format does. A click on it opens the list of the two formats, as does
 * Alt+Down in the text field; in the open list the arrow keys choose and Enter and Escape close it, as in JavaFX's combo
 * box. The selector is not focus traversable and never takes the focus, which stays in the text field. A format
 * property the application bound is not changed by a choice: the selector goes back to the bound format.
 *
 * <p><b>Binding.</b> Only the expression binds, through the input's {@linkplain #getBinding() binding}, shown by its
 * shell (see {@link FieldShell}): the chip lies over the text, which is kept, hidden, and comes back after unbinding.
 * The format selector stays usable while bound, Alt+Down still opens it, and the format is not bound: it says how the
 * variable's value is to be read, and a change of it converts nothing. The info line is empty while bound.
 *
 * <p><b>Read-only and disabled.</b> A {@linkplain #readOnlyProperty() read-only} input shows its text to select and copy
 * but not change, hides the {@code {}} icon and shows the format as plain text, as a fixed format; its info line stays
 * as usual. A disabled input greys out text, selector and {@code {}} once, with its whole row, and nothing opens.
 *
 * <p><b>Keyboard</b>, in the text field, the input's {@linkplain #getFocusTarget() focus target}: Alt+Down opens the
 * format list (not while disabled, read-only or the format is fixed, when it passes unconsumed), Enter makes the error
 * due and passes on (a dialog's default button still gets it), Ctrl+Space opens the binding list while {@code {}}
 * shows, Backspace or Delete unbind while bound and not read-only. The selector, the {@code {}} icon and the chip's
 * <i>&times;</i> are not focus traversable: Tab passes the input in one step.
 *
 * <p><b>Font and height.</b> The expression shows in the monospace family karthu-fx shares with the Markdown view's
 * code (the first installed of JetBrains Mono NL, Cascadia Mono and Consolas, else {@code Monospaced}), set on the text
 * field as an inline style that names the family only, at the frame's normal size. Its line is lower than the line of
 * the frame's own font, so the input keeps the height of a plain text field, its text centred.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The input carries {@code form-cron-input} beside the shell's {@code form-frame}, and its text
 * field the frame's {@code form-frame-text} (see {@link FieldShell} and {@link FieldFrame} for their classes and
 * pseudo-classes).
 * <ul>
 *   <li>{@code form-cron-format}: the format selector, a combo box (keeps {@code combo-box}), an own control of the
 *       shell, shown while the format is selectable and the input not read-only</li>
 *   <li>{@code form-cron-format-text}: the format's name as plain text, a label, an own control of the shell in the
 *       selector's place while the format is fixed or the input read-only</li>
 * </ul>
 * A cron input adds no pseudo-class: the shell's {@code :warning}, {@code :error}, {@code :readonly}, {@code :bound}
 * and {@code :disabled} apply.
 *
 * <p>Like every karthu-fx control, the input is used on the JavaFX Application Thread only.
 */
public class CronInput extends FieldFrame implements FormInput<String> {

    /** The key that opens the format list in the text field: Alt+Down, as the other inputs open their choice. */
    private static final KeyCombination FORMAT_LIST_SHORTCUT =
            new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN);

    private static final String UNIX_PROMPT_TEXT = "min hour day month weekday";
    private static final String QUARTZ_PROMPT_TEXT = "sec min hour day month weekday [year]";

    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final ObjectProperty<CronFormat> format = new FormatProperty(this);
    private final BooleanProperty formatSelectable = new SimpleBooleanProperty(this, "formatSelectable", true);
    private final StringProperty unixPromptText = new SimpleStringProperty(this, "unixPromptText", UNIX_PROMPT_TEXT);
    private final StringProperty quartzPromptText =
            new SimpleStringProperty(this, "quartzPromptText", QUARTZ_PROMPT_TEXT);
    private final ReadOnlyStringWrapper problem = new ReadOnlyStringWrapper(this, "problem", "");
    private final ReadOnlyStringWrapper description = new ReadOnlyStringWrapper(this, "description", "");
    /** The info line, at most one message; handed out unmodifiable, the same list every time. */
    private final ObservableList<FieldMessage> messages = FXCollections.observableArrayList();
    private final ObservableList<FieldMessage> readOnlyMessages = FXCollections.unmodifiableObservableList(messages);

    /** The format selector, an own control of the shell, shown while the user may choose the format. */
    private final CronFormatSelector selector =
            new CronFormatSelector(this, this::openFormatList, this::hideFormatList, this::focusText);
    /** The format as plain text, in the selector's place while the format is fixed or the input read-only. */
    private final Label formatText = new Label();
    /** Measures a line of the frame's own font, a plain text field's, for the input's height. */
    private final Text lineProbe = new Text();

    /** True while the input puts the value into the text field, or the text field's text into the value. */
    private boolean syncing;
    /** True while the input puts the format into the selector: the selector's changes meanwhile are no choice. */
    private boolean syncingFormat;
    /** True while the user's choice in the selector sets the format: the format change is the user's. */
    private boolean formatChosenByUser;
    /**
     * Whether the error of an invalid text is due: shown in the info line. It becomes due at a due point (the focus
     * leaving the text field, Enter, a format change, a value set other than by typing) and ends when the text
     * is valid or empty again, so that a new error waits for the next due point.
     */
    private boolean errorDue;

    /**
     * Creates an empty input in the Quartz format, its format selectable, with English placeholders.
     */
    public CronInput() {
        getStyleClass().add("form-cron-input");
        // The family only: the text keeps the size of the frame's text, the theme's.
        getTextField().setStyle(MonospaceFont.style());
        getTextField().promptTextProperty().bind(Bindings.createStringBinding(this::shownPromptText,
                format, unixPromptText, quartzPromptText));
        value.addListener((observable, oldValue, newValue) -> valueChanged());
        getTextField().textProperty().addListener((observable, oldText, newText) -> textChanged());
        format.addListener((observable, oldFormat, newFormat) -> formatChanged(oldFormat, formatChosenByUser));
        getBinding().boundNameProperty().addListener((observable, oldName, newName) -> check());

        buildFormatControls();

        // The due points in the text field: another node of the scene takes the focus, Enter (not consumed, so a
        // dialog's default button still gets it). The window losing the focus is none: the user comes back to the
        // same place and goes on typing.
        whenFocusLeaves(getTextField(), this::makeErrorDue);
        getTextField().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                makeErrorDue();
            }
        });
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
     * The input's value: the expression exactly as it stands in the field, the empty text when there is none; null
     * counts as the empty text. Setting it sets the text, also while the input is bound or read-only, and makes the
     * error of an invalid expression due at once.
     *
     * @return the value property
     */
    @Override
    public StringProperty valueProperty() {
        return value;
    }

    /**
     * Returns the expression.
     *
     * @return the text in the field
     */
    public String getValue() {
        return value.get();
    }

    /**
     * Sets the expression, as it is.
     *
     * @param text the new expression
     */
    public void setValue(String text) {
        value.set(text);
    }

    /**
     * Returns the input's own messages, its info line: at most one, the description of a valid expression as an info
     * message or, once due, the problem of an invalid one as an error message; none while the text is empty or the
     * input is bound (see the class description). The same unmodifiable list every time.
     *
     * @return the input's own messages, never null
     */
    @Override
    public ObservableList<FieldMessage> getInputMessages() {
        return readOnlyMessages;
    }

    // --- Format --------------------------------------------------------------------------------------

    /**
     * The syntax the expression is checked against (default {@link CronFormat#QUARTZ}). Never null: setting null, or
     * binding it to a value that is null, gives Quartz. The user's choice in the selector sets it and converts a valid
     * expression into the new format when nothing is lost. Setting it here, or through a binding, converts nothing:
     * the text stays and is checked against the new format, its error due at once (see the class description).
     *
     * @return the format property
     */
    public ObjectProperty<CronFormat> formatProperty() {
        return format;
    }

    /**
     * Returns the format.
     *
     * @return the format, never null
     */
    public CronFormat getFormat() {
        return format.get();
    }

    /**
     * Sets the format.
     *
     * @param newFormat the new format; null counts as Quartz
     */
    public void setFormat(CronFormat newFormat) {
        format.set(newFormat);
    }

    /**
     * Whether the user may choose the format (default true). While false, the application has fixed the format: the
     * selector is replaced in the same place by the format's name as plain text, without chevron and hover, and
     * nothing opens.
     *
     * @return the format selectable property
     */
    public BooleanProperty formatSelectableProperty() {
        return formatSelectable;
    }

    /**
     * Returns whether the user may choose the format.
     *
     * @return true while the format selector shows
     */
    public boolean isFormatSelectable() {
        return formatSelectable.get();
    }

    /**
     * Sets whether the user may choose the format.
     *
     * @param selectable false to fix the format
     */
    public void setFormatSelectable(boolean selectable) {
        formatSelectable.set(selectable);
    }

    // --- Placeholder ---------------------------------------------------------------------------------

    /**
     * The text the empty field shows in the Unix format (default <i>min hour day month weekday</i>), the field names in
     * their order. Null counts as empty.
     *
     * @return the Unix prompt text property
     */
    public StringProperty unixPromptTextProperty() {
        return unixPromptText;
    }

    /**
     * Returns the Unix prompt text.
     *
     * @return the text the empty field shows in the Unix format, or null
     */
    public String getUnixPromptText() {
        return unixPromptText.get();
    }

    /**
     * Sets the Unix prompt text.
     *
     * @param text the text the empty field shows in the Unix format; null or empty for none
     */
    public void setUnixPromptText(String text) {
        unixPromptText.set(text);
    }

    /**
     * The text the empty field shows in the Quartz format (default <i>sec min hour day month weekday [year]</i>), the
     * field names in their order, the optional year in brackets. Null counts as empty.
     *
     * @return the Quartz prompt text property
     */
    public StringProperty quartzPromptTextProperty() {
        return quartzPromptText;
    }

    /**
     * Returns the Quartz prompt text.
     *
     * @return the text the empty field shows in the Quartz format, or null
     */
    public String getQuartzPromptText() {
        return quartzPromptText.get();
    }

    /**
     * Sets the Quartz prompt text.
     *
     * @param text the text the empty field shows in the Quartz format; null or empty for none
     */
    public void setQuartzPromptText(String text) {
        quartzPromptText.set(text);
    }

    /** The placeholder the text field shows: the current format's, null as empty. */
    private String shownPromptText() {
        String text = format.get() == CronFormat.UNIX ? unixPromptText.get() : quartzPromptText.get();
        return text == null ? "" : text;
    }

    // --- Reading the result --------------------------------------------------------------------------

    /**
     * The message of the first rule the expression breaks in the current format, such as <i>Hour must be 0 to 23,
     * found 24</i> (English, one sentence, no full stop); empty while the text is empty or valid, and while the input
     * is bound. It follows the text and the format at once, also while the error is not yet due.
     *
     * @return the read-only problem property
     */
    public ReadOnlyStringProperty problemProperty() {
        return problem.getReadOnlyProperty();
    }

    /**
     * Returns the problem.
     *
     * @return the message of the first broken rule, or empty
     */
    public String getProblem() {
        return problem.get();
    }

    /**
     * The plain text of a valid expression, what it means, such as <i>At 09:00, Monday to Friday</i> (English, no full
     * stop); empty while the text is empty or invalid, and while the input is bound. It follows the text and the
     * format at once.
     *
     * @return the read-only description property
     */
    public ReadOnlyStringProperty descriptionProperty() {
        return description.getReadOnlyProperty();
    }

    /**
     * Returns the description.
     *
     * @return the plain text of a valid expression, or empty
     */
    public String getDescription() {
        return description.get();
    }

    // --- Checking ------------------------------------------------------------------------------------

    /**
     * The format changed, by the user's choice or by the application: the one place every format change passes. The
     * selector shows the new format. Only the user's choice converts a valid expression of the old format, when
     * nothing is lost. The application's change never converts: it loads a format beside a value, and in either order
     * the stored expression must stay as stored. Then the text is checked against the new format, its error due at
     * once, as for a value the application sets.
     *
     * @param oldFormat the format before
     * @param byUser    true when the user chose the format in the selector
     */
    private void formatChanged(CronFormat oldFormat, boolean byUser) {
        pushFormat();
        if (byUser) {
            convert(oldFormat);
        }
        makeErrorDue();
    }

    /**
     * Converts the text, when it is a valid expression of the format before, into the format now, if that loses
     * nothing ({@link CronConversion}): the value becomes the conversion. Anything else stays as it is. Nothing is
     * converted while bound, where the kept text is hidden and the format says how the variable's value is to be read,
     * nor while the application bound the value, which then decides it.
     *
     * @param oldFormat the format the text was written in
     */
    private void convert(CronFormat oldFormat) {
        String text = shownValue();
        if (getBinding().isBound() || value.isBound() || text.isBlank()) {
            return;
        }
        if (CronRules.check(text, oldFormat) instanceof CronCheck.Valid valid) {
            Optional<String> converted = CronConversion.convert(valid.expression());
            if (converted.isPresent()) {
                value.set(converted.get());
            }
        }
    }

    /** A due point: the error of an invalid text shows from now on, until the text is valid or empty again. */
    private void makeErrorDue() {
        errorDue = true;
        check();
    }

    /**
     * Checks the text against the format and shows the result: the problem and the description, both empty while
     * bound or while the text is empty (whitespace only counts as empty), and the info line, at most one message: the
     * description of a valid text, the error of an invalid one once due, nothing while bound or empty. A valid or empty
     * text ends a due error; while bound it stays as it was, for the kept text after unbinding.
     */
    private void check() {
        String text = shownValue();
        String newProblem = "";
        String newDescription = "";
        boolean bound = getBinding().isBound();
        if (!bound && !text.isBlank()) {
            CronCheck result = CronRules.check(text, format.get());
            if (result instanceof CronCheck.Valid valid) {
                newDescription = CronDescription.describe(valid.expression());
            } else if (result instanceof CronCheck.Invalid invalid) {
                newProblem = invalid.problem();
            }
        }
        if (!bound && newProblem.isEmpty()) {
            errorDue = false;
        }
        problem.set(newProblem);
        description.set(newDescription);

        List<FieldMessage> next = new ArrayList<>();
        if (!newDescription.isEmpty()) {
            next.add(FieldMessage.info(newDescription));
        } else if (!newProblem.isEmpty() && errorDue) {
            next.add(FieldMessage.error(newProblem));
        }
        if (!messages.equals(next)) {
            messages.setAll(next);
        }
    }

    // --- The format selector -------------------------------------------------------------------------

    /**
     * Builds the format's controls, the shell's own controls, in one place: the selector and the format as plain text,
     * one shown at a time. Alt+Down in the field opens the list; disabling closes it (the selector's own rule).
     */
    private void buildFormatControls() {
        formatText.getStyleClass().add("form-cron-format-text");
        formatText.textProperty().bind(Bindings.createStringBinding(() -> format.get().displayName(), format));
        selector.valueProperty().addListener((observable, oldFormat, chosen) -> selectorValueChanged(chosen));
        pushFormat();
        getOwnControls().addAll(selector, formatText);
        formatSelectable.addListener((observable, wasSelectable, selectable) -> updateFormatControls());
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleFormatListKey);
        updateFormatControls();
    }

    /**
     * Shows the selector while the user may choose the format (selectable and not read-only), else the format as plain
     * text in its place; a list that shows closes when the selector goes. Not the shell's own-control rule: the
     * selector stays enabled while bound.
     */
    private void updateFormatControls() {
        boolean selectable = formatSelectable.get() && !isReadOnly();
        selector.setVisible(selectable);
        selector.setManaged(selectable);
        formatText.setVisible(!selectable);
        formatText.setManaged(!selectable);
        if (!selectable) {
            hideFormatList();
        }
    }

    /**
     * Whether the user may choose the format now: not while disabled, read-only or the format is fixed. Unlike the
     * value, the format may be chosen while bound: only the expression is bound.
     */
    private boolean userMayChangeFormat() {
        return !isDisabled() && !isReadOnly() && formatSelectable.get();
    }

    /**
     * Opens the format list while the user may choose the format: every way to open it passes here, a click on the
     * selector (through its {@code show()}) and Alt+Down in the text field.
     */
    private void openFormatList() {
        if (userMayChangeFormat()) {
            showFormatList();
        }
    }

    /**
     * Shows the format list, in a shown window only, as a popup needs one. Package-private: the tests' seam, so that a
     * test sees the list open without a window; it is no part of the API.
     */
    void showFormatList() {
        selector.showList();
    }

    /** Closes the format list if it shows: the format became fixed, or the input read-only or disabled. */
    private void hideFormatList() {
        selector.hideList();
    }

    /** Alt+Down in the field opens the format list; while the user may not choose the format it passes. */
    private void handleFormatListKey(KeyEvent event) {
        if (FORMAT_LIST_SHORTCUT.match(event) && userMayChangeFormat()) {
            event.consume();
            openFormatList();
        }
    }

    /** The selector asks for the focus: the text field takes it, where the keyboard works. */
    private void focusText() {
        getTextField().requestFocus();
    }

    /** Puts the format into the selector; the selector's own changes meanwhile are ignored. */
    private void pushFormat() {
        syncingFormat = true;
        try {
            selector.getSelectionModel().select(format.get());
            selector.setValue(format.get());
        } finally {
            syncingFormat = false;
        }
    }

    /**
     * The selector's value changed: the user's choice becomes the format while the user may choose it and the
     * application has not bound the format property; any other change of the selector's own is undone.
     */
    private void selectorValueChanged(CronFormat chosen) {
        if (syncingFormat) {
            return;
        }
        if (chosen == null || format.isBound() || !userMayChangeFormat()) {
            pushFormat();
            return;
        }
        formatChosenByUser = true;
        try {
            format.set(chosen);
        } finally {
            formatChosenByUser = false;
        }
    }

    /**
     * Follows read-only and the binding: the text field editable while neither (the frame's rule), and the format's
     * controls, the selector only while not read-only.
     *
     * @param editable true while the user may edit the text: neither bound nor read-only
     */
    @Override
    protected void updateContentEditable(boolean editable) {
        super.updateContentEditable(editable);
        // The frame's constructor sets its content, and so calls this hook, before this input's selector exists;
        // buildFormatControls shows the format's controls then.
        if (selector != null) {
            updateFormatControls();
        }
    }

    // --- Height --------------------------------------------------------------------------------------

    /**
     * Returns the input's height: as high as a plain text field, though its text is in the monospace family, whose
     * line is lower than the line of the frame's own font. The text field is as high as a text field in the frame's own
     * font (its padding and one line, measured as a text field measures it), its text centred, the selector centred in
     * the frame as every trailing node.
     *
     * @param width the width, ignored
     * @return the preferred height, also the minimum and the maximum
     */
    @Override
    protected double computePrefHeight(double width) {
        double height = super.computePrefHeight(width);
        // Asked for before this input's fields exist, while the frame is being built.
        if (formatText == null) {
            return height;
        }
        // The format's text has the frame's own font, which a plain text field in the same place has. A text field
        // measures its line as centred text does, the same for every text, also an empty one.
        lineProbe.setBoundsType(TextBoundsType.LOGICAL_VERTICAL_CENTER);
        lineProbe.setFont(formatText.getFont());
        TextField field = getTextField();
        double plainField = field.snappedTopInset() + lineProbe.getLayoutBounds().getHeight()
                + field.snappedBottomInset();
        return Math.max(height, snappedTopInset() + snapSizeY(plainField) + snappedBottomInset());
    }

    // --- The value and the text field ----------------------------------------------------------------

    /**
     * Returns what a paste inserts: the clipboard's text with each tab and line break as a space. JavaFX's text field
     * drops them, which would join the fields of an expression copied with tabs between them ({@code 0\t9} would
     * become {@code 09}); as spaces they keep the fields apart, which is all whitespace means to the rules.
     *
     * @param pasted the text on the clipboard
     * @return the text with its control whitespace as spaces
     */
    @Override
    protected String pastedText(String pasted) {
        return withSpaces(pasted);
    }

    /**
     * The value changed. Set other than by the user's typing (the application, a binding, loading), the text field
     * takes it and its error is due at once; then the text is checked.
     */
    private void valueChanged() {
        if (!syncing) {
            syncing = true;
            try {
                getTextField().setText(withSpaces(shownValue()));
            } finally {
                syncing = false;
            }
            errorDue = true;
        }
        check();
    }

    /**
     * The text field's text changed by the user: typing, deleting, pasting, undoing. It becomes the value, unless the
     * application bound the value, which then wins and goes back into the field.
     */
    private void textChanged() {
        if (syncing) {
            return;
        }
        String typed = getTextField().getText() == null ? "" : getTextField().getText();
        syncing = true;
        try {
            if (!setValueByUser(value, typed)) {
                getTextField().setText(withSpaces(shownValue()));
            }
        } finally {
            syncing = false;
        }
    }

    /** The value as the field shows it: null as the empty text. */
    private String shownValue() {
        String text = value.get();
        return text == null ? "" : text;
    }

    /**
     * Returns the text with each whitespace character that JavaFX's text field drops (a tab, a line break, the other
     * control characters {@link Character#isWhitespace} counts) as a space, so that the fields stay apart in the field.
     */
    private static String withSpaces(String text) {
        StringBuilder spaced = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < ' ' && Character.isWhitespace(c)) {
                spaced.append(' ');
            } else {
                spaced.append(c);
            }
        }
        return spaced.toString();
    }

    /** The format property: never null, as null counts as Quartz, also when bound to a value that is null. */
    private static final class FormatProperty extends SimpleObjectProperty<CronFormat> {

        private FormatProperty(CronInput input) {
            super(input, "format", CronFormat.QUARTZ);
        }

        @Override
        public CronFormat get() {
            CronFormat current = super.get();
            return current == null ? CronFormat.QUARTZ : current;
        }
    }
}
