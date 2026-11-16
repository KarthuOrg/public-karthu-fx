package org.karthu.fx.form;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.ThemeManager;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The suggestion input's chevron through its API and its style class, without a window: off by default, its place and
 * look when turned on, the clicks that open and close the popup (its window replaced, never shown), and the field's
 * states, in a scene with the theme that is never shown.
 */
class SuggestionInputChevronTest {

    /** The entries of the recording input, in their order. */
    private static final List<String> NAMES =
            List.of("Anna Muster", "Ben Keller", "Hanna Meier", "Jan Novak", "Lea Zürcher");

    private RecordingInput input;
    private TextField other;
    private VBox root;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInput() {
        input = new RecordingInput();
        input.getItems().addAll(NAMES);
        other = new TextField();
        root = new VBox(input, other);
        root.setFillWidth(false);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        layOut();
    }

    // --- Off by default ------------------------------------------------------------------------------

    @Test
    @DisplayName("off by default: no chevron shows or takes room, the field lays out as a search field")
    void offByDefault() {
        SearchInput search = new SearchInput();
        search.setText("an");
        input.setText("an");
        root.getChildren().add(search);
        layOut();
        assertEquals(search.prefWidth(-1), input.prefWidth(-1), 0.01);

        input.setPrefWidth(300);
        search.setPrefWidth(300);
        layOut();

        assertFalse(input.isChevronVisible());
        assertFalse(isShown(chevron()));
        assertEquals(search.getTextField().getWidth(), input.getTextField().getWidth(), 0.01);
        assertEquals(search.getHeight(), input.getHeight(), 0.01);
    }

    // --- Turned on ----------------------------------------------------------------------------------

    @Test
    @DisplayName("turned on: a chevron down at the right end after the x, as the combo input's, out of the tab order")
    void turnedOnAtTheRightEnd() {
        ComboInput<String> combo = new ComboInput<>();
        root.getChildren().add(combo);
        input.setPrefWidth(300);
        input.setText("an");
        double heightWithout = input.prefHeight(-1);

        input.setChevronVisible(true);
        layOut();

        Button chevron = chevron();
        assertTrue(isShown(chevron));
        assertTrue(chevron.getStyleClass().containsAll(List.of("form-frame-icon", "icon-button")));
        assertFalse(chevron.isFocusTraversable());
        FontIcon glyph = assertInstanceOf(FontIcon.class, chevron.getGraphic());
        assertEquals(BootstrapIcons.CHEVRON_DOWN, glyph.getIconCode());
        Button clear = (Button) input.lookup(".form-search-clear");
        assertTrue(clear.getBoundsInParent().getMaxX() <= chevron.getBoundsInParent().getMinX());
        assertTrue(input.getTextField().getBoundsInParent().getMaxX() <= clear.getBoundsInParent().getMinX());
        assertEquals(input.getWidth() - input.snappedRightInset(), chevron.getBoundsInParent().getMaxX(), 0.01);

        Button comboChevron = (Button) combo.lookup(".form-combo-chevron");
        FontIcon comboGlyph = (FontIcon) comboChevron.getGraphic();
        assertEquals(comboChevron.getWidth(), chevron.getWidth(), 0.01);
        assertEquals(comboChevron.getHeight(), chevron.getHeight(), 0.01);
        assertEquals(comboGlyph.getIconSize(), glyph.getIconSize());
        assertEquals(comboGlyph.getIconColor(), glyph.getIconColor());
        assertEquals(heightWithout, input.getHeight(), 0.01);
    }

    @Test
    @DisplayName("turned off again, the chevron leaves: the text field gets its room back")
    void turnedOffAgain() {
        input.setPrefWidth(300);
        layOut();
        double textWidth = input.getTextField().getWidth();

        input.setChevronVisible(true);
        layOut();
        assertTrue(input.getTextField().getWidth() < textWidth);

        input.setChevronVisible(false);
        layOut();
        assertFalse(isShown(chevron()));
        assertEquals(textWidth, input.getTextField().getWidth(), 0.01);
    }

    // --- Clicks -------------------------------------------------------------------------------------

    @Test
    @DisplayName("a click in the empty field opens the popup with every entry, the first preselected, as Down does")
    void clickInTheEmptyFieldOpensEveryEntry() {
        input.setChevronVisible(true);
        layOut();
        input.requestFocus();

        chevron().fire();

        assertTrue(input.isPopupOpen());
        assertEquals(1, input.windowsShown);
        assertEquals(NAMES, list().getItems());
        assertEquals(0, list().getSelectionModel().getSelectedIndex());
    }

    @Test
    @DisplayName("a click with text opens the popup with the matches of the text, as Down does; the text stays")
    void clickWithTextOpensTheMatches() {
        input.setChevronVisible(true);
        layOut();
        input.setText("an");
        input.requestFocus();
        assertFalse(input.isPopupOpen());

        chevron().fire();

        assertTrue(input.isPopupOpen());
        assertEquals(List.of("Anna Muster", "Hanna Meier", "Jan Novak"), list().getItems());
        assertEquals("Anna Muster", list().getSelectionModel().getSelectedItem());
        assertEquals("an", input.getText());
    }

    @Test
    @DisplayName("a click on the chevron of the open popup only closes it: its press keeps it open, the click closes it")
    void clickOfTheOpenPopupCloses() {
        input.setChevronVisible(true);
        layOut();
        input.requestFocus();
        chevron().fire();
        assertTrue(input.isPopupOpen());

        assertTrue(pressReaches(chevron()));
        assertTrue(input.isPopupOpen());
        chevron().fireEvent(mouse(MouseEvent.MOUSE_RELEASED));

        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals(1, input.windowsShown);
        assertEquals("", input.getText());
        assertSame(input.getTextField(), input.getScene().getFocusOwner());

        chevron().fire();
        assertTrue(input.isPopupOpen());
        assertEquals(2, input.windowsShown);
    }

    @Test
    @DisplayName("a click gives the text field the focus, from another field, so the keys move through the popup")
    void clickFocusesTheTextField() {
        input.setChevronVisible(true);
        layOut();
        other.requestFocus();

        chevron().fire();

        assertSame(input.getTextField(), input.getScene().getFocusOwner());
        assertTrue(input.isPopupOpen());
        input.getTextField().fireEvent(FormTestSupport.keyPressed(KeyCode.DOWN));
        assertEquals("Ben Keller", list().getSelectionModel().getSelectedItem());
        other.requestFocus();
        assertFalse(input.isPopupOpen());
    }

    // --- Tooltip ------------------------------------------------------------------------------------

    @Test
    @DisplayName("the tooltip and accessible text: Show suggestions, a property the application sets; empty for none")
    void tooltipText() {
        input.setChevronVisible(true);
        layOut();
        assertEquals("Show suggestions", input.getChevronText());
        assertEquals("Show suggestions", chevron().getTooltip().getText());
        assertEquals("Show suggestions", chevron().getAccessibleText());

        input.setChevronText("Recent files");
        assertEquals("Recent files", chevron().getTooltip().getText());
        assertEquals("Recent files", chevron().getAccessibleText());

        input.setChevronText("");
        assertNull(chevron().getTooltip());
        input.chevronTextProperty().set(null);
        assertNull(chevron().getTooltip());
        input.setChevronText("Letzte Dateien");
        assertEquals("Letzte Dateien", chevron().getTooltip().getText());
    }

    // --- Read-only and disabled ---------------------------------------------------------------------

    @Test
    @DisplayName("read-only: the chevron stays, greyed out as a disabled icon, and a click opens nothing; back after")
    void readOnlyGreysTheChevronOut() {
        input.setChevronVisible(true);
        input.setReadOnly(true);
        Button disabledIcon = new Button();
        disabledIcon.getStyleClass().add("icon-button");
        disabledIcon.setDisable(true);
        root.getChildren().add(disabledIcon);
        layOut();
        input.requestFocus();

        assertTrue(isShown(chevron()));
        assertTrue(chevron().isDisabled());
        assertEquals(disabledIcon.getOpacity(), chevron().getOpacity(), 0.001);
        assertTrue(chevron().getOpacity() < 1);
        chevron().fire();
        pressReaches(chevron());
        chevron().fireEvent(mouse(MouseEvent.MOUSE_RELEASED));
        assertFalse(input.isPopupOpen());
        assertEquals(0, input.windowsShown);

        input.setReadOnly(false);
        layOut();
        assertFalse(chevron().isDisabled());
        assertEquals(1, chevron().getOpacity(), 0.001);
        chevron().fire();
        assertTrue(input.isPopupOpen());
    }

    @Test
    @DisplayName("disabled: the chevron is greyed out with the field, dimmed once, and a click opens nothing")
    void disabledGreysTheChevronOutWithTheField() {
        input.setChevronVisible(true);
        input.setDisable(true);
        Button disabledButton = new Button("button");
        disabledButton.setDisable(true);
        root.getChildren().add(disabledButton);
        layOut();

        assertTrue(isShown(chevron()));
        assertTrue(chevron().isDisabled());
        assertEquals(disabledButton.getOpacity(), input.getOpacity(), 0.001);
        assertEquals(1, chevron().getOpacity(), 0.001);
        chevron().fire();
        assertFalse(input.isPopupOpen());
        assertEquals(0, input.windowsShown);
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A CSS and layout pass of the scene's root at its preferred size. */
    private void layOut() {
        root.applyCss();
        root.autosize();
        root.layout();
    }

    /** The input's chevron, by its style class. */
    private Button chevron() {
        return (Button) input.lookup(".form-suggestion-chevron");
    }

    /** The popup's list. */
    @SuppressWarnings("unchecked")
    private ListView<String> list() {
        return (ListView<String>) input.popupContent().lookup(".form-suggestion-list");
    }

    /**
     * Presses the primary button on a node of the scene and returns whether the press reached the node. While the
     * popup's window shows, the window around it hands every press to the field first, a filter of the window; with no
     * window here, the same filter sits on the scene's root, so the press passes it on its way to the node.
     */
    private boolean pressReaches(Node target) {
        List<Event> reached = new ArrayList<>();
        EventHandler<MouseEvent> windowFilter = input::pressedInWindow;
        EventHandler<Event> recorder = reached::add;
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, windowFilter);
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, recorder);
        target.fireEvent(mouse(MouseEvent.MOUSE_PRESSED));
        target.removeEventFilter(MouseEvent.MOUSE_PRESSED, recorder);
        root.removeEventFilter(MouseEvent.MOUSE_PRESSED, windowFilter);
        return !reached.isEmpty();
    }

    /** A mouse event of the primary button at the top left, still since its press. */
    private static MouseEvent mouse(EventType<MouseEvent> type) {
        return new MouseEvent(type, 2, 2, 2, 2, MouseButton.PRIMARY, 1, false, false, false, false,
                true, false, false, false, false, true, null);
    }

    /** Whether the node is in the field and takes room, so that it shows. */
    private static boolean isShown(Node node) {
        return node != null && node.getParent() != null && node.isVisible() && node.isManaged();
    }

    /**
     * A suggestion input over names whose popup window neither opens nor closes, as a popup needs a shown window: it
     * records each opening and whether the window would show, the tests' seam. Everything else is the field's own.
     */
    private static final class RecordingInput extends SuggestionInput<String> {

        private int windowsShown;
        private boolean windowShowing;

        @Override
        void showPopupWindow() {
            windowsShown++;
            windowShowing = true;
        }

        @Override
        void hidePopupWindow() {
            windowShowing = false;
        }
    }
}
