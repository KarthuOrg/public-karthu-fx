package org.karthu.fx.form;

import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.karthu.fx.form.FormTestSupport.shiftKeyPressed;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The read-only state of a frame-based input, through the path input's public API and without a window: the text can
 * be selected but not changed, the icons are hidden, the chip has no remove button, and no shortcut or drop changes
 * the value or the binding.
 */
class FieldReadOnlyTest {

    @TempDir
    Path temp;

    private PathInput field;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createField() {
        field = new PathInput();
    }

    @Test
    @DisplayName("a new input is not read-only; read-only shows as :readonly on the frame, the input's property")
    void readOnlyShowsAsPseudoClass() {
        assertFalse(field.isReadOnly());
        assertFalse(hasPseudoClass(field, "readonly"));
        assertSame(field.readOnlyProperty(), ((FormInput<String>) field).readOnlyProperty());

        field.setReadOnly(true);
        assertTrue(hasPseudoClass(field, "readonly"));

        field.setReadOnly(false);
        assertFalse(hasPseudoClass(field, "readonly"));
    }

    @Test
    @DisplayName("read-only: the text keeps its value and can be selected, but is not editable until read-only ends")
    void textIsSelectableButNotEditable() {
        field.setValue("files/data.csv");

        field.setReadOnly(true);
        assertFalse(field.getTextField().isEditable());
        field.getTextField().selectRange(6, 10);
        assertEquals("data", field.getTextField().getSelectedText());
        field.getTextField().selectAll();
        assertEquals("files/data.csv", field.getTextField().getSelectedText());
        assertEquals("files/data.csv", field.getValue());

        field.setReadOnly(false);
        assertTrue(field.getTextField().isEditable());
    }

    @Test
    @DisplayName("read-only hides the folder and {} icons, also while bindable; they show again when it ends")
    void iconsAreHidden() {
        ButtonBase browse = field.getIcons().getFirst();
        field.getBinding().setBindable(true);
        assertTrue(isShown(browse));
        assertTrue(isShown(bindIcon(field)));

        field.setReadOnly(true);
        assertFalse(isShown(browse));
        assertFalse(isShown(bindIcon(field)));

        field.setReadOnly(false);
        assertTrue(isShown(browse));
        assertTrue(isShown(bindIcon(field)));
        assertSame(bindIcon(field), field.getChildrenUnmodifiable().getLast());
    }

    @Test
    @DisplayName("read-only leaves a trailing icon's own properties as the application set them")
    void iconsKeepTheirOwnProperties() {
        FieldFrame frame = new FieldFrame();
        Button own = new Button();
        Button hidden = new Button();
        frame.getIcons().addAll(own, hidden);
        own.setDisable(true);
        hidden.setVisible(false);
        hidden.setManaged(false);

        frame.setReadOnly(true);
        assertFalse(isShown(own));
        frame.setReadOnly(false);

        assertTrue(isShown(own));
        assertTrue(own.isDisable());
        assertSame(frame, hidden.getParent());
        assertFalse(hidden.isVisible());
        assertFalse(hidden.isManaged());
    }

    @Test
    @DisplayName("read-only, the text field takes the icons' room, and the frame stays as high as a text field")
    void textFieldFillsTheFrame() {
        TextField plain = new TextField("plain");
        field.setPrefWidth(300);
        field.setValue("files/data.csv");
        field.getBinding().setBindable(true);
        field.setReadOnly(true);
        layOut(plain, field);

        assertEquals(300, field.getWidth(), 0.01);
        assertEquals(plain.getHeight(), field.getHeight(), 0.01);
        Bounds text = field.getTextField().getBoundsInParent();
        assertEquals(field.snappedLeftInset(), text.getMinX(), 0.01);
        // Without trailing nodes the shell leaves out the room after them, its right padding.
        assertEquals(field.getWidth(), text.getMaxX(), 0.01);
    }

    @Test
    @DisplayName("read-only and bound: the chip shows the name without its x, the path stays hidden and kept")
    void boundChipHasNoRemoveButton() {
        field.setValue("files/data.csv");
        field.getBinding().setBoundName("inputFile");
        Node remove = chip(field).getGraphic();
        assertTrue(remove.getStyleClass().contains("form-chip-remove"));

        field.setReadOnly(true);
        assertTrue(chip(field).isVisible());
        assertEquals("inputFile", chip(field).getText());
        assertNull(chip(field).getGraphic());
        assertTrue(hasPseudoClass(field, "bound"));
        assertNotNull(field.getTextField().getClip());
        assertFalse(isShown(bindIcon(field)));

        field.setReadOnly(false);
        assertSame(remove, chip(field).getGraphic());
        assertTrue(isShown(bindIcon(field)));
        assertFalse(field.getTextField().isEditable());
        assertEquals("files/data.csv", field.getValue());
    }

    @Test
    @DisplayName("read-only and bound to a missing variable: the chip still shows :missing and its tooltip")
    void missingChipWhileReadOnly() {
        field.getBinding().setBoundName("gone");
        field.getBinding().setMissing(true);

        field.setReadOnly(true);

        assertTrue(hasPseudoClass(chip(field), "missing"));
        assertEquals("This variable does not exist", chip(field).getTooltip().getText());
        assertNull(chip(field).getGraphic());
    }

    @Test
    @DisplayName("read-only: Alt+Down opens no chooser and passes the field, nor does the browse icon fired by code")
    void altDownOpensNothing() {
        List<PathChooser.Request> requests = recordRequests();
        ButtonBase browse = field.getIcons().getFirst();
        field.setReadOnly(true);

        assertEquals(1, firesThrough(field, altKeyPressed(KeyCode.DOWN)).size());
        browse.fire();
        assertTrue(requests.isEmpty());

        field.setReadOnly(false);
        browse.fire();
        assertEquals(1, requests.size());
    }

    @Test
    @DisplayName("read-only: Ctrl+Space opens no binding list and passes the field")
    void ctrlSpaceOpensNothing() {
        field.getBinding().setBindable(true);
        field.getBinding().getCandidates().add(new BindingCandidate("inputFile"));
        field.setReadOnly(true);

        assertEquals(1, firesThrough(field, controlKeyPressed(KeyCode.SPACE)).size());

        assertTrue(field.getBindingMenu().getItems().isEmpty());
    }

    @Test
    @DisplayName("read-only and bound: Backspace and Delete pass the field and leave the binding")
    void backspaceAndDeleteDoNotUnbind() {
        field.getBinding().setBoundName("inputFile");
        field.setReadOnly(true);

        assertEquals(1, firesThrough(field, keyPressed(KeyCode.BACK_SPACE)).size());
        assertEquals(1, firesThrough(field, keyPressed(KeyCode.DELETE)).size());

        assertEquals("inputFile", field.getBinding().getBoundName());
    }

    @Test
    @DisplayName("a binding list built before read-only binds, switches and unbinds nothing once read-only")
    void bindingListDoesNothingOnceReadOnly() {
        field.getBinding().getCandidates().addAll(new BindingCandidate("inputFile"), new BindingCandidate("dataDir"));
        field.getBinding().setBoundName("inputFile");
        field.getBinding().setMissing(true);
        bindIcon(field).fire();
        List<MenuItem> items = new ArrayList<>(field.getBindingMenu().getItems());
        Node remove = chip(field).getGraphic();

        field.setReadOnly(true);
        items.get(1).fire();
        items.getLast().fire();
        ((ButtonBase) remove).fire();

        assertEquals("inputFile", field.getBinding().getBoundName());
        assertTrue(field.getBinding().isMissing());
    }

    @Test
    @DisplayName("read-only: a dragged file shows nothing and its drop changes neither value nor memory")
    void dropIsRefused() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        DirectoryMemory memory = DirectoryMemory.inMemory();
        field.setDirectoryMemory(memory);
        field.setValue("files/kept.csv");
        field.setReadOnly(true);

        assertFalse(field.handleDragOver(List.of(file)));
        assertFalse(hasPseudoClass(field, "drop-target"));
        assertFalse(field.handleDragDropped(List.of(file)));

        assertEquals("files/kept.csv", field.getValue());
        assertEquals(Optional.empty(), memory.lastDirectory());

        field.setReadOnly(false);
        assertTrue(field.handleDragOver(List.of(file)));
    }

    @Test
    @DisplayName("read-only: typing, Backspace, Delete and paste change nothing, Shift+Home still selects the text")
    void keysAndPasteChangeNothingButSelectionWorks() {
        PathInput editable = new PathInput();
        editable.setValue("files/data.csv");
        field.setValue("files/data.csv");
        field.setReadOnly(true);
        // The text fields get their skin, and with it their key handling, in a CSS pass.
        layOut(editable, field);

        for (PathInput each : List.of(editable, field)) {
            each.getTextField().end();
            typeInto(each, "x");
            each.getTextField().fireEvent(keyPressed(KeyCode.BACK_SPACE));
            each.getTextField().home();
            each.getTextField().fireEvent(keyPressed(KeyCode.DELETE));
            each.setClipboard(new RecordingClipboard("pasted/"));
            each.getTextField().paste();
        }
        // The editable twin shows that the keys reach the text field: x typed and deleted again, the first character
        // deleted, the paste inserted.
        assertEquals("pasted/iles/data.csv", editable.getValue());
        assertEquals("files/data.csv", field.getValue());

        field.getTextField().end();
        field.getTextField().fireEvent(shiftKeyPressed(KeyCode.HOME));
        assertEquals("files/data.csv", field.getTextField().getSelectedText());
    }

    @Test
    @DisplayName("read-only: the selected text is copied, but a cut called by code removes nothing")
    void copyButNoCut() {
        RecordingClipboard clipboard = new RecordingClipboard(null);
        field.setClipboard(clipboard);
        field.setValue("files/data.csv");
        field.setReadOnly(true);
        field.getTextField().selectRange(6, 10);

        field.getTextField().copy();
        field.getTextField().cut();

        assertEquals(List.of("data"), clipboard.copied());
        assertEquals("files/data.csv", field.getValue());
    }

    @Test
    @DisplayName("a read-only frame lies on the read-only background of a text field, its text up to its edge")
    void readOnlyLookInTheTheme() {
        TextField plain = new TextField("plain");
        plain.setEditable(false);
        field.setReadOnly(true);
        Region readOnlyBackground = tokenSwatch("-fx-readonly-bg");
        Region border = tokenSwatch("-fx-control-border");
        layOut(plain, field, readOnlyBackground, border);

        assertEquals(fill(plain, 1), fill(field, 1));
        assertEquals(fill(readOnlyBackground, 0), fill(field, 1));
        assertEquals(fill(border, 0), fill(field, 0));
        assertEquals(field.getWidth(), field.getTextField().getBoundsInParent().getMaxX(), 0.01);
    }

    @Test
    @DisplayName("read-only keeps the focus border and the warning and error borders, on the read-only background")
    void readOnlyKeepsTheBorders() {
        PathInput focused = readOnlyField(FieldState.NORMAL);
        // :focus-within is set by the toolkit only in a shown, focused window; the test sets it as the toolkit would.
        focused.pseudoClassStateChanged(PseudoClass.getPseudoClass("focus-within"), true);
        PathInput warning = readOnlyField(FieldState.WARNING);
        PathInput error = readOnlyField(FieldState.ERROR);
        PathInput focusedError = readOnlyField(FieldState.ERROR);
        focusedError.pseudoClassStateChanged(PseudoClass.getPseudoClass("focus-within"), true);
        Region accent = tokenSwatch("-fx-accent-color");
        Region paused = tokenSwatch("-fx-status-paused");
        Region statusError = tokenSwatch("-fx-status-error");
        Region readOnlyBackground = tokenSwatch("-fx-readonly-bg");
        layOut(focused, warning, error, focusedError, accent, paused, statusError, readOnlyBackground);

        assertEquals(fill(accent, 0), fill(focused, 0));
        assertEquals(fill(paused, 0), fill(warning, 0));
        assertEquals(fill(statusError, 0), fill(error, 0));
        assertEquals(fill(statusError, 0), fill(focusedError, 0));
        for (PathInput each : List.of(focused, warning, error, focusedError)) {
            assertEquals(fill(readOnlyBackground, 0), fill(each, 1));
        }
    }

    @Test
    @DisplayName("the theme pads a read-only chip alike on both sides, a missing one outlined on the read-only colour")
    void readOnlyChipInTheTheme() {
        field.getBinding().setBoundName("inputFile");
        field.setReadOnly(true);
        PathInput missing = readOnlyField(FieldState.NORMAL);
        missing.getBinding().setBoundName("gone");
        missing.getBinding().setMissing(true);
        Region readOnlyBackground = tokenSwatch("-fx-readonly-bg");
        Region statusError = tokenSwatch("-fx-status-error");
        layOut(field, missing, readOnlyBackground, statusError);

        assertEquals(chip(field).getPadding().getLeft(), chip(field).getPadding().getRight(), 0.01);
        Label missingChip = (Label) missing.lookup(".form-chip");
        assertEquals(fill(statusError, 0), fill(missingChip, 0));
        assertEquals(fill(readOnlyBackground, 0), fill(missingChip, 1));
    }

    private static PathInput readOnlyField(FieldState state) {
        PathInput readOnlyField = new PathInput();
        readOnlyField.setState(state);
        readOnlyField.setReadOnly(true);
        return readOnlyField;
    }

    /** Whether the node is in the field and visible, so that it shows while the field does. */
    private static boolean isShown(Node node) {
        return node.getParent() != null && node.isVisible() && node.isManaged();
    }

    /** Puts a chooser in place that opens nothing and records each request; returns the requests. */
    private List<PathChooser.Request> recordRequests() {
        List<PathChooser.Request> requests = new ArrayList<>();
        field.setChooser(request -> {
            requests.add(request);
            return Optional.empty();
        });
        return requests;
    }

    /** Types the text into the field's text field as the user would: a key typed event. */
    private static void typeInto(PathInput target, String text) {
        target.getTextField().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, text, "", KeyCode.UNDEFINED,
                false, false, false, false));
    }
}
