package org.karthu.fx.form;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.markdown.MarkdownView;
import org.karthu.fx.markdown.SearchDirection;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.altKeyPressed;
import static org.karthu.fx.form.FormTestSupport.controlKeyPressed;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.shiftKeyPressed;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The search field connected to a Markdown view, through the field's public API and its style classes and the view's
 * search API, without a window: the field's text and the view's search, the counter, the keys sent as events to the
 * text field and the arrow buttons, in a scene with the theme that is never shown.
 */
class SearchInputViewTest {

    /** A document with {@code anna} four times: in a heading, twice in a paragraph, in a code block. */
    private static final String DOCUMENT = """
            # Anna

            A paragraph about anna and ANNA.

            ```
            code with anna
            ```
            """;

    private SearchInput input;
    private MarkdownView view;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInputAndView() {
        input = new SearchInput();
        view = new MarkdownView(DOCUMENT);
    }

    // --- Connection ----------------------------------------------------------------------------------

    @Test
    @DisplayName("connected, the view takes the field's text; from then on they stay equal both ways, empty as null")
    void textAndSearchStayEqual() {
        input.setText("anna");

        input.search(view);
        assertEquals("anna", view.getSearchText());
        assertEquals(4, view.getSearchMatchCount());

        input.setText("paragraph");
        assertEquals("paragraph", view.getSearchText());

        view.setSearchText("code");
        assertEquals("code", input.getText());

        input.setText("");
        assertNull(view.getSearchText());

        view.setSearchText("anna");
        view.clearSearch();
        assertEquals("", input.getText());
    }

    @Test
    @DisplayName("a second view replaces the first, null ends the connection: a disconnected view keeps its search")
    void reconnectingAndDisconnecting() {
        MarkdownView other = new MarkdownView(DOCUMENT);
        input.search(view);
        input.setText("anna");

        input.search(other);
        assertEquals("anna", other.getSearchText());
        input.setText("paragraph");
        assertEquals("anna", view.getSearchText(), "the view before keeps its search");
        assertEquals("paragraph", other.getSearchText());
        view.setSearchText("code");
        assertEquals("paragraph", input.getText(), "the view before no longer sets the text");

        input.search(null);
        input.setText("heading");
        assertEquals("paragraph", other.getSearchText());
        other.setSearchText("code");
        assertEquals("heading", input.getText());
    }

    @Test
    @DisplayName("a view whose search text is bound one way is refused, the field's connection left as it was")
    void viewWithBoundSearchIsRefused() {
        MarkdownView bound = new MarkdownView(DOCUMENT);
        bound.searchTextProperty().bind(new SimpleStringProperty("code"));
        input.search(view);
        input.setText("anna");

        assertThrows(IllegalArgumentException.class, () -> input.search(bound));

        input.setText("paragraph");
        assertEquals("paragraph", view.getSearchText());
        assertEquals("code", bound.getSearchText());
    }

    @Test
    @DisplayName("a long-lived view the field searches does not keep a dropped field alive")
    void connectedViewDoesNotKeepTheFieldAlive() {
        WeakReference<SearchInput> dropped = connectedField(view);

        assertTrue(isCollected(dropped), "the view keeps a dropped field alive");
        view.setSearchText("paragraph");
        view.nextSearchMatch();
        assertEquals(1, view.getSearchMatchCount());
    }

    /** A field, connected to the view and searching, that nothing else holds. */
    private static WeakReference<SearchInput> connectedField(MarkdownView searched) {
        SearchInput field = new SearchInput();
        field.search(searched);
        field.setText("anna");
        return new WeakReference<>(field);
    }

    @Test
    @DisplayName("one connection at a time: a view ends the list connection, the list gets every entry back")
    void viewEndsTheListConnection() {
        ObservableList<String> entries = FXCollections.observableArrayList("Anna", "Ben");
        FilteredList<String> filtered = new FilteredList<>(entries);
        input.filter(filtered, Function.identity());
        input.setText("anna");
        assertEquals(List.of("Anna"), filtered);

        input.search(view);

        assertEquals(List.of("Anna", "Ben"), filtered);
        input.setText("ben");
        assertEquals(List.of("Anna", "Ben"), filtered);
        assertEquals("ben", view.getSearchText());
    }

    @Test
    @DisplayName("one connection at a time: a list ends the view connection, the view keeps its search")
    void listEndsTheViewConnection() {
        ObservableList<String> entries = FXCollections.observableArrayList("Anna", "Ben");
        FilteredList<String> filtered = new FilteredList<>(entries);
        input.search(view);
        input.setText("anna");

        input.filter(filtered, Function.identity());

        assertEquals(List.of("Anna"), filtered);
        input.setText("ben");
        assertEquals(List.of("Ben"), filtered);
        assertEquals("anna", view.getSearchText());
        view.setSearchText("code");
        assertEquals("ben", input.getText());
    }

    // --- Counter -------------------------------------------------------------------------------------

    @Test
    @DisplayName("the counter shows the current match from 1 and the number of matches while connected and with text")
    void counterShowsTheCurrentMatch() {
        input.setText("anna");
        input.search(view);

        assertTrue(isShown(counter()));
        assertEquals("1 of 4", counter().getText());
        assertFalse(hasPseudoClass(counter(), "no-match"));

        view.nextSearchMatch();
        assertEquals("2 of 4", counter().getText());
        view.previousSearchMatch();
        view.previousSearchMatch();
        assertEquals("4 of 4", counter().getText());

        input.setText("paragraph");
        assertEquals("1 of 1", counter().getText());

        input.setText("");
        assertFalse(isShown(counter()), "an empty field: no counter");
    }

    @Test
    @DisplayName("the counter follows the view's matches: a new text of the view, its direction, the format")
    void counterFollowsTheView() {
        view.setSearchDirection(SearchDirection.BACKWARD);
        input.search(view);

        input.setText("anna");
        assertEquals("4 of 4", counter().getText(), "backward a new search starts at the last match");

        view.setMarkdown("anna and anna");
        assertEquals("2 of 2", counter().getText());

        input.setMatchCountFormat("{0} von {1}");
        assertEquals("2 von 2", counter().getText());
    }

    @Test
    @DisplayName("nothing matches: 0 of 0 with :no-match, in the warning colour")
    void counterWithoutMatches() {
        Region warning = tokenSwatch("-fx-status-paused");
        input.search(view);
        input.setText("anna");
        layOut(input, warning);

        input.setText("annax");
        input.applyCss();

        assertEquals("0 of 0", counter().getText());
        assertTrue(hasPseudoClass(counter(), "no-match"));
        assertEquals(fill(warning), counter().getTextFill());

        input.setText("anna");
        assertFalse(hasPseudoClass(counter(), "no-match"));
    }

    @Test
    @DisplayName("disconnected, the counter hides")
    void counterAfterDisconnecting() {
        input.search(view);
        input.setText("anna");

        input.search(null);

        assertFalse(isShown(counter()));
        view.nextSearchMatch();
        assertFalse(isShown(counter()));
    }

    // --- Keys ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Enter goes to the next match, Shift+Enter to the previous one, both consumed")
    void enterAndShiftEnter() {
        layOut(input);
        input.search(view);
        input.setText("anna");

        assertTrue(firesThrough(input, keyPressed(KeyCode.ENTER)).isEmpty());
        assertEquals(1, view.getSearchMatchIndex());
        firesThrough(input, keyPressed(KeyCode.ENTER));
        assertEquals("3 of 4", counter().getText());

        assertTrue(firesThrough(input, shiftKeyPressed(KeyCode.ENTER)).isEmpty());
        assertEquals(1, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("Enter and Shift+Enter follow the direction: backward Enter goes up")
    void enterFollowsTheDirection() {
        layOut(input);
        view.setSearchDirection(SearchDirection.BACKWARD);
        input.search(view);
        input.setText("anna");

        firesThrough(input, keyPressed(KeyCode.ENTER));
        assertEquals(2, view.getSearchMatchIndex());
        firesThrough(input, shiftKeyPressed(KeyCode.ENTER));
        assertEquals(3, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("without matches Enter is consumed too, so a default button does not fire while the reader searches")
    void enterWithoutMatchesIsConsumed() {
        layOut(input);
        input.search(view);
        input.setText("annax");

        assertTrue(firesThrough(input, keyPressed(KeyCode.ENTER)).isEmpty());
        assertTrue(firesThrough(input, shiftKeyPressed(KeyCode.ENTER)).isEmpty());
        assertEquals(-1, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("Enter passes on in an empty field, in a field with no view, and with Control or Alt down")
    void enterPassesOn() {
        layOut(input);
        input.setText("anna");
        assertEquals(1, firesThrough(input, keyPressed(KeyCode.ENTER)).size(), "no view");

        input.search(view);
        input.setText("");
        assertEquals(1, firesThrough(input, keyPressed(KeyCode.ENTER)).size(), "an empty field");

        input.setText("anna");
        assertEquals(1, firesThrough(input, controlKeyPressed(KeyCode.ENTER)).size(), "Control+Enter");
        assertEquals(1, firesThrough(input, altKeyPressed(KeyCode.ENTER)).size(), "Alt+Enter");
        assertEquals(0, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("Escape empties the field and with it the view's search")
    void escapeEndsTheSearch() {
        layOut(input);
        input.search(view);
        input.setText("anna");

        assertTrue(firesThrough(input, keyPressed(KeyCode.ESCAPE)).isEmpty());

        assertEquals("", input.getText());
        assertNull(view.getSearchText());
        assertEquals(0, view.getSearchMatchCount());
    }

    // --- Arrows --------------------------------------------------------------------------------------

    @Test
    @DisplayName("the arrows, chevrons up and down out of the tab order, show while connected and with text")
    void arrowsShowWhileConnectedWithText() {
        assertEquals(BootstrapIcons.CHEVRON_UP, glyph(upArrow()).getIconCode());
        assertEquals(BootstrapIcons.CHEVRON_DOWN, glyph(downArrow()).getIconCode());
        assertFalse(upArrow().isFocusTraversable());
        assertFalse(downArrow().isFocusTraversable());
        input.setText("anna");
        assertFalse(isShown(upArrow()), "no view: no arrows");
        assertFalse(isShown(downArrow()));

        input.search(view);
        assertTrue(isShown(upArrow()));
        assertTrue(isShown(downArrow()));

        input.setText("");
        assertFalse(isShown(upArrow()), "an empty field: no arrows");
        input.setText("anna");
        input.search(null);
        assertFalse(isShown(downArrow()), "disconnected: no arrows");
    }

    @Test
    @DisplayName("left to right: the text, the counter, the up arrow, the down arrow, the x; the arrows look like the x")
    void arrowsBetweenTheCounterAndTheClearButton() {
        input.search(view);
        input.setText("anna");
        input.setPrefWidth(400);
        layOut(input);

        Bounds counter = counter().getBoundsInParent();
        Bounds up = upArrow().getBoundsInParent();
        Bounds down = downArrow().getBoundsInParent();
        Bounds clear = clearButton().getBoundsInParent();
        assertTrue(up.getWidth() > 0);
        assertTrue(counter.getMaxX() <= up.getMinX());
        assertTrue(up.getMaxX() <= down.getMinX());
        assertTrue(down.getMaxX() <= clear.getMinX());
        assertTrue(upArrow().getStyleClass().containsAll(List.of("icon-button", "form-frame-icon")));
        assertEquals(glyph(clearButton()).getIconColor(), glyph(upArrow()).getIconColor());
        assertEquals(glyph(clearButton()).getIconSize(), glyph(downArrow()).getIconSize());
    }

    @Test
    @DisplayName("forward, the up arrow goes to the previous match and the down arrow to the next, wrapping around")
    void arrowsForward() {
        input.search(view);
        input.setText("anna");

        downArrow().fire();
        downArrow().fire();
        assertEquals(2, view.getSearchMatchIndex());
        upArrow().fire();
        assertEquals(1, view.getSearchMatchIndex());
        upArrow().fire();
        upArrow().fire();
        assertEquals(3, view.getSearchMatchIndex());
        assertEquals("4 of 4", counter().getText());
    }

    @Test
    @DisplayName("backward, the up arrow still goes up, to the next match, and the down arrow down, to the previous one")
    void arrowsBackward() {
        view.setSearchDirection(SearchDirection.BACKWARD);
        input.search(view);
        input.setText("anna");
        assertEquals(3, view.getSearchMatchIndex());

        upArrow().fire();
        upArrow().fire();
        assertEquals(1, view.getSearchMatchIndex());
        downArrow().fire();
        assertEquals(2, view.getSearchMatchIndex());
        downArrow().fire();
        downArrow().fire();
        assertEquals(0, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("tooltips and accessible texts name what Enter would do: Previous match up and Next match down"
            + " forward, the other way round backward, following the direction")
    void arrowTexts() {
        input.search(view);
        input.setText("anna");
        assertEquals("Previous match", input.getPreviousMatchText());
        assertEquals("Next match", input.getNextMatchText());
        assertArrowTexts("Previous match", "Next match");

        view.setSearchDirection(SearchDirection.BACKWARD);
        assertArrowTexts("Next match", "Previous match");

        input.setNextMatchText("Nächster Treffer");
        input.previousMatchTextProperty().set("Vorheriger Treffer");
        assertArrowTexts("Nächster Treffer", "Vorheriger Treffer");

        view.setSearchDirection(null);
        assertArrowTexts("Vorheriger Treffer", "Nächster Treffer");
    }

    @Test
    @DisplayName("the arrows are disabled without matches and while the field is disabled; read-only they work")
    void arrowsDisabled() {
        input.search(view);
        input.setText("annax");
        assertTrue(upArrow().isDisabled());
        assertTrue(downArrow().isDisabled());

        input.setText("anna");
        assertFalse(upArrow().isDisabled());
        assertFalse(downArrow().isDisabled());

        input.setDisable(true);
        assertTrue(upArrow().isDisabled());
        downArrow().fire();
        assertEquals(0, view.getSearchMatchIndex());
        input.setDisable(false);

        input.setReadOnly(true);
        assertTrue(isShown(upArrow()));
        assertFalse(upArrow().isDisabled());
        downArrow().fire();
        assertEquals(1, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("in the theme, disabled arrows are dimmed as a disabled icon button; in a disabled field once, with it")
    void disabledArrowsLook() {
        Button disabledIcon = new Button();
        disabledIcon.getStyleClass().add("icon-button");
        disabledIcon.setDisable(true);
        input.search(view);
        input.setText("annax");
        layOut(input, disabledIcon);

        assertTrue(upArrow().getOpacity() < 1);
        assertEquals(disabledIcon.getOpacity(), upArrow().getOpacity(), 0.001);

        input.setText("anna");
        input.setDisable(true);
        input.applyCss();
        assertEquals(1, downArrow().getOpacity(), 0.001);
    }

    @Test
    @DisplayName("a click on an arrow leaves the focus where it is")
    void arrowClickKeepsTheFocus() {
        TextField other = new TextField();
        input.search(view);
        input.setText("anna");
        layOut(other, input);
        Scene scene = input.getScene();
        other.requestFocus();

        click(downArrow());
        assertSame(other, scene.getFocusOwner());

        input.requestFocus();
        click(downArrow());
        assertSame(input.getTextField(), scene.getFocusOwner());
        assertEquals(2, view.getSearchMatchIndex());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /**
     * Whether the referenced object is collected: the garbage collector is asked a few times, with some allocation in
     * between, since one request is a hint only.
     */
    private static boolean isCollected(WeakReference<?> reference) {
        for (int attempt = 0; attempt < 20 && reference.get() != null; attempt++) {
            System.gc();
            byte[][] garbage = new byte[64][];
            for (int index = 0; index < garbage.length; index++) {
                garbage[index] = new byte[16 * 1024];
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return reference.get() == null;
    }

    /** Asserts the tooltips and accessible texts of the up and the down arrow. */
    private void assertArrowTexts(String up, String down) {
        assertEquals(up, upArrow().getTooltip().getText());
        assertEquals(up, upArrow().getAccessibleText());
        assertEquals(down, downArrow().getTooltip().getText());
        assertEquals(down, downArrow().getAccessibleText());
    }

    /** Clicks the node with the primary button, as the mouse does: a press, a release and a click fired at it. */
    private static void click(Node node) {
        node.fireEvent(mouse(MouseEvent.MOUSE_PRESSED));
        node.fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
        node.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));
    }

    private static MouseEvent mouse(EventType<MouseEvent> type) {
        boolean down = type == MouseEvent.MOUSE_PRESSED;
        return new MouseEvent(type, 2, 2, 2, 2, MouseButton.PRIMARY, 1, false, false, false, false,
                down, false, false, false, false, true, null);
    }

    /** The up arrow of the field. */
    private Button upArrow() {
        return (Button) input.lookup(".form-search-up");
    }

    /** The down arrow of the field. */
    private Button downArrow() {
        return (Button) input.lookup(".form-search-down");
    }

    /** The clear button of the field. */
    private Button clearButton() {
        return (Button) input.lookup(".form-search-clear");
    }

    /** The icon a button shows. */
    private static FontIcon glyph(Labeled button) {
        return (FontIcon) button.getGraphic();
    }

    /** The counter of the field. */
    private Label counter() {
        return (Label) input.lookup(".form-search-count");
    }

    /** Whether the node is in the field and takes room, so that it shows while the field does. */
    private static boolean isShown(Node node) {
        return node != null && node.getParent() != null && node.isVisible() && node.isManaged();
    }
}
