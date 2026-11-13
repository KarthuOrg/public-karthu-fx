package org.karthu.fx.form;

import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The suggestion input with the application's own cells and the footer, through its API and its style classes, without
 * a window: a cell factory of cards of one to three lines, the popup's height measured from the rows a layout pass of
 * its content builds, the look of the marks in the theme, and the footer, in scenes with the theme that are never
 * shown. The popup's content is built and never shown: only the opening and closing of its window are replaced.
 */
class SuggestionInputCellsTest {

    /** The entries: cards of one, two and three lines, the first line the name. */
    private static final List<Card> CARDS = List.of(
            new Card("Anna Muster", List.of("Developer \u00B7 Engineering", "Bahnhofstrasse 1, Z\u00FCrich")),
            new Card("Ben Keller", List.of("Designer \u00B7 Product")),
            new Card("Hanna Meier", List.of()),
            new Card("Jan Novak", List.of("Tester \u00B7 Quality", "jan@example.org")),
            new Card("Lea Z\u00FCrcher", List.of("Writer \u00B7 Documentation")));

    /** A name far longer than a narrow field: a row cannot show it on one line. */
    private static final Card LONG_NAME =
            new Card("Anna-Barbara Kunigunde von Gr\u00FCnenfelden-Steinmann-Oberholzer", List.of());

    /** A card with lines far longer than a narrow field. */
    private static final Card LONG_CARD = new Card("Anna Lang", List.of(
            "Head of the department for long department names \u00B7 Engineering",
            "Bahnhofstrasse 100, 8001 Z\u00FCrich, Switzerland"));

    private PopupFreeInput input;
    private VBox root;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInput() {
        input = new PopupFreeInput();
        input.getItems().addAll(CARDS);
        input.setItemText(card -> card.name());
        input.setSearchTexts(card -> card.texts());
        input.setPrefWidth(320);
        root = new VBox(input);
        root.setFillWidth(false);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        root.applyCss();
        root.autosize();
        root.layout();
        input.requestFocus();
    }

    // --- Own cells -----------------------------------------------------------------------------------

    @Test
    @DisplayName("a cell factory replaces the default cell: the popup's rows are the application's cards")
    void cellFactoryReplacesTheDefaultCell() {
        input.setCellFactory(view -> new CardCell(input));

        type("an");
        layOutPopup();

        ListCell<Card> row = shownCell(0);
        assertInstanceOf(CardCell.class, row);
        assertEquals(List.of("[An]na Muster", "Developer \u00B7 Engineering", "Bahnhofstrasse 1, Z\u00FCrich"),
                lines(row));
        assertEquals(List.of("H[an]na Meier"), lines(shownCell(1)));
        assertEquals(List.of("J[an] Novak", "Tester \u00B7 Quality", "j[an]@example.org"), lines(shownCell(2)));
    }

    @Test
    @DisplayName("a new factory while the popup is open builds the shown rows anew; null brings the default rows back")
    void newFactoryWhileOpen() {
        assertNull(input.getCellFactory());
        type("an");
        layOutPopup();
        assertEquals(List.of("[An]na Muster"), lines(shownCell(0)));
        assertFalse(shownCell(0) instanceof CardCell);

        input.setCellFactory(view -> new CardCell(input));
        layOutPopup();
        assertInstanceOf(CardCell.class, shownCell(0));
        assertEquals(3, lines(shownCell(0)).size());

        input.cellFactoryProperty().set(null);
        layOutPopup();
        assertFalse(shownCell(0) instanceof CardCell);
        assertEquals(List.of("[An]na Muster"), lines(shownCell(0)));
    }

    @Test
    @DisplayName("a cell factory that returns no cell is refused with a message")
    void factoryWithoutACell() {
        input.setCellFactory(view -> null);

        IllegalStateException refused = assertThrows(IllegalStateException.class, () -> builtCell(0));
        assertTrue(refused.getMessage().contains("cell factory"));
    }

    @Test
    @DisplayName("a click on the application's row applies its entry, though its cell does nothing for clicks")
    void clickOnAnOwnRowApplies() {
        input.setCellFactory(view -> new CardCell(input));
        List<Card> chosen = new ArrayList<>();
        input.setOnItemChosen(chosen::add);
        type("an");

        ListCell<Card> row = builtCell(2);
        assertInstanceOf(CardCell.class, row);
        row.fireEvent(mouse(MouseEvent.MOUSE_CLICKED, MouseButton.PRIMARY));

        assertEquals("Jan Novak", input.getText());
        assertSame(CARDS.get(3), input.getChosenItem());
        assertEquals(List.of(CARDS.get(3)), chosen);
        assertFalse(input.isPopupOpen());
    }

    @Test
    @DisplayName("the application's preselected row takes the selection look; the marks keep theirs on it")
    void ownRowTakesTheSelectionLook() {
        input.setCellFactory(view -> new CardCell(input));
        Region selection = FormTestSupport.tokenSwatch("-fx-selection-color");
        Region selectionText = FormTestSupport.tokenSwatch("-fx-selection-text-color");
        Region text = FormTestSupport.tokenSwatch("-fx-text-color");
        FormTestSupport.layOut(selection, selectionText, text);
        type("an");
        press(KeyCode.DOWN);
        layOutPopup();

        ListCell<Card> preselected = shownCell(1);
        ListCell<Card> other = shownCell(2);
        assertTrue(preselected.getStyleClass().contains("list-cell"));
        assertTrue(FormTestSupport.hasPseudoClass(preselected, "selected"));
        assertFalse(FormTestSupport.hasPseudoClass(other, "selected"));
        assertEquals(FormTestSupport.fill(selection), FormTestSupport.fill(preselected));
        assertNotEquals(FormTestSupport.fill(selection), FormTestSupport.fill(other));

        // H[an]na Meier, the name strong: the mark bold and underlined, on the preselected row in its text colour.
        assertEquals(List.of("H[an]na Meier"), lines(preselected));
        for (Text run : runs(preselected)) {
            assertEquals(FormTestSupport.fill(selectionText), run.getFill(), () -> "not the selection's: " + run);
        }
        assertLook("H", true, false, runs(preselected).get(0));
        assertLook("an", true, true, runs(preselected).get(1));
        // J[an] Novak, the name strong; j[an]@example.org, a plain line: its mark bold, not underlined.
        List<Text> otherRuns = runs(other);
        assertLook("an", true, true, otherRuns.get(1));
        assertLook("Tester \u00B7 Quality", false, false, otherRuns.get(3));
        assertLook("j", false, false, otherRuns.get(4));
        assertLook("an", true, false, otherRuns.get(5));
        for (Text run : otherRuns) {
            assertEquals(FormTestSupport.fill(text), run.getFill(), () -> "not the text colour: " + run);
        }
    }

    @Test
    @DisplayName("outside the popup a strong highlight shows its runs bold, its marks underlined in the accent colour")
    void strongHighlightInAnApplicationsRow() {
        Region accent = FormTestSupport.tokenSwatch("-fx-accent-color");
        Region text = FormTestSupport.tokenSwatch("-fx-text-color");
        input.setText("mus");
        TextFlow plain = input.highlight("Anna Muster");
        TextFlow strong = input.highlight("Anna Muster");
        strong.getStyleClass().add("search-strong");
        FormTestSupport.layOut(accent, text, plain, strong);

        assertLook("Anna ", false, false, (Text) plain.getChildren().get(0));
        assertLook("Mus", true, false, (Text) plain.getChildren().get(1));
        assertLook("Anna ", true, false, (Text) strong.getChildren().get(0));
        assertLook("Mus", true, true, (Text) strong.getChildren().get(1));
        assertLook("ter", true, false, (Text) strong.getChildren().get(2));
        assertEquals(FormTestSupport.fill(accent), ((Text) strong.getChildren().get(1)).getFill());
        assertEquals(FormTestSupport.fill(text), ((Text) strong.getChildren().get(2)).getFill());
    }

    // --- The popup's height --------------------------------------------------------------------------

    @Test
    @DisplayName("fewer rows than visibleRowCount: the popup's list is as tall as its rows, each as high as it is")
    void fewerRowsThanVisibleRowCount() {
        input.setCellFactory(view -> new CardCell(input));
        assertEquals(8, input.getVisibleRowCount());

        // All five cards: three, two, one, three and two lines.
        type("er");
        layOutPopup();

        List<Double> heights = rowHeights(5);
        assertTrue(new HashSet<>(heights).size() >= 3, () -> "rows of different heights expected: " + heights);
        assertListHeight(heights);
        assertFalse(scrolls(), "the list scrolls with five rows of eight");
    }

    @Test
    @DisplayName("more rows than visibleRowCount: the popup's list shows that many, measured by their heights, then scrolls")
    void moreRowsThanVisibleRowCount() {
        input.setCellFactory(view -> new CardCell(input));
        input.setVisibleRowCount(3);

        type("er");
        layOutPopup();

        List<Double> heights = rowHeights(3);
        assertTrue(new HashSet<>(heights).size() >= 2, () -> "rows of different heights expected: " + heights);
        assertListHeight(heights);
        assertTrue(scrolls(), "the list does not scroll with five rows of three");
    }

    @Test
    @DisplayName("by default eight rows show, then the list scrolls; a smaller count while open shrinks the list")
    void defaultEightRowsThenScrolls() {
        for (int number = 1; number <= 12; number++) {
            input.getItems().add(new Card("Card " + number, number % 2 == 0 ? List.of("Line " + number) : List.of()));
        }
        input.setCellFactory(view -> new CardCell(input));
        type("card");
        layOutPopup();

        assertEquals(12, list().getItems().size());
        assertListHeight(rowHeights(8));
        assertTrue(scrolls());

        input.setVisibleRowCount(2);
        layOutPopup();
        assertListHeight(rowHeights(2));
    }

    @Test
    @DisplayName("the default rows count alike; a visibleRowCount below one shows one row")
    void defaultRowsAndAtLeastOne() {
        type("an");
        layOutPopup();
        assertListHeight(rowHeights(3));

        input.setVisibleRowCount(0);
        layOutPopup();
        assertListHeight(rowHeights(1));
        assertTrue(scrolls());
    }

    @Test
    @DisplayName("Page Down and Page Up move visibleRowCount rows")
    void pageKeysMoveVisibleRowCountRows() {
        input.setVisibleRowCount(2);
        type("er");

        press(KeyCode.PAGE_DOWN);
        assertEquals(2, list().getSelectionModel().getSelectedIndex());
        press(KeyCode.PAGE_DOWN);
        press(KeyCode.PAGE_DOWN);
        assertEquals(4, list().getSelectionModel().getSelectedIndex());
        press(KeyCode.PAGE_UP);
        assertEquals(2, list().getSelectionModel().getSelectedIndex());
    }

    @Test
    @DisplayName("the first measure, before the list has laid out a row, is already its rows' height: the application's")
    void firstMeasureOfOwnRows() {
        input.setCellFactory(view -> new CardCell(input));
        input.setVisibleRowCount(4);
        type("er");

        assertFirstMeasureHolds(4);
    }

    @Test
    @DisplayName("the first measure, before the list has laid out a row, is already its rows' height: the default rows")
    void firstMeasureOfDefaultRows() {
        type("an");

        assertFirstMeasureHolds(3);
    }

    // --- Rows never wider than the popup -------------------------------------------------------------

    @Test
    @DisplayName("a long default row in a narrow field gets the list's width: one line, clipped with an ellipsis, no "
            + "horizontal scroll bar")
    void longDefaultRowGetsTheListsWidth() {
        input.getItems().add(LONG_NAME);
        narrowField(160);

        // Anna Muster, Hanna Meier and the long name.
        type("anna");
        layOutPopup();

        assertFalse(scrollsSideways(), "the popup's list scrolls sideways");
        ListCell<Card> shortRow = shownCell(0);
        ListCell<Card> longRow = shownCell(2);
        assertEquals(rowWidth(), shortRow.getWidth(), 0.5);
        assertEquals(rowWidth(), longRow.getWidth(), 0.5);
        assertEquals(shortRow.getHeight(), longRow.getHeight(), 0.5, "the long row is not one line high");
        assertFalse(isShown(ellipsis(shortRow)), "a row that fits shows an ellipsis");

        // The whole text with its marks in the row, the part that does not fit hidden, the ellipsis after the rest.
        assertEquals(List.of("[Anna]-Barbara Kunigunde von Gr\u00FCnenfelden-Steinmann-Oberholzer"), lines(longRow));
        Text ellipsis = (Text) ellipsis(longRow).lookup(".search-text");
        assertEquals("\u2026", ellipsis.getText());
        assertTrue(isShown(ellipsis(longRow)), "the long row shows no ellipsis");
        double rowRight = sceneBounds(longRow).getMaxX() - longRow.snappedRightInset();
        double shownRight = sceneBounds(flows(longRow).getFirst()).getMaxX();
        double ellipsisLeft = sceneBounds(ellipsis).getMinX();
        assertTrue(shownRight <= ellipsisLeft + 0.5, "the text shows under the ellipsis: " + shownRight);
        assertTrue(shownRight > rowRight - 40, "the text is cut far before the row's end: " + shownRight);
        assertTrue(sceneBounds(ellipsis).getMaxX() <= rowRight + 0.5, "the ellipsis ends after the row");
    }

    @Test
    @DisplayName("a long row of the application gets the list's width too: its card wraps its lines, no horizontal "
            + "scroll bar, and the list stays as tall as its rows")
    void longOwnRowGetsTheListsWidth() {
        input.getItems().add(LONG_CARD);
        input.setCellFactory(view -> new CardCell(input));
        narrowField(200);

        // Anna Muster, Hanna Meier and the long card.
        type("anna");
        layOutPopup();

        assertFalse(scrollsSideways(), "the popup's list scrolls sideways");
        ListCell<Card> longRow = shownCell(2);
        assertEquals(rowWidth(), shownCell(0).getWidth(), 0.5);
        assertEquals(rowWidth(), longRow.getWidth(), 0.5);
        List<TextFlow> lines = flows(longRow);
        assertTrue(lines.get(1).getHeight() > 1.5 * lines.get(0).getHeight(), "the long line does not wrap");
        double rowRight = sceneBounds(longRow).getMaxX() - longRow.snappedRightInset();
        for (TextFlow line : lines) {
            // The box the row gives the line, not the ink of its text, which a font may let reach a pixel over it.
            double lineRight = line.localToScene(line.getLayoutBounds()).getMaxX();
            assertTrue(lineRight <= rowRight + 0.5, "a line ends after the row: " + marked(line));
        }
        assertListHeight(rowHeights(3));
    }

    @Test
    @DisplayName("with more rows than show, the rows get the list's width beside its vertical scroll bar, and the "
            + "measure of wrapped rows holds at every width of the field")
    void longRowsBesideTheScrollBar() {
        input.getItems().add(0, LONG_CARD);
        input.setCellFactory(view -> new CardCell(input));
        input.setVisibleRowCount(2);
        narrowField(200);

        // The long card, Anna Muster and Hanna Meier: two of three show.
        type("anna");

        assertFirstMeasureHolds(2);
        assertTrue(scrolls(), "the list does not scroll with three rows of two");
        assertFalse(scrollsSideways(), "the popup's list scrolls sideways");
        assertEquals(rowWidth() - verticalBar().getWidth(), shownCell(0).getWidth(), 0.5);
        assertEquals(rowWidth() - verticalBar().getWidth(), shownCell(1).getWidth(), 0.5);
        // Field by field narrower: somewhere a line of the long card breaks only in the scroll bar's room, which the
        // measure must leave out, as the list does.
        for (double width = 320; width >= 160; width -= 2) {
            narrowField(width);
            assertFirstMeasureHolds(2);
        }
    }

    // --- Footer --------------------------------------------------------------------------------------

    @Test
    @DisplayName("the footer is off by default: built, it takes no room in the popup")
    void footerIsOffByDefault() {
        assertFalse(input.isFooterVisible());

        type("an");

        assertTrue(footer().getStyleClass().contains("form-suggestion-footer"));
        assertFalse(isShown(footer()));
    }

    @Test
    @DisplayName("turned on, the footer shows the matches and all entries on the left and the key hints on the right")
    void footerShowsCountAndHints() {
        input.setFooterVisible(true);

        type("an");

        assertTrue(isShown(footer()));
        assertEquals("3 of 5", footerCount().getText());
        assertEquals("\u2191\u2193 select \u00B7 Enter apply \u00B7 Esc close", footerHint().getText());
        assertEquals("{0} of {1}", input.getFooterCountFormat());
        assertEquals("\u2191\u2193 select \u00B7 Enter apply \u00B7 Esc close", input.getFooterHintText());
    }

    @Test
    @DisplayName("the footer's count follows the typing, No matches included, and changes of the entries")
    void footerCountFollowsTheMatches() {
        input.setFooterVisible(true);
        type("an");

        type("n");
        assertEquals("2 of 5", footerCount().getText());
        // Not "x" typed alone: see SuggestionInputTest.noMatchEnterOnlyCloses.
        type("qqq");
        assertEquals("0 of 5", footerCount().getText());
        assertTrue(isShown(footer()));
        assertTrue(isShown((Node) popup().lookup(".form-suggestion-empty")));

        input.setText("");
        type("an");
        input.getItems().add(new Card("Dana Frei", List.of()));
        assertEquals("4 of 6", footerCount().getText());
        input.getItems().remove(0);
        assertEquals("3 of 5", footerCount().getText());
    }

    @Test
    @DisplayName("the footer's texts are properties: an application translates them; null shows nothing")
    void footerTexts() {
        input.setFooterVisible(true);
        input.setFooterCountFormat("{0} of {1} people");
        input.setFooterHintText("\u2191\u2193 w\u00E4hlen");
        type("an");

        assertEquals("3 of 5 people", footerCount().getText());
        assertEquals("\u2191\u2193 w\u00E4hlen", footerHint().getText());

        input.footerCountFormatProperty().set(null);
        input.footerHintTextProperty().set(null);
        assertEquals("", footerCount().getText());
        assertEquals("", footerHint().getText());
    }

    @Test
    @DisplayName("in the theme the footer lies under the rows, the count left and the hints right, both muted")
    void footerLook() {
        Region muted = FormTestSupport.tokenSwatch("-fx-text-secondary-color");
        FormTestSupport.layOut(muted);
        input.setFooterVisible(true);
        input.setCellFactory(view -> new CardCell(input));
        type("an");
        layOutPopup();

        double listBottom = list().getBoundsInParent().getMaxY();
        assertTrue(footer().getBoundsInParent().getMinY() >= listBottom, "the footer is not under the rows");
        double countRight = footerCount().localToScene(footerCount().getLayoutBounds()).getMaxX();
        double hintLeft = footerHint().localToScene(footerHint().getLayoutBounds()).getMinX();
        double hintRight = footerHint().localToScene(footerHint().getLayoutBounds()).getMaxX();
        double footerRight = footer().localToScene(footer().getLayoutBounds()).getMaxX();
        assertTrue(hintLeft > countRight, "the hints do not follow the count");
        assertTrue(footerRight - hintRight < 12, "the hints are not at the right: " + hintRight + " of " + footerRight);
        assertEquals(FormTestSupport.fill(muted), footerCount().getTextFill());
        assertEquals(FormTestSupport.fill(muted), footerHint().getTextFill());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** The popup's footer. */
    private Region footer() {
        return (Region) popup().lookup(".form-suggestion-footer");
    }

    /** The footer's count, on its left. */
    private Label footerCount() {
        return (Label) popup().lookup(".form-suggestion-footer-count");
    }

    /** The footer's key hints, on its right. */
    private Label footerHint() {
        return (Label) popup().lookup(".form-suggestion-footer-hint");
    }

    /** Whether the node is in the popup and takes room, so that it shows while the popup does. */
    private static boolean isShown(Node node) {
        return node != null && node.getParent() != null && node.isVisible() && node.isManaged();
    }

    /**
     * Checks that the popup's content asks for its final height in its first CSS pass, before the list has laid out a
     * row: the popup's window takes the size its content asks for when it shows, before any layout. The final height is
     * the one after {@link #layOutPopup()}, where the list is as tall as the rows it shows, measured.
     */
    private void assertFirstMeasureHolds(int rows) {
        ThemeManager.apply(popup().getScene());
        popup().applyCss();
        double first = popup().prefHeight(-1);

        layOutPopup();

        assertListHeight(rowHeights(rows));
        assertEquals(popup().getHeight(), first, 0.5, "the first measure differs from the laid out height");
    }

    /** The heights of the first rows the list shows, after {@link #layOutPopup()}, top to bottom. */
    private List<Double> rowHeights(int rows) {
        List<Double> heights = new ArrayList<>();
        for (int index = 0; index < rows; index++) {
            heights.add(shownCell(index).getHeight());
        }
        return heights;
    }

    /**
     * Checks the list's height against the rows it should show, measured: their sum, and at most the one pixel a combo
     * box's list adds, so that rounding never brings up a scroll bar, rounded up to whole pixels.
     */
    private void assertListHeight(List<Double> heights) {
        double sum = 0;
        for (double height : heights) {
            assertTrue(height > 10, () -> "a row without height: " + heights);
            sum += height;
        }
        double listHeight = list().getHeight();
        assertTrue(listHeight >= sum && listHeight <= Math.ceil(sum + 1),
                "the list is " + listHeight + " high for rows of " + heights + ", " + sum + " in all");
    }

    /** Whether the list shows its vertical scroll bar. */
    private boolean scrolls() {
        for (Node node : list().lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL && bar.isVisible()) {
                return true;
            }
        }
        return false;
    }

    /** Whether the list shows its horizontal scroll bar. */
    private boolean scrollsSideways() {
        for (Node node : list().lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.HORIZONTAL && bar.isVisible()) {
                return true;
            }
        }
        return false;
    }

    /** The list's vertical scroll bar. */
    private ScrollBar verticalBar() {
        for (Node node : list().lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                return bar;
            }
        }
        throw new AssertionError("the list has no vertical scroll bar");
    }

    /** The width of the list inside its insets, after {@link #layOutPopup()}: the room its rows have. */
    private double rowWidth() {
        return list().getWidth() - list().snappedLeftInset() - list().snappedRightInset();
    }

    /** Gives the field a narrower width and lays it out again. */
    private void narrowField(double width) {
        input.setPrefWidth(width);
        root.applyCss();
        root.autosize();
        root.layout();
    }

    /** The ellipsis of a default row, which shows while the row's text does not fit. */
    private static Node ellipsis(ListCell<?> cell) {
        Node ellipsis = cell.lookup(".form-suggestion-ellipsis");
        assertNotNull(ellipsis, "the default row has no ellipsis");
        return ellipsis;
    }

    /** The node's bounds in its scene, its clip included. */
    private static Bounds sceneBounds(Node node) {
        return node.localToScene(node.getBoundsInLocal());
    }

    /** Types the characters into the field's text field, one key at a time, as the user does. */
    private void type(String characters) {
        for (int index = 0; index < characters.length(); index++) {
            String character = String.valueOf(characters.charAt(index));
            input.getTextField().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED,
                    false, false, false, false));
        }
    }

    /** Fires a key press without modifiers at the text field, where the keyboard sends it. */
    private void press(KeyCode code) {
        input.getTextField().fireEvent(FormTestSupport.keyPressed(code));
    }

    /** The popup's content, built and never shown. */
    private Region popup() {
        return input.popupContent();
    }

    /** The popup's list. */
    @SuppressWarnings("unchecked")
    private ListView<Card> list() {
        return (ListView<Card>) popup().lookup(".form-suggestion-list");
    }

    /**
     * Lays the popup's content out in its own scene with the theme, never shown: the list builds its rows in a layout
     * pass, the next CSS pass styles them, and the next layout pass measures the styled rows.
     */
    private void layOutPopup() {
        ThemeManager.apply(popup().getScene());
        for (int pass = 0; pass < 3; pass++) {
            popup().applyCss();
            popup().autosize();
            popup().layout();
        }
    }

    /** The row the popup's list itself shows for the entry at the index, after {@link #layOutPopup()}. */
    @SuppressWarnings("unchecked")
    private ListCell<Card> shownCell(int index) {
        for (Node node : list().lookupAll(".list-cell")) {
            ListCell<Card> cell = (ListCell<Card>) node;
            if (cell.isVisible() && cell.getIndex() == index && cell.getParent() != null
                    && cell.getParent().isVisible()) {
                return cell;
            }
        }
        throw new AssertionError("the list shows no row for the entry at " + index);
    }

    /** A cell of the list's own cell factory, showing the entry at the index, as the popup builds its rows. */
    private ListCell<Card> builtCell(int index) {
        ListCell<Card> cell = list().getCellFactory().call(list());
        cell.updateListView(list());
        cell.updateIndex(index);
        return cell;
    }

    /** The lines a row shows, each with its marks in brackets: {@code [An]na Muster}. */
    private static List<String> lines(ListCell<?> cell) {
        List<String> lines = new ArrayList<>();
        for (TextFlow flow : flows(cell)) {
            lines.add(marked(flow));
        }
        return lines;
    }

    /**
     * The highlight nodes a row shows, top to bottom: the lines of a card, or the default row's one, the first highlight
     * node in its line (the ellipsis after it is none).
     */
    private static List<TextFlow> flows(ListCell<?> cell) {
        List<TextFlow> flows = new ArrayList<>();
        Node graphic = cell.getGraphic();
        if (graphic instanceof VBox box) {
            for (Node child : box.getChildren()) {
                flows.add((TextFlow) child);
            }
        } else if (graphic != null) {
            flows.add((TextFlow) graphic.lookup(".search-highlight"));
        }
        return flows;
    }

    /** The runs a row shows, line by line, left to right. */
    private static List<Text> runs(ListCell<?> cell) {
        List<Text> runs = new ArrayList<>();
        for (TextFlow flow : flows(cell)) {
            for (Node child : flow.getChildren()) {
                runs.add((Text) child);
            }
        }
        return runs;
    }

    /** Checks a run's text and its look in the theme: bold or not, underlined or not. */
    private static void assertLook(String expected, boolean bold, boolean underlined, Text run) {
        assertEquals(expected, run.getText());
        assertEquals(bold, run.getFont().getStyle().toLowerCase(Locale.ROOT).contains("bold"),
                () -> "bold " + !bold + ": " + run + ", " + run.getFont());
        assertEquals(underlined, run.isUnderline(), () -> "underlined " + !underlined + ": " + run);
    }

    /** The text of a highlight node, with its marks in brackets. */
    private static String marked(TextFlow flow) {
        StringBuilder marked = new StringBuilder();
        for (Node child : flow.getChildren()) {
            Text run = (Text) child;
            if (run.getStyleClass().contains("search-match")) {
                marked.append('[').append(run.getText()).append(']');
            } else {
                marked.append(run.getText());
            }
        }
        return marked.toString();
    }

    /** A mouse event of a button at the top left, still since its press. */
    private static MouseEvent mouse(EventType<MouseEvent> type, MouseButton button) {
        return new MouseEvent(type, 2, 2, 2, 2, button, 1, false, false, false, false,
                button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY, false, false, true, null);
    }

    /**
     * An entry of the tests: a card with a name, the item text, and further lines; the search texts are all of them.
     *
     * @param name  the name, the card's first line
     * @param lines the further lines, none to two
     */
    private record Card(String name, List<String> lines) {

        /** The texts the card shows, the name first: its search texts. */
        List<String> texts() {
            List<String> texts = new ArrayList<>();
            texts.add(name);
            texts.addAll(lines);
            return texts;
        }
    }

    /**
     * The application's cell of the tests, a plain JavaFX cell: a card with the name and its further lines, one
     * highlight node per line, the name strong ({@code search-strong}). It does nothing for clicks or the selection.
     */
    private static final class CardCell extends ListCell<Card> {

        private final SuggestionInput<Card> field;

        CardCell(SuggestionInput<Card> field) {
            this.field = field;
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Card card, boolean empty) {
            super.updateItem(card, empty);
            setText(null);
            if (empty || card == null) {
                setGraphic(null);
                return;
            }
            TextFlow name = field.highlight(card.name());
            name.getStyleClass().add("search-strong");
            VBox lines = new VBox(name);
            for (String line : card.lines()) {
                lines.getChildren().add(field.highlight(line));
            }
            setGraphic(lines);
        }
    }

    /**
     * A suggestion input over cards whose popup window neither opens nor closes, as a popup needs a shown window: the
     * tests' seam. Everything else is the field's own.
     */
    private static final class PopupFreeInput extends SuggestionInput<Card> {

        @Override
        void showPopupWindow() {
            // No window: the tests' seam.
        }

        @Override
        void hidePopupWindow() {
            // No window: the tests' seam.
        }
    }
}
