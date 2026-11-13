package org.karthu.fx.form;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.font.MonospaceFont;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.firesThrough;

/**
 * The cron input through its public API, without a window: the expression in monospace in the form collection's shell,
 * its format and the format selector, the problem and the plain text it reads from the expression, its info line (when
 * an error is due), every state of the collection and the keyboard every input shares. The rules themselves are pinned
 * down by {@code CronRulesTest} and {@code CronDescriptionTest}; this test checks what the input makes of them.
 */
class CronInputTest {

    private CronInput input;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInput() {
        input = new CronInput();
    }

    // --- A new input and its value -------------------------------------------------------------------

    @Test
    @DisplayName("a new input is an empty frame with form-cron-input, Quartz, its format selectable, nothing reported")
    void newInputIsAnEmptyQuartzFrame() {
        FormInput<String> formInput = input;

        assertSame(input, formInput.getNode());
        assertSame(input.getTextField(), formInput.getFocusTarget());
        assertTrue(input.getStyleClass().containsAll(List.of("form-frame", "form-cron-input")));
        assertFalse(input.getStyleClass().contains("text-input"));
        assertEquals("", input.getValue());
        assertEquals(CronFormat.QUARTZ, input.getFormat());
        assertTrue(input.isFormatSelectable());
        assertEquals("", input.getProblem());
        assertEquals("", input.getDescription());
        assertTrue(input.getInputMessages().isEmpty());
        assertSame(input.getInputMessages(), formInput.getInputMessages(), "the same list every time");
        assertEquals(FieldState.NORMAL, input.getState());
    }

    @Test
    @DisplayName("the value is the text exactly as entered, both ways: spaces and lower case kept, null as empty")
    void valueIsTheTextAsEntered() {
        List<String> values = new ArrayList<>();
        input.valueProperty().addListener((observable, oldValue, newValue) -> values.add(newValue));

        input.setValue("0 0 9 ? * MON-FRI");
        assertEquals("0 0 9 ? * MON-FRI", input.getTextField().getText());

        input.getTextField().setText("  0  9 * * mon-fri ");
        assertEquals("  0  9 * * mon-fri ", input.getValue());

        input.getTextField().clear();
        assertEquals("", input.getValue());
        assertEquals(List.of("0 0 9 ? * MON-FRI", "  0  9 * * mon-fri ", ""), values);

        input.setValue(null);
        assertEquals("", input.getTextField().getText());
    }

    @Test
    @DisplayName("a paste with tabs or line breaks between the fields keeps them apart, as spaces")
    void pastedTabsBecomeSpaces() {
        // JavaFX's text field drops tabs and line breaks, which would join the fields: 0\t9 would become 09.
        input.setClipboard(new FormTestSupport.RecordingClipboard("0\t9\t*\t*\t1-5\r\n"));
        input.getTextField().paste();

        assertEquals("0 9 * * 1-5  ", input.getValue());
    }

    @Test
    @DisplayName("a value set with tabs stays as set; the field shows the tabs as spaces, so the fields stay apart")
    void valueWithTabsShowsSpaces() {
        input.setFormat(CronFormat.UNIX);
        input.setValue("0\t9\t*\t*\t1-5");

        assertEquals("0\t9\t*\t*\t1-5", input.getValue());
        assertEquals("0 9 * * 1-5", input.getTextField().getText());
    }

    @Test
    @DisplayName("typed keys reach the text field: what the user types is the value")
    void typingSetsTheValue() {
        FormTestSupport.layOut(input);

        typeInto(input, "0 9 * * 1-5");
        input.getTextField().fireEvent(FormTestSupport.keyPressed(KeyCode.BACK_SPACE));

        assertEquals("0 9 * * 1-", input.getValue());
    }

    // --- Monospace and placeholder -------------------------------------------------------------------

    @Test
    @DisplayName("the expression is in the shared monospace family, at the size of a plain text field's text")
    void expressionInMonospace() {
        TextField plain = new TextField("plain");
        input.setValue("0 0 9 ? * MON-FRI");
        FormTestSupport.layOut(plain, input);

        Font font = input.getTextField().getFont();
        assertEquals(MonospaceFont.family(), font.getFamily());
        assertEquals(plain.getFont().getSize(), font.getSize(), 0.001);
    }

    @Test
    @DisplayName("the placeholder names the fields of the current format, from two properties with English defaults")
    void placeholderPerFormat() {
        assertEquals("min hour day month weekday", input.getUnixPromptText());
        assertEquals("sec min hour day month weekday [year]", input.getQuartzPromptText());
        assertEquals("sec min hour day month weekday [year]", input.getTextField().getPromptText());

        input.setFormat(CronFormat.UNIX);
        assertEquals("min hour day month weekday", input.getTextField().getPromptText());

        input.setUnixPromptText("Minute Stunde Tag Monat Wochentag");
        assertEquals("Minute Stunde Tag Monat Wochentag", input.getTextField().getPromptText());
        input.unixPromptTextProperty().set(null);
        assertEquals("", input.getTextField().getPromptText());

        input.setFormat(CronFormat.QUARTZ);
        input.quartzPromptTextProperty().set("Sekunde Minute Stunde Tag Monat Wochentag [Jahr]");
        assertEquals("Sekunde Minute Stunde Tag Monat Wochentag [Jahr]", input.getTextField().getPromptText());
    }

    @Test
    @DisplayName("the format is never null: null counts as Quartz")
    void formatIsNeverNull() {
        input.setFormat(CronFormat.UNIX);
        input.setFormat(null);

        assertEquals(CronFormat.QUARTZ, input.getFormat());
        assertEquals(CronFormat.QUARTZ, input.formatProperty().getValue());
        assertEquals("sec min hour day month weekday [year]", input.getTextField().getPromptText());
    }

    // --- Problem and description --------------------------------------------------------------------

    @Test
    @DisplayName("problem and description follow the text at once, whatever the info line shows")
    void problemAndDescriptionFollowTheText() {
        input.getTextField().setText("0 0 9 ? * MON-FRI");
        assertEquals("At 09:00:00, Monday to Friday", input.getDescription());
        assertEquals("", input.getProblem());

        // Typed, so no error is due yet: the problem is there all the same.
        input.getTextField().setText("0 0 9 * * MON-FRI");
        assertEquals("Day of month or weekday must be ?", input.getProblem());
        assertEquals("", input.getDescription());

        input.setValue("0 0 24 ? * *");
        assertEquals("Hour must be 0 to 23, found 24", input.getProblem());
    }

    @Test
    @DisplayName("problem and description follow the format at once: the text is checked against the new one")
    void problemAndDescriptionFollowTheFormat() {
        // The application's format change converts nothing: the text stays as it is.
        input.setValue("0 0 1 * 1");
        assertEquals("Quartz cron has 6 or 7 fields, found 5", input.getProblem());

        input.setFormat(CronFormat.UNIX);
        assertEquals("", input.getProblem());
        assertEquals("At 00:00, on day 1 of the month or on Monday", input.getDescription());
        assertEquals("0 0 1 * 1", input.getValue());

        input.setFormat(CronFormat.QUARTZ);
        assertEquals("Quartz cron has 6 or 7 fields, found 5", input.getProblem());
        assertEquals("", input.getDescription());
    }

    @Test
    @DisplayName("an empty text, or one of only whitespace, is no problem and has no description; the value stays")
    void emptyTextIsNoProblem() {
        input.setValue("");
        assertEquals("", input.getProblem());
        assertEquals("", input.getDescription());

        input.setValue(" \t ");
        assertEquals("", input.getProblem());
        assertEquals("", input.getDescription());
        assertEquals(" \t ", input.getValue());

        input.setValue(null);
        assertEquals("", input.getProblem());
        assertEquals("", input.getDescription());
    }

    @Test
    @DisplayName("while bound, problem and description are empty; after unbinding they follow the kept text again")
    void boundHasNoProblemAndNoDescription() {
        input.setValue("0 0 9 * * MON-FRI");
        input.getBinding().setBoundName("schedule");
        assertEquals("", input.getProblem());
        assertEquals("", input.getDescription());

        input.setValue("0 0 9 ? * MON-FRI");
        assertEquals("", input.getDescription());

        input.getBinding().setBoundName(null);
        assertEquals("0 0 9 ? * MON-FRI", input.getValue());
        assertEquals("At 09:00:00, Monday to Friday", input.getDescription());
        assertEquals("", input.getProblem());
    }

    @Test
    @DisplayName("problem and description are read-only properties that tell their listeners")
    void problemAndDescriptionAreObservable() {
        List<String> problems = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        input.problemProperty().addListener((observable, oldValue, newValue) -> problems.add(newValue));
        input.descriptionProperty().addListener((observable, oldValue, newValue) -> descriptions.add(newValue));

        input.setValue("0 0 9 ? * *");
        input.setValue("0 0 99 ? * *");
        input.setValue("");

        assertEquals(List.of("Hour must be 0 to 23, found 99", ""), problems);
        assertEquals(List.of("At 09:00:00, every day", ""), descriptions);
    }

    // --- The info line: the input's own messages ------------------------------------------------------

    @Test
    @DisplayName("a valid expression reports its plain text at once, as an info message, also while typing")
    void validExpressionReportsItsDescription() {
        input.setFormat(CronFormat.UNIX);

        input.getTextField().setText("0 9 * * 1-5");

        assertEquals(List.of(FieldMessage.info("At 09:00, Monday to Friday")), input.getInputMessages());
    }

    @Test
    @DisplayName("an empty text reports nothing, also one of only whitespace")
    void emptyTextReportsNothing() {
        input.getTextField().setText("0 0 9 ? * *");
        input.getTextField().setText("   ");
        assertTrue(input.getInputMessages().isEmpty());

        input.setValue(" ");
        assertTrue(input.getInputMessages().isEmpty());
    }

    @Test
    @DisplayName("while typing, an invalid expression reports nothing: its error is not due yet")
    void typedErrorIsNotDue() {
        input.getTextField().setText("0 0 9 * * MON-FRI");

        assertTrue(input.getInputMessages().isEmpty());
        assertEquals("Day of month or weekday must be ?", input.getProblem());
    }

    @Test
    @DisplayName("the error becomes due when the focus leaves the text field, then follows the text until valid")
    void errorDueWhenTheFocusLeaves() {
        TextField other = new TextField();
        new Scene(new VBox(input, other));
        input.getTextField().requestFocus();
        input.getTextField().setText("0 0 9 * * MON-FRI");

        other.requestFocus();
        assertEquals(List.of(FieldMessage.error("Day of month or weekday must be ?")), input.getInputMessages());

        // Once due, the error follows every change of the text, back in the field.
        input.getTextField().requestFocus();
        input.getTextField().setText("0 0 24 * * MON-FRI");
        assertEquals(List.of(FieldMessage.error("Hour must be 0 to 23, found 24")), input.getInputMessages());

        // Valid again: the plain text at once, and a new error waits for the next due point.
        input.getTextField().setText("0 0 9 ? * MON-FRI");
        assertEquals(List.of(FieldMessage.info("At 09:00:00, Monday to Friday")), input.getInputMessages());
        input.getTextField().setText("0 0 9 ? * MON-FRY");
        assertTrue(input.getInputMessages().isEmpty());

        other.requestFocus();
        assertEquals(List.of(FieldMessage.error("Unknown weekday FRY")), input.getInputMessages());
    }

    @Test
    @DisplayName("Enter in the text field makes the error due, and passes on to the form")
    void errorDueOnEnter() {
        input.getTextField().setText("0 0 9 ? * 6#6");

        List<Event> passed = firesThrough(input, FormTestSupport.keyPressed(KeyCode.ENTER));

        assertEquals(List.of(FieldMessage.error("Weekday occurrence must be 1 to 5, found 6")),
                input.getInputMessages());
        assertEquals(1, passed.size(), "a dialog's default button still gets Enter");
    }

    @Test
    @DisplayName("an empty text ends a due error too: a new error waits for the next due point")
    void emptyTextEndsTheDueError() {
        input.getTextField().setText("0 0 9 ? * 6#6");
        firesThrough(input, FormTestSupport.keyPressed(KeyCode.ENTER));

        input.getTextField().setText("");
        assertTrue(input.getInputMessages().isEmpty());
        input.getTextField().setText("0 0 9 ? * 6#6");
        assertTrue(input.getInputMessages().isEmpty());
    }

    @Test
    @DisplayName("a value set by the application is due at once: a broken schedule shows when the form opens")
    void valueSetByTheApplicationIsDue() {
        input.setValue("0 9 * * 1-5");

        assertEquals(List.of(FieldMessage.error("Quartz cron has 6 or 7 fields, found 5")), input.getInputMessages());

        StringProperty source = new SimpleStringProperty("0 0 9 ? * 6#6");
        input.valueProperty().bind(source);
        assertEquals(List.of(FieldMessage.error("Weekday occurrence must be 1 to 5, found 6")),
                input.getInputMessages());
    }

    @Test
    @DisplayName("read-only and disabled report as unbound: the plain text, or the error of a value set at once")
    void readOnlyAndDisabledReportAsUnbound() {
        input.setReadOnly(true);
        input.setValue("0 0 9 ? * MON-FRI");
        assertEquals(List.of(FieldMessage.info("At 09:00:00, Monday to Friday")), input.getInputMessages());
        input.setValue("0 0 9 * * MON-FRI");
        assertEquals(List.of(FieldMessage.error("Day of month or weekday must be ?")), input.getInputMessages());

        CronInput disabled = new CronInput();
        disabled.setDisable(true);
        disabled.setValue("0 0 9 * * MON-FRI");
        assertEquals(List.of(FieldMessage.error("Day of month or weekday must be ?")), disabled.getInputMessages());
    }

    @Test
    @DisplayName("while bound, nothing is reported; after unbinding the kept text reports again")
    void boundReportsNothing() {
        input.setValue("0 0 9 * * MON-FRI");
        input.getBinding().setBoundName("schedule");
        assertTrue(input.getInputMessages().isEmpty());

        input.setValue("0 0 9 ? * MON-FRI");
        assertTrue(input.getInputMessages().isEmpty());

        input.getBinding().setBoundName(null);
        assertEquals(List.of(FieldMessage.info("At 09:00:00, Monday to Friday")), input.getInputMessages());
    }

    @Test
    @DisplayName("the input's own messages cannot be changed by the application")
    void inputMessagesAreUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> input.getInputMessages().add(FieldMessage.error("mine")));
    }

    // --- The format selector -------------------------------------------------------------------------

    @Test
    @DisplayName("the selector is a combo box of Unix and Quartz with a chevron, after the text, before {}, no divider")
    void selectorAfterTheTextBeforeTheBindIcon() {
        input.getBinding().setBindable(true);
        input.setPrefWidth(320);
        FormTestSupport.layOut(input);

        ComboBox<CronFormat> selector = selector(input);
        assertEquals(List.of(CronFormat.UNIX, CronFormat.QUARTZ), selector.getItems());
        assertEquals(CronFormat.QUARTZ, selector.getValue());
        assertFalse(selector.isFocusTraversable());
        assertTrue(selector.isVisible());
        FontIcon chevron = (FontIcon) selector.lookup(".ikonli-font-icon");
        assertEquals(BootstrapIcons.CHEVRON_DOWN, chevron.getIconCode());
        assertNull(input.lookup(".form-shell-divider"));
        assertTrue(input.getTextField().getBoundsInParent().getMaxX() <= selector.getBoundsInParent().getMinX());
        assertTrue(selector.getBoundsInParent().getMaxX() <= bindIcon(input).getBoundsInParent().getMinX());
        assertEquals("Quartz", shownFormat(input));
    }

    @Test
    @DisplayName("the selector keeps its width for both formats, and sits centred in a field as high as a text field")
    void selectorKeepsItsWidth() {
        TextField plain = new TextField("plain");
        CronInput unix = new CronInput();
        unix.setFormat(CronFormat.UNIX);
        FormTestSupport.layOut(plain, input, unix);

        Bounds quartzBounds = selector(input).getBoundsInParent();
        Bounds unixBounds = selector(unix).getBoundsInParent();
        assertEquals(quartzBounds.getWidth(), unixBounds.getWidth(), 0.001);
        assertEquals("Unix", shownFormat(unix));
        assertEquals(plain.getHeight(), input.getHeight(), 0.001);
        assertTrue(quartzBounds.getHeight() < input.getHeight());
        assertEquals(input.getHeight() / 2, quartzBounds.getCenterY(), 0.5, "centred vertically");
        // As wide as the widest entry: the shown name fits without being cut.
        Labeled shownName = shownNameNode(input);
        assertEquals(shownName.getText(), ((Text) shownName.lookup(".text")).getText());
    }

    @Test
    @DisplayName("the selector has its own background and radius from the theme; the fixed format's text is muted")
    void selectorLook() {
        Region button = FormTestSupport.tokenSwatch("-fx-button-color");
        Region hover = FormTestSupport.tokenSwatch("-fx-button-hover-color");
        Region muted = FormTestSupport.tokenSwatch("-fx-text-secondary-color");
        CronInput hovered = new CronInput();
        selector(hovered).pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        CronInput fixed = new CronInput();
        fixed.setFormatSelectable(false);
        FormTestSupport.layOut(button, hover, muted, input, hovered, fixed);

        ComboBox<CronFormat> selector = selector(input);
        assertEquals(FormTestSupport.fill(button), FormTestSupport.fill(selector));
        assertEquals(1, selector.getBackground().getFills().size(), "no border of JavaFX's combo box");
        assertEquals(3, selector.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius(), 0.001);
        assertEquals(FormTestSupport.fill(hover), FormTestSupport.fill(selector(hovered)));
        assertEquals(FormTestSupport.fill(muted), formatText(fixed).getTextFill());
        assertNull(formatText(fixed).getBackground());
    }

    @Test
    @DisplayName("the selector follows the format the application sets, and the user's choice sets the format")
    void selectorAndFormatInStep() {
        FormTestSupport.layOut(input);
        input.setFormat(CronFormat.UNIX);
        assertEquals(CronFormat.UNIX, selector(input).getValue());
        assertEquals("Unix", shownFormat(input));

        choose(input, CronFormat.QUARTZ);
        assertEquals(CronFormat.QUARTZ, input.getFormat());
        assertEquals("Quartz", shownFormat(input));
    }

    @Test
    @DisplayName("a format change, the user's or the application's, makes the error due")
    void formatChangeMakesTheErrorDue() {
        // Not convertible (day of month or weekday), so the text stays and is checked against each format.
        input.getTextField().setText("0 0 1 * 1");
        input.setFormat(CronFormat.QUARTZ);
        assertTrue(input.getInputMessages().isEmpty(), "typed, and the format did not change");

        choose(input, CronFormat.UNIX);
        assertEquals(List.of(FieldMessage.info("At 00:00, on day 1 of the month or on Monday")),
                input.getInputMessages());

        choose(input, CronFormat.QUARTZ);
        assertEquals(List.of(FieldMessage.error("Quartz cron has 6 or 7 fields, found 5")), input.getInputMessages());

        CronInput typed = new CronInput();
        typed.getTextField().setText("0 24 * * *");
        typed.setFormat(CronFormat.UNIX);
        assertEquals(List.of(FieldMessage.error("Hour must be 0 to 23, found 24")), typed.getInputMessages());
    }

    @Test
    @DisplayName("a format the application bound is not changed by a choice: the selector goes back to it")
    void boundFormatIsNotChangedByAChoice() {
        ObjectProperty<CronFormat> source = new SimpleObjectProperty<>(CronFormat.UNIX);
        input.formatProperty().bind(source);

        choose(input, CronFormat.QUARTZ);

        assertEquals(CronFormat.UNIX, input.getFormat());
        assertEquals(CronFormat.UNIX, selector(input).getValue());
    }

    @Test
    @DisplayName("Alt+Down in the text field opens the format list, consumed; a click on the selector opens it too")
    void altDownOpensTheFormatList() {
        RecordingCronInput recording = new RecordingCronInput();

        assertTrue(firesThrough(recording, FormTestSupport.altKeyPressed(KeyCode.DOWN)).isEmpty());
        assertEquals(1, recording.listsShown);

        selector(recording).show();
        assertEquals(2, recording.listsShown);

        // Down without Alt is the text field's.
        assertEquals(1, firesThrough(recording, FormTestSupport.keyPressed(KeyCode.DOWN)).size());
        assertEquals(2, recording.listsShown);
    }

    @Test
    @DisplayName("bound, the selector stays usable: Alt+Down opens the list, a choice sets the format")
    void boundSelectorStaysUsable() {
        RecordingCronInput recording = new RecordingCronInput();
        recording.setValue("0 9 * * 1-5");
        recording.getBinding().setBoundName("schedule");

        firesThrough(recording, FormTestSupport.altKeyPressed(KeyCode.DOWN));
        assertEquals(1, recording.listsShown);
        assertFalse(selector(recording).isDisabled());

        choose(recording, CronFormat.UNIX);
        assertEquals(CronFormat.UNIX, recording.getFormat());
        assertTrue(recording.getInputMessages().isEmpty(), "bound: nothing reported");
        assertEquals("0 9 * * 1-5", recording.getValue());
    }

    @Test
    @DisplayName("a fixed format shows the format's name as plain text without chevron; nothing opens")
    void fixedFormatShowsPlainText() {
        RecordingCronInput recording = new RecordingCronInput();
        recording.setFormatSelectable(false);
        FormTestSupport.layOut(recording);

        assertFalse(selector(recording).isVisible());
        assertFalse(selector(recording).isManaged());
        Label text = formatText(recording);
        assertTrue(text.isVisible());
        assertEquals("Quartz", text.getText());
        assertNull(text.lookup(".ikonli-font-icon"));
        recording.setFormat(CronFormat.UNIX);
        assertEquals("Unix", text.getText());

        assertEquals(1, firesThrough(recording, FormTestSupport.altKeyPressed(KeyCode.DOWN)).size());
        selector(recording).show();
        assertEquals(0, recording.listsShown);

        recording.setFormatSelectable(true);
        assertTrue(selector(recording).isVisible());
        assertFalse(text.isVisible());
        assertFalse(text.isManaged());
    }

    @Test
    @DisplayName("read-only: the text selectable, not editable, no {}, the format as plain text, the info line as usual")
    void readOnlyShowsTheFormatAsText() {
        RecordingCronInput recording = new RecordingCronInput();
        recording.getBinding().setBindable(true);
        recording.setValue("0 0 9 ? * MON-FRI");
        recording.setReadOnly(true);
        FormTestSupport.layOut(recording);

        assertFalse(recording.getTextField().isEditable());
        recording.getTextField().selectAll();
        assertEquals("0 0 9 ? * MON-FRI", recording.getTextField().getSelectedText());
        assertFalse(bindIcon(recording).isVisible());
        assertFalse(selector(recording).isVisible());
        assertEquals("Quartz", formatText(recording).getText());
        assertEquals(1, firesThrough(recording, FormTestSupport.altKeyPressed(KeyCode.DOWN)).size());
        assertEquals(0, recording.listsShown);
        assertEquals(List.of(FieldMessage.info("At 09:00:00, Monday to Friday")), recording.getInputMessages());

        recording.setReadOnly(false);
        assertTrue(selector(recording).isVisible());
        assertFalse(formatText(recording).isVisible());
    }

    @Test
    @DisplayName("disabled: the selector disabled with the field, Alt+Down opens nothing")
    void disabledOpensNothing() {
        RecordingCronInput recording = new RecordingCronInput();
        recording.setDisable(true);

        assertTrue(selector(recording).isDisabled());
        firesThrough(recording, FormTestSupport.altKeyPressed(KeyCode.DOWN));
        selector(recording).show();
        assertEquals(0, recording.listsShown);
    }

    @Test
    @DisplayName("the selector shows the formats by name in its list")
    void listShowsTheFormatsByName() {
        ListView<CronFormat> list = list(input);
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < list.getItems().size(); i++) {
            ListCell<CronFormat> cell = list.getCellFactory().call(list);
            cell.updateListView(list);
            cell.updateIndex(i);
            texts.add(cell.getText());
        }
        assertEquals(List.of("Unix", "Quartz"), texts);
    }

    // --- Conversion on a format change ---------------------------------------------------------------

    @Test
    @DisplayName("the user's format change converts a valid expression, weekdays as names; switching back converts back")
    void userFormatChangeConverts() {
        FormTestSupport.layOut(input);
        input.setFormat(CronFormat.UNIX);
        input.getTextField().setText("0 9 * * 1-5");

        choose(input, CronFormat.QUARTZ);
        assertEquals("0 0 9 ? * MON-FRI", input.getValue());
        assertEquals("0 0 9 ? * MON-FRI", input.getTextField().getText());
        assertEquals(List.of(FieldMessage.info("At 09:00:00, Monday to Friday")), input.getInputMessages());

        choose(input, CronFormat.UNIX);
        assertEquals("0 9 * * MON-FRI", input.getValue());
        assertEquals(List.of(FieldMessage.info("At 09:00, Monday to Friday")), input.getInputMessages());
    }

    @Test
    @DisplayName("the application's format change never converts: the text stays and its error is due at once")
    void applicationFormatChangeDoesNotConvert() {
        input.setFormat(CronFormat.UNIX);
        input.setValue("@daily");
        List<String> seen = new ArrayList<>();
        input.valueProperty().addListener((observable, oldValue, newValue) -> seen.add(newValue));

        input.setFormat(CronFormat.QUARTZ);
        assertEquals("@daily", input.getValue());
        assertEquals("@daily", input.getTextField().getText());
        assertEquals(List.of(), seen, "the value is not touched");
        assertEquals(List.of(FieldMessage.error("Quartz cron has no macros")), input.getInputMessages());

        // Typed, so not due yet: the application's format change makes it due, as a value it sets.
        CronInput typed = new CronInput();
        typed.setFormat(CronFormat.UNIX);
        typed.getTextField().setText("0 9 * * 1-5");
        typed.setFormat(CronFormat.QUARTZ);
        assertEquals("0 9 * * 1-5", typed.getValue());
        assertEquals(List.of(FieldMessage.error("Quartz cron has 6 or 7 fields, found 5")), typed.getInputMessages());

        // Valid in the new format: its plain text, nothing converted.
        typed.setFormat(CronFormat.UNIX);
        assertEquals(List.of(FieldMessage.info("At 09:00, Monday to Friday")), typed.getInputMessages());
    }

    @Test
    @DisplayName("an application loading value and format shows the same in either order, and writes nothing back")
    void bothLoadOrdersShowTheSame() {
        // A Quartz expression saved with the format Unix: an error, in either order, never converted.
        CronInput valueFirst = new CronInput();
        valueFirst.setValue("0 0 0 L * ?");
        valueFirst.setFormat(CronFormat.UNIX);
        CronInput formatFirst = new CronInput();
        formatFirst.setFormat(CronFormat.UNIX);
        formatFirst.setValue("0 0 0 L * ?");
        for (CronInput loaded : List.of(valueFirst, formatFirst)) {
            assertEquals("0 0 0 L * ?", loaded.getValue());
            assertEquals(List.of(FieldMessage.error("Unix cron has 5 fields, found 6")), loaded.getInputMessages());
        }

        // A stored expression that would convert stays as stored, also in a property bound both ways.
        StringProperty stored = new SimpleStringProperty("0 0 9 ? * MON-FRI");
        CronInput bound = new CronInput();
        bound.valueProperty().bindBidirectional(stored);
        bound.setFormat(CronFormat.UNIX);
        assertEquals("0 0 9 ? * MON-FRI", stored.get());
        assertEquals("0 0 9 ? * MON-FRI", bound.getValue());
        assertEquals(List.of(FieldMessage.error("Unix cron has 5 fields, found 6")), bound.getInputMessages());

        // A valid expression in its own format: its plain text, in either order.
        CronInput unixValueFirst = new CronInput();
        unixValueFirst.setValue("0 9 * * 1-5");
        unixValueFirst.setFormat(CronFormat.UNIX);
        CronInput unixFormatFirst = new CronInput();
        unixFormatFirst.setFormat(CronFormat.UNIX);
        unixFormatFirst.setValue("0 9 * * 1-5");
        for (CronInput loaded : List.of(unixValueFirst, unixFormatFirst)) {
            assertEquals("0 9 * * 1-5", loaded.getValue());
            assertEquals(List.of(FieldMessage.info("At 09:00, Monday to Friday")), loaded.getInputMessages());
        }
    }

    @Test
    @DisplayName("an expression that cannot be converted without loss stays as typed and is checked against the new format")
    void notConvertibleStays() {
        input.setFormat(CronFormat.UNIX);
        input.getTextField().setText("0 0 1 * 1");

        choose(input, CronFormat.QUARTZ);
        assertEquals("0 0 1 * 1", input.getValue());
        assertEquals(List.of(FieldMessage.error("Quartz cron has 6 or 7 fields, found 5")), input.getInputMessages());

        input.setValue("0 0 0 L * ?");
        input.setFormat(CronFormat.UNIX);
        assertEquals("0 0 0 L * ?", input.getValue());
        assertEquals("Unix cron has 5 fields, found 6", input.getProblem());
    }

    @Test
    @DisplayName("an invalid or empty text is not converted: it stays as typed")
    void invalidOrEmptyStays() {
        input.setFormat(CronFormat.UNIX);
        input.getTextField().setText("0 9 * *");
        choose(input, CronFormat.QUARTZ);
        assertEquals("0 9 * *", input.getValue());
        assertEquals("Quartz cron has 6 or 7 fields, found 4", input.getProblem());

        input.setValue(" ");
        input.setFormat(CronFormat.UNIX);
        assertEquals(" ", input.getValue());
    }

    @Test
    @DisplayName("bound, nothing is converted: after unbinding the kept text is checked against the new format")
    void boundConvertsNothing() {
        input.setFormat(CronFormat.UNIX);
        input.setValue("0 9 * * 1-5");
        input.getBinding().setBoundName("schedule");

        choose(input, CronFormat.QUARTZ);
        assertEquals("0 9 * * 1-5", input.getValue());

        input.getBinding().setBoundName(null);
        assertEquals("0 9 * * 1-5", input.getValue());
        assertEquals(List.of(FieldMessage.error("Quartz cron has 6 or 7 fields, found 5")), input.getInputMessages());
    }

    @Test
    @DisplayName("the user's choice: a value bound one way stays, one bound both ways converts and its source gets it")
    void boundValueIsNotConverted() {
        StringProperty source = new SimpleStringProperty("0 9 * * 1-5");
        input.setFormat(CronFormat.UNIX);
        input.valueProperty().bind(source);

        choose(input, CronFormat.QUARTZ);
        assertEquals("0 9 * * 1-5", input.getValue());
        assertEquals("Quartz cron has 6 or 7 fields, found 5", input.getProblem());

        input.valueProperty().unbind();
        choose(input, CronFormat.UNIX);
        input.valueProperty().bindBidirectional(source);
        choose(input, CronFormat.QUARTZ);
        assertEquals("0 0 9 ? * MON-FRI", source.get());
        assertEquals("0 0 9 ? * MON-FRI", input.getValue());
    }

    // --- States and the shared keyboard -------------------------------------------------------------

    @Test
    @DisplayName("bound: the chip in place of the text, the text kept and hidden, red while the variable is missing")
    void boundShowsTheChip() {
        input.setValue("0 0 9 ? * MON-FRI");
        input.getBinding().setBindable(true);
        input.getBinding().setBoundName("schedule");
        FormTestSupport.layOut(input);

        assertTrue(FormTestSupport.hasPseudoClass(input, "bound"));
        assertEquals("schedule", chip(input).getText());
        assertTrue(chip(input).isVisible());
        assertFalse(input.getTextField().isEditable());
        input.getTextField().selectAll();
        assertEquals("", input.getTextField().getSelectedText(), "nothing of the hidden text is selected");

        input.getBinding().setMissing(true);
        assertTrue(FormTestSupport.hasPseudoClass(chip(input), "missing"));

        input.getBinding().setBoundName(null);
        assertTrue(input.getTextField().isEditable());
        assertEquals("0 0 9 ? * MON-FRI", input.getTextField().getText());
    }

    @Test
    @DisplayName("Ctrl+Space opens the binding list while {} shows, and Backspace or Delete unbind while bound")
    void sharedBindingKeys() {
        input.setValue("0 0 9 ? * MON-FRI");
        input.getBinding().setBindable(true);
        input.getBinding().getCandidates().add(new BindingCandidate("schedule", "String"));

        assertTrue(firesThrough(input, FormTestSupport.controlKeyPressed(KeyCode.SPACE)).isEmpty());
        assertEquals(1, input.getBindingMenu().getItems().size());
        input.getBindingMenu().getItems().getFirst().fire();
        assertEquals("schedule", input.getBinding().getBoundName());

        assertTrue(firesThrough(input, FormTestSupport.keyPressed(KeyCode.BACK_SPACE)).isEmpty());
        assertNull(input.getBinding().getBoundName());
        input.getBinding().setBoundName("schedule");
        assertTrue(firesThrough(input, FormTestSupport.keyPressed(KeyCode.DELETE)).isEmpty());
        assertNull(input.getBinding().getBoundName());
        assertEquals("0 0 9 ? * MON-FRI", input.getValue());
    }

    @Test
    @DisplayName("the selector, {} and the chip's x are out of the tab order: Tab passes the field in one step")
    void onlyTheTextIsInTheTabOrder() {
        input.getBinding().setBindable(true);
        input.getBinding().setBoundName("schedule");
        FormTestSupport.layOut(input);

        assertTrue(input.getTextField().isFocusTraversable());
        assertFalse(selector(input).isFocusTraversable());
        assertFalse(bindIcon(input).isFocusTraversable());
        assertFalse(input.lookup(".form-chip-remove").isFocusTraversable());
    }

    @Test
    @DisplayName("a request for the focus on the selector gives it to the text field, where the keyboard works")
    void selectorPassesTheFocusOn() {
        TextField other = new TextField();
        Scene scene = new Scene(new VBox(other, input));
        other.requestFocus();

        selector(input).requestFocus();

        assertSame(input.getTextField(), scene.getFocusOwner());
    }

    // --- In a form row -------------------------------------------------------------------------------

    @Test
    @DisplayName("in a row, the info line stands above the application's messages, and a due error colours the frame")
    void inARow() {
        FormField row = new FormField("Schedule", input);
        row.getMessages().add(FieldMessage.warning("The job runs on the server's time zone"));
        FormTestSupport.layOut(row);

        input.setValue("0 0 9 ? * MON-FRI");
        assertEquals(List.of("At 09:00:00, Monday to Friday", "The job runs on the server's time zone"),
                messageTexts(row));
        assertEquals(FieldState.WARNING, input.getState());

        input.setValue("0 0 9 * * MON-FRI");
        assertEquals(List.of("Day of month or weekday must be ?", "The job runs on the server's time zone"),
                messageTexts(row));
        assertEquals(FieldState.ERROR, input.getState());
        assertTrue(FormTestSupport.hasPseudoClass(input, "error"));

        input.setValue("");
        assertEquals(List.of("The job runs on the server's time zone"), messageTexts(row));
        assertEquals(FieldState.WARNING, input.getState());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** The texts of a row's messages, top to bottom: the input's own, then the application's. */
    private static List<String> messageTexts(FormField row) {
        List<String> texts = new ArrayList<>();
        for (Node message : row.lookupAll(".form-field-message")) {
            texts.add(((TextInputControl) message).getText());
        }
        return texts;
    }

    /** The input's format selector. */
    @SuppressWarnings("unchecked")
    private static ComboBox<CronFormat> selector(CronInput target) {
        return (ComboBox<CronFormat>) target.lookup(".form-cron-format");
    }

    /** The format as plain text, shown while the format is fixed or the input read-only. */
    private static Label formatText(CronInput target) {
        return (Label) target.lookup(".form-cron-format-text");
    }

    /**
     * Returns the selector's list: the combo box's list view, which its skin builds in a layout pass and shows only in a
     * window. Choosing an entry in it is what a click on the entry does.
     */
    @SuppressWarnings("unchecked")
    private static ListView<CronFormat> list(CronInput target) {
        FormTestSupport.layOut(target);
        ComboBoxListViewSkin<CronFormat> skin = (ComboBoxListViewSkin<CronFormat>) selector(target).getSkin();
        return (ListView<CronFormat>) skin.getPopupContent();
    }

    /** The user chooses a format in the selector's list. */
    private static void choose(CronInput target, CronFormat chosen) {
        list(target).getSelectionModel().select(chosen);
    }

    /** The node in the selector that shows the current format's name. */
    private static Labeled shownNameNode(CronInput target) {
        for (Node node : selector(target).lookupAll(".label")) {
            if (node instanceof Labeled labeled && labeled.isVisible()) {
                return labeled;
            }
        }
        throw new AssertionError("no shown name");
    }

    /** The format's name the selector shows. */
    private static String shownFormat(CronInput target) {
        return shownNameNode(target).getText();
    }

    /** Types a text into the input's text field, as the keyboard does. */
    private static void typeInto(CronInput target, String text) {
        target.getTextField().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, text, "", KeyCode.UNDEFINED,
                false, false, false, false));
    }

    /** A cron input that counts the times its format list would show, without a window: the tests' seam. */
    private static final class RecordingCronInput extends CronInput {

        private int listsShown;

        @Override
        void showFormatList() {
            listsShown++;
        }
    }
}
