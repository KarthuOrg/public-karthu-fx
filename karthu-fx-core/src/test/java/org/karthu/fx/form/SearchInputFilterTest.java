package org.karthu.fx.form;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The search field over a list, through its public API and its style classes, without a window: the list filter on a
 * real {@link FilteredList}, the counter, and the highlight node an application's cells show, in a scene with the theme
 * that is never shown.
 */
class SearchInputFilterTest {

    /** An entry of the sample list: a workflow, whose row shows its name. */
    private record Workflow(String name) {
    }

    private SearchInput input;
    private ObservableList<Workflow> workflows;
    private FilteredList<Workflow> filtered;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInputAndList() {
        input = new SearchInput();
        workflows = FXCollections.observableArrayList(new Workflow("TEST-FILE"), new Workflow("Import Z\u00fcrich"),
                new Workflow("Nightly report"), new Workflow("test import"));
        filtered = new FilteredList<>(workflows);
    }

    // --- List filter ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a connected list keeps the entries whose text matches as the text is set: every word, case and accents folded")
    void textFiltersTheList() {
        input.filter(filtered, Workflow::name);
        assertEquals(names(workflows), names(filtered), "connected to an empty field, the list shows everything");

        input.setText("file");
        assertEquals(List.of("TEST-FILE"), names(filtered));

        input.setText("zurich");
        assertEquals(List.of("Import Z\u00fcrich"), names(filtered));

        input.setText("import TEST");
        assertEquals(List.of("test import"), names(filtered));

        input.setText("");
        assertEquals(names(workflows), names(filtered));
    }

    @Test
    @DisplayName("the list follows the typing, character by character")
    void typingFiltersTheList() {
        layOut(input);
        input.filter(filtered, Workflow::name);

        type("i");
        assertEquals(List.of("TEST-FILE", "Import Z\u00fcrich", "Nightly report", "test import"), names(filtered));
        type("m");
        assertEquals(List.of("Import Z\u00fcrich", "test import"), names(filtered));
        type("p z");
        assertEquals(List.of("Import Z\u00fcrich"), names(filtered));
    }

    @Test
    @DisplayName("a second call replaces the first: the list before gets every entry back and no longer follows the text")
    void reconnecting() {
        FilteredList<Workflow> other = new FilteredList<>(workflows);
        input.filter(filtered, Workflow::name);
        input.setText("file");

        input.filter(other, Workflow::name);

        assertEquals(names(workflows), names(filtered));
        assertEquals(List.of("TEST-FILE"), names(other), "the new list is filtered by the current text at once");
        input.setText("report");
        assertEquals(names(workflows), names(filtered));
        assertEquals(List.of("Nightly report"), names(other));
    }

    @Test
    @DisplayName("the same list connected again with another text of its entries is filtered by that text")
    void reconnectingTheSameList() {
        input.filter(filtered, Workflow::name);
        input.setText("nightly");

        input.filter(filtered, workflow -> workflow.name() + " nightly");

        assertEquals(names(workflows), names(filtered));
        input.setText("file");
        assertEquals(List.of("TEST-FILE"), names(filtered));
    }

    @Test
    @DisplayName("null disconnects: the list gets every entry back and no longer follows the text")
    void disconnecting() {
        input.filter(filtered, Workflow::name);
        input.setText("file");

        input.filter(null, null);

        assertEquals(names(workflows), names(filtered));
        input.setText("report");
        assertEquals(names(workflows), names(filtered));
    }

    @Test
    @DisplayName("a list without the text of its entries is refused")
    void listWithoutText() {
        assertThrows(IllegalArgumentException.class, () -> input.filter(filtered, null));
    }

    @Test
    @DisplayName("Escape in a connected field with text empties it, so the list shows everything again")
    void escapeLiftsTheFilter() {
        layOut(input);
        input.filter(filtered, Workflow::name);
        input.setText("file");

        List<Event> passed = firesThrough(input, keyPressed(KeyCode.ESCAPE));

        assertTrue(passed.isEmpty());
        assertEquals("", input.getText());
        assertEquals(names(workflows), names(filtered));
        assertFalse(isShown(counter()));
    }

    @Test
    @DisplayName("a long-lived list the field filters does not keep a dropped field alive")
    void connectedListDoesNotKeepTheFieldAlive() {
        WeakReference<SearchInput> dropped = connectedField(workflows);

        assertTrue(isCollected(dropped), "the list keeps a dropped field alive");
        workflows.add(new Workflow("Import Basel"));
        assertEquals(5, workflows.size());
    }

    /** A field, connected to a filtered list over the entries and searching, that nothing else holds. */
    private static WeakReference<SearchInput> connectedField(ObservableList<Workflow> entries) {
        SearchInput field = new SearchInput();
        field.filter(new FilteredList<>(entries), Workflow::name);
        field.setText("import");
        return new WeakReference<>(field);
    }

    // --- Counter -------------------------------------------------------------------------------------

    @Test
    @DisplayName("the counter shows the matches and all entries while connected and with text, between the text and the x")
    void counterWhileConnectedWithText() {
        input.setText("file");
        assertNotNull(counter());
        assertFalse(isShown(counter()), "not connected: no counter");

        input.filter(filtered, Workflow::name);
        assertTrue(isShown(counter()));
        assertEquals("1 of 4", counter().getText());
        assertFalse(hasPseudoClass(counter(), "no-match"));
        assertFalse(counter().isFocusTraversable());

        input.setText("import");
        assertEquals("2 of 4", counter().getText());

        input.setText("");
        assertFalse(isShown(counter()), "an empty field: no counter");
    }

    @Test
    @DisplayName("left to right: the text, the counter, the x at the right end")
    void counterBeforeTheClearButton() {
        input.filter(filtered, Workflow::name);
        input.setText("file");
        input.setPrefWidth(300);
        layOut(input);

        Bounds text = input.getTextField().getBoundsInParent();
        Bounds counter = counter().getBoundsInParent();
        Bounds clear = clearButton().getBoundsInParent();
        assertTrue(counter.getWidth() > 0);
        assertTrue(text.getMaxX() <= counter.getMinX());
        assertTrue(counter.getMaxX() <= clear.getMinX());
        List<Node> children = input.getChildrenUnmodifiable();
        assertTrue(children.indexOf(counter()) < children.indexOf(clearButton()));
    }

    @Test
    @DisplayName("nothing matches: 0 of 4 with :no-match, in the warning colour; muted otherwise")
    void noMatch() {
        Region muted = tokenSwatch("-fx-text-secondary-color");
        Region warning = tokenSwatch("-fx-status-paused");
        input.filter(filtered, Workflow::name);
        input.setText("file");
        layOut(input, muted, warning);
        assertEquals(fill(muted), counter().getTextFill());

        input.setText("filex");
        input.applyCss();

        assertEquals("0 of 4", counter().getText());
        assertTrue(hasPseudoClass(counter(), "no-match"));
        assertEquals(fill(warning), counter().getTextFill());

        input.setText("file");
        assertFalse(hasPseudoClass(counter(), "no-match"));
    }

    @Test
    @DisplayName("the counter follows changes of the source list, also those the filtered list does not show")
    void counterFollowsTheSourceList() {
        input.filter(filtered, Workflow::name);
        input.setText("import");
        assertEquals("2 of 4", counter().getText());

        workflows.add(new Workflow("Import Basel"));
        assertEquals("3 of 5", counter().getText());

        workflows.add(new Workflow("Cleanup"));
        assertEquals("3 of 6", counter().getText());

        workflows.removeIf(workflow -> workflow.name().startsWith("Import"));
        assertEquals("1 of 4", counter().getText());

        workflows.clear();
        assertEquals("0 of 0", counter().getText());
    }

    @Test
    @DisplayName("the counter's format is settable: {0} the matches, {1} all entries; null shows nothing")
    void counterFormat() {
        input.filter(filtered, Workflow::name);
        input.setText("import");
        assertEquals("{0} of {1}", input.getMatchCountFormat());

        input.setMatchCountFormat("{0} von {1}");
        assertEquals("2 von 4", counter().getText());

        input.matchCountFormatProperty().set("{1}: {0} ({0})");
        assertEquals("4: 2 (2)", counter().getText());

        input.setMatchCountFormat(null);
        assertEquals("", counter().getText());
    }

    @Test
    @DisplayName("disconnected, the counter hides; a field connected again counts its new list")
    void counterAfterDisconnecting() {
        input.filter(filtered, Workflow::name);
        input.setText("import");

        input.filter(null, null);
        assertFalse(isShown(counter()));
        workflows.add(new Workflow("Import Basel"));
        assertFalse(isShown(counter()));

        ObservableList<Workflow> others = FXCollections.observableArrayList(new Workflow("Import Bern"));
        input.filter(new FilteredList<>(others), Workflow::name);
        assertEquals("1 of 1", counter().getText());
        workflows.add(new Workflow("Cleanup"));
        assertEquals("1 of 1", counter().getText(), "the list before no longer counts");
    }

    @Test
    @DisplayName("read-only the counter stays; disabled it stays too, dimmed once with the field")
    void counterReadOnlyAndDisabled() {
        input.filter(filtered, Workflow::name);
        input.setText("import");

        input.setReadOnly(true);
        assertTrue(isShown(counter()));
        input.setReadOnly(false);

        input.setDisable(true);
        layOut(input);
        assertTrue(isShown(counter()));
        assertEquals(1, counter().getOpacity(), 0.001);
    }

    // --- Highlight ----------------------------------------------------------------------------------

    @Test
    @DisplayName("highlight: a TextFlow with search-highlight, its runs carry search-text, the marked ones also search-match")
    void highlightRuns() {
        input.setText("im");

        TextFlow flow = input.highlight("Import Z\u00fcrich import");

        assertTrue(flow.getStyleClass().contains("search-highlight"));
        assertEquals(4, flow.getChildren().size());
        assertEquals("[Im]port Z\u00fcrich [im]port", marked(flow));
    }

    @Test
    @DisplayName("an empty search marks nothing: the text in one run")
    void highlightWithoutSearch() {
        TextFlow flow = input.highlight("Import Z\u00fcrich");

        assertEquals(1, flow.getChildren().size());
        assertEquals("Import Z\u00fcrich", marked(flow));
    }

    @Test
    @DisplayName("the highlight node follows the search text by itself, change after change, accents and case folded")
    void highlightFollowsTheText() {
        layOut(input);
        TextFlow flow = input.highlight("Import Z\u00fcrich");

        type("z");
        assertEquals("Import [Z]\u00fcrich", marked(flow));
        type("u");
        assertEquals("Import [Z\u00fc]rich", marked(flow));
        input.setText("rich zu");
        assertEquals("Import [Z\u00fcrich]", marked(flow), "touching marks merge");
        input.setText("PORT xyz");
        assertEquals("Im[port] Z\u00fcrich", marked(flow), "a word that occurs is marked, also when another does not");
        input.setText("");
        assertEquals("Import Z\u00fcrich", marked(flow));
        assertEquals(1, flow.getChildren().size());
    }

    @Test
    @DisplayName("a highlight of an empty text, or of null, is one empty run, so it keeps a line's height")
    void highlightOfNothing() {
        input.setText("anna");

        assertEquals(List.of(""), runTexts(input.highlight("")));
        assertEquals(List.of(""), runTexts(input.highlight(null)));
    }

    @Test
    @DisplayName("in the theme a run shows in the text colour, a mark outside the popup bold in the accent colour")
    void highlightLook() {
        input.setText("im");
        TextFlow flow = input.highlight("Import");
        Region text = tokenSwatch("-fx-text-color");
        Region accent = tokenSwatch("-fx-accent-color");
        layOut(new VBox(flow), text, accent);

        Text mark = (Text) flow.getChildren().get(0);
        Text plain = (Text) flow.getChildren().get(1);
        assertEquals(fill(accent), mark.getFill());
        assertTrue(isBold(mark.getFont()), () -> "not bold: " + mark.getFont());
        assertEquals(fill(text), plain.getFill());
        assertFalse(isBold(plain.getFont()), () -> "bold: " + plain.getFont());
    }

    @Test
    @DisplayName("a highlight node the application drops is not kept alive by the field")
    void droppedHighlightIsNotKeptAlive() {
        input.setText("im");
        WeakReference<TextFlow> dropped = new WeakReference<>(input.highlight("Import"));
        TextFlow kept = input.highlight("Import");

        assertTrue(isCollected(dropped), "the field keeps a dropped highlight node alive");
        input.setText("port");
        assertEquals("Im[port]", marked(kept), "a node the application holds still follows the text");
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

    /**
     * The text of a highlight node with its marks in brackets: {@code [Im]port}. Every run is a {@link Text} with
     * {@code search-text}; a marked one also carries {@code search-match}.
     */
    private static String marked(TextFlow flow) {
        StringBuilder marked = new StringBuilder();
        for (Node child : flow.getChildren()) {
            Text run = (Text) child;
            assertTrue(run.getStyleClass().contains("search-text"), () -> "a run without search-text: " + run);
            if (run.getStyleClass().contains("search-match")) {
                marked.append('[').append(run.getText()).append(']');
            } else {
                marked.append(run.getText());
            }
        }
        return marked.toString();
    }

    /** The texts of a highlight node's runs. */
    private static List<String> runTexts(TextFlow flow) {
        List<String> texts = new ArrayList<>();
        for (Node child : flow.getChildren()) {
            texts.add(((Text) child).getText());
        }
        return texts;
    }

    /** Whether the font is a bold one. */
    private static boolean isBold(Font font) {
        return font.getStyle().toLowerCase(Locale.ROOT).contains("bold");
    }

    /** The counter of the field. */
    private Label counter() {
        return (Label) input.lookup(".form-search-count");
    }

    /** The clear button of the field. */
    private Button clearButton() {
        return (Button) input.lookup(".form-search-clear");
    }

    /** Whether the node is in the field and takes room, so that it shows while the field does. */
    private static boolean isShown(Node node) {
        return node != null && node.getParent() != null && node.isVisible() && node.isManaged();
    }

    /** Types the characters into the field's text field, one key at a time, as the user does. */
    private void type(String characters) {
        for (int index = 0; index < characters.length(); index++) {
            String character = String.valueOf(characters.charAt(index));
            input.getTextField().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED,
                    false, false, false, false));
        }
    }

    /** The names of the entries, in their order. */
    private static List<String> names(List<Workflow> entries) {
        List<String> names = new ArrayList<>();
        for (Workflow workflow : entries) {
            names.add(workflow.name());
        }
        return names;
    }
}
