package org.karthu.fx.form;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.altKeyPressed;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.controlKeyPressed;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The combo input through its public API, without a window: its value and what it shows, its list built but never
 * shown (its entries and what choosing one does), its chevron, the keys and clicks that open the list, and the bound,
 * read-only and disabled states.
 */
class ComboInputTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a new input: a shell with form-combo-input around a non-editable combo box, its focus target, no value")
    void newInput() {
        ComboInput<String> input = new ComboInput<>();
        Scene scene = new Scene(new VBox(new TextField(), input));

        assertTrue(input.getStyleClass().contains("form-frame"));
        assertTrue(input.getStyleClass().contains("form-combo-input"));
        ComboBox<?> combo = assertInstanceOf(ComboBox.class, input.getFocusTarget());
        assertFalse(combo.isEditable());
        assertTrue(combo.getStyleClass().contains("form-shell-content"));
        assertNull(input.getValue());
        assertTrue(input.getItems().isEmpty());
        assertSame(input, input.getNode());
        assertSame(input.valueProperty(), ((FormInput<String>) input).valueProperty());

        input.requestFocus();
        assertSame(combo, scene.getFocusOwner());
    }

    // --- What the field shows ------------------------------------------------------------------------

    @Test
    @DisplayName("the field shows the value's display text: by default its toString, else the display function's")
    void showsTheDisplayText() {
        ComboInput<Integer> input = new ComboInput<>();
        input.getItems().addAll(1, 2, 3);
        input.setValue(2);
        layOut(input);
        assertEquals("2", shownText(input));
        assertFalse(hasPseudoClass(display(input), "prompt"));

        input.setDisplayText(level -> "Level " + level);
        assertEquals("Level 2", shownText(input));

        input.setValue(3);
        assertEquals("Level 3", shownText(input));

        input.setDisplayText(null);
        assertEquals("3", shownText(input));
        input.setDisplayText(level -> null);
        assertEquals("", shownText(input));
    }

    @Test
    @DisplayName("a value the application bound is not changed by a choice: the field goes back to it")
    void boundValuePropertyIsNotChanged() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        ObjectProperty<String> source = new SimpleObjectProperty<>("UTF-8");
        input.valueProperty().bind(source);
        ListView<String> list = list(input);

        list.getSelectionModel().select(1);

        assertEquals("UTF-8", input.getValue());
        assertEquals("UTF-8", shownText(input));
        source.set("ISO-8859-1");
        assertEquals("ISO-8859-1", shownText(input));
    }

    @Test
    @DisplayName("without a value the field shows the prompt text, Select a value by default, as :prompt")
    void showsThePromptWithoutAValue() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        layOut(input);

        assertEquals("Select a value", input.getPromptText());
        assertEquals("Select a value", shownText(input));
        assertTrue(hasPseudoClass(display(input), "prompt"));

        input.setPromptText("Choose an encoding");
        assertEquals("Choose an encoding", shownText(input));

        input.setValue("UTF-8");
        input.setValue(null);
        assertEquals("Choose an encoding", shownText(input));
        assertTrue(hasPseudoClass(display(input), "prompt"));
    }

    @Test
    @DisplayName("a value that is not in the list is shown with its display text, and kept")
    void showsAValueNotInTheList() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        input.setDisplayText(code -> "<" + code + ">");
        input.setValue("windows-1252");
        layOut(input);

        assertEquals("<windows-1252>", shownText(input));
        assertEquals("windows-1252", input.getValue());
    }

    // --- The list, built but not shown ---------------------------------------------------------------

    @Test
    @DisplayName("the list holds the items in their order, each with its display text")
    void listHoldsTheItems() {
        ComboInput<Integer> input = new ComboInput<>();
        input.getItems().addAll(3, 1, 2);
        input.setDisplayText(level -> "Level " + level);

        assertEquals(List.of("Level 3", "Level 1", "Level 2"), entryTexts(list(input)));

        input.getItems().remove(Integer.valueOf(1));
        assertEquals(List.of("Level 3", "Level 2"), entryTexts(list(input)));
    }

    @Test
    @DisplayName("choosing an entry in the list makes it the value, shown in the field")
    void choosingAnEntrySetsTheValue() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1", "windows-1252");
        ListView<String> list = list(input);

        list.getSelectionModel().select(1);
        assertEquals("ISO-8859-1", input.getValue());
        assertEquals("ISO-8859-1", shownText(input));

        list.getSelectionModel().select(2);
        assertEquals("windows-1252", input.getValue());
    }

    @Test
    @DisplayName("the empty entry: off by default; allowed, the list starts with (none), which sets no value")
    void emptyEntry() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        input.setValue("UTF-8");
        assertFalse(input.isEmptyAllowed());
        assertEquals("(none)", input.getEmptyText());
        assertEquals(List.of("UTF-8", "ISO-8859-1"), entryTexts(list(input)));

        input.setEmptyAllowed(true);
        ListView<String> list = list(input);
        assertEquals(List.of("(none)", "UTF-8", "ISO-8859-1"), entryTexts(list));
        assertEquals("UTF-8", input.getValue());

        list.getSelectionModel().select(0);
        assertNull(input.getValue());
        assertEquals("Select a value", shownText(input));

        input.setEmptyText("(default)");
        assertEquals(List.of("(default)", "UTF-8", "ISO-8859-1"), entryTexts(list(input)));
        assertEquals(List.of("UTF-8", "ISO-8859-1"), input.getItems());
    }

    @Test
    @DisplayName("the value stays when the application changes the list, also when it is no longer in it")
    void valueSurvivesListChanges() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        input.setValue("ISO-8859-1");
        layOut(input);

        input.getItems().remove("ISO-8859-1");
        assertEquals("ISO-8859-1", input.getValue());
        assertEquals("ISO-8859-1", shownText(input));

        input.getItems().setAll("US-ASCII", "UTF-16");
        input.setEmptyAllowed(true);
        assertEquals("ISO-8859-1", input.getValue());

        input.getItems().add("ISO-8859-1");
        list(input).getSelectionModel().select(1);
        assertEquals("US-ASCII", input.getValue());
    }

    // --- The chevron ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a chevron down sits right before {}, styled as an icon, out of the tab order, with no divider")
    void chevronBeforeBind() {
        ComboInput<String> input = new ComboInput<>();
        input.getBinding().setBindable(true);
        input.setPrefWidth(240);
        layOut(input);

        Button chevron = chevron(input);
        assertTrue(chevron.getStyleClass().contains("form-frame-icon"));
        assertTrue(chevron.getStyleClass().contains("icon-button"));
        assertFalse(chevron.isFocusTraversable());
        FontIcon glyph = assertInstanceOf(FontIcon.class, chevron.getGraphic());
        assertEquals(BootstrapIcons.CHEVRON_DOWN, glyph.getIconCode());
        List<Node> children = input.getChildrenUnmodifiable();
        assertEquals(children.indexOf(bindIcon(input)) - 1, children.indexOf(chevron));
        assertNull(input.lookup(".form-shell-divider"));
        assertTrue(chevron.getBoundsInParent().getMaxX() <= bindIcon(input).getBoundsInParent().getMinX());
        assertTrue(input.getFocusTarget().getBoundsInParent().getMaxX() <= chevron.getBoundsInParent().getMinX());
    }

    // --- Opening the list ----------------------------------------------------------------------------

    @Test
    @DisplayName("the chevron, Alt+Down, F4 and Space open the list, and so does a click on the field anywhere")
    void waysToOpenTheList() {
        RecordingComboInput input = recordingInput();

        chevron(input).fire();
        assertEquals(1, input.listsShown);

        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        assertEquals(2, input.listsShown);

        firesThrough(input, keyReleased(KeyCode.F4));
        assertEquals(3, input.listsShown);

        assertTrue(firesThrough(input, keyPressed(KeyCode.SPACE)).isEmpty());
        assertEquals(4, input.listsShown);

        firesThrough(input, mouse(MouseEvent.MOUSE_PRESSED));
        firesThrough(input, mouse(MouseEvent.MOUSE_RELEASED));
        assertEquals(5, input.listsShown);

        // A click on the field beside the combo box: its right padding, the gap between two icons.
        input.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));
        assertEquals(6, input.listsShown);
    }

    @Test
    @DisplayName("a click on the chevron or beside the combo box whose press closed the open list only closes it")
    void clickThatClosedTheListOpensNone() throws InterruptedException {
        RecordingComboInput input = recordingInput();
        Button chevron = chevron(input);

        // The list's popup closes on the press before the press reaches the field; the event ends before the next.
        onFxThread(() -> {
            input.listClosed();
            chevron.fireEvent(mouse(MouseEvent.MOUSE_PRESSED));
            chevron.fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
        });
        onFxThread(() -> {
            input.listClosed();
            input.fireEvent(mouse(MouseEvent.MOUSE_PRESSED));
            input.fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
            input.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));
        });
        assertEquals(0, input.listsShown);

        onFxThread(() -> {
            chevron.fireEvent(mouse(MouseEvent.MOUSE_PRESSED));
            chevron.fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
        });
        assertEquals(1, input.listsShown);
    }

    @Test
    @DisplayName("Down alone and Ctrl+Space open no list; a click with another button neither")
    void otherKeysOpenNoList() {
        RecordingComboInput input = recordingInput();
        input.getBinding().setBindable(true);

        firesThrough(input, controlKeyPressed(KeyCode.SPACE));
        input.fireEvent(mouse(MouseEvent.MOUSE_CLICKED, MouseButton.SECONDARY));

        assertEquals(0, input.listsShown);
    }

    @Test
    @DisplayName("bound: no list by the chevron, a key or a click, the chevron disabled; it opens again after unbinding")
    void boundOpensNoList() {
        RecordingComboInput input = recordingInput();
        input.getBinding().setBoundName("encoding");

        assertTrue(chevron(input).isDisable());
        chevron(input).fire();
        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        firesThrough(input, keyReleased(KeyCode.F4));
        firesThrough(input, keyPressed(KeyCode.SPACE));
        input.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));
        assertEquals(0, input.listsShown);

        input.getBinding().setBoundName(null);
        assertFalse(chevron(input).isDisable());
        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        assertEquals(1, input.listsShown);
    }

    @Test
    @DisplayName("disabled: no list by the chevron, a key or a click")
    void disabledOpensNoList() {
        RecordingComboInput input = recordingInput();
        input.setDisable(true);

        chevron(input).fire();
        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        firesThrough(input, keyPressed(KeyCode.SPACE));
        input.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));

        assertEquals(0, input.listsShown);
    }

    // --- Keys on the closed list ---------------------------------------------------------------------

    @Test
    @DisplayName("Up and Down choose the entry before and after, as JavaFX's combo box does, the empty one too")
    void upAndDownChoose() {
        RecordingComboInput input = recordingInput();
        input.setEmptyAllowed(true);

        firesThrough(input, keyPressed(KeyCode.DOWN));
        assertEquals("ISO-8859-1", input.getValue());
        firesThrough(input, keyPressed(KeyCode.UP));
        firesThrough(input, keyPressed(KeyCode.UP));
        assertNull(input.getValue());
        assertEquals(0, input.listsShown);
    }

    // --- Bound ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("bound: the chip in place of the hidden value, which Up and Down leave as it is; it shows after unbinding")
    void boundKeepsTheValue() {
        RecordingComboInput input = recordingInput();
        input.getBinding().setBindable(true);
        input.getBinding().getCandidates().add(new BindingCandidate("encoding"));
        input.getBinding().setBoundName("encoding");

        assertTrue(hasPseudoClass(input, "bound"));
        assertTrue(chip(input).isVisible());
        assertNotNull(input.getFocusTarget().getClip());
        firesThrough(input, keyPressed(KeyCode.DOWN));
        assertEquals("UTF-8", input.getValue());
        input.setValue("ISO-8859-1");
        firesThrough(input, keyPressed(KeyCode.UP));
        assertEquals("ISO-8859-1", input.getValue());
        input.setValue("UTF-8");

        assertTrue(firesThrough(input, controlKeyPressed(KeyCode.SPACE)).isEmpty());
        assertEquals(3, input.getBindingMenu().getItems().size());

        assertTrue(firesThrough(input, keyPressed(KeyCode.BACK_SPACE)).isEmpty());
        assertNull(input.getBinding().getBoundName());
        assertNull(input.getFocusTarget().getClip());
        assertEquals("UTF-8", input.getValue());
        assertEquals("UTF-8", shownText(input));
    }

    @Test
    @DisplayName("bound: the application still sets the value, shown after unbinding")
    void boundTakesTheApplicationsValue() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        input.getBinding().setBoundName("encoding");
        layOut(input);

        input.setValue("ISO-8859-1");
        input.getBinding().setBoundName(null);

        assertEquals("ISO-8859-1", shownText(input));
    }

    // --- Read-only -----------------------------------------------------------------------------------

    @Test
    @DisplayName("read-only: the display text as selectable text, no chevron, no {}, no list; all back when it ends")
    void readOnlyShowsSelectableText() {
        RecordingComboInput input = recordingInput();
        input.setDisplayText(code -> code + " encoding");
        input.getBinding().setBindable(true);

        input.setReadOnly(true);
        TextField text = assertInstanceOf(TextField.class, input.getFocusTarget());
        assertTrue(text.getStyleClass().contains("form-combo-text"));
        assertTrue(text.getStyleClass().contains("form-shell-content"));
        assertSame(text, input.getChildrenUnmodifiable().getFirst());
        assertFalse(text.isEditable());
        assertEquals("UTF-8 encoding", text.getText());
        text.selectRange(0, 5);
        assertEquals("UTF-8", text.getSelectedText());
        assertFalse(chevron(input).isVisible());
        assertFalse(chevron(input).isManaged());
        assertFalse(bindIcon(input).isVisible());

        firesThrough(input, altKeyPressed(KeyCode.DOWN));
        firesThrough(input, keyPressed(KeyCode.SPACE));
        input.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));
        assertEquals(0, input.listsShown);
        assertEquals("UTF-8", input.getValue());

        input.setReadOnly(false);
        assertInstanceOf(ComboBox.class, input.getFocusTarget());
        assertFalse(input.getChildrenUnmodifiable().contains(text));
        assertTrue(chevron(input).isVisible());
        assertTrue(bindIcon(input).isVisible());
        assertEquals("UTF-8 encoding", shownText(input));
    }

    @Test
    @DisplayName("read-only: the text follows the value and its display text; without a value it shows the prompt")
    void readOnlyTextFollowsTheValue() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        input.setReadOnly(true);
        TextField text = (TextField) input.getFocusTarget();

        assertEquals("", text.getText());
        assertEquals("Select a value", text.getPromptText());

        input.setValue("ISO-8859-1");
        assertEquals("ISO-8859-1", text.getText());
        input.setDisplayText(code -> code.toLowerCase());
        assertEquals("iso-8859-1", text.getText());
        input.setValue("windows-1252");
        assertEquals("windows-1252", text.getText());
        input.setPromptText("No encoding");
        assertEquals("No encoding", text.getPromptText());
    }

    @Test
    @DisplayName("read-only: the selected text is copied, nothing is cut or pasted")
    void readOnlyCopies() {
        ComboInput<String> input = new ComboInput<>();
        input.setValue("UTF-8");
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
    }

    @Test
    @DisplayName("read-only and bound: the chip without its x over the hidden text, which gives nothing to copy")
    void readOnlyAndBound() {
        ComboInput<String> input = new ComboInput<>();
        input.setValue("UTF-8");
        RecordingClipboard clipboard = new RecordingClipboard(null);
        input.setClipboard(clipboard);
        input.getBinding().setBoundName("encoding");
        input.setReadOnly(true);
        TextField text = (TextField) input.getFocusTarget();

        assertTrue(chip(input).isVisible());
        assertNull(chip(input).getGraphic());
        assertNotNull(text.getClip());
        text.selectAll();
        text.copy();
        assertEquals("", text.getSelectedText());
        assertTrue(clipboard.copied().isEmpty());

        input.getBinding().setBoundName(null);
        assertNull(text.getClip());
        text.selectAll();
        assertEquals("UTF-8", text.getSelectedText());
    }

    @Test
    @DisplayName("read-only moves the focus with the content: from the combo box to the text, and back")
    void readOnlyMovesTheFocus() {
        ComboInput<String> input = new ComboInput<>();
        input.setValue("UTF-8");
        Scene scene = new Scene(new VBox(new TextField(), input));
        input.requestFocus();

        input.setReadOnly(true);
        assertSame(input.getFocusTarget(), scene.getFocusOwner());
        assertInstanceOf(TextField.class, scene.getFocusOwner());

        input.setReadOnly(false);
        assertSame(input.getFocusTarget(), scene.getFocusOwner());
        assertInstanceOf(ComboBox.class, scene.getFocusOwner());
    }

    @Test
    @DisplayName("a row's label names the input whether it is read-only or not")
    void rowLabelNamesEitherContent() {
        ComboInput<String> input = new ComboInput<>();
        new FormField("Encoding", input);
        assertEquals("Encoding", input.getFocusTarget().getAccessibleText());

        input.setReadOnly(true);
        assertEquals("Encoding", input.getFocusTarget().getAccessibleText());

        ComboInput<String> readOnlyFirst = new ComboInput<>();
        readOnlyFirst.setReadOnly(true);
        new FormField("Encoding", readOnlyFirst);
        readOnlyFirst.setReadOnly(false);
        assertEquals("Encoding", readOnlyFirst.getFocusTarget().getAccessibleText());
    }

    // --- States --------------------------------------------------------------------------------------

    @Test
    @DisplayName("error and warning show on the shell; a missing variable's chip is :missing")
    void statesShowOnTheShell() {
        ComboInput<String> input = filledInput();

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
        ComboInput<String> input = filledInput();
        input.setDisable(true);
        Button button = new Button("button");
        button.setDisable(true);
        layOut(input, button);

        assertEquals(button.getOpacity(), input.getOpacity(), 0.001);
        assertEquals(1, input.getFocusTarget().getOpacity(), 0.001);
        assertEquals(1, chevron(input).getOpacity(), 0.001);
    }

    // --- Look ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("no focus border without the focus: the cell that shows a value in the list never counts as focused")
    void noFocusBorderWithoutTheFocus() {
        ComboInput<String> filled = filledInput();
        ComboInput<String> notInList = filledInput();
        notInList.setValue("UTF-32");
        ComboInput<String> optional = filledInput();
        optional.setEmptyAllowed(true);
        optional.setValue(null);
        layOut(filled, notInList, optional);

        assertFalse(filled.isFocusWithin());
        assertFalse(notInList.isFocusWithin());
        assertFalse(optional.isFocusWithin());

        list(filled).getSelectionModel().select(2);
        assertFalse(filled.isFocusWithin());

        // The list's view lies in the field until its popup first shows: its entries, measured for the field's width,
        // take its focused entry as their own focus too.
        ComboInput<String> labelled = new ComboInput<>();
        labelled.getItems().addAll("UTF-8", "UTF-16", "ISO-8859-1", "windows-1252", "US-ASCII");
        labelled.setDisplayText(code -> "Encoding " + code);
        labelled.setValue("UTF-8");
        labelled.setValue("ISO-8859-1");
        labelled.setPrefWidth(360);
        layOut(labelled);
        layOut(labelled);
        assertFalse(labelled.isFocusWithin());

        ListView<String> list = list(labelled);
        ListCell<String> entry = list.getCellFactory().call(list);
        entry.updateListView(list);
        list.getFocusModel().focus(2);
        entry.updateIndex(2);
        assertFalse(entry.isFocused());
    }

    @Test
    @DisplayName("as high as a plain text field, also with its prompt, read-only and bound")
    void asHighAsATextField() {
        TextField plain = new TextField("plain");
        ComboInput<String> filled = filledInput();
        ComboInput<String> empty = new ComboInput<>();
        ComboInput<String> readOnly = filledInput();
        readOnly.setReadOnly(true);
        ComboInput<String> bound = filledInput();
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName("encoding");
        layOut(plain, filled, empty, readOnly, bound);

        assertEquals(plain.getHeight(), filled.getHeight(), 0.01);
        assertEquals(plain.getHeight(), empty.getHeight(), 0.01);
        assertEquals(plain.getHeight(), readOnly.getHeight(), 0.01);
        assertEquals(plain.getHeight(), bound.getHeight(), 0.01);
    }

    @Test
    @DisplayName("inside the shell the combo box has no frame, no background and no arrow of its own")
    void comboBoxLosesItsOwnLook() {
        ComboInput<String> input = filledInput();
        input.setPrefWidth(240);
        layOut(input);
        Region combo = (Region) input.getFocusTarget();

        assertTrue(isTransparent(combo));
        combo.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        combo.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
        combo.applyCss();
        assertTrue(isTransparent(combo));
        assertEquals(0, ((Region) combo.lookup(".arrow-button")).getWidth(), 0.01);
        assertEquals(chevron(input).getBoundsInParent().getMinX(), combo.getBoundsInParent().getMaxX(), 0.01);
    }

    @Test
    @DisplayName("the prompt text in the secondary text colour, a value in the text colour, the read-only text bare")
    void textColours() {
        ComboInput<String> empty = new ComboInput<>();
        ComboInput<String> filled = filledInput();
        ComboInput<String> readOnly = filledInput();
        readOnly.setReadOnly(true);
        Region secondary = tokenSwatch("-fx-text-secondary-color");
        Region text = tokenSwatch("-fx-text-color");
        layOut(empty, filled, readOnly, secondary, text);

        assertEquals(fill(secondary), display(empty).getTextFill());
        assertEquals(fill(text), display(filled).getTextFill());
        assertTrue(isTransparent((Region) readOnly.getFocusTarget()));
    }

    @Test
    @DisplayName("a hand cursor over the field, the default one while bound, read-only or disabled")
    void handCursor() {
        ComboInput<String> input = filledInput();
        ComboInput<String> bound = filledInput();
        bound.getBinding().setBoundName("encoding");
        ComboInput<String> readOnly = filledInput();
        readOnly.setReadOnly(true);
        ComboInput<String> disabled = filledInput();
        disabled.setDisable(true);
        layOut(input, bound, readOnly, disabled);

        assertEquals(Cursor.HAND, input.getCursor());
        assertEquals(Cursor.HAND, input.getFocusTarget().getCursor());
        assertEquals(Cursor.DEFAULT, bound.getCursor());
        assertEquals(Cursor.DEFAULT, readOnly.getCursor());
        assertEquals(Cursor.DEFAULT, disabled.getCursor());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** An input with three encodings to choose from, the first chosen. */
    private static ComboInput<String> filledInput() {
        ComboInput<String> input = new ComboInput<>();
        input.getItems().addAll("UTF-8", "ISO-8859-1", "windows-1252");
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

    /**
     * A combo input that records each time its list would show, as a popup shows only in a window. Its skin is built,
     * so that JavaFX's own keys and clicks reach the combo box's behaviour.
     */
    private static RecordingComboInput recordingInput() {
        RecordingComboInput input = new RecordingComboInput();
        input.getItems().addAll("UTF-8", "ISO-8859-1");
        input.setValue("UTF-8");
        layOut(input);
        return input;
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

    /** A mouse event of the primary button at the top left. */
    private static MouseEvent mouse(EventType<MouseEvent> type) {
        return mouse(type, MouseButton.PRIMARY);
    }

    /** A mouse event of a button at the top left, still since its press. */
    private static MouseEvent mouse(EventType<MouseEvent> type, MouseButton button) {
        return new MouseEvent(type, 2, 2, 2, 2, button, 1, false, false, false, false,
                button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY, false, false, true, null);
    }

    /** The input's chevron. */
    private static Button chevron(ComboInput<?> input) {
        return (Button) input.lookup(".form-combo-chevron");
    }

    /**
     * Returns the input's list: the combo box's list view, which its skin builds in a layout pass and shows only in a
     * window. Choosing an entry in it is what a click on the entry does.
     */
    @SuppressWarnings("unchecked")
    private static <V> ListView<V> list(ComboInput<V> input) {
        layOut(input);
        ComboBox<V> combo = (ComboBox<V>) input.getFocusTarget();
        ComboBoxListViewSkin<V> skin = (ComboBoxListViewSkin<V>) combo.getSkin();
        return (ListView<V>) skin.getPopupContent();
    }

    /** The texts the list's entries show, in order, each from a cell of the list's own cell factory. */
    private static <V> List<String> entryTexts(ListView<V> list) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < list.getItems().size(); i++) {
            ListCell<V> cell = list.getCellFactory().call(list);
            cell.updateListView(list);
            cell.updateIndex(i);
            texts.add(cell.getText());
        }
        return texts;
    }

    /** The cell that shows the input's value in the field, the combo box's button cell. */
    private static ListCell<?> display(ComboInput<?> input) {
        return (ListCell<?>) input.lookup(".form-combo-display");
    }

    /** The text the field shows, its value's display text or the prompt text. */
    private static String shownText(ComboInput<?> input) {
        return display(input).getText();
    }

    /** A combo input that counts the times its list would show, without a window: the tests' seam. */
    private static final class RecordingComboInput extends ComboInput<String> {

        private int listsShown;

        @Override
        void showList() {
            listsShown++;
        }
    }
}
