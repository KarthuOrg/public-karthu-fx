package org.karthu.fx.form;

import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;
import org.karthu.fx.theme.ThemeManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.shiftKeyPressed;

/**
 * The text area input through its public API, without a window: several lines in the shell, the {@code {}} icon at
 * the top right, the grip at the bottom right that the user drags (mouse events fired at it), Tab that moves on, and
 * the shell's states.
 */
class TextAreaInputTest {

    private TextAreaInput input;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInput() {
        input = new TextAreaInput();
    }

    /** The wrapped text area, the input's focus target. */
    private TextArea area() {
        return (TextArea) input.getFocusTarget();
    }

    @Test
    @DisplayName("a text area in the shell: form-frame, form-text-area-input, the area its content and focus target")
    void textAreaInTheShell() {
        assertTrue(input.getStyleClass().contains("form-frame"));
        assertTrue(input.getStyleClass().contains("form-text-area-input"));
        assertTrue(area().getStyleClass().contains("form-text-area-text"));
        assertTrue(area().getStyleClass().contains("form-shell-content"));
        assertSame(area(), input.getChildrenUnmodifiable().getFirst());
        assertSame(input, input.getNode());
    }

    @Test
    @DisplayName("the value is the area's text as it stands, both ways; empty is no value")
    void valueIsTheText() {
        assertEquals("", input.getValue());

        input.setValue("first line\nsecond line");
        assertEquals("first line\nsecond line", area().getText());

        area().setText("  typed\n");
        assertEquals("  typed\n", input.getValue());
        assertEquals("  typed\n", ((FormInput<String>) input).valueProperty().getValue());
    }

    @Test
    @DisplayName("defaults: lines wrap, four rows, no prompt; each passes to the area")
    void defaultsPassToTheArea() {
        assertTrue(input.isWrapText());
        assertTrue(area().isWrapText());
        assertEquals(4, input.getPrefRowCount());
        assertEquals(4, area().getPrefRowCount());
        assertEquals("", input.getPromptText());
        assertNull(input.getUserSize());

        input.setWrapText(false);
        input.setPrefRowCount(8);
        input.setPromptText("Describe the step");
        assertEquals(false, area().isWrapText());
        assertEquals(8, area().getPrefRowCount());
        assertEquals("Describe the step", area().getPromptText());
    }

    // --- Layout --------------------------------------------------------------------------------------

    @Test
    @DisplayName("as high as its rows: four by default, each further row adds the same height")
    void heightFollowsTheRows() {
        TextField plain = new TextField();
        TextAreaInput two = new TextAreaInput();
        two.setPrefRowCount(2);
        TextAreaInput six = new TextAreaInput();
        six.setPrefRowCount(6);
        layOut(plain, two, input, six);

        assertTrue(input.getHeight() > 2 * plain.getHeight());
        assertTrue(input.getHeight() > two.getHeight());
        // Rows of a fractional line height, snapped to whole pixels: one pixel apart at most.
        assertEquals(input.getHeight() - two.getHeight(), six.getHeight() - input.getHeight(), 1.01);
        assertEquals(input.prefHeight(-1), input.maxHeight(-1), 0.01);
        assertEquals(input.prefHeight(-1), input.minHeight(-1), 0.01);
    }

    @Test
    @DisplayName("as high as its rows in the font it shows, also when a stylesheet changes the font after a layout")
    void heightFollowsTheFont() {
        // JavaFX's text area skin measures its rows again when its padding or row count changes, not its font (nor its
        // lines' bounds type), so styles that come in late left fields of one font differently high.
        String largerFont = "data:text/css;base64," + Base64.getEncoder().encodeToString(
                ".root { -fx-font-size: 20px; }".getBytes(StandardCharsets.UTF_8));
        VBox lateRoot = new VBox(input);
        lateRoot.setFillWidth(false);
        Scene late = new Scene(lateRoot);
        ThemeManager.apply(late);
        lateRoot.applyCss();
        lateRoot.layout();
        double before = input.prefHeight(-1);

        late.getStylesheets().add(largerFont);
        lateRoot.applyCss();
        lateRoot.layout();
        TextAreaInput fromTheStart = new TextAreaInput();
        VBox freshRoot = new VBox(fromTheStart);
        freshRoot.setFillWidth(false);
        Scene fresh = new Scene(freshRoot);
        ThemeManager.apply(fresh);
        fresh.getStylesheets().add(largerFont);
        freshRoot.applyCss();
        freshRoot.layout();

        assertEquals(20, area().getFont().getSize(), 0.01);
        assertTrue(input.prefHeight(-1) > before);
        assertEquals(fromTheStart.prefHeight(-1), input.prefHeight(-1), 0.01);
    }

    @Test
    @DisplayName("{} at the top right where a single-line field has it, the grip at the bottom right, the text left")
    void bindIconTopRightGripBottomRight() {
        input.getBinding().setBindable(true);
        PathInput single = new PathInput();
        single.getBinding().setBindable(true);
        input.setPrefWidth(300);
        layOut(single, input);

        Bounds bind = bindIcon(input).getBoundsInParent();
        assertEquals(input.getWidth() - input.snappedRightInset(), bind.getMaxX(), 0.01);
        assertEquals(bindIcon(single).getBoundsInParent().getMinY(), bind.getMinY(), 1.01);

        Region grip = grip(input);
        Bounds gripBounds = grip.getBoundsInParent();
        assertEquals(input.getWidth() - input.snappedRightInset(), gripBounds.getMaxX(), 0.01);
        assertEquals(input.getHeight() - input.snappedBottomInset(), gripBounds.getMaxY(), 0.01);
        assertTrue(gripBounds.getMinY() >= bind.getMaxY());

        Bounds text = area().getBoundsInParent();
        assertEquals(input.snappedLeftInset(), text.getMinX(), 0.01);
        assertTrue(text.getMaxX() <= bind.getMinX());
        assertTrue(text.getMaxX() <= gripBounds.getMinX());
        assertEquals(input.getHeight() - input.snappedTopInset() - input.snappedBottomInset(), text.getHeight(), 0.01);
    }

    @Test
    @DisplayName("the grip: form-text-area-grip, out of the tab order, there also without {}")
    void gripIsAnOwnControl() {
        layOut(input);

        Region grip = grip(input);
        assertTrue(grip.getStyleClass().contains("form-text-area-grip"));
        assertFalse(grip.isFocusTraversable());
        assertFalse(bindIcon(input).isVisible());
        assertEquals(input.getWidth() - input.snappedRightInset(), grip.getBoundsInParent().getMaxX(), 0.01);
    }

    @Test
    @DisplayName("the grip's tooltip and accessible text name the drag and the double click, a property to translate")
    void gripTooltip() {
        assertEquals("Drag to resize (double-click: default size)", input.getGripTooltipText());
        assertEquals(input.getGripTooltipText(), grip(input).getAccessibleText());

        input.setGripTooltipText("Größe ziehen");
        assertEquals("Größe ziehen", grip(input).getAccessibleText());
    }

    @Test
    @DisplayName("the area's own frame is hidden: no background of its own, the shell draws border and background")
    void areaHasNoFrameOfItsOwn() {
        layOut(input);

        assertTransparent(area());
        assertTransparent((Region) area().lookup(".content"));
        assertTransparent((Region) area().lookup(".scroll-pane"));
        assertNotNull(input.getBackground());
    }

    // --- The grip ------------------------------------------------------------------------------------

    @Test
    @DisplayName("dragging the grip makes the input as much wider and taller, the size kept in userSize")
    void dragResizes() {
        VBox parent = parentOfWidth(700, input);
        double width = input.getWidth();
        double height = input.getHeight();

        dragGrip(input, 40, 30);
        layOutAgain(parent);

        assertEquals(new Dimension2D(width + 40, height + 30), input.getUserSize());
        assertEquals(width + 40, input.getWidth(), 0.01);
        assertEquals(height + 30, input.getHeight(), 0.01);
    }

    @Test
    @DisplayName("the width stops at the width the parent gives, the height at two rows")
    void dragStopsAtTheLimits() {
        TextAreaInput twoRows = new TextAreaInput();
        twoRows.setPrefRowCount(2);
        VBox parent = parentOfWidth(700, twoRows, input);

        dragGrip(input, 1000, -1000);
        layOutAgain(parent);

        assertEquals(700, input.getWidth(), 0.01);
        assertEquals(twoRows.getHeight(), input.getHeight(), 1.01);

        dragGrip(input, -1000, 0);
        layOutAgain(parent);
        assertEquals(input.minWidth(-1), input.getWidth(), 0.01);
    }

    @Test
    @DisplayName("in a row that fills its width: at first as wide as the row, dragged narrower, and back to the row")
    void dragInARowThatFills() {
        FormField row = new FormField("Description", input);
        VBox parent = new VBox(row);
        parent.setPrefWidth(600);
        layOutParent(parent);
        assertEquals(600, input.getWidth(), 0.01);

        dragGrip(input, -100, 0);
        layOutAgain(parent);
        assertEquals(500, input.getWidth(), 0.01);

        dragGrip(input, 300, 0);
        layOutAgain(parent);
        assertEquals(600, input.getWidth(), 0.01);
    }

    @Test
    @DisplayName("a double click on the grip goes back to the default size")
    void doubleClickResets() {
        VBox parent = parentOfWidth(700, input);
        double width = input.getWidth();
        double height = input.getHeight();
        dragGrip(input, 60, 80);
        layOutAgain(parent);

        grip(input).fireEvent(mouse(MouseEvent.MOUSE_CLICKED, 0, 0, 2));
        layOutAgain(parent);

        assertNull(input.getUserSize());
        assertEquals(width, input.getWidth(), 0.01);
        assertEquals(height, input.getHeight(), 0.01);
    }

    @Test
    @DisplayName("userSize set by the application restores a size; null is the default size")
    void userSizeRestoresASize() {
        input.setUserSize(new Dimension2D(320, 150));
        VBox parent = parentOfWidth(700, input);

        assertEquals(320, input.getWidth(), 0.01);
        assertEquals(150, input.getHeight(), 0.01);

        input.setUserSize(null);
        layOutAgain(parent);
        assertEquals(input.prefHeight(-1), input.getHeight(), 0.01);
        assertTrue(input.getHeight() < 150);
    }

    @Test
    @DisplayName("a userSize the application restores is never wider than the parent gives; the size stays as set")
    void restoredSizeStaysWithinTheParent() {
        Pane parent = paneOfWidth(400, input);

        input.setUserSize(new Dimension2D(1000, 150));
        layOutAgain(parent);

        assertEquals(400, input.getWidth(), 0.01);
        assertEquals(400, input.prefWidth(-1), 0.01);
        assertEquals(150, input.getHeight(), 0.01);
        assertEquals(new Dimension2D(1000, 150), input.getUserSize());
    }

    @Test
    @DisplayName("a parent that narrows after a drag narrows the input; widened again, the dragged width is back")
    void parentNarrowingAfterADrag() {
        Pane parent = paneOfWidth(700, input);
        dragGrip(input, 100, 0);
        layOutAgain(parent);
        double dragged = input.getWidth();

        parent.setPrefWidth(dragged - 120);
        layOutAgain(parent);
        assertEquals(dragged - 120, input.getWidth(), 0.01);

        parent.setPrefWidth(700);
        layOutAgain(parent);
        assertEquals(dragged, input.getWidth(), 0.01);
    }

    @Test
    @DisplayName("disabled: the grip is disabled, and neither a drag nor a double click changes the size")
    void disabledGripDoesNothing() {
        input.setUserSize(new Dimension2D(320, 150));
        VBox parent = parentOfWidth(700, input);
        input.setDisable(true);

        dragGrip(input, 40, 30);
        grip(input).fireEvent(mouse(MouseEvent.MOUSE_CLICKED, 0, 0, 2));
        layOutAgain(parent);

        assertTrue(grip(input).isDisabled());
        assertEquals(new Dimension2D(320, 150), input.getUserSize());
    }

    @Test
    @DisplayName("read-only and bound: the grip stays in its place and resizes, the value does not change")
    void gripResizesWhileReadOnlyAndBound() {
        input.setValue("kept");
        input.getBinding().setBindable(true);
        VBox parent = parentOfWidth(700, input);
        double roomAfterGrip = input.getWidth() - grip(input).getBoundsInParent().getMaxX();

        input.setReadOnly(true);
        layOutAgain(parent);
        assertSame(input, grip(input).getParent());
        assertEquals(roomAfterGrip, input.getWidth() - grip(input).getBoundsInParent().getMaxX(), 0.01);
        assertEquals(input.getHeight(), grip(input).getBoundsInParent().getMaxY(), 0.01);
        double height = input.getHeight();
        dragGrip(input, 0, 30);
        layOutAgain(parent);
        assertEquals(height + 30, input.getHeight(), 0.01);

        input.setReadOnly(false);
        input.getBinding().setBoundName("description");
        dragGrip(input, 0, 30);
        layOutAgain(parent);
        assertEquals(height + 60, input.getHeight(), 0.01);
        assertEquals("kept", input.getValue());
    }

    // --- Keyboard ------------------------------------------------------------------------------------

    @Test
    @DisplayName("Tab moves the focus to the next field, Shift+Tab to the one before, neither inserts a tab")
    void tabMovesTheFocus() {
        TextField before = new TextField();
        TextField after = new TextField();
        Scene scene = new Scene(new VBox(before, input, after));
        input.setValue("text");

        // Fired at the area in its place: firesThrough would take the input out of the scene.
        input.requestFocus();
        area().fireEvent(keyPressed(KeyCode.TAB));
        assertSame(after, scene.getFocusOwner());

        input.requestFocus();
        area().fireEvent(shiftKeyPressed(KeyCode.TAB));
        assertSame(before, scene.getFocusOwner());
        assertEquals("text", input.getValue());
    }

    @Test
    @DisplayName("Ctrl+Tab inserts a tab where the caret is; not while read-only or bound")
    void controlTabInsertsATab() {
        input.setValue("ab");
        area().positionCaret(1);

        assertTrue(firesThrough(input, controlKeyPressed(KeyCode.TAB)).isEmpty());
        assertEquals("a\tb", input.getValue());

        input.setReadOnly(true);
        firesThrough(input, controlKeyPressed(KeyCode.TAB));
        assertEquals("a\tb", input.getValue());

        input.setReadOnly(false);
        input.getBinding().setBoundName("description");
        firesThrough(input, controlKeyPressed(KeyCode.TAB));
        assertEquals("a\tb", input.getValue());
    }

    @Test
    @DisplayName("Ctrl+Space opens the binding list, Backspace and Delete unbind and keep the text")
    void bindingKeys() {
        input.setValue("kept");
        input.getBinding().setBindable(true);
        input.getBinding().getCandidates().add(new BindingCandidate("description"));

        assertTrue(firesThrough(input, controlKeyPressed(KeyCode.SPACE)).isEmpty());
        assertEquals(1, input.getBindingMenu().getItems().size());

        input.getBinding().setBoundName("description");
        assertTrue(firesThrough(input, keyPressed(KeyCode.BACK_SPACE)).isEmpty());
        assertNull(input.getBinding().getBoundName());

        input.getBinding().setBoundName("description");
        assertTrue(firesThrough(input, keyPressed(KeyCode.DELETE)).isEmpty());
        assertNull(input.getBinding().getBoundName());
        assertEquals("kept", input.getValue());
    }

    @Test
    @DisplayName("no choice of its own: Alt+Down changes neither the text nor the binding")
    void altDownDoesNothing() {
        input.setValue("text");
        input.getBinding().setBindable(true);

        firesThrough(input, altKeyPressed(KeyCode.DOWN));

        assertEquals("text", input.getValue());
        assertNull(input.getBinding().getBoundName());
    }

    // --- Binding, read-only, disabled, state ---------------------------------------------------------

    @Test
    @DisplayName("bound: the chip at the top left where the text starts, the text hidden and kept, back after unbinding")
    void boundShowsTheChipAtTheTopLeft() {
        input.setValue("several\nlines");
        input.getBinding().setBindable(true);
        input.getBinding().setBoundName("description");
        PathInput single = new PathInput();
        single.getBinding().setBoundName("description");
        input.setPrefWidth(300);
        layOut(single, input);

        assertTrue(hasPseudoClass(input, "bound"));
        assertTrue(chip(input).isVisible());
        Bounds chipBounds = chip(input).getBoundsInParent();
        Bounds singleChip = chip(single).getBoundsInParent();
        assertEquals(singleChip.getMinX(), chipBounds.getMinX(), 0.01);
        assertEquals(singleChip.getMinY(), chipBounds.getMinY(), 1.01);
        assertTrue(chipBounds.getMaxX() <= bindIcon(input).getBoundsInParent().getMinX());
        assertNotNull(area().getClip());
        assertFalse(area().isEditable());
        assertEquals("several\nlines", input.getValue());

        input.getBinding().setBoundName(null);
        assertNull(area().getClip());
        assertTrue(area().isEditable());
        assertFalse(chip(input).isVisible());
        assertEquals("several\nlines", area().getText());
    }

    @Test
    @DisplayName("text: copy and paste through the clipboard; bound, nothing of the hidden text is selected or copied")
    void clipboardAndTheHiddenText() {
        RecordingClipboard clipboard = new RecordingClipboard("pasted");
        input.setClipboard(clipboard);
        input.setValue("secret");
        area().selectRange(0, 3);
        area().copy();
        area().positionCaret(6);
        area().paste();
        assertEquals(List.of("sec"), clipboard.copied());
        assertEquals("secretpasted", input.getValue());

        input.getBinding().setBoundName("description");
        area().selectAll();
        assertEquals("", area().getSelectedText());
        area().selectRange(0, 3);
        area().copy();
        area().cut();
        area().paste();
        assertEquals(List.of("sec"), clipboard.copied());
        assertEquals("secretpasted", input.getValue());
    }

    @Test
    @DisplayName("read-only: not editable, {} hidden, the text selected and copied, nothing cut or pasted")
    void readOnlyText() {
        RecordingClipboard clipboard = new RecordingClipboard("pasted");
        input.setClipboard(clipboard);
        input.setValue("first\nsecond");
        input.getBinding().setBindable(true);

        input.setReadOnly(true);

        assertTrue(hasPseudoClass(input, "readonly"));
        assertFalse(area().isEditable());
        assertFalse(bindIcon(input).isVisible());
        assertTrue(area().isFocusTraversable());
        area().selectRange(0, 5);
        area().copy();
        area().cut();
        area().paste();
        assertEquals(List.of("first"), clipboard.copied());
        assertEquals("first\nsecond", input.getValue());

        input.setReadOnly(false);
        assertTrue(area().isEditable());
        assertTrue(bindIcon(input).isVisible());
    }

    @Test
    @DisplayName("disabled: dimmed once, the text and the grip inside at full opacity, the grip disabled")
    void disabledDimsOnce() {
        input.setDisable(true);
        layOut(input);

        assertTrue(input.getOpacity() < 1);
        assertEquals(1, area().getOpacity(), 0.001);
        assertEquals(1, grip(input).getOpacity(), 0.001);
        assertTrue(area().isDisabled());
        assertTrue(grip(input).isDisabled());
    }

    @Test
    @DisplayName("the state shows on the shell as :error or :warning, through the form input contract")
    void stateShowsOnTheShell() {
        FormInput<String> formInput = input;

        formInput.stateProperty().set(FieldState.ERROR);
        assertTrue(hasPseudoClass(input, "error"));
        formInput.stateProperty().set(FieldState.WARNING);
        assertTrue(hasPseudoClass(input, "warning"));
        assertFalse(hasPseudoClass(input, "error"));
        formInput.readOnlyProperty().set(true);
        assertTrue(input.isReadOnly());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /**
     * Lays the nodes out one under the other in a parent of the given width that leaves them at their preferred
     * widths, in a themed scene that is never shown: the parent gives each node up to that width.
     */
    private static VBox parentOfWidth(double width, Node... nodes) {
        VBox parent = new VBox(nodes);
        parent.setFillWidth(false);
        parent.setPrefWidth(width);
        layOutParent(parent);
        return parent;
    }

    /**
     * Lays the node out in a pane of the given width, which gives it its preferred size and keeps it from nothing, in
     * a themed scene that is never shown: the width the pane gives is its own.
     */
    private static Pane paneOfWidth(double width, Node node) {
        Pane parent = new Pane(node);
        parent.setPrefWidth(width);
        layOutParent(parent);
        return parent;
    }

    /** Lays a parent out at its preferred size in a themed scene that is never shown. */
    private static void layOutParent(Region parent) {
        Scene scene = new Scene(parent);
        ThemeManager.apply(scene);
        layOutAgain(parent);
    }

    /** Lays the parent out again at its preferred size, after a change. */
    private static void layOutAgain(Region parent) {
        parent.applyCss();
        parent.autosize();
        parent.layout();
    }

    /** The user drags the input's grip by the distance with the primary button, in two steps, and lets it go. */
    private static void dragGrip(TextAreaInput textArea, double dx, double dy) {
        Region grip = grip(textArea);
        grip.fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 500, 500, 1));
        grip.fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 500 + dx / 2, 500 + dy / 2, 1));
        grip.fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 500 + dx, 500 + dy, 1));
        grip.fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 500 + dx, 500 + dy, 1));
    }

    /** A mouse event of the primary button at a point on the screen. */
    private static MouseEvent mouse(EventType<MouseEvent> type, double screenX, double screenY, int clicks) {
        boolean down = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        return new MouseEvent(type, 0, 0, screenX, screenY, MouseButton.PRIMARY, clicks,
                false, false, false, false, down, false, false, false, false, true, null);
    }

    /** The input's grip at its bottom right corner. */
    private static Region grip(TextAreaInput textArea) {
        return (Region) textArea.lookup(".form-text-area-grip");
    }

    /** Asserts that a region draws no background: none, or only transparent fills. */
    private static void assertTransparent(Region region) {
        assertNotNull(region);
        Background background = region.getBackground();
        if (background == null) {
            return;
        }
        for (BackgroundFill fill : background.getFills()) {
            assertEquals(Color.TRANSPARENT, fill.getFill(), "a fill of " + region.getStyleClass());
        }
    }
}
