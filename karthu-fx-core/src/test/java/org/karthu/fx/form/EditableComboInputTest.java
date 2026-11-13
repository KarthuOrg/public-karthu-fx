package org.karthu.fx.form;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;
import org.karthu.fx.theme.ThemeManager;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.altKeyPressed;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.controlKeyPressed;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;

/**
 * The editable combo input through its public API, without a window: its value, the text the user types and the
 * suggestions it filters, its list built but never shown (its entries and what choosing one does), its chevron, the
 * keys that open, step through and close the list, and the bound, read-only and disabled states.
 */
class EditableComboInputTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a new input: a shell with form-editable-combo-input around an editable combo box, its focus target")
    void newInput() {
        EditableComboInput input = new EditableComboInput();
        Scene scene = new Scene(new VBox(new TextField(), input));

        assertTrue(input.getStyleClass().contains("form-frame"));
        assertTrue(input.getStyleClass().contains("form-editable-combo-input"));
        ComboBox<?> combo = assertInstanceOf(ComboBox.class, input.getFocusTarget());
        assertTrue(combo.isEditable());
        assertTrue(combo.getStyleClass().contains("form-shell-content"));
        assertEquals("", input.getValue());
        assertEquals("", input.getPromptText());
        assertTrue(input.getSuggestions().isEmpty());
        assertSame(input, input.getNode());
        assertSame(input.valueProperty(), ((FormInput<String>) input).valueProperty());

        input.requestFocus();
        assertSame(combo, scene.getFocusOwner());
    }

    @Test
    @DisplayName("the value is the editor's text, both ways, exactly as it is")
    void valueIsTheEditorsText() {
        EditableComboInput input = new EditableComboInput();
        layOut(input);

        input.setValue("  my value ");
        assertEquals("  my value ", editor(input).getText());

        editor(input).setText("UTF-8");
        assertEquals("UTF-8", input.getValue());

        input.setValue(null);
        assertEquals("", editor(input).getText());
        assertNull(input.getValue());
    }

    @Test
    @DisplayName("the prompt text shows in the empty editor")
    void promptText() {
        EditableComboInput input = new EditableComboInput();
        input.setPromptText("Encoding, such as UTF-8");
        layOut(input);

        assertEquals("Encoding, such as UTF-8", editor(input).getPromptText());
        input.setPromptText(null);
        assertEquals("", editor(input).getPromptText());
    }

    // --- Typing filters the suggestions --------------------------------------------------------------

    @Test
    @DisplayName("typing opens the list with the suggestions that contain the text, ignoring case; nothing is filled in")
    void typingOpensTheFilteredList() {
        RecordingInput input = recordingInput();

        type(input, "utf");

        assertEquals("utf", input.getValue());
        assertEquals("utf", editor(input).getText());
        assertEquals(List.of("UTF-8", "UTF-16"), entryTexts(list(input)));
        assertEquals(1, input.listsShown);
        assertTrue(input.showing);

        type(input, "-1");
        assertEquals("utf-1", input.getValue());
        assertEquals(List.of("UTF-16"), entryTexts(list(input)));
        assertEquals(1, input.listsShown);
    }

    @Test
    @DisplayName("deleting filters too: the empty text offers every suggestion; a text none contains closes the list")
    void deletingFiltersAndNoMatchCloses() {
        RecordingInput input = recordingInput();
        type(input, "u");

        firesThrough(input, keyPressed(KeyCode.BACK_SPACE));
        assertEquals("", input.getValue());
        assertEquals(ENCODINGS, entryTexts(list(input)));
        assertTrue(input.showing);

        type(input, "utf-32");
        assertEquals("utf-32", input.getValue());
        assertEquals(List.of(), entryTexts(list(input)));
        assertFalse(input.showing);

        firesThrough(input, keyPressed(KeyCode.BACK_SPACE));
        firesThrough(input, keyPressed(KeyCode.BACK_SPACE));
        assertEquals("utf-", input.getValue());
        assertEquals(List.of("UTF-8", "UTF-16"), entryTexts(list(input)));
        assertTrue(input.showing);
        assertEquals(2, input.listsShown);
    }

    @Test
    @DisplayName("without suggestions typing opens no list; a value set by the application opens none either")
    void noListWithoutSuggestionsOrForTheApplication() {
        RecordingInput input = recordingInput();
        input.setValue("ISO");
        assertEquals(0, input.listsShown);

        input.getSuggestions().clear();
        editor(input).end();
        type(input, "x");
        assertEquals("ISOx", input.getValue());
        assertEquals(0, input.listsShown);
    }

    @Test
    @DisplayName("the list follows the suggestions when the application changes them, filtered by the last text")
    void listFollowsTheSuggestions() {
        RecordingInput input = recordingInput();
        type(input, "utf");

        input.getSuggestions().add("UTF-32");
        assertEquals(List.of("UTF-8", "UTF-16", "UTF-32"), entryTexts(list(input)));
        input.getSuggestions().remove("UTF-8");
        assertEquals(List.of("UTF-16", "UTF-32"), entryTexts(list(input)));
        assertEquals("utf", input.getValue());
    }

    // --- Opening the full list -----------------------------------------------------------------------

    @Test
    @DisplayName("the chevron, Alt+Down and F4 open the full list, also after typing, and keep the text")
    void chevronAndAltDownOpenTheFullList() {
        RecordingInput input = recordingInput();
        input.setValue("utf");
        editor(input).end();

        chevron(input).fire();
        assertEquals(1, input.listsShown);
        assertEquals(ENCODINGS, entryTexts(list(input)));
        assertEquals("utf", input.getValue());

        input.showing = false;
        // The combo box took the focus, and JavaFX's text field selects its text then: the caret goes to its end.
        editor(input).end();
        type(input, "-1");
        assertEquals(List.of("UTF-16"), entryTexts(list(input)));
        input.showing = false;
        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        assertEquals(3, input.listsShown);
        assertEquals(ENCODINGS, entryTexts(list(input)));

        input.showing = false;
        firesThrough(input, keyReleased(KeyCode.F4));
        assertEquals(4, input.listsShown);
        assertEquals("utf-1", input.getValue());
    }

    @Test
    @DisplayName("a click on the chevron whose press closed the open list only closes it")
    void clickThatClosedTheListOpensNone() throws InterruptedException {
        RecordingInput input = recordingInput();
        Button chevron = chevron(input);

        onFxThread(() -> {
            input.listClosed();
            chevron.fireEvent(mouse(MouseEvent.MOUSE_PRESSED));
            chevron.fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
        });
        assertEquals(0, input.listsShown);

        onFxThread(() -> {
            chevron.fireEvent(mouse(MouseEvent.MOUSE_PRESSED));
            chevron.fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
        });
        assertEquals(1, input.listsShown);
    }

    // --- The open list -------------------------------------------------------------------------------

    @Test
    @DisplayName("in the open list Up and Down move the highlight and write nothing; Enter chooses the highlighted one")
    void upAndDownHighlightEnterChooses() {
        RecordingInput input = recordingInput();
        type(input, "utf");
        ListView<String> list = popupList(input);
        assertEquals(-1, list.getSelectionModel().getSelectedIndex());

        assertTrue(firesAtPopupList(list, keyPressed(KeyCode.DOWN)).isEmpty());
        assertEquals("UTF-8", list.getSelectionModel().getSelectedItem());
        firesAtPopupList(list, keyPressed(KeyCode.DOWN));
        firesAtPopupList(list, keyPressed(KeyCode.DOWN));
        assertEquals("UTF-16", list.getSelectionModel().getSelectedItem());
        firesAtPopupList(list, keyPressed(KeyCode.UP));
        assertEquals("UTF-8", list.getSelectionModel().getSelectedItem());
        assertEquals("utf", input.getValue());
        assertEquals("utf", editor(input).getText());

        firesAtPopupList(list, keyPressed(KeyCode.DOWN));
        assertTrue(firesAtPopupList(list, keyPressed(KeyCode.ENTER)).isEmpty());
        assertEquals("UTF-16", input.getValue());
        assertEquals("UTF-16", editor(input).getText());
        assertEquals(6, editor(input).getCaretPosition());
        assertFalse(input.showing);
        assertEquals(1, input.listsShown);
    }

    @Test
    @DisplayName("typing again after a highlight starts without one: Enter then keeps the typed text")
    void typingClearsTheHighlight() {
        RecordingInput input = recordingInput();
        type(input, "u");
        ListView<String> list = popupList(input);
        firesAtPopupList(list, keyPressed(KeyCode.DOWN));
        assertEquals("UTF-8", list.getSelectionModel().getSelectedItem());

        type(input, "t");
        assertEquals(-1, list.getSelectionModel().getSelectedIndex());
        firesAtPopupList(list, keyPressed(KeyCode.DOWN));
        assertEquals("UTF-8", list.getSelectionModel().getSelectedItem());

        type(input, "f");
        assertTrue(firesAtPopupList(list, keyPressed(KeyCode.ENTER)).isEmpty());
        assertFalse(input.showing);
        assertEquals("utf", input.getValue());
    }

    @Test
    @DisplayName("Escape in the open list keeps the text, also after a highlight; it goes no further")
    void escapeKeepsTheText() throws InterruptedException {
        RecordingInput input = recordingInput();
        type(input, "utf");
        ListView<String> list = popupList(input);
        firesAtPopupList(list, keyPressed(KeyCode.DOWN));
        List<Event> passed = new ArrayList<>();

        // The list closes its combo box on Escape, which JavaFX allows on its thread only.
        onFxThread(() -> passed.addAll(firesAtPopupList(list, keyPressed(KeyCode.ESCAPE))));

        assertTrue(passed.isEmpty());

        assertEquals("utf", input.getValue());
        assertEquals("utf", editor(input).getText());
    }

    @Test
    @DisplayName("a click on a suggestion chooses it: it becomes the text and the list closes")
    void clickChoosesASuggestion() {
        RecordingInput input = recordingInput();
        type(input, "-1");
        ListView<String> list = list(input);
        assertEquals(List.of("UTF-16", "ISO-8859-1", "windows-1252"), entryTexts(list));

        cell(list, 2).fireEvent(mouse(MouseEvent.MOUSE_RELEASED));

        assertEquals("windows-1252", input.getValue());
        assertEquals("windows-1252", editor(input).getText());
        assertFalse(input.showing);
        assertEquals(1, input.listsShown);
    }

    @Test
    @DisplayName("the list's highlight, moved as a click on an entry moves it, writes nothing into the field")
    void highlightWritesNothing() {
        RecordingInput input = recordingInput();
        type(input, "utf");

        list(input).getSelectionModel().select(1);
        list(input).getSelectionModel().select("UTF-8");

        assertEquals("utf", input.getValue());
        assertEquals("utf", editor(input).getText());
    }

    @Test
    @DisplayName("with the list closed, Up, Down and Enter are the editor's: they choose nothing and open nothing")
    void keysOfTheClosedList() {
        RecordingInput input = recordingInput();
        input.setValue("UTF");

        firesThrough(input, keyPressed(KeyCode.DOWN));
        firesThrough(input, keyPressed(KeyCode.UP));
        firesThrough(input, keyPressed(KeyCode.ENTER));

        assertEquals("UTF", input.getValue());
        assertEquals(0, input.listsShown);
    }

    @Test
    @DisplayName("a list that shows closes when the field loses the focus")
    void losingTheFocusClosesTheList() {
        RecordingInput input = recordingInput();
        TextField other = new TextField();
        new Scene(new VBox(input, other));
        input.setValue("utf");
        input.requestFocus();
        input.showing = true;

        other.requestFocus();

        assertFalse(input.showing);
        assertEquals("utf", input.getValue());
    }

    @Test
    @DisplayName("a value the application bound is not changed by typing or a choice: the field goes back to it")
    void boundValuePropertyIsNotChanged() {
        RecordingInput input = recordingInput();
        StringProperty source = new SimpleStringProperty("UTF-8");
        input.valueProperty().bind(source);
        assertEquals("UTF-8", editor(input).getText());

        editor(input).end();
        type(input, "x");
        assertEquals("UTF-8", input.getValue());
        assertEquals("UTF-8", editor(input).getText());

        chevron(input).fire();
        cell(list(input), 2).fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
        assertEquals("UTF-8", editor(input).getText());

        source.set("US-ASCII");
        assertEquals("US-ASCII", editor(input).getText());
    }

    // --- The divider and the chevron -----------------------------------------------------------------

    @Test
    @DisplayName("a divider and a chevron down sit right before {}, styled as the shell's, out of the tab order")
    void dividerAndChevronBeforeBind() {
        EditableComboInput input = new EditableComboInput();
        input.getBinding().setBindable(true);
        input.setPrefWidth(240);
        layOut(input);

        Region divider = (Region) input.lookup(".form-shell-divider");
        Button chevron = chevron(input);
        assertTrue(chevron.getStyleClass().contains("form-frame-icon"));
        assertTrue(chevron.getStyleClass().contains("icon-button"));
        assertFalse(chevron.isFocusTraversable());
        assertFalse(divider.isFocusTraversable());
        FontIcon glyph = assertInstanceOf(FontIcon.class, chevron.getGraphic());
        assertEquals(BootstrapIcons.CHEVRON_DOWN, glyph.getIconCode());
        List<Node> children = input.getChildrenUnmodifiable();
        int bind = children.indexOf(bindIcon(input));
        assertEquals(bind - 1, children.indexOf(chevron));
        assertEquals(bind - 2, children.indexOf(divider));
        assertEquals(1, divider.getWidth(), 0.01);
        assertTrue(input.getFocusTarget().getBoundsInParent().getMaxX() <= divider.getBoundsInParent().getMinX());
        assertTrue(divider.getBoundsInParent().getMaxX() <= chevron.getBoundsInParent().getMinX());
        assertTrue(chevron.getBoundsInParent().getMaxX() <= bindIcon(input).getBoundsInParent().getMinX());
    }

    // --- Bound ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("bound: the chip over the hidden text, the chevron disabled, no list; the text shows after unbinding")
    void boundKeepsTheText() {
        RecordingInput input = recordingInput();
        input.setValue("UTF-8");
        input.getBinding().setBindable(true);
        input.getBinding().getCandidates().add(new BindingCandidate("encoding"));
        input.getBinding().setBoundName("encoding");

        assertTrue(hasPseudoClass(input, "bound"));
        assertTrue(chip(input).isVisible());
        assertNotNull(input.getFocusTarget().getClip());
        assertTrue(chevron(input).isDisable());
        assertTrue(input.lookup(".form-shell-divider").isVisible());
        chevron(input).fire();
        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        type(input, "x");
        assertEquals(0, input.listsShown);
        assertEquals("UTF-8", input.getValue());

        assertTrue(firesThrough(input, controlKeyPressed(KeyCode.SPACE)).isEmpty());
        assertEquals(3, input.getBindingMenu().getItems().size());

        input.setValue("US-ASCII");
        assertTrue(firesThrough(input, keyPressed(KeyCode.BACK_SPACE)).isEmpty());
        assertNull(input.getBinding().getBoundName());
        assertInstanceOf(ComboBox.class, input.getFocusTarget());
        assertNull(input.getFocusTarget().getClip());
        assertFalse(chevron(input).isDisable());
        assertEquals("US-ASCII", editor(input).getText());
        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        assertEquals(1, input.listsShown);
    }

    @Test
    @DisplayName("bound: the hidden text can be neither selected nor copied")
    void boundHandsNothingOut() {
        EditableComboInput input = new EditableComboInput();
        input.setValue("UTF-8");
        RecordingClipboard clipboard = new RecordingClipboard(null);
        input.setClipboard(clipboard);
        input.getBinding().setBoundName("encoding");
        TextField text = (TextField) input.getFocusTarget();

        text.selectAll();
        text.copy();
        text.cut();

        assertEquals("", text.getSelectedText());
        assertTrue(clipboard.copied().isEmpty());
        assertEquals("UTF-8", input.getValue());
    }

    @Test
    @DisplayName("becoming bound, read-only or disabled closes a list that shows")
    void listClosesWhenTheUserMayNoLongerChoose() {
        RecordingInput bound = recordingInput();
        bound.showing = true;
        bound.getBinding().setBoundName("encoding");
        assertFalse(bound.showing);

        RecordingInput readOnly = recordingInput();
        readOnly.showing = true;
        readOnly.setReadOnly(true);
        assertFalse(readOnly.showing);

        RecordingInput disabled = recordingInput();
        disabled.showing = true;
        disabled.setDisable(true);
        assertFalse(disabled.showing);
    }

    @Test
    @DisplayName("disabled: neither the chevron nor Alt+Down opens the list")
    void disabledOpensNoList() {
        RecordingInput input = recordingInput();
        input.setDisable(true);

        chevron(input).fire();
        firesThrough(input, altKeyPressed(KeyCode.DOWN));

        assertEquals(0, input.listsShown);
    }

    // --- Read-only -----------------------------------------------------------------------------------

    @Test
    @DisplayName("read-only: the text to select, no divider, chevron or {}, no list; all back when it ends")
    void readOnlyShowsSelectableText() {
        RecordingInput input = recordingInput();
        input.setValue("ISO-8859-1");
        input.getBinding().setBindable(true);

        input.setReadOnly(true);
        TextField text = assertInstanceOf(TextField.class, input.getFocusTarget());
        assertTrue(text.getStyleClass().contains("form-combo-text"));
        assertTrue(text.getStyleClass().contains("form-shell-content"));
        assertSame(text, input.getChildrenUnmodifiable().getFirst());
        assertFalse(text.isEditable());
        assertEquals("ISO-8859-1", text.getText());
        text.selectRange(0, 3);
        assertEquals("ISO", text.getSelectedText());
        assertFalse(input.lookup(".form-shell-divider").isManaged());
        assertFalse(input.lookup(".form-shell-divider").isVisible());
        assertFalse(chevron(input).isVisible());
        assertFalse(chevron(input).isManaged());
        assertFalse(bindIcon(input).isVisible());

        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        type(input, "x");
        assertEquals(0, input.listsShown);
        assertEquals("ISO-8859-1", input.getValue());

        input.setValue("UTF-8");
        assertEquals("UTF-8", text.getText());

        input.setReadOnly(false);
        assertInstanceOf(ComboBox.class, input.getFocusTarget());
        assertFalse(input.getChildrenUnmodifiable().contains(text));
        assertTrue(chevron(input).isVisible());
        assertTrue(input.lookup(".form-shell-divider").isVisible());
        assertTrue(bindIcon(input).isVisible());
        assertEquals("UTF-8", editor(input).getText());
    }

    @Test
    @DisplayName("read-only: the selected text is copied, nothing is cut or pasted; the prompt shows without a text")
    void readOnlyCopies() {
        EditableComboInput input = new EditableComboInput();
        input.setValue("UTF-8");
        input.setPromptText("Encoding");
        RecordingClipboard clipboard = new RecordingClipboard("pasted");
        input.setClipboard(clipboard);
        input.setReadOnly(true);
        TextField text = (TextField) input.getFocusTarget();

        text.selectAll();
        text.copy();
        text.cut();
        text.paste();

        assertEquals(List.of("UTF-8", "UTF-8"), clipboard.copied());
        assertEquals("UTF-8", text.getText());
        assertEquals("UTF-8", input.getValue());
        assertEquals("Encoding", text.getPromptText());
    }

    @Test
    @DisplayName("read-only and bound: the chip without its x over the hidden text")
    void readOnlyAndBound() {
        EditableComboInput input = new EditableComboInput();
        input.setValue("UTF-8");
        input.getBinding().setBoundName("encoding");
        input.setReadOnly(true);

        assertTrue(chip(input).isVisible());
        assertNull(chip(input).getGraphic());
        assertNotNull(input.getFocusTarget().getClip());

        input.getBinding().setBoundName(null);
        assertNull(input.getFocusTarget().getClip());
        assertEquals("UTF-8", ((TextField) input.getFocusTarget()).getText());
    }

    @Test
    @DisplayName("read-only and binding move the focus with the content: from the combo box to the text, and back")
    void focusMovesWithTheContent() {
        EditableComboInput input = new EditableComboInput();
        input.setValue("UTF-8");
        Scene scene = new Scene(new VBox(new TextField(), input));
        input.requestFocus();

        input.setReadOnly(true);
        assertSame(input.getFocusTarget(), scene.getFocusOwner());
        assertInstanceOf(TextField.class, scene.getFocusOwner());
        input.setReadOnly(false);
        assertSame(input.getFocusTarget(), scene.getFocusOwner());
        assertInstanceOf(ComboBox.class, scene.getFocusOwner());

        input.getBinding().setBoundName("encoding");
        assertSame(input.getFocusTarget(), scene.getFocusOwner());
        input.getBinding().setBoundName(null);
        assertInstanceOf(ComboBox.class, scene.getFocusOwner());
    }

    @Test
    @DisplayName("a row's label names the input whether it is read-only or not")
    void rowLabelNamesEitherContent() {
        EditableComboInput input = new EditableComboInput();
        new FormField("Encoding", input);
        assertEquals("Encoding", input.getFocusTarget().getAccessibleText());

        input.setReadOnly(true);
        assertEquals("Encoding", input.getFocusTarget().getAccessibleText());

        EditableComboInput readOnlyFirst = new EditableComboInput();
        readOnlyFirst.setReadOnly(true);
        new FormField("Encoding", readOnlyFirst);
        readOnlyFirst.setReadOnly(false);
        assertEquals("Encoding", readOnlyFirst.getFocusTarget().getAccessibleText());
    }

    // --- States and look -----------------------------------------------------------------------------

    @Test
    @DisplayName("error and warning show on the shell; a missing variable's chip is :missing")
    void statesShowOnTheShell() {
        EditableComboInput input = new EditableComboInput();
        input.setValue("UTF-8");

        input.setState(FieldState.ERROR);
        assertTrue(hasPseudoClass(input, "error"));
        input.setState(FieldState.WARNING);
        assertTrue(hasPseudoClass(input, "warning"));
        assertFalse(hasPseudoClass(input, "error"));

        input.getBinding().setBoundName("deletedVariable");
        input.getBinding().setMissing(true);
        assertTrue(hasPseudoClass(chip(input), "missing"));
        assertEquals("UTF-8", input.getValue());
    }

    @Test
    @DisplayName("disabled: the field is dimmed once, as a disabled button; the combo box and the chevron not again")
    void disabledIsDimmedOnce() {
        EditableComboInput input = filledInput();
        input.setDisable(true);
        Button button = new Button("button");
        button.setDisable(true);
        layOut(input, button);

        assertEquals(button.getOpacity(), input.getOpacity(), 0.001);
        assertEquals(1, input.getFocusTarget().getOpacity(), 0.001);
        assertEquals(1, editor(input).getOpacity(), 0.001);
        assertEquals(1, chevron(input).getOpacity(), 0.001);
    }

    @Test
    @DisplayName("as high as a plain text field, also empty, read-only and bound")
    void asHighAsATextField() {
        TextField plain = new TextField("plain");
        EditableComboInput filled = filledInput();
        EditableComboInput empty = new EditableComboInput();
        EditableComboInput readOnly = filledInput();
        readOnly.setReadOnly(true);
        EditableComboInput bound = filledInput();
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName("encoding");
        layOut(plain, filled, empty, readOnly, bound);

        assertEquals(plain.getHeight(), filled.getHeight(), 0.01);
        assertEquals(plain.getHeight(), empty.getHeight(), 0.01);
        assertEquals(plain.getHeight(), readOnly.getHeight(), 0.01);
        assertEquals(plain.getHeight(), bound.getHeight(), 0.01);
    }

    @Test
    @DisplayName("inside the shell the combo box and its editor have no frame, background or arrow; the text sits as a"
            + " text field's")
    void comboBoxLosesItsOwnLook() {
        EditableComboInput input = filledInput();
        input.setPrefWidth(240);
        TextField plain = new TextField("plain");
        layOut(input, plain);
        Region combo = (Region) input.getFocusTarget();

        assertTrue(isTransparent(combo));
        assertTrue(isTransparent(editor(input)));
        combo.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        combo.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
        combo.applyCss();
        assertTrue(isTransparent(combo));
        assertEquals(0, ((Region) combo.lookup(".arrow-button")).getWidth(), 0.01);
        assertEquals(input.lookup(".form-shell-divider").getBoundsInParent().getMinX(),
                combo.getBoundsInParent().getMaxX(), 1.01);
        assertEquals(plain.getPadding(), editor(input).getPadding());
        assertEquals(0, editor(input).getBoundsInParent().getMinX(), 0.01);
    }

    @Test
    @DisplayName("the pointer over the text is a text cursor, as in a text field, never the combo box's hand")
    void textCursor() {
        EditableComboInput input = filledInput();
        TextField plain = new TextField("plain");
        layOut(input, plain);

        assertEquals(plain.getCursor(), editor(input).getCursor());
        assertNotEquals(Cursor.HAND, input.getCursor());
    }

    @Test
    @DisplayName("no focus border without the focus, also with the list's entries measured in the field")
    void noFocusBorderWithoutTheFocus() {
        EditableComboInput input = filledInput();
        input.getSuggestions().addAll(ENCODINGS);
        input.setPrefWidth(360);
        layOut(input);
        layOut(input);

        assertFalse(input.isFocusWithin());
        ListView<String> list = list(input);
        list.getSelectionModel().select(1);
        assertFalse(input.isFocusWithin());
        ListCell<String> entry = cell(list, 2);
        list.getFocusModel().focus(2);
        entry.updateIndex(2);
        assertFalse(entry.isFocused());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** An input with a text, without suggestions. */
    private static EditableComboInput filledInput() {
        EditableComboInput input = new EditableComboInput();
        input.setValue("UTF-8");
        return input;
    }

    /** Whether a region draws no background: none at all, or only transparent layers. */
    private static boolean isTransparent(Region region) {
        if (region.getBackground() == null) {
            return true;
        }
        for (BackgroundFill layer : region.getBackground().getFills()) {
            if (!Color.TRANSPARENT.equals(layer.getFill())) {
                return false;
            }
        }
        return true;
    }

    /** The suggestions of the recording input, in their order. */
    private static final List<String> ENCODINGS = List.of("UTF-8", "UTF-16", "ISO-8859-1", "windows-1252", "US-ASCII");

    /**
     * An input with five encodings as suggestions that records each time its list would show, as a popup shows only in
     * a window. Its skin is built, so that keys reach the combo box's editor and behaviour.
     */
    private static RecordingInput recordingInput() {
        RecordingInput input = new RecordingInput();
        input.getSuggestions().addAll(ENCODINGS);
        layOut(input);
        return input;
    }

    /** Types the text into the input, a key typed per character at its focus target, as the keyboard does. */
    private static void type(EditableComboInput input, String text) {
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            firesThrough(input, new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED,
                    false, false, false, false));
        }
    }

    /** The editor of the input's combo box, where the user types. */
    @SuppressWarnings("unchecked")
    private static TextField editor(EditableComboInput input) {
        return ((ComboBox<String>) input.getFocusTarget()).getEditor();
    }

    /** The input's chevron. */
    private static Button chevron(EditableComboInput input) {
        return (Button) input.lookup(".form-combo-chevron");
    }

    /**
     * Returns the input's list: the combo box's list view, which its skin builds in a layout pass and shows only in a
     * window.
     */
    @SuppressWarnings("unchecked")
    private static ListView<String> list(EditableComboInput input) {
        ComboBox<String> combo = (ComboBox<String>) input.getContent();
        ComboBoxListViewSkin<String> skin = (ComboBoxListViewSkin<String>) combo.getSkin();
        return (ListView<String>) skin.getPopupContent();
    }

    /**
     * Returns the input's list as its popup holds it: in a scene of its own with the theme, out of the combo box, so
     * that keys reach it as JavaFX's popup hands them, before the field sees them.
     */
    private static ListView<String> popupList(EditableComboInput input) {
        ListView<String> list = list(input);
        if (!(list.getParent() instanceof StackPane)) {
            StackPane popupRoot = new StackPane(list);
            Scene popupScene = new Scene(popupRoot);
            ThemeManager.apply(popupScene);
            popupRoot.applyCss();
            popupRoot.layout();
        }
        return list;
    }

    /**
     * Fires an event at the popup's list, as the popup hands the window's keys to it, inside the popup's root, which
     * records what passes the list unconsumed.
     *
     * @return the events of the event's type that passed the list, empty when it consumed the event
     */
    private static List<Event> firesAtPopupList(ListView<String> list, Event event) {
        List<Event> passed = new ArrayList<>();
        Parent root = list.getParent();
        EventHandler<Event> recorder = passed::add;
        root.addEventHandler(event.getEventType(), recorder);
        list.fireEvent(event);
        root.removeEventHandler(event.getEventType(), recorder);
        return passed;
    }

    /** A cell of the list's own cell factory, showing the entry at the index, as the popup shows it. */
    private static ListCell<String> cell(ListView<String> list, int index) {
        ListCell<String> cell = list.getCellFactory().call(list);
        cell.updateListView(list);
        cell.updateIndex(index);
        return cell;
    }

    /** The texts the list's entries show, in order, each from a cell of the list's own cell factory. */
    private static List<String> entryTexts(ListView<String> list) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < list.getItems().size(); i++) {
            texts.add(cell(list, i).getText());
        }
        return texts;
    }

    /**
     * Runs an action on the JavaFX Application Thread and waits for it, as a sequence of events within one turn of
     * the thread; what the action queues with {@code Platform.runLater} runs after it.
     */
    private static void onFxThread(Runnable action) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable thrown) {
                failure.set(thrown);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run the action");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    /** A key release without modifiers, as F4 opens a combo box's list on its release. */
    private static KeyEvent keyReleased(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false);
    }

    /** A mouse event of the primary button at the top left, still since its press. */
    private static MouseEvent mouse(EventType<MouseEvent> type) {
        return new MouseEvent(type, 2, 2, 2, 2, MouseButton.PRIMARY, 1, false, false, false, false,
                true, false, false, false, false, true, null);
    }

    /**
     * An editable combo input that records each time its list would show, and whether it shows, without a window: the
     * tests' seam.
     */
    private static final class RecordingInput extends EditableComboInput {

        private int listsShown;
        private boolean showing;

        @Override
        void showList() {
            listsShown++;
            showing = true;
        }

        @Override
        void hideList() {
            if (showing) {
                showing = false;
                listClosed();
            }
        }

        @Override
        boolean isListShowing() {
            return showing;
        }
    }
}
