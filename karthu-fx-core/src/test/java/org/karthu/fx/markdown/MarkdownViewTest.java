package org.karthu.fx.markdown;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.karthu.fx.theme.ThemeManager;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The Markdown view seen from outside: Markdown text in, rendered nodes found by their documented style classes.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewTest {

    private static final double SCENE_WIDTH = 400;
    private static final double SCENE_HEIGHT = 300;

    /**
     * A code line far wider than the view, as in the sample documents (up to 105 characters).
     */
    private static final String LONG_CODE_LINE = "sorted.sort(java.util.Comparator.comparing((Order o) -> o.get(\"loaves\","
            + " tLong), java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));";

    /**
     * A word, code without spaces, wider than the test scene's document: a table column cannot be narrower.
     */
    private static final String LONG_WORD = "java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())";

    /**
     * A cell text that wraps over several lines in any column of the test scene, as the longest cells of the sample
     * documents do (up to 200 characters).
     */
    private static final String LONG_CELL = "one pass over the orders into a map of breads among them, its sums and"
            + " counts written as one plan line per bread, sorted by the bread's name, orders without a count"
            + " skipped";

    /**
     * A table of three rows and three columns, each cell on one line, for selecting: code in the first column, bold
     * text in the second.
     */
    private static final String SELECTION_TABLE = "| Key | Type | Meaning |\n|---|---|---|\n| `a` | INT64 | first |\n"
            + "| `b` | **STRING** | second |\n| `c` | DOUBLE | third |";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        // The view needs the JavaFX toolkit, but no stage is ever shown, so the nodes may live on the test thread.
        // The first control created off the FX thread hands the loading of JavaFX's default stylesheet to the FX
        // thread; a scene styled while that runs comes out half styled. So the first control is created on the FX
        // thread, which loads the stylesheet there and then, and the test waits for it.
        CountDownLatch ready = new CountDownLatch(1);
        Runnable loadDefaultStylesheet = () -> {
            new Label();
            ready.countDown();
        };
        try {
            Platform.startup(loadDefaultStylesheet);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(loadDefaultStylesheet);
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    @DisplayName("headings and paragraphs are text flows with their style classes")
    void headingsAndParagraphs() {
        MarkdownView view = layOut(new MarkdownView("# Title\n\nFirst paragraph.\n\n## Section\n\nSecond."));

        List<TextFlow> headings = textFlows(view, ".markdown-heading");
        assertEquals(2, headings.size());
        assertEquals("Title", textOf(headings.get(0)));
        assertTrue(headings.get(0).getStyleClass().contains("markdown-heading-1"));
        assertEquals("Section", textOf(headings.get(1)));
        assertTrue(headings.get(1).getStyleClass().contains("markdown-heading-2"));

        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        assertEquals(List.of("First paragraph.", "Second."), List.of(textOf(paragraphs.get(0)), textOf(paragraphs.get(1))));
    }

    @Test
    @DisplayName("each style run is one text node whose style classes combine its styles")
    void styleRunsCombineClasses() {
        MarkdownView view = layOut(new MarkdownView("Plain **bold *both*** `code` *it\nalic* and***all***"));

        List<Text> runs = runs(textFlows(view, ".markdown-paragraph").get(0));
        assertEquals(9, runs.size(), () -> "runs: " + runs);
        assertRun(runs.get(0), "Plain ");
        assertRun(runs.get(1), "bold ", "markdown-strong");
        assertRun(runs.get(2), "both", "markdown-strong", "markdown-emphasis");
        assertRun(runs.get(3), " ");
        assertRun(runs.get(4), "code", "markdown-code");
        assertRun(runs.get(5), " ");
        assertRun(runs.get(6), "it alic", "markdown-emphasis");
        assertRun(runs.get(7), " and");
        assertRun(runs.get(8), "all", "markdown-emphasis", "markdown-strong");
    }

    @Test
    @DisplayName("a hard line break breaks the run's text, a soft one is a space")
    void lineBreaksInRuns() {
        MarkdownView view = layOut(new MarkdownView("one\ntwo  \nthree\\\nfour"));

        assertEquals("one two\nthree\nfour", textOf(textFlows(view, ".markdown-paragraph").get(0)));
    }

    @Test
    @DisplayName("the view has its style class, and null or the empty text give an empty view")
    void emptyView() {
        MarkdownView view = layOut(new MarkdownView());

        assertTrue(view.getStyleClass().contains("markdown-view"));
        assertNull(view.getMarkdown());
        assertTrue(view.lookupAll(".markdown-text").isEmpty());

        view.setMarkdown("");
        assertTrue(view.lookupAll(".markdown-text").isEmpty());
        view.setMarkdown("Some text");
        view.setMarkdown(null);
        assertTrue(view.lookupAll(".markdown-text").isEmpty());
    }

    @Test
    @DisplayName("a change of the text property rebuilds the content, also through a binding")
    void textChangeRebuilds() {
        MarkdownView view = layOut(new MarkdownView("# Old"));
        StringProperty source = new SimpleStringProperty("# New\n\nText");

        view.markdownProperty().bind(source);

        assertEquals("# New\n\nText", view.getMarkdown());
        assertEquals("New", textOf(textFlows(view, ".markdown-heading").get(0)));
        source.set("Only a paragraph");
        assertTrue(view.lookupAll(".markdown-heading").isEmpty());
        assertEquals("Only a paragraph", textOf(textFlows(view, ".markdown-paragraph").get(0)));
    }

    @Test
    @DisplayName("a new text scrolls the view back to the top left")
    void newTextResetsScrollPosition() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(60)));
        ScrollPane scrollPane = (ScrollPane) view.lookup(".scroll-pane");
        scrollPane.setVvalue(scrollPane.getVmax());
        scrollPane.setHvalue(scrollPane.getHmax());

        view.setMarkdown(manyParagraphs(50));
        view.layout();

        assertEquals(scrollPane.getVmin(), scrollPane.getVvalue());
        assertEquals(scrollPane.getHmin(), scrollPane.getHvalue());
    }

    @Test
    @DisplayName("paragraphs wrap to the width of the view")
    void paragraphsWrapToViewWidth() {
        MarkdownView view = layOut(new MarkdownView(
                "word ".repeat(150) + "\n\n" + "# A heading that is long enough to need more than one line in the view"));

        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(0);
        assertTrue(paragraph.getWidth() <= SCENE_WIDTH, () -> "paragraph width " + paragraph.getWidth());
        assertTrue(paragraph.getHeight() > 100, () -> "paragraph height " + paragraph.getHeight());
    }

    @Test
    @DisplayName("a long paragraph does not widen the view's preferred size")
    void longTextKeepsPreferredWidth() {
        MarkdownView shortText = layOut(new MarkdownView("Short."));
        MarkdownView longText = layOut(new MarkdownView("word ".repeat(500)));

        assertEquals(shortText.prefWidth(-1), longText.prefWidth(-1));
    }

    @Test
    @DisplayName("the theme sets headings larger than body text, larger the higher their level, and bold")
    void themeSizesHeadings() {
        MarkdownView view = layOut(new MarkdownView("# One\n\n## Two\n\n### Three\n\n#### Four\n\nBody"));

        List<TextFlow> headings = textFlows(view, ".markdown-heading");
        Font one = runs(headings.get(0)).get(0).getFont();
        Font two = runs(headings.get(1)).get(0).getFont();
        Font three = runs(headings.get(2)).get(0).getFont();
        Font four = runs(headings.get(3)).get(0).getFont();
        Font body = runs(textFlows(view, ".markdown-paragraph").get(0)).get(0).getFont();
        assertTrue(one.getSize() > two.getSize(), () -> one + " / " + two);
        assertTrue(two.getSize() > three.getSize(), () -> two + " / " + three);
        assertTrue(three.getSize() > body.getSize(), () -> three + " / " + body);
        assertEquals(body.getSize(), four.getSize());
        for (Font heading : List.of(one, two, three, four)) {
            assertTrue(isBold(heading), () -> "not bold: " + heading);
        }
        assertFalse(isBold(body), () -> "bold: " + body);
    }

    @Test
    @DisplayName("the theme draws a line under level 1 and 2 headings only")
    void themeUnderlinesTopHeadings() {
        MarkdownView view = layOut(new MarkdownView("# One\n\n## Two\n\n### Three"));

        List<TextFlow> headings = textFlows(view, ".markdown-heading");
        assertTrue(hasBottomLine(headings.get(0)), "level 1");
        assertTrue(hasBottomLine(headings.get(1)), "level 2");
        assertFalse(hasBottomLine(headings.get(2)), "level 3");
    }

    @Test
    @DisplayName("the theme shows bold, italic and monospace code in their own colour")
    void themeStylesRuns() {
        MarkdownView view = layOut(new MarkdownView("Plain **bold** *italic* `code`"));

        List<Text> runs = runs(textFlows(view, ".markdown-paragraph").get(0));
        Text plain = runs.get(0);
        Text bold = runs.get(1);
        Text italic = runs.get(3);
        Text code = runs.get(5);
        assertTrue(isBold(bold.getFont()), () -> "not bold: " + bold.getFont());
        String italicStyle = italic.getFont().getStyle().toLowerCase(Locale.ROOT);
        assertTrue(italicStyle.contains("italic") || italicStyle.contains("oblique"), italicStyle);
        assertTrue(isMonospaced(code.getFont()), () -> "not monospaced: " + code.getFont());
        assertFalse(isMonospaced(plain.getFont()), () -> "monospaced: " + plain.getFont());
        assertNotEquals(Color.BLACK, plain.getFill(), "the theme's text colour, not the default");
        assertNotEquals(plain.getFill(), code.getFill(), "code has its own colour");
    }

    @Test
    @DisplayName("a bullet list is a box of item rows, each with a bullet marker and the item's blocks")
    void bulletList() {
        MarkdownView view = layOut(new MarkdownView("Ports:\n\n- **in** \u2014 the table\n- out"));

        Node list = view.lookup(".markdown-bullet-list");
        List<Node> items = new ArrayList<>(list.lookupAll(".markdown-list-item"));
        assertEquals(2, items.size());
        assertEquals(List.of("\u2022", "\u2022"), markers(list));
        assertEquals("in \u2014 the table", textOf(textFlows(items.get(0), ".markdown-paragraph").get(0)));
        assertRun(runs(textFlows(items.get(0), ".markdown-paragraph").get(0)).get(0), "in", "markdown-strong");
        assertEquals("out", textOf(textFlows(items.get(1), ".markdown-paragraph").get(0)));
        assertEquals("Ports:", textOf(textFlows(view, ".markdown-paragraph").get(0)));
    }

    @Test
    @DisplayName("an ordered list numbers its items from the source's start number, with the source's delimiter")
    void orderedListNumbering() {
        MarkdownView view = layOut(new MarkdownView("3. three\n1. four\n\n9) nine\n9) ten"));

        List<Node> lists = new ArrayList<>(view.lookupAll(".markdown-ordered-list"));
        assertEquals(2, lists.size());
        assertEquals(List.of("3.", "4."), markers(lists.get(0)));
        assertEquals(List.of("9)", "10)"), markers(lists.get(1)));
        assertEquals("ten", textOf(textFlows(lists.get(1), ".markdown-paragraph").get(1)));
    }

    @Test
    @DisplayName("lists nest in either kind, each level indented further than its parent")
    void nestedListsAreIndentedPerLevel() {
        MarkdownView view = layOut(new MarkdownView("- one\n  1. two\n     - three\n- four"));

        assertEquals(List.of("\u2022", "1.", "\u2022", "\u2022"), markers(view));
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        assertEquals(List.of(1, 2, 3, 1), List.of(listDepth(paragraphs.get(0)), listDepth(paragraphs.get(1)),
                listDepth(paragraphs.get(2)), listDepth(paragraphs.get(3))));
        assertTrue(listAncestor(paragraphs.get(1)).getStyleClass().contains("markdown-ordered-list"));
        assertTrue(listAncestor(paragraphs.get(2)).getStyleClass().contains("markdown-bullet-list"));
        double one = sceneX(paragraphs.get(0));
        double two = sceneX(paragraphs.get(1));
        double three = sceneX(paragraphs.get(2));
        assertTrue(one < two && two < three, () -> "x: " + one + ", " + two + ", " + three);
        assertEquals(one, sceneX(paragraphs.get(3)));
    }

    @Test
    @DisplayName("a marker sits left of its item's blocks, on the baseline of the first line, in the text colour")
    void markerPlacement() {
        MarkdownView view = layOut(new MarkdownView(
                "8. eight\n9. nine " + "word ".repeat(40) + "\n10. ten\n\n- # Heading in an item"));

        List<Node> markers = new ArrayList<>(view.lookupAll(".markdown-list-marker"));
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        for (int i = 0; i < 3; i++) {
            Label marker = (Label) markers.get(i);
            Text firstRun = runs(paragraphs.get(i)).get(0);
            Bounds markerBounds = marker.localToScene(marker.getLayoutBounds());
            assertTrue(markerBounds.getMaxX() <= sceneX(paragraphs.get(i)), () -> "marker " + marker.getText());
            assertEquals(sceneBaseline(firstRun), sceneBaseline(marker), 1.0, () -> "baseline of " + marker.getText());
            assertNotEquals(Color.BLACK, marker.getTextFill(), "the theme's text colour");
        }
        assertEquals(sceneX(paragraphs.get(0)), sceneX(paragraphs.get(2)), "blocks of 8. and 10. start alike");
        assertTrue(paragraphs.get(1).getHeight() > 30, "the long item wraps");
        TextFlow heading = textFlows(view, ".markdown-heading").get(0);
        assertEquals(sceneBaseline(runs(heading).get(0)), sceneBaseline(markers.get(3)), 1.0, "heading baseline");
    }

    @Test
    @DisplayName("an empty item keeps the height of its marker, so the next item does not cover it")
    void emptyItemKeepsMarkerHeight() {
        MarkdownView view = layOut(new MarkdownView("- one\n-\n- three"));

        List<Node> markers = new ArrayList<>(view.lookupAll(".markdown-list-marker"));
        Bounds empty = markers.get(1).localToScene(markers.get(1).getLayoutBounds());
        Bounds next = markers.get(2).localToScene(markers.get(2).getLayoutBounds());
        assertTrue(empty.getMaxY() <= next.getMinY(), () -> "empty item " + empty + ", next " + next);
    }

    @Test
    @DisplayName("a loose list spaces its items and their blocks like paragraphs, a tight list keeps them close")
    void looseListsSpaceLikeParagraphs() {
        MarkdownView view = layOut(new MarkdownView("- a\n- b\n\nText\n\n- c\n\n- d\n\n  d2"));

        List<Node> lists = new ArrayList<>(view.lookupAll(".markdown-bullet-list"));
        PseudoClass loose = PseudoClass.getPseudoClass("loose");
        assertFalse(lists.get(0).getPseudoClassStates().contains(loose), "tight list");
        assertTrue(lists.get(1).getPseudoClassStates().contains(loose), "loose list");
        double tightGap = gap(itemRows(lists.get(0)).get(0), itemRows(lists.get(0)).get(1));
        double looseGap = gap(itemRows(lists.get(1)).get(0), itemRows(lists.get(1)).get(1));
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        double paragraphGap = gap(paragraphs.get(2), lists.get(1));
        assertTrue(tightGap < looseGap, () -> "tight " + tightGap + ", loose " + looseGap);
        assertEquals(paragraphGap, looseGap, "items of a loose list are as far apart as paragraphs");
        assertEquals(paragraphGap, gap(paragraphs.get(4), paragraphs.get(5)), "blocks of a loose item too");
    }

    @Test
    @DisplayName("a block quote is a box of its blocks: paragraphs, lists and further quotes")
    void blockQuote() {
        MarkdownView view = layOut(new MarkdownView(
                "Before.\n\n> A **note** with *emphasis*.\n>\n> - one\n> - two\n>\n> > Nested.\n\nAfter."));

        List<Node> quotes = new ArrayList<>(view.lookupAll(".markdown-quote"));
        assertEquals(2, quotes.size());
        Node quote = quotes.get(0);
        assertTrue(quote instanceof VBox, () -> "quote: " + quote);
        assertEquals(List.of("A note with emphasis.", "one", "two", "Nested."), texts(quote, ".markdown-paragraph"));
        List<Text> runs = runs(textFlows(quote, ".markdown-paragraph").get(0));
        assertRun(runs.get(1), "note", "markdown-strong");
        assertRun(runs.get(3), "emphasis", "markdown-emphasis");
        assertEquals(List.of("\u2022", "\u2022"), markers(quote));
        assertEquals(List.of("Nested."), texts(quotes.get(1), ".markdown-paragraph"));
        assertTrue(quotes.get(1).getParent() == quote, "the nested quote sits in the outer one");
        assertEquals(List.of("Before.", "After."), texts(view, ".markdown-document > .markdown-paragraph"));
    }

    @Test
    @DisplayName("a thematic break is a horizontal separator between the blocks around it")
    void thematicBreak() {
        MarkdownView view = layOut(new MarkdownView("Above.\n\n---\n\n- item\n- * * *\n\nBelow."));

        List<Node> separators = new ArrayList<>(view.lookupAll(".markdown-separator"));
        assertEquals(2, separators.size());
        Separator separator = (Separator) separators.get(0);
        assertEquals(Orientation.HORIZONTAL, separator.getOrientation());
        assertTrue(separator.getParent().getStyleClass().contains("markdown-document"));
        assertTrue(listAncestor(separators.get(1)).getStyleClass().contains("markdown-bullet-list"));
        List<TextFlow> paragraphs = textFlows(view, ".markdown-document > .markdown-paragraph");
        assertTrue(gap(paragraphs.get(0), separator) > 0, "below the paragraph above");
        assertTrue(gap(separator, view.lookup(".markdown-bullet-list")) > 0, "above the list below");
        assertTrue(separator.getWidth() > SCENE_WIDTH / 2, () -> "width " + separator.getWidth());
    }

    @Test
    @DisplayName("the theme marks a quote with an accent bar, indents it and shows its text in the secondary colour")
    void themeStylesQuotes() {
        MarkdownView view = layOut(new MarkdownView("Body\n\n> Quoted `code`\n>\n> - item"));

        Region quote = (Region) view.lookup(".markdown-quote");
        BorderStroke stroke = quote.getBorder().getStrokes().get(0);
        assertTrue(stroke.getWidths().getLeft() >= 2, () -> "bar width " + stroke.getWidths().getLeft());
        assertEquals(0, stroke.getWidths().getTop());
        assertEquals(0, stroke.getWidths().getRight());
        assertEquals(0, stroke.getWidths().getBottom());
        assertEquals(themeColor(view, "-fx-accent-color"), stroke.getLeftStroke());

        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        Text body = runs(paragraphs.get(0)).get(0);
        Text quoted = runs(paragraphs.get(1)).get(0);
        Text code = runs(paragraphs.get(1)).get(1);
        assertTrue(sceneX(paragraphs.get(1)) - sceneX(paragraphs.get(0)) > stroke.getWidths().getLeft(),
                "the quote's blocks are indented beyond the bar");
        assertEquals(themeColor(view, "-fx-text-color"), body.getFill());
        assertEquals(themeColor(view, "-fx-text-secondary-color"), quoted.getFill());
        assertEquals(themeColor(view, "-fx-code-text-color"), code.getFill(), "code keeps its colour");
        Label marker = (Label) view.lookup(".markdown-list-marker");
        assertEquals(themeColor(view, "-fx-text-secondary-color"), marker.getTextFill());
    }

    @Test
    @DisplayName("a fenced code block in a language the view does not colour shows its code as written, monospace, in"
            + " the code colour on the code background, one run")
    void codeBlock() {
        MarkdownView view = layOut(new MarkdownView(
                "Before.\n\n```text\nint x = 1;\n    **not bold** \\* `tick`\n\n// after a blank line\n```\n\nAfter."));

        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        assertEquals(1, blocks.size());
        Region block = (Region) blocks.get(0);
        Text code = codeText(block);
        assertEquals(List.of(code), runs(blockFlow(block)), "one run");
        assertEquals("int x = 1;\n    **not bold** \\* `tick`\n\n// after a blank line", code.getText());
        assertTrue(code.getStyleClass().containsAll(List.of("markdown-text", "markdown-code")), "" + code.getStyleClass());
        assertTrue(isMonospaced(code.getFont()), () -> "not monospaced: " + code.getFont());
        assertEquals(themeColor(view, "-fx-code-text-color"), code.getFill());
        assertEquals(themeColor(view, "-fx-code-background-color"), block.getBackground().getFills().get(0).getFill());
        assertTrue(block.getParent().getStyleClass().contains("markdown-document"));
        assertEquals(List.of("Before.", "After."), texts(view, ".markdown-document > .markdown-paragraph"));
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        assertTrue(gap(paragraphs.get(0), block) > 0 && gap(block, paragraphs.get(1)) > 0, "between the paragraphs");
    }

    @Test
    @DisplayName("a code or source block's text is a text flow of runs, the box that pads it: plain code runs in the code"
            + " colour, source runs in the text colour, all monospace in the theme's LCD smoothing, coloured ones too")
    void blockTextIsATextFlowOfRuns() {
        MarkdownView view = layOut(new MarkdownView("```java\nint x = 1;\n\nreturn x;\n```\n\n<div>\nName INT64\n</div>"));
        Region code = (Region) view.lookup(".markdown-code-block");
        Region source = (Region) view.lookup(".markdown-source-block");

        TextFlow codeFlow = blockFlow(code);
        assertEquals("int x = 1;\n\nreturn x;", textOf(codeFlow));
        assertEquals(new Insets(8, 12, 8, 12), codeFlow.getPadding(), "the box that pads the code");
        assertTrue(runs(codeFlow).size() > 1, "a coloured block");
        for (Text run : runs(codeFlow)) {
            assertTrue(run.getStyleClass().containsAll(List.of("markdown-text", "markdown-code")), "" + run.getStyleClass());
            assertTrue(isMonospaced(run.getFont()), () -> "not monospaced: " + run.getFont());
            if (run.getStyleClass().size() == 2) {
                assertEquals(themeColor(view, "-fx-code-text-color"), run.getFill(), "plain code: " + run.getText());
            }
            assertEquals(FontSmoothingType.LCD, run.getFontSmoothingType());
        }
        TextFlow sourceFlow = blockFlow(source);
        assertEquals("<div>\nName INT64\n</div>", textOf(sourceFlow));
        for (Text run : runs(sourceFlow)) {
            assertEquals(List.of("markdown-text"), run.getStyleClass());
            assertTrue(isMonospaced(run.getFont()), () -> "not monospaced: " + run.getFont());
            assertEquals(themeColor(view, "-fx-text-color"), run.getFill());
            assertEquals(FontSmoothingType.LCD, run.getFontSmoothingType());
        }
    }

    @Test
    @DisplayName("a long code line stays whole: the block keeps the view's width and shows a horizontal bar")
    void longCodeLineScrollsSideways() {
        MarkdownView view = layOut(new MarkdownView("```\n" + LONG_CODE_LINE + "\nshort\n```"));

        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);
        ScrollPane pane = innerScrollPane(block);
        assertEquals(0, code.getWrappingWidth());
        assertTrue(block.getWidth() < SCENE_WIDTH, () -> "block width " + block.getWidth());
        assertTrue(code.getLayoutBounds().getWidth() > block.getWidth(), "the long line is not wrapped");
        assertEquals(2 * lineHeight(code), code.getLayoutBounds().getHeight(), 1.0, "two lines");
        assertTrue(scrollBar(pane, Orientation.HORIZONTAL).isVisible(), "horizontal bar");
        assertFalse(isShown(scrollBar(pane, Orientation.VERTICAL)), "no vertical bar");
        assertEquals(pane.getContent().getLayoutBounds().getHeight(), pane.getViewportBounds().getHeight(), 0.5,
                "the bar takes room of its own, it covers no line");
        assertTrue(block.getHeight() >= code.getLayoutBounds().getHeight() + scrollBar(pane, Orientation.HORIZONTAL)
                .getHeight(), () -> "block height " + block.getHeight());
    }

    @Test
    @DisplayName("short code shows no scroll bar and no room for one")
    void shortCodeHasNoScrollBar() {
        MarkdownView view = layOut(new MarkdownView("```\nint x = 1;\n```"));

        Region block = (Region) view.lookup(".markdown-code-block");
        ScrollPane pane = innerScrollPane(block);
        assertFalse(isShown(scrollBar(pane, Orientation.HORIZONTAL)), "no horizontal bar");
        assertFalse(isShown(scrollBar(pane, Orientation.VERTICAL)), "no vertical bar");
        assertEquals(pane.getContent().getLayoutBounds().getHeight(), block.getHeight(), 0.5);
    }

    @Test
    @DisplayName("a code block's line height and tab stops: the block as tall as its lines and its padding, a character"
            + " after a tab at the next multiple of four columns")
    void codeBlockLineHeightAndTabStops() {
        MarkdownView view = layOut(new MarkdownView("```\nint a;\n\tb = 1;\nend();\n```\n\n<div>\nName INT64\n</div>"));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);
        double line = lineHeight(code);
        double width = characterWidth(code);
        Bounds blockBounds = sceneBounds(block);
        Bounds codeBounds = sceneBounds(code);

        // As before the code became a text flow: the theme pads the code by 8 above and below and by 12 on the left,
        // and the block's height is snapped up to whole pixels.
        assertEquals(Math.ceil(8 + 3 * line + 8), block.getHeight(), 0.01, "three lines and the padding");
        assertEquals(Math.ceil(8 + 3 * line + 8), ((Region) view.lookup(".markdown-source-block")).getHeight(), 0.01,
                "a source block of three lines too");
        assertEquals(3 * line, codeBounds.getHeight(), 0.5, "three lines");
        assertEquals(blockBounds.getMinX() + 12, codeBounds.getMinX(), 0.5, "the code 12px from the left edge");
        assertEquals(blockBounds.getMinY() + 8, codeBounds.getMinY(), 0.5, "and 8px from the top edge");

        // A tab advances to the next multiple of four of the code font's columns. A lone Text, as the code was
        // before, measured its tab in spaces of JavaFX's default font instead (26.3px on Windows, about three and a
        // half columns), so the text after a tab fell between the columns.
        clicks(code, new Point2D(codeBounds.getMinX() + 4.5 * width, codeBounds.getMinY() + 1.5 * line), 2);
        shortcut(view, KeyCode.C);
        assertEquals(List.of("b"), copied, "the character after the tab, in the fifth column");
        Bounds highlight = sceneBounds(selectionHighlight(block));
        assertEquals(codeBounds.getMinX() + 4 * width, highlight.getMinX(), 0.5, "a tab stop every four columns");
        assertEquals(codeBounds.getMinX() + 5 * width, highlight.getMaxX(), 0.5);
        assertEquals(codeBounds.getMinY() + line, highlight.getMinY(), 0.5, "on the second line");
        assertEquals(codeBounds.getMinY() + 2 * line, highlight.getMaxY(), 0.5);
    }

    @Test
    @DisplayName("a tab in a code block, coloured or not, and in a source block moves to the next multiple of four"
            + " columns")
    void tabStopsEveryFourColumns() {
        MarkdownView view = layOut(new MarkdownView("```\nab\tc\nabcd\te\n```\n\n```java\nint\tx;\n```\n\n"
                + "<div>\n\tx\n</div>"));
        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        double width = characterWidth(codeText(blocks.get(0)));

        TextFlow plain = blockFlow(blocks.get(0));
        double left = characterBounds(plain, 0).getMinX();
        assertEquals(left + 4 * width, characterBounds(plain, 3).getMinX(), 0.5, "c after ab and a tab");
        assertEquals(left + 8 * width, characterBounds(plain, 10).getMinX(), 0.5, "e after abcd and a tab");

        TextFlow colored = blockFlow(blocks.get(1));
        assertTrue(runs(colored).size() > 1, "a coloured block");
        left = characterBounds(colored, 0).getMinX();
        assertEquals(left + 4 * width, characterBounds(colored, 4).getMinX(), 0.5, "x after int and a tab");

        TextFlow source = blockFlow(view.lookup(".markdown-source-block"));
        left = characterBounds(source, 0).getMinX();
        assertEquals(left + 4 * width, characterBounds(source, 7).getMinX(), 0.5, "x after a tab in source");
    }

    @Test
    @DisplayName("a code block in a list item sits in the item, its marker on the code's first line, coloured or not")
    void codeBlockInListItem() {
        MarkdownView view = layOut(new MarkdownView("- Example:\n\n  ```java\n  int x;\n    indented();\n  ```\n"
                + "- ```\n  first();\n  ```\n- ```java\n  second();\n  ```"));

        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        assertEquals(3, blocks.size());
        assertEquals("int x;\n  indented();", codeOf(blocks.get(0)));
        assertEquals(1, listDepth(blocks.get(0)));
        List<Node> items = itemRows(view.lookup(".markdown-bullet-list"));
        assertTrue(blocks.get(0).getParent() == items.get(0), "in the first item");
        assertTrue(sceneX(blocks.get(0)) >= sceneX(textFlows(items.get(0), ".markdown-paragraph").get(0)),
                "aligned with the item's text");
        Label marker = (Label) items.get(1).lookup(".markdown-list-marker");
        assertEquals(sceneBaseline(codeText(blocks.get(1))), sceneBaseline(marker), 1.0, "marker on the code's line");
        Label coloredMarker = (Label) items.get(2).lookup(".markdown-list-marker");
        assertTrue(runs(blockFlow(blocks.get(2))).size() > 1, "a coloured block");
        assertEquals(sceneBaseline(codeText(blocks.get(2))), sceneBaseline(coloredMarker), 1.0,
                "marker on the coloured code's line");
    }

    @Test
    @DisplayName("a code block in a quote keeps the code colour and its background")
    void codeBlockInQuote() {
        MarkdownView view = layOut(new MarkdownView("> Quoted:\n>\n> ```\n> code();\n> ```"));

        Region block = (Region) view.lookup(".markdown-code-block");
        assertTrue(block.getParent().getStyleClass().contains("markdown-quote"));
        assertEquals("code();", codeText(block).getText());
        assertEquals(themeColor(view, "-fx-code-text-color"), codeText(block).getFill());
        assertEquals(themeColor(view, "-fx-code-background-color"), block.getBackground().getFills().get(0).getFill());
    }

    @Test
    @DisplayName("the mouse wheel over a code block scrolls the document, not the block")
    void wheelOverCodeBlockScrollsDocument() {
        MarkdownView view = layOut(new MarkdownView(documentWithCodeBlock()));
        ScrollPane document = (ScrollPane) view.lookup(".scroll-pane");
        Region block = (Region) view.lookup(".markdown-code-block");
        ScrollPane pane = innerScrollPane(block);
        Text code = codeText(block);

        Event.fireEvent(code, wheel(code, 0, -40, false));

        double scrolled = document.getVvalue();
        assertTrue(scrolled > document.getVmin(), "the document scrolls down");
        assertEquals(pane.getHmin(), pane.getHvalue(), "the block stays");
        assertEquals(pane.getVmin(), pane.getVvalue(), "the block stays");
        Event.fireEvent(pane.getContent(), wheel(pane.getContent(), 0, 40, false));
        assertTrue(document.getVvalue() < scrolled, "and up again");
    }

    @Test
    @DisplayName("shift plus wheel, which JavaFX delivers as a horizontal scroll, moves the code block sideways")
    void shiftWheelScrollsCodeBlockSideways() {
        MarkdownView view = layOut(new MarkdownView(documentWithCodeBlock()));
        ScrollPane document = (ScrollPane) view.lookup(".scroll-pane");
        Region block = (Region) view.lookup(".markdown-code-block");
        ScrollPane pane = innerScrollPane(block);
        Text code = codeText(block);

        // On Windows, JavaFX turns shift plus wheel down into deltaX < 0 with deltaY 0 and the shift flag set
        // (Glass's ViewContainer.cpp; Linux swaps the deltas too, macOS delivers it so itself).
        double codeX = sceneX(code);
        Event.fireEvent(code, wheel(code, -40, 0, true));

        double scrolled = pane.getHvalue();
        assertTrue(scrolled > pane.getHmin(), "the block scrolls right");
        assertEquals(codeX - 40, sceneX(code), 0.5, "the code moves left by the wheel's 40 pixels");
        assertEquals(document.getVmin(), document.getVvalue(), "the document stays");
        Event.fireEvent(code, wheel(code, 40, 0, true));
        assertTrue(pane.getHvalue() < scrolled, "and left again");
    }

    @Test
    @DisplayName("a touchpad's diagonal scroll over a code block moves the block sideways and the document down")
    void diagonalScrollSplitsBetweenBlockAndDocument() {
        MarkdownView view = layOut(new MarkdownView(documentWithCodeBlock()));
        ScrollPane document = (ScrollPane) view.lookup(".scroll-pane");
        Region block = (Region) view.lookup(".markdown-code-block");
        ScrollPane pane = innerScrollPane(block);
        Text code = codeText(block);

        Event.fireEvent(code, wheel(code, -30, -20, false));

        assertTrue(pane.getHvalue() > pane.getHmin(), "the block scrolls right");
        assertTrue(document.getVvalue() > document.getVmin(), "the document scrolls down");
        assertEquals(pane.getVmin(), pane.getVvalue(), "the block never scrolls vertically");
    }

    @Test
    @DisplayName("a code block's copy button copies the block's code as written")
    void copyButtonCopiesCode() {
        MarkdownView view = layOut(new MarkdownView("Before.\n\n```java\nint x = 1;\n\n    **not bold** \\* `tick`\n```\n\n"
                + "- In an item:\n\n  ```\n  item();\n    deeper();\n  ```\n\n> ```\n> quoted();\n> ```"));
        List<String> copied = recordCopies(view);

        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        assertEquals(3, blocks.size());
        for (Node block : blocks) {
            copyButton(block).fire();
        }

        assertEquals(List.of("int x = 1;\n\n    **not bold** \\* `tick`", "item();\n  deeper();", "quoted();"), copied);
    }

    @Test
    @DisplayName("the copy button is a clipboard icon in the block's top right corner and stays there when the code scrolls")
    void copyButtonStaysInTopRightCorner() {
        MarkdownView view = layOut(new MarkdownView(documentWithCodeBlock()));
        Region block = (Region) view.lookup(".markdown-code-block");
        Button button = copyButton(block);
        Text code = codeText(block);

        assertTrue(button.getStyleClass().contains("icon-button"), "the theme's icon button");
        FontIcon icon = (FontIcon) button.getGraphic();
        assertEquals(BootstrapIcons.CLIPBOARD, icon.getIconCode());
        assertEquals(themeColor(view, "-fx-text-secondary-color"), icon.getIconColor(), "a quiet icon at rest");
        assertEquals(themeColor(view, "-fx-code-background-color"), button.getBackground().getFills().get(0).getFill(),
                "the code's background, so code under the button does not show through");

        Bounds blockBounds = block.localToScene(block.getLayoutBounds());
        Bounds buttonBounds = button.localToScene(button.getLayoutBounds());
        assertTrue(buttonBounds.getWidth() > 0 && buttonBounds.getHeight() > 0, "laid out");
        double right = blockBounds.getMaxX() - buttonBounds.getMaxX();
        double top = buttonBounds.getMinY() - blockBounds.getMinY();
        assertTrue(right >= 0 && right < 8, () -> "right edge " + right);
        assertTrue(top >= 0 && top < 8, () -> "top edge " + top);
        assertTrue(buttonBounds.getMaxY() < blockBounds.getMaxY(), "inside the block");
        assertTrue(sceneX(code) < buttonBounds.getMinX() && codeBounds(block).getMaxX() > buttonBounds.getMaxX(),
                "laid over the long line");

        double codeX = sceneX(code);
        Event.fireEvent(code, wheel(code, -40, 0, true));
        assertEquals(codeX - 40, sceneX(code), 0.5, "the code scrolls sideways");
        assertEquals(buttonBounds, button.localToScene(button.getLayoutBounds()), "the button stays");
    }

    @Test
    @DisplayName("the view's context menu has an entry that copies the whole Markdown source as it was set")
    void contextMenuCopiesMarkdown() {
        String source = "# Title\r\n\r\nSome **bold** text  \r\nand a [link](/a).\r\n\r\n```\r\ncode();\r\n```\r\n";
        MarkdownView view = layOut(new MarkdownView(source));
        List<String> copied = recordCopies(view);
        MenuItem entry = copyMarkdownEntry(view);

        assertFalse(entry.isDisable());
        entry.fire();
        view.setMarkdown("Changed *text*.");
        entry.fire();

        assertEquals(List.of(source, "Changed *text*."), copied);
    }

    @Test
    @DisplayName("without a text, the context menu entry is disabled and copies nothing")
    void contextMenuWithoutText() {
        MarkdownView view = layOut(new MarkdownView());
        List<String> copied = recordCopies(view);
        MenuItem entry = copyMarkdownEntry(view);

        assertTrue(entry.isDisable(), "null");
        entry.fire();
        view.setMarkdown("");
        assertTrue(entry.isDisable(), "empty");
        entry.fire();
        view.setMarkdown(" ");
        assertFalse(entry.isDisable(), "a space is a text");

        assertEquals(List.of(), copied);
    }

    @Test
    @DisplayName("the copy labels are English by default, and a change applies to the existing buttons and menu entry")
    void copyLabels() {
        MarkdownView view = layOut(new MarkdownView("```\na();\n```\n\n- ```\n  b();\n  ```"));
        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        assertEquals(2, blocks.size());
        MenuItem entry = copyMarkdownEntry(view);

        assertEquals("Copy code", view.getCopyCodeText());
        assertEquals("Copy Markdown", view.getCopyMarkdownText());
        assertEquals("Copy Markdown", entry.getText());
        for (Node block : blocks) {
            assertEquals("Copy code", copyButton(block).getTooltip().getText());
            assertEquals("Copy code", copyButton(block).getAccessibleText());
        }

        view.setCopyCodeText("Code kopieren");
        view.copyMarkdownTextProperty().set("Markdown kopieren");

        assertEquals("Code kopieren", view.copyCodeTextProperty().get());
        assertEquals("Markdown kopieren", view.getCopyMarkdownText());
        assertEquals("Markdown kopieren", entry.getText());
        for (Node block : blocks) {
            assertEquals("Code kopieren", copyButton(block).getTooltip().getText());
            assertEquals("Code kopieren", copyButton(block).getAccessibleText());
        }
        view.setCopyMarkdownText("Copy source");
        assertEquals("Copy source", entry.getText());
        view.setMarkdown("```\nc();\n```");
        assertEquals("Code kopieren", copyButton(view.lookup(".markdown-code-block")).getTooltip().getText(),
                "a block rendered later takes the label too");
    }

    @Test
    @DisplayName("dragging over a code line selects the characters between press and pointer; the shortcut key plus C copies them")
    void dragSelectsWithinLine() {
        MarkdownView view = layOut(new MarkdownView("Intro.\n\n```\nint total = price * count;\nreturn total;\n```"));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);

        drag(code, caretPoint(code, 0, 4), caretPoint(code, 0, 9));

        assertSame(block, view.getScene().getFocusOwner(), "the block takes the focus on the press");
        assertEquals(Cursor.TEXT, block.lookup(".markdown-block-content").getCursor(), "the text cursor over the code");
        shortcut(view, KeyCode.C);
        assertEquals(List.of("total"), copied);
    }

    @Test
    @DisplayName("a selection in a code block shows as a highlight over exactly the selected characters, which show on it"
            + " in the selection's text colour, as in a paragraph")
    void selectionHighlight() {
        MarkdownView view = layOut(new MarkdownView("```\nint total = price * count;\nreturn total;\n```"));
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);

        drag(code, caretPoint(code, 0, 4), caretPoint(code, 0, 9));
        relayOut(view);

        Path highlight = selectionHighlight(block);
        assertEquals(themeColor(view, "-fx-selection-color"), highlight.getFill());
        // A run's own selection does not show inside a text flow: a layer over the runs shows the selection.
        TextFlow flow = blockFlow(block);
        Parent layer = highlight.getParent();
        assertSame(flow, layer.getParent(), "a layer of the code's flow");
        assertTrue(flow.getChildrenUnmodifiable().indexOf(layer) > flow.getChildrenUnmodifiable().indexOf(code),
                "the highlight lies over the code's own characters");
        StringBuilder shown = new StringBuilder();
        for (Node child : layer.getChildrenUnmodifiable()) {
            if (child instanceof Text copy) {
                assertTrue(layer.getChildrenUnmodifiable().indexOf(highlight) < layer.getChildrenUnmodifiable().indexOf(copy),
                        "behind the characters shown on it");
                assertEquals(themeColor(view, "-fx-selection-text-color"), copy.getFill(), "the selected characters");
                assertEquals(code.getFont(), copy.getFont());
                assertEquals(FontSmoothingType.LCD, copy.getFontSmoothingType());
                shown.append(copy.getText());
            }
        }
        assertEquals("int total = price * count;\nreturn total;", shown.toString(), "the code shown again on the highlight");
        assertEquals(sceneBounds(highlight), layer.localToScene(layer.getClip().getLayoutBounds()),
                "and only there: the layer is clipped to the highlight");
        Bounds codeBounds = code.localToScene(code.getLayoutBounds());
        Bounds highlightBounds = highlight.localToScene(highlight.getLayoutBounds());
        double width = characterWidth(code);
        assertEquals(codeBounds.getMinX() + 4 * width, highlightBounds.getMinX(), 1.0, "from the fifth character");
        assertEquals(codeBounds.getMinX() + 9 * width, highlightBounds.getMaxX(), 1.0, "to the ninth");
        assertEquals(codeBounds.getMinY(), highlightBounds.getMinY(), 1.0, "on the first line");
        assertEquals(codeBounds.getMinY() + lineHeight(code), highlightBounds.getMaxY(), 1.0, "on the first line only");

        clicks(code, caretPoint(code, 1, 2), 1);
        assertTrue(highlight.getElements().isEmpty(), "a plain click clears the selection");
    }

    @Test
    @DisplayName("a drag selects across lines, upwards as well as downwards, and up to a line's end beyond it")
    void dragSelectsAcrossLines() {
        MarkdownView view = layOut(new MarkdownView("```\nint total = price * count;\n    return total;\n```"));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);

        drag(code, caretPoint(code, 0, 12), caretPoint(code, 1, 10));
        shortcut(view, KeyCode.C);
        Bounds highlightBounds = selectionHighlight(block).localToScene(selectionHighlight(block).getLayoutBounds());
        assertEquals(2 * lineHeight(code), highlightBounds.getHeight(), 1.0, "the highlight spans both lines");
        drag(code, caretPoint(code, 1, 10), caretPoint(code, 0, 12));
        shortcut(view, KeyCode.C);
        drag(code, caretPoint(code, 0, 4), caretPoint(code, 0, 60));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("price * count;\n    return", "price * count;\n    return", "total = price * count;"),
                copied);
    }

    @Test
    @DisplayName("a double click selects the word under the pointer, a triple click its line without the line feed")
    void doubleAndTripleClick() {
        MarkdownView view = layOut(new MarkdownView(
                "```\nlong ovens = getKitchenValue(\"ovens\", tLong);\nif (o.index() >= max_rows) break;\nend();\n```"));
        List<String> copied = recordCopies(view);
        Text code = codeText(view.lookup(".markdown-code-block"));

        clicks(code, caretPoint(code, 0, 16), 2);
        shortcut(view, KeyCode.C);
        clicks(code, caretPoint(code, 1, 20), 2);
        shortcut(view, KeyCode.C);
        clicks(code, caretPoint(code, 1, 5), 3);
        shortcut(view, KeyCode.C);

        // The second press of a double click and a small move of the mouse before the release keep the word.
        clicks(code, caretPoint(code, 0, 7), 1);
        Event.fireEvent(code, mouse(MouseEvent.MOUSE_PRESSED, caretPoint(code, 0, 7), 2));
        Event.fireEvent(code, mouse(MouseEvent.MOUSE_DRAGGED, caretPoint(code, 0, 6), 2));
        Event.fireEvent(code, mouse(MouseEvent.MOUSE_RELEASED, caretPoint(code, 0, 6), 2));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("getKitchenValue", "max_rows", "if (o.index() >= max_rows) break;", "ovens"), copied);
    }

    @Test
    @DisplayName("without a selection the shortcut key plus C copies nothing; plus A selects the block's whole text")
    void copyWithoutSelectionAndSelectAll() {
        String markdown = "```\nint total = price * count;\n\nreturn total;\n```";
        MarkdownView view = layOut(new MarkdownView(markdown));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);

        clicks(code, caretPoint(code, 0, 4), 1);
        shortcut(view, KeyCode.C);
        assertEquals(List.of(), copied, "a click selects nothing");
        drag(code, caretPoint(code, 0, 4), caretPoint(code, 0, 9));
        clicks(code, caretPoint(code, 0, 4), 1);
        shortcut(view, KeyCode.C);
        assertEquals(List.of(), copied, "a click clears the selection");

        shortcut(view, KeyCode.A);
        assertFalse(selectionHighlight(block).getElements().isEmpty(), "the whole text highlighted");
        shortcut(view, KeyCode.C);
        copyButton(block).fire();
        copyMarkdownEntry(view).fire();

        assertEquals(List.of("int total = price * count;\n\nreturn total;", "int total = price * count;\n\nreturn total;",
                markdown), copied);
    }

    @Test
    @DisplayName("one selection at a time: a selection in another block, or a new text, clears it")
    void oneSelectionAtATime() {
        MarkdownView view = layOut(new MarkdownView("```\nfirst();\n```\n\n- ```\n  second();\n  ```\n\n"
                + "<div>amount INT64</div>"));
        List<String> copied = recordCopies(view);
        Node first = view.lookup(".markdown-code-block");
        Node second = new ArrayList<>(view.lookupAll(".markdown-code-block")).get(1);
        Node html = view.lookup(".markdown-source-block");

        drag(codeText(first), caretPoint(codeText(first), 0, 0), caretPoint(codeText(first), 0, 5));
        drag(codeText(second), caretPoint(codeText(second), 0, 0), caretPoint(codeText(second), 0, 6));
        assertTrue(selectionHighlight(first).getElements().isEmpty(), "the first block's selection is gone");
        shortcutAt(first, KeyCode.C);
        shortcut(view, KeyCode.C);
        Text htmlText = sourceText(html);
        drag(htmlText, caretPoint(htmlText, 0, 5), caretPoint(htmlText, 0, 11));
        assertTrue(selectionHighlight(second).getElements().isEmpty(), "a source block's selection clears it too");
        shortcutAt(second, KeyCode.C);
        shortcut(view, KeyCode.C);
        assertEquals(List.of("second", "amount"), copied);

        view.setMarkdown("```\nfirst();\n```\n\n- ```\n  second();\n  ```\n\n<div>x</div>");
        // A block's scroll pane adds its content once its skin exists: after the next CSS pass, as a pulse would.
        view.getScene().getRoot().applyCss();
        view.getScene().getRoot().layout();
        for (Node block : List.of(view.lookup(".markdown-code-block"), view.lookup(".markdown-source-block"))) {
            assertTrue(selectionHighlight(block).getElements().isEmpty(), "nothing selected in the new text");
            shortcutAt(block, KeyCode.C);
        }
        assertEquals(List.of("second", "amount"), copied);
    }

    @Test
    @DisplayName("in a block scrolled sideways, a drag and a double click select the characters shown under the pointer")
    void selectionInScrolledBlock() {
        MarkdownView view = layOut(new MarkdownView(documentWithCodeBlock()));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);
        Event.fireEvent(code, wheel(code, -40, 0, true));
        double codeX = sceneX(code);
        assertEquals(sceneX(block) + 12 - 40, codeX, 0.5, "scrolled 40px, past the 12px padding");

        drag(code, caretPoint(code, 0, 12), caretPoint(code, 0, 21));
        shortcut(view, KeyCode.C);
        clicks(code, caretPoint(code, 0, 25), 2);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("java.util", "Comparator"), copied);
        assertEquals(codeX, sceneX(code), 0.5, "selecting does not scroll the block");
        ScrollPane document = (ScrollPane) view.lookup(".scroll-pane");
        assertEquals(document.getVmin(), document.getVvalue(), "nor the document");
    }

    @Test
    @DisplayName("a source block selects and copies like a code block: an HTML block's word, an indented code line, all"
            + " of it")
    void selectionInSourceBlock() {
        MarkdownView view = layOut(new MarkdownView("<div>\nName INT64\n</div>\n\n"
                + "    int x = 1;\n    return x;"));
        List<String> copied = recordCopies(view);
        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-source-block"));
        assertEquals(2, blocks.size());
        Text html = sourceText(blocks.get(0));
        Text indented = sourceText(blocks.get(1));

        clicks(html, caretPoint(html, 1, 7), 2);
        shortcut(view, KeyCode.C);
        assertEquals(themeColor(view, "-fx-selection-color"), selectionHighlight(blocks.get(0)).getFill());
        clicks(indented, caretPoint(indented, 1, 6), 3);
        shortcut(view, KeyCode.C);
        assertTrue(selectionHighlight(blocks.get(0)).getElements().isEmpty(), "one selection at a time");
        shortcut(view, KeyCode.A);
        shortcut(view, KeyCode.C);

        assertSame(blocks.get(1), view.getScene().getFocusOwner());
        // A source block shows its lines as written, so indented code keeps its own four spaces.
        assertEquals(List.of("INT64", "    return x;", "    int x = 1;\n    return x;"), copied);
    }

    @Test
    @DisplayName("dragging over a paragraph selects the characters between press and pointer; the shortcut key plus C"
            + " at the focus owner the press left copies them")
    void dragSelectsWithinParagraph() {
        MarkdownView view = layOut(new MarkdownView("Intro.\n\nThe quick brown fox jumps."));
        List<String> copied = recordCopies(view);
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(1);

        drag(runAt(paragraph, 4), textPoint(paragraph, 4), textPoint(paragraph, 9));

        Node focusOwner = view.getScene().getFocusOwner();
        assertNotNull(focusOwner, "a press in text gives the view the keyboard focus");
        assertTrue(isInside(focusOwner, view), () -> "the focus is inside the view: " + focusOwner);
        assertEquals(Cursor.TEXT, paragraph.getCursor(), "the text cursor over text");
        shortcut(view, KeyCode.C);
        assertEquals(List.of("quick"), copied);
    }

    @Test
    @DisplayName("a text selection shows as a highlight over exactly the selected characters, which show on it in the"
            + " selection's text colour; a plain click clears it")
    void textSelectionHighlight() {
        MarkdownView view = layOut(new MarkdownView("The quick brown fox jumps."));
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(0);
        Text run = runs(paragraph).get(0);

        drag(run, textPoint(paragraph, 4), textPoint(paragraph, 9));
        relayOut(view);

        Path highlight = selectionHighlight(paragraph);
        assertEquals(themeColor(view, "-fx-selection-color"), highlight.getFill());
        Parent layer = highlight.getParent();
        assertTrue(paragraph.getChildrenUnmodifiable().indexOf(layer) > paragraph.getChildrenUnmodifiable().indexOf(run),
                "the highlight lies over the paragraph's own characters");
        List<Text> shown = new ArrayList<>();
        for (Node child : layer.getChildrenUnmodifiable()) {
            if (child instanceof Text copy) {
                assertTrue(layer.getChildrenUnmodifiable().indexOf(highlight) < layer.getChildrenUnmodifiable().indexOf(copy),
                        "behind the characters shown on it");
                assertEquals(themeColor(view, "-fx-selection-text-color"), copy.getFill(), "the selected characters");
                assertEquals(run.getFont(), copy.getFont());
                shown.add(copy);
            }
        }
        assertEquals("The quick brown fox jumps.", shown.get(0).getText());
        Bounds runBounds = sceneBounds(run);
        Bounds highlightBounds = highlight.localToScene(highlight.getLayoutBounds());
        assertEquals(runBounds.getMinX() + textWidth("The ", run.getFont()), highlightBounds.getMinX(), 1.0,
                "from the fifth character");
        assertEquals(runBounds.getMinX() + textWidth("The quick", run.getFont()), highlightBounds.getMaxX(), 1.0,
                "to the ninth");
        assertEquals(runBounds.getMinY(), highlightBounds.getMinY(), 1.0, "on the line");
        assertEquals(runBounds.getMaxY(), highlightBounds.getMaxY(), 3.0, "on the line only");

        clicks(run, textPoint(paragraph, 12), 1);
        assertFalse(isTreeVisible(highlight) && !highlight.getElements().isEmpty(), "a plain click clears the selection");
    }

    @Test
    @DisplayName("a drag across a heading, a paragraph, a nested list and a quote selects all between, forwards and"
            + " backwards, and copies it as visible text with markers, indentation, quote prefixes and blank lines")
    void dragSelectsAcrossBlocks() {
        MarkdownView view = layOut(new MarkdownView("# Title\n\nFirst paragraph with *emphasis*.\n\n- one\n- two\n"
                + "  - nested\n\n> quoted **text**\n>\n> second\n\nLast line."));
        List<String> copied = recordCopies(view);
        TextFlow title = textFlows(view, ".markdown-heading").get(0);
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        TextFlow last = paragraphs.getLast();
        assertEquals("Last line.", textOf(last));

        drag(runAt(title, 2), textPoint(title, 2), textPoint(last, 4));
        shortcut(view, KeyCode.C);
        drag(runAt(last, 4), textPoint(last, 4), textPoint(title, 2));
        shortcut(view, KeyCode.C);

        String expected = "tle\n\nFirst paragraph with emphasis.\n\n- one\n- two\n  - nested\n\n> quoted text\n>\n"
                + "> second\n\nLast";
        assertEquals(List.of(expected, expected), copied);
        for (TextFlow paragraph : paragraphs) {
            assertFalse(selectionHighlight(paragraph).getElements().isEmpty(),
                    () -> "highlighted: " + textOf(paragraph));
        }
    }

    @Test
    @DisplayName("in text a double click selects a word, a triple click the block's text, and shift plus a click extends"
            + " the selection from where it was anchored")
    void wordBlockAndShiftClickInText() {
        MarkdownView view = layOut(new MarkdownView("Alpha don't gamma.\n\nDelta *epsilon* zeta."));
        List<String> copied = recordCopies(view);
        TextFlow first = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow second = textFlows(view, ".markdown-paragraph").get(1);

        clicks(runAt(first, 8), textPoint(first, 8), 2);
        shortcut(view, KeyCode.C);
        clicks(runAt(second, 8), textPoint(second, 8), 3);
        shortcut(view, KeyCode.C);
        clicks(runAt(first, 6), textPoint(first, 6), 1);
        shiftClick(runAt(second, 5), textPoint(second, 5));
        shortcut(view, KeyCode.C);
        shiftClick(runAt(first, 1), textPoint(first, 1));
        shortcut(view, KeyCode.C);
        // A drag after a double click extends from the whole word.
        Event.fireEvent(runAt(first, 14), mouse(MouseEvent.MOUSE_PRESSED, textPoint(first, 14), 1));
        Event.fireEvent(runAt(first, 14), mouse(MouseEvent.MOUSE_RELEASED, textPoint(first, 14), 1));
        Event.fireEvent(runAt(first, 14), mouse(MouseEvent.MOUSE_PRESSED, textPoint(first, 14), 2));
        Event.fireEvent(runAt(first, 14), mouse(MouseEvent.MOUSE_DRAGGED, textPoint(second, 2), 2));
        Event.fireEvent(runAt(first, 14), mouse(MouseEvent.MOUSE_RELEASED, textPoint(second, 2), 2));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("don't", "Delta epsilon zeta.", "don't gamma.\n\nDelta", "lpha ",
                "gamma.\n\nDe"), copied);
    }

    @Test
    @DisplayName("the shortcut key plus A selects the whole text; the copy resolves inline Markdown and writes markers,"
            + " numbers, indentation, quote prefixes, line breaks and blank lines as plain text")
    void selectAllCopiesPlainText() {
        MarkdownView view = layOut(new MarkdownView("## Setup\n\n"
                + "Run `make` with **care**, see [the docs](https://example.com).  \nSecond line after a hard break.\n\n"
                + "3) third\n4) fourth\n   with soft break\n\n---\n\n"
                + "- loose one\n\n- loose two\n\n  second paragraph of two\n\n"
                + "> outer\n> > inner quote\n>\n> - item in quote\n\n"
                + "- item\n  > quote in item"));
        List<String> copied = recordCopies(view);
        TextFlow first = textFlows(view, ".markdown-paragraph").get(0);

        clicks(runAt(first, 0), textPoint(first, 0), 1);
        shortcut(view, KeyCode.C);
        assertEquals(List.of(), copied, "nothing selected, nothing copied");
        shortcut(view, KeyCode.A);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("Setup\n\n"
                + "Run make with care, see the docs.\nSecond line after a hard break.\n\n"
                + "3) third\n4) fourth with soft break\n\n"
                + "- loose one\n\n- loose two\n\n  second paragraph of two\n\n"
                + "> outer\n>\n> > inner quote\n>\n> - item in quote\n"
                + "- item\n\n  > quote in item"), copied);
    }

    @Test
    @DisplayName("a selection that starts at a block's first character copies its marker or prefix, one that starts"
            + " inside it only the selected part")
    void partlySelectedBlocksCopyTheirSelectedPart() {
        MarkdownView view = layOut(new MarkdownView("> outer\n> > inner quote\n>\n> - item in quote\n\n"
                + "1. one\n   1. nested **two**"));
        List<String> copied = recordCopies(view);
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        TextFlow outer = paragraphs.get(0);
        TextFlow inner = paragraphs.get(1);
        TextFlow itemInQuote = paragraphs.get(2);
        TextFlow nested = paragraphs.get(4);
        assertEquals("nested two", textOf(nested));

        drag(runAt(inner, 0), textPoint(inner, 0), textPoint(itemInQuote, 15));
        shortcut(view, KeyCode.C);
        drag(runAt(outer, 2), textPoint(outer, 2), textPoint(inner, 5));
        shortcut(view, KeyCode.C);
        drag(runAt(nested, 0), textPoint(nested, 0), textPoint(nested, 10));
        shortcut(view, KeyCode.C);
        drag(runAt(nested, 7), textPoint(nested, 7), textPoint(nested, 10));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("> > inner quote\n>\n> - item in quote", "ter\n>\n> > inner", "  1. nested two", "two"),
                copied);
    }

    @Test
    @DisplayName("after a selection made with the mouse, the shortcut key plus C copies it from wherever the focus is"
            + " inside the view, and goes no further")
    void shortcutCopiesFromAnywhereInTheView() {
        MarkdownView view = layOut(new MarkdownView("Intro text.\n\n- first item\n- second item\n\n---\n\n"
                + "```\ncode();\n```\n\nOutro text."));
        List<String> copied = recordCopies(view);
        List<KeyEvent> beyondView = new ArrayList<>();
        view.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, event -> beyondView.add(event));
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        TextFlow intro = paragraphs.get(0);
        TextFlow second = paragraphs.get(2);
        Node marker = view.lookupAll(".markdown-list-marker").toArray(new Node[0])[1];
        Node code = codeText(view.lookup(".markdown-code-block"));

        // A press on a code block gives it the focus; the press in text that follows takes it into the view.
        clicks(code, caretPoint((Text) code, 0, 1), 1);
        assertSame(view.lookup(".markdown-code-block"), view.getScene().getFocusOwner());
        drag(runAt(intro, 6), textPoint(intro, 6), textPoint(second, 6));
        Node focusOwner = view.getScene().getFocusOwner();
        assertTrue(isInside(focusOwner, view), () -> "the focus is inside the view: " + focusOwner);
        assertFalse(isInside(focusOwner, view.lookup(".markdown-code-block")), "not in the code block any more");
        shortcut(view, KeyCode.C);
        for (Node target : List.of(view, view.lookup(".markdown-document"), intro, runAt(second, 0), marker,
                view.lookup(".markdown-separator"))) {
            shortcutAt(target, KeyCode.C);
        }
        // A press on a list marker, between blocks, takes the focus into the view too.
        Point2D onMarker = new Point2D(sceneBounds(marker).getCenterX(), sceneBounds(marker).getCenterY());
        Event.fireEvent(marker, mouse(MouseEvent.MOUSE_PRESSED, onMarker, 1));
        assertTrue(isInside(view.getScene().getFocusOwner(), view));

        String expected = "text.\n\n- first item\n- second";
        assertEquals(List.of(expected, expected, expected, expected, expected, expected, expected), copied);
        assertEquals(List.of(), beyondView, "the shortcut is consumed in the view");
    }

    @Test
    @DisplayName("a click on a link without moving follows it and selects nothing; a press on a link followed by a drag"
            + " selects text and follows nothing")
    void linkClickFollowsAndDragSelects() {
        MarkdownView view = layOut(new MarkdownView("See [the docs](https://example.com/docs) for more."));
        List<String> copied = recordCopies(view);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(0);
        Text link = linkRuns(view).get(0);
        assertSame(link, runAt(paragraph, 6));

        clicks(link, textPoint(paragraph, 6), 1);
        shortcut(view, KeyCode.C);
        assertEquals(List.of("https://example.com/docs"), followed, "a still click follows the link");
        assertEquals(List.of(), copied, "and selects nothing");

        Event.fireEvent(link, mouse(MouseEvent.MOUSE_PRESSED, textPoint(paragraph, 4), 1));
        Event.fireEvent(link, mouse(MouseEvent.MOUSE_DRAGGED, textPoint(paragraph, 16), 1));
        Event.fireEvent(link, mouse(MouseEvent.MOUSE_RELEASED, textPoint(paragraph, 16), 1));
        // JavaFX still sends a click when the release is on the pressed node, marked as not still since the press.
        Event.fireEvent(link, movedClick(textPoint(paragraph, 16)));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("https://example.com/docs"), followed, "a drag follows nothing");
        assertEquals(List.of("the docs for"), copied);
        assertEquals(Cursor.HAND, link.getCursor(), "a link keeps its hand cursor");
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("at another zoom, selecting by a drag, copying, a link, the context menu, a copy button and a table's"
            + " cells work as at 100 %, the copy button scaled with the code")
    @ValueSource(doubles = {1.5, 0.5})
    void selectionLinksMenuAndCopyAtAnotherZoom(double zoom) {
        MarkdownView view = layOut(new MarkdownView("The quick brown fox jumps over [the docs](https://example.com/docs)."
                + "\n\n```\nint total = price * count;\n```\n\n" + SELECTION_TABLE));
        view.setZoom(zoom);
        relayOut(view);
        List<String> copied = recordCopies(view);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(0);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);
        TableView<?> table = table(view);

        drag(runAt(paragraph, 4), textPoint(paragraph, 4), textPoint(paragraph, 9));
        shortcut(view, KeyCode.C);
        menuEntry(contextMenuAt(view, runAt(paragraph, 12)), "Copy").fire();
        clicks(runAt(paragraph, 33), textPoint(paragraph, 33), 1);
        drag(code, caretPoint(code, 0, 4), caretPoint(code, 0, 9));
        shortcut(view, KeyCode.C);
        copyButton(block).fire();
        clickCell(table, 1, 2, false, false);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("https://example.com/docs"), followed);
        assertEquals(List.of("quick", "quick", "total", "int total = price * count;", "second"), copied);
        Button copyButton = copyButton(block);
        assertEquals(copyButton.getWidth() * zoom, sceneBounds(copyButton).getWidth(), 0.5, "the copy button scaled");
    }

    @Test
    @DisplayName("one selection per view: a selection in a code block or a table clears the text selection and the"
            + " other way round, and a new text starts without one")
    void textSelectionIsTheViewsOneSelection() {
        MarkdownView view = layOut(new MarkdownView("Text before.\n\n```\ncode();\n```\n\n" + SELECTION_TABLE
                + "\n\nText after."));
        List<String> copied = recordCopies(view);
        TextFlow before = textFlows(view, ".markdown-paragraph").get(0);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);
        TableView<?> table = table(view);

        drag(runAt(before, 0), textPoint(before, 0), textPoint(before, 4));
        assertTrue(isHighlighted(before));
        drag(code, caretPoint(code, 0, 0), caretPoint(code, 0, 4));
        assertFalse(isHighlighted(before), "a code block's selection clears the text selection");
        shortcut(view, KeyCode.C);
        drag(runAt(before, 5), textPoint(before, 5), textPoint(before, 11));
        assertTrue(selectionHighlight(block).getElements().isEmpty(), "and the other way round");
        shortcut(view, KeyCode.C);
        clickCell(table, 0, 2, false, false);
        assertFalse(isHighlighted(before), "a table's selection clears the text selection");
        shortcut(view, KeyCode.C);
        drag(runAt(before, 0), textPoint(before, 0), textPoint(before, 4));
        assertEquals("...\n...\n...", selection(table), "and the other way round");
        shortcut(view, KeyCode.C);

        view.setMarkdown("Text before.\n\nNew text.");
        relayOut(view);
        for (TextFlow flow : textFlows(view, ".markdown-paragraph")) {
            assertFalse(isHighlighted(flow), "nothing selected in a new text");
        }
        shortcutAt(view, KeyCode.C);
        TextFlow fresh = textFlows(view, ".markdown-paragraph").get(1);
        shiftClick(runAt(fresh, 4), textPoint(fresh, 4));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("code", "before", "first", "Text"), copied);
    }

    @Test
    @DisplayName("on text and between blocks the context menu has Copy for the text selection, disabled without one,"
            + " beside Copy Markdown")
    void textContextMenu() {
        String markdown = "First *paragraph* here.\n\n- item\n\n---\n\nSecond.";
        MarkdownView view = layOut(new MarkdownView(markdown));
        List<String> copied = recordCopies(view);
        TextFlow first = textFlows(view, ".markdown-paragraph").get(0);
        Text emphasis = runAt(first, 7);

        List<MenuItem> nothingSelected = contextMenuAt(view, emphasis);
        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(nothingSelected));
        assertTrue(menuEntry(nothingSelected, "Copy").isDisable(), "nothing selected");
        assertFalse(menuEntry(nothingSelected, "Copy Markdown").isDisable());

        drag(runAt(first, 6), textPoint(first, 6), textPoint(first, 15));
        for (Node target : List.of(emphasis, first, view.lookup(".markdown-list-marker"), view.lookup(".markdown-separator"),
                view.lookup(".markdown-document"))) {
            List<MenuItem> entries = contextMenuAt(view, target);
            assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(entries), () -> "at " + target);
            assertFalse(menuEntry(entries, "Copy").isDisable(), () -> "at " + target);
        }
        menuEntry(contextMenuAt(view, emphasis), "Copy").fire();
        menuEntry(contextMenuAt(view, emphasis), "Copy Markdown").fire();
        view.setCopyText("Kopieren");
        assertEquals("Kopieren", contextMenuAt(view, first).get(0).getText());

        assertEquals(List.of("paragraph", markdown), copied);
    }

    @Test
    @DisplayName("a drag below the view's visible area scrolls the document along, faster the further the pointer is"
            + " beyond the edge, and the selection follows to the text at the edge")
    void dragBeyondTheBottomEdgeScrollsAlong() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(30)));
        ManualFrames frames = new ManualFrames();
        view.setAnimationFrames(frames);
        List<String> copied = recordCopies(view);
        Node document = view.lookup(".markdown-document");
        TextFlow first = textFlows(view, ".markdown-paragraph").get(0);
        Text firstRun = runAt(first, 0);
        double bottom = sceneBounds(view).getMaxY();
        double x = sceneBounds(first).getMinX() + 20;

        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_PRESSED, textPoint(first, 0), 1));
        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_DRAGGED, new Point2D(x, bottom + 10), 1));
        assertTrue(frames.running(), "dragging beyond the edge starts scrolling");
        double top = sceneY(document);
        frames.advance(0.5);
        double slow = top - sceneY(document);
        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_DRAGGED, new Point2D(x, bottom + 40), 1));
        double before = sceneY(document);
        frames.advance(0.5);
        double fast = before - sceneY(document);
        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_DRAGGED, new Point2D(x, bottom - 5), 1));
        assertFalse(frames.running(), "back inside, the scrolling stops");
        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_DRAGGED, new Point2D(x, bottom + 10), 1));
        frames.advance(0.1);
        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_RELEASED, new Point2D(x, bottom + 10), 1));
        assertFalse(frames.running(), "the release stops it");
        shortcut(view, KeyCode.C);

        assertTrue(slow > 10, () -> "scrolled " + slow + "px");
        assertTrue(fast > 3 * slow, () -> "scrolled " + fast + "px, beyond " + slow + "px");
        String text = copied.getFirst();
        assertTrue(text.startsWith("Paragraph number 1.\n\nSection 2\n\nParagraph number 2."), text);
        for (TextFlow flow : textFlows(view, ".markdown-paragraph")) {
            Bounds bounds = sceneBounds(flow);
            if (bounds.getMaxY() < bottom - 2) {
                assertTrue(text.contains(textOf(flow)), () -> "selected up to the edge: " + textOf(flow));
            } else if (bounds.getMinY() > bottom + 2) {
                assertFalse(text.contains(textOf(flow)), () -> "not beyond the edge: " + textOf(flow));
            }
        }
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("at another zoom a drag below the visible area scrolls the document along as fast as at 100 %, in the"
            + " visible area's pixels")
    @ValueSource(doubles = {2.0, 0.5})
    void dragBeyondTheEdgeScrollsAlongAsFastAtAnotherZoom(double zoom) {
        double atOne = scrolledAlongBelowTheEdge(1.0);

        double atZoom = scrolledAlongBelowTheEdge(zoom);

        assertTrue(atOne > 10, () -> "scrolled " + atOne + "px");
        assertEquals(atOne, atZoom, 1.5);
    }

    /**
     * How far, in the scene's pixels, half a second of a drag 10px below the visible area of a view at a zoom scrolls
     * its document.
     */
    private static double scrolledAlongBelowTheEdge(double zoom) {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(60)));
        view.setZoom(zoom);
        relayOut(view);
        ManualFrames frames = new ManualFrames();
        view.setAnimationFrames(frames);
        Node document = view.lookup(".markdown-document");
        TextFlow first = textFlows(view, ".markdown-paragraph").get(0);
        Text firstRun = runAt(first, 0);
        double bottom = sceneBounds(view).getMaxY();
        double x = sceneBounds(first).getMinX() + 5;

        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_PRESSED, textPoint(first, 0), 1));
        Event.fireEvent(firstRun, mouse(MouseEvent.MOUSE_DRAGGED, new Point2D(x, bottom + 10), 1));
        double top = sceneY(document);
        frames.advance(0.5);
        return top - sceneY(document);
    }

    @Test
    @DisplayName("a drag from a paragraph into a code block selects up to the character under the pointer, a drag from"
            + " the middle of the code into the next paragraph from there; the shortcut key plus C at the focus owner the"
            + " press left copies the code as written, set apart from the paragraphs")
    void dragIntoAndOutOfCodeBlock() {
        MarkdownView view = layOut(new MarkdownView("Text before.\n\n```\nint total = price * count;\n    return total;\n"
                + "```\n\nText after."));
        List<String> copied = recordCopies(view);
        TextFlow before = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow after = textFlows(view, ".markdown-paragraph").get(1);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);

        drag(runAt(before, 5), textPoint(before, 5), caretPoint(code, 0, 9));
        assertTrue(isHighlighted(before));
        assertFalse(selectionHighlight(block).getElements().isEmpty(), "the code is highlighted up to the pointer");
        assertFalse(isInside(view.getScene().getFocusOwner(), block), "a press in text leaves the focus outside the block");
        shortcut(view, KeyCode.C);
        drag(code, caretPoint(code, 1, 4), textPoint(after, 4));
        assertSame(block, view.getScene().getFocusOwner(), "a press in code gives the block the focus");
        assertFalse(isHighlighted(before), "the selection before is gone");
        assertTrue(isHighlighted(after));
        shortcut(view, KeyCode.C);
        drag(runAt(after, 4), textPoint(after, 4), caretPoint(code, 0, 4));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("before.\n\nint total", "return total;\n\nText",
                "total = price * count;\n    return total;\n\nText"), copied);
    }

    @Test
    @DisplayName("a drag over whole code blocks highlights all their code and copies every line as written, prefixed"
            + " as the lines of the list item or quote around it, forwards and backwards")
    void dragOverWholeCodeBlocks() {
        MarkdownView view = layOut(new MarkdownView("1. Read the limit:\n\n   ```java\n   long limit = 3;\n"
                + "   if (limit > 2) {\n       stop();\n   }\n   ```\n\n2. Stop.\n\n> ```\n> quoted();\n>\n> ```\n\n"
                + "After."));
        List<String> copied = recordCopies(view);
        TextFlow read = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow after = textFlows(view, ".markdown-paragraph").getLast();
        assertEquals("After.", textOf(after));
        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        Text code = codeText(blocks.get(0));

        drag(runAt(read, 0), textPoint(read, 0), textPoint(after, 5));
        Path highlight = selectionHighlight(blocks.get(0));
        Bounds highlightBounds = highlight.localToScene(highlight.getLayoutBounds());
        assertEquals(4 * lineHeight(code), highlightBounds.getHeight(), 1.0, "every line of the code highlighted");
        assertFalse(selectionHighlight(blocks.get(1)).getElements().isEmpty());
        shortcut(view, KeyCode.C);
        drag(runAt(after, 5), textPoint(after, 5), textPoint(read, 0));
        shortcut(view, KeyCode.C);

        String expected = "1. Read the limit:\n\n  long limit = 3;\n  if (limit > 2) {\n      stop();\n  }\n\n2. Stop.\n\n"
                + "> quoted();\n>\n\nAfter";
        assertEquals(List.of(expected, expected), copied);
    }

    @Test
    @DisplayName("a drag over a table takes it whole: every shown cell and header looks selected, and it copies as its"
            + " header line and all its rows, tab-separated, the shown columns and rows in their shown order; a drag"
            + " that ends in the table's upper half leaves it out, one that ends in its lower half takes it")
    void dragOverWholeTable() {
        MarkdownView view = layOut(new MarkdownView("Before the table.\n\n" + SELECTION_TABLE + "\n\nAfter the table."));
        List<String> copied = recordCopies(view);
        TextFlow before = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow after = textFlows(view, ".markdown-paragraph").get(1);
        TableView<?> table = table(view);
        moveColumn(table, 2, 0);
        relayOut(view);
        clickHeader(table, 1, false);
        clickHeader(table, 1, false);
        table.getColumns().get(2).setVisible(false);
        relayOut(view);
        assertEquals(List.of("c", "b", "a"), shownColumn(table, 1), "sorted by Key, descending");
        Bounds tableBounds = sceneBounds(table);
        double x = tableBounds.getMinX() + 20;
        Point2D upperHalf = new Point2D(x, tableBounds.getMinY() + 0.3 * tableBounds.getHeight());
        Point2D lowerHalf = new Point2D(x, tableBounds.getMinY() + 0.7 * tableBounds.getHeight());

        drag(runAt(before, 7), textPoint(before, 7), textPoint(after, 5));
        relayOut(view);
        assertTableLooksSelected(view, table, true);
        assertEquals("..-\n..-\n..-", selection(table), "no cell of the table's own selection");
        shortcut(view, KeyCode.C);
        drag(runAt(after, 5), textPoint(after, 5), textPoint(before, 7));
        shortcut(view, KeyCode.C);
        drag(runAt(before, 7), textPoint(before, 7), upperHalf);
        relayOut(view);
        assertTableLooksSelected(view, table, false);
        shortcut(view, KeyCode.C);
        drag(runAt(before, 7), textPoint(before, 7), lowerHalf);
        shortcut(view, KeyCode.C);
        drag(runAt(after, 5), textPoint(after, 5), lowerHalf);
        shortcut(view, KeyCode.C);
        drag(runAt(after, 5), textPoint(after, 5), upperHalf);
        shortcut(view, KeyCode.C);

        String tableText = "Meaning\tKey\nthird\tc\nsecond\tb\nfirst\ta";
        assertEquals(List.of("the table.\n\n" + tableText + "\n\nAfter", "the table.\n\n" + tableText + "\n\nAfter",
                "the table.", "the table.\n\n" + tableText, "After", tableText + "\n\nAfter"), copied);
    }

    @Test
    @DisplayName("a press on a cell of a table the text selection passes over still selects cells as in a spreadsheet,"
            + " and ends the text selection; the shortcut key plus A there selects the table's cells only")
    void pressInSelectedTableSelectsCells() {
        MarkdownView view = layOut(new MarkdownView("Before the table.\n\n" + SELECTION_TABLE + "\n\nAfter the table."));
        List<String> copied = recordCopies(view);
        TextFlow before = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow after = textFlows(view, ".markdown-paragraph").get(1);
        TableView<?> table = table(view);

        drag(runAt(before, 0), textPoint(before, 0), textPoint(after, 5));
        clickCell(table, 1, 1, false, false);
        relayOut(view);
        assertEquals("...\n.#.\n...", selection(table));
        assertFalse(isHighlighted(before), "the text selection is gone");
        assertFalse(isHighlighted(after));
        assertFalse(fills(cell(table, 0, 0)).contains(themeColor(view, "-fx-selection-color")),
                "the table no longer looks selected whole");
        shortcut(view, KeyCode.C);
        key(view, KeyCode.A, false, true);
        assertEquals("###\n###\n###", selection(table));
        assertFalse(isHighlighted(before), "the cells only");
        shortcut(view, KeyCode.C);
        drag(runAt(after, 0), textPoint(after, 0), textPoint(after, 5));
        assertEquals("...\n...\n...", selection(table), "a press in text ends the table's selection");
        shortcut(view, KeyCode.C);

        assertEquals(List.of("STRING", "a\tINT64\tfirst\nb\tSTRING\tsecond\nc\tDOUBLE\tthird", "After"), copied);
    }

    @Test
    @DisplayName("after a selection through a code block and a table, the shortcut key plus C at the focus owner the"
            + " press left copies all of it: a press in text, in the code, from below; plus A in text selects the whole"
            + " document, code and table included; the key goes no further than the view")
    void shortcutCopiesThroughCodeBlockAndTable() {
        MarkdownView view = layOut(new MarkdownView("Intro text.\n\n```\nint total = price * count;\nreturn total;\n```\n\n"
                + SELECTION_TABLE + "\n\nOutro text."));
        List<String> copied = recordCopies(view);
        List<KeyEvent> beyondView = new ArrayList<>();
        view.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, event -> beyondView.add(event));
        TextFlow intro = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow outro = textFlows(view, ".markdown-paragraph").get(1);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);
        TableView<?> table = table(view);

        drag(runAt(intro, 6), textPoint(intro, 6), textPoint(outro, 5));
        Node focusOwner = view.getScene().getFocusOwner();
        assertTrue(isInside(focusOwner, view) && !isInside(focusOwner, block) && !isInside(focusOwner, table),
                () -> "the focus in the view, outside the code block and the table: " + focusOwner);
        shortcut(view, KeyCode.C);
        drag(code, caretPoint(code, 0, 12), textPoint(outro, 5));
        assertSame(block, view.getScene().getFocusOwner());
        relayOut(view);
        assertTableLooksSelected(view, table, true);
        shortcut(view, KeyCode.C);
        drag(runAt(outro, 5), textPoint(outro, 5), caretPoint(code, 1, 7));
        assertFalse(isInside(view.getScene().getFocusOwner(), block));
        shortcut(view, KeyCode.C);
        shortcut(view, KeyCode.A);
        shortcut(view, KeyCode.C);

        String tableText = "Key\tType\tMeaning\na\tINT64\tfirst\nb\tSTRING\tsecond\nc\tDOUBLE\tthird";
        assertEquals(List.of("text.\n\nint total = price * count;\nreturn total;\n" + tableText + "\n\nOutro",
                "price * count;\nreturn total;\n" + tableText + "\n\nOutro",
                "total;\n" + tableText + "\n\nOutro",
                "Intro text.\n\nint total = price * count;\nreturn total;\n" + tableText + "\n\nOutro text."), copied);
        assertEquals(List.of(), beyondView, "the shortcut is consumed in the view");
    }

    @Test
    @DisplayName("the context menu's Copy copies the current selection: on a code block or a table it passes over, on"
            + " text; greyed out on a code block it does not reach; after a sort the shortcut key plus C at the table"
            + " copies it in the new order")
    void copyOnBlocksInTheTextSelection() {
        MarkdownView view = layOut(new MarkdownView("Intro text.\n\n```\nint total = price * count;\nreturn total;\n```\n\n"
                + SELECTION_TABLE + "\n\nOutro text.\n\n```\nother();\n```"));
        List<String> copied = recordCopies(view);
        TextFlow intro = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow outro = textFlows(view, ".markdown-paragraph").get(1);
        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        Text code = codeText(blocks.get(0));
        TableView<?> table = table(view);
        Node header = runs((TextFlow) table.getColumns().get(1).getGraphic()).get(0);

        drag(code, caretPoint(code, 1, 7), textPoint(outro, 5));
        menuEntry(contextMenuAt(view, code), "Copy").fire();
        List<MenuItem> onHeader = contextMenuAt(view, header);
        assertEquals(List.of("Copy", "Copy with header", "Select column", "-", "Copy Markdown"), menuTexts(onHeader));
        assertFalse(menuEntry(onHeader, "Copy").isDisable(), "the text selection passes over the table");
        assertTrue(menuEntry(onHeader, "Copy with header").isDisable(), "no cell of the table's own is selected");
        menuEntry(onHeader, "Copy").fire();
        menuEntry(contextMenuAt(view, runAt(intro, 0)), "Copy").fire();
        List<MenuItem> onOtherCode = contextMenuAt(view, codeText(blocks.get(1)));
        assertTrue(menuEntry(onOtherCode, "Copy").isDisable(), "a code block the selection does not reach");
        clickHeader(table, 1, false);
        assertSame(table, view.getScene().getFocusOwner(), "a click on a header gives the table the focus");
        shortcut(view, KeyCode.C);

        String selected = "total;\nKey\tType\tMeaning\na\tINT64\tfirst\nb\tSTRING\tsecond\nc\tDOUBLE\tthird\n\nOutro";
        assertEquals(List.of(selected, selected, selected,
                "total;\nKey\tType\tMeaning\nc\tDOUBLE\tthird\na\tINT64\tfirst\nb\tSTRING\tsecond\n\nOutro"), copied);
    }

    @Test
    @DisplayName("a drag from a code block below the view's visible area scrolls the document along, the selection"
            + " following from the code, and the release stops it")
    void dragFromCodeBlockBeyondTheBottomEdgeScrollsAlong() {
        MarkdownView view = layOut(new MarkdownView(documentWithCodeBlock()));
        ManualFrames frames = new ManualFrames();
        view.setAnimationFrames(frames);
        List<String> copied = recordCopies(view);
        Text code = codeText(view.lookup(".markdown-code-block"));
        Node document = view.lookup(".markdown-document");
        double bottom = sceneBounds(view).getMaxY();
        Point2D below = new Point2D(sceneBounds(code).getMinX() + 20, bottom + 30);

        Event.fireEvent(code, mouse(MouseEvent.MOUSE_PRESSED, caretPoint(code, 1, 0), 1));
        Event.fireEvent(code, mouse(MouseEvent.MOUSE_DRAGGED, below, 1));
        assertTrue(frames.running(), "dragging beyond the edge starts scrolling");
        double top = sceneY(document);
        frames.advance(0.5);
        assertTrue(top - sceneY(document) > 10, "the document scrolls");
        Event.fireEvent(code, mouse(MouseEvent.MOUSE_RELEASED, below, 1));
        assertFalse(frames.running(), "the release stops it");
        shortcut(view, KeyCode.C);

        String text = copied.getFirst();
        assertTrue(text.startsWith("short();\n\nSection 1\n\nParagraph number 1."), text);
    }

    @Test
    @DisplayName("an HTML block and indented code are source blocks too, their lines as written")
    void htmlAndIndentedCodeAsSourceBlocks() {
        MarkdownView view = layOut(new MarkdownView(
                "<details>\n  <summary>More</summary>\n</details>\n\nText:\n\n    indented();\n      deeper();"));

        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-source-block"));
        assertEquals(2, blocks.size());
        assertEquals("<details>\n  <summary>More</summary>\n</details>", sourceText(blocks.get(0)).getText());
        assertEquals("    indented();\n      deeper();", sourceText(blocks.get(1)).getText());
        assertEquals(List.of("Text:"), texts(view, ".markdown-paragraph"));
    }

    @Test
    @DisplayName("a table is a TableView as tall as its header and all its rows, each row as tall as its wrapped text,"
            + " without a vertical bar")
    void tableShowsEveryRowAtItsHeight() {
        MarkdownView view = layOut(new MarkdownView("Before.\n\n| Name | Meaning |\n|---|---|\n| `a` | short |\n"
                + "| `b` | " + LONG_CELL + " |\n| `c` | short |\n\nAfter."));

        TableView<?> table = table(view);
        List<TableRow<?>> rows = shownRows(table);
        assertEquals(3, rows.size());
        assertEquals(rows.get(0).getHeight(), rows.get(2).getHeight(), 0.5, "two rows of one line");
        TextFlow wrapped = cellFlow(rows.get(1), 1);
        double line = lineHeight(runs(wrapped).get(0));
        assertTrue(textBounds(wrapped).getHeight() > 2.5 * line, () -> "the long cell wraps: " + textBounds(wrapped));
        assertTrue(rows.get(1).getHeight() > rows.get(0).getHeight() + 1.5 * line, "its row grows with it");
        for (TableRow<?> row : rows) {
            assertInside(textBounds(cellFlow(row, 1)), row);
        }
        assertAllRowsShown(table, rows);
        List<TextFlow> paragraphs = textFlows(view, ".markdown-paragraph");
        assertTrue(gap(paragraphs.get(0), table) > 0 && gap(table, paragraphs.get(1)) > 0, "between the paragraphs");
    }

    @Test
    @DisplayName("in a narrower view a long cell wraps over more lines, and its row and the table grow with it in the"
            + " same layout pass")
    void rowsFollowTheWidth() {
        MarkdownView view = layOut(new MarkdownView("| Name | Meaning |\n|---|---|\n| `a` | short |\n| `b` | "
                + LONG_CELL + " |"));
        TableView<?> table = table(view);
        double wideRow = shownRows(table).get(1).getHeight();
        double wideTable = table.getHeight();

        view.setMaxWidth(260);
        view.getScene().getRoot().applyCss();
        view.getScene().getRoot().layout();

        List<TableRow<?>> rows = shownRows(table);
        assertEquals(2, rows.size());
        double line = lineHeight(runs(cellFlow(rows.get(1), 1)).get(0));
        assertTrue(rows.get(1).getHeight() > wideRow + line, () -> "row " + wideRow + " -> " + rows.get(1).getHeight());
        assertTrue(table.getHeight() > wideTable + line, "the table grows");
        assertInside(textBounds(cellFlow(rows.get(1), 1)), rows.get(1));
        assertAllRowsShown(table, rows);

        view.setMaxWidth(Double.MAX_VALUE);
        view.getScene().getRoot().applyCss();
        view.getScene().getRoot().layout();

        assertEquals(wideRow, shownRows(table).get(1).getHeight(), 0.5, "and back");
        assertEquals(wideTable, table.getHeight(), 0.5);
        assertAllRowsShown(table, shownRows(table));
    }


    @Test
    @DisplayName("header and body cells show their inlines as styled runs, as paragraphs do, and a link in a cell"
            + " works")
    void tableCellsAreFormatted() {
        MarkdownView view = layOut(new MarkdownView("Before.\n\n| `Name` | **Type** \\| *kind* |\n|---|---|\n"
                + "| `amount` | **INT64**, *or* [docs](https://example.com/types) |\n\nAfter."));
        List<String> addresses = new ArrayList<>();
        view.setOnLinkAction(address -> addresses.add(address));

        TableView<?> table = table(view);
        assertEquals(List.of("Name", "Type | kind"), columnTexts(table));
        List<Text> name = runs((TextFlow) table.getColumns().get(0).getGraphic());
        assertEquals(1, name.size());
        assertRun(name.get(0), "Name", "markdown-code");
        List<Text> type = runs((TextFlow) table.getColumns().get(1).getGraphic());
        assertEquals(3, type.size());
        assertRun(type.get(0), "Type", "markdown-strong");
        assertRun(type.get(1), " | ");
        assertRun(type.get(2), "kind", "markdown-emphasis");
        TableRow<?> row = shownRows(table).get(0);
        Text amount = runs(cellFlow(row, 0)).get(0);
        assertRun(amount, "amount", "markdown-code");
        assertTrue(isMonospaced(amount.getFont()), () -> "not monospaced: " + amount.getFont());
        assertEquals(themeColor(view, "-fx-code-text-color"), amount.getFill());
        List<Text> runs = runs(cellFlow(row, 1));
        assertEquals(5, runs.size(), () -> "runs: " + runs);
        assertRun(runs.get(0), "INT64", "markdown-strong");
        assertRun(runs.get(1), ", ");
        assertRun(runs.get(2), "or", "markdown-emphasis");
        assertRun(runs.get(3), " ");
        assertRun(runs.get(4), "docs", "markdown-link");
        assertEquals("https://example.com/types", tooltip(runs.get(4)).getText());
        click(runs.get(4), MouseButton.PRIMARY, 1);
        assertEquals(List.of("https://example.com/types"), addresses);
        for (Node label : table.lookupAll(".column-header .label")) {
            assertEquals(ContentDisplay.GRAPHIC_ONLY, ((Labeled) label).getContentDisplay(),
                    "a header label shows the formatted text alone, not the column's plain text");
        }
        assertTrue(view.lookupAll(".markdown-source-block").isEmpty(), "no source block");
        assertEquals(List.of("Before.", "After."), texts(view, ".markdown-document > .markdown-paragraph"));
    }

    @Test
    @DisplayName("each column is aligned as its delimiter cell says, header and body alike")
    void tableAlignment() {
        MarkdownView view = layOut(new MarkdownView(
                "| none | left | centre | right |\n|---|:--|:-:|--:|\n| 1 | 2 | 3 | 4 |"));

        TableView<?> table = table(view);
        TableRow<?> row = shownRows(table).get(0);
        List<TextAlignment> expected = List.of(TextAlignment.LEFT, TextAlignment.LEFT, TextAlignment.CENTER,
                TextAlignment.RIGHT);
        for (int column = 0; column < expected.size(); column++) {
            assertEquals(expected.get(column),
                    ((TextFlow) table.getColumns().get(column).getGraphic()).getTextAlignment(), "header " + column);
            assertEquals(expected.get(column), cellFlow(row, column).getTextAlignment(), "cell " + column);
        }
        TextFlow left = cellFlow(row, 1);
        TextFlow centre = cellFlow(row, 2);
        TextFlow right = cellFlow(row, 3);
        assertEquals(sceneBounds(left).getMinX(), textBounds(left).getMinX(), 1.0, "left");
        assertEquals(sceneBounds(centre).getCenterX(), textBounds(centre).getCenterX(), 1.0, "centred");
        assertEquals(sceneBounds(right).getMaxX(), textBounds(right).getMaxX(), 1.0, "right");
        assertTrue(sceneBounds(right).getWidth() > 4 * textBounds(right).getWidth(),
                "in a cell much wider than its text");
    }

    @Test
    @DisplayName("the columns fill the table and share its width by their content, none narrower than its longest word")
    void columnsShareTheWidth() {
        MarkdownView view = layOut(new MarkdownView("| Key | Explanation |\n|---|---|\n| `java.util.Comparator` | "
                + LONG_CELL + " |\n| `k` | short |"));

        TableView<?> table = table(view);
        double first = table.getColumns().get(0).getWidth();
        double second = table.getColumns().get(1).getWidth();
        assertEquals(table.getWidth() - table.snappedLeftInset() - table.snappedRightInset(), first + second, 0.5,
                "the columns fill the table");
        assertEquals(((Region) table.getParent()).getWidth(), table.getWidth(), 0.5, "the table fills its block");
        assertTrue(second > first, "the long text gets more room");
        TableRow<?> row = shownRows(table).get(0);
        TextFlow key = cellFlow(row, 0);
        double line = lineHeight(runs(key).get(0));
        assertEquals(line, textBounds(key).getHeight(), 1.0, "the longest word stays whole on one line");
        assertTrue(textBounds(cellFlow(row, 1)).getHeight() > 2 * line, "the long text wraps");
        assertInside(textBounds(key), row);
        assertAllRowsShown(table, shownRows(table));
    }

    @Test
    @DisplayName("a column fitted to exactly 80 pixels, the width JavaFX gives a column nobody sized, keeps that"
            + " width: JavaFX does not fit it to its content, and the columns still fill the table")
    void columnFittedToJavaFxDefaultWidthKeepsIt() {
        // The columns share the table's width by their content, so the width that gives a column exactly 80 pixels
        // depends on the font: the table is laid out at every width around the one that should, and at one of them a
        // column has 80. Each width gets a view of its own, from its very first layout: JavaFX fits a column by
        // itself only when it first lays out the column's header.
        String markdown = "| a | a | a |\n|---|---|---|\n| a | a | a |";
        TableView<?> probe = table(layOut(new MarkdownView(markdown)));
        double probeInner = probe.getWidth() - probe.snappedLeftInset() - probe.snappedRightInset();
        double around = SCENE_WIDTH - probeInner;
        double expected = Math.rint(probeInner * 80 / columnWidths(probe)[0]);

        boolean aColumnOfEighty = false;
        for (double inner = expected - 15; inner <= expected + 15; inner++) {
            MarkdownView view = new MarkdownView(markdown);
            StackPane root = new StackPane(view);
            Scene scene = new Scene(root, inner + around, SCENE_HEIGHT);
            ThemeManager.apply(scene);
            root.applyCss();
            root.layout();

            TableView<?> table = table(view);
            String at = "in a table of " + inner;
            assertEquals(inner, table.getWidth() - table.snappedLeftInset() - table.snappedRightInset(), 0.01, at);
            double[] widths = columnWidths(table);
            assertEquals(inner, widths[0] + widths[1] + widths[2], 0.01, "the columns fill the table, " + at);
            assertFalse(isShown(scrollBar(table, Orientation.HORIZONTAL)), "no bar of the table's own, " + at);
            for (double width : widths) {
                aColumnOfEighty = aColumnOfEighty || Math.abs(width - 80) < 0.01;
            }
        }
        assertTrue(aColumnOfEighty, "one of the widths gives a column exactly 80 pixels");
    }

    @Test
    @DisplayName("in a table with a long column, the short columns keep their cells on one line and only the long"
            + " column wraps, as in a web browser")
    void shortColumnsKeepTheirFullWidth() {
        // The showcase sample's table.
        MarkdownView view = layOut(new MarkdownView("| Setting | Values | Default |\n|---|---|:-:|\n"
                + "| **Mode** | `Per row`, `Whole table` | `Per row` |\n"
                + "| **On error** | `Fail`, `Set missing and count` | `Fail` |\n"
                + "| **Locale** | a language tag such as `de-CH`, `fr` or `en-US`; empty, the output is locale-neutral"
                + " | empty |"));

        TableView<?> table = table(view);
        List<TableRow<?>> rows = shownRows(table);
        assertEquals(List.of("Mode", "Per row, Whole table", "Per row"), cellTexts(rows.get(0)));
        for (int column : new int[] {0, 2}) {
            TextFlow header = (TextFlow) table.getColumns().get(column).getGraphic();
            assertEquals(lineHeight(runs(header).get(0)), textBounds(header).getHeight(), 1.0,
                    "header " + column + " on one line");
            for (TableRow<?> row : rows) {
                TextFlow cell = cellFlow(row, column);
                // A code span's line is lower than the bold text's: each cell against its own run.
                assertEquals(lineHeight(runs(cell).get(0)), textBounds(cell).getHeight(), 1.0,
                        () -> "on one line: " + textOf(cell));
            }
        }
        TextFlow locale = cellFlow(rows.get(2), 1);
        assertTrue(textBounds(locale).getHeight() > 2 * lineHeight(runs(locale).get(0)), "the long column wraps");
        double inner = table.getWidth() - table.snappedLeftInset() - table.snappedRightInset();
        double[] widths = columnWidths(table);
        assertEquals(inner, widths[0] + widths[1] + widths[2], 0.5, "the columns fill the table");
        assertFalse(isShown(blockBar((Region) table.getParent())), "and the table fits the view");
        assertAllRowsShown(table, rows);
    }

    @Test
    @DisplayName("a table whose longest words do not fit keeps them whole and scrolls sideways under a bar of its own,"
            + " and the wheel over it scrolls the document")
    void wideTableScrollsSideways() {
        String markdown = "| Code | Type |\n|---|---|\n| `" + LONG_WORD + "` | `tLong` |";
        MarkdownView view = layOut(new MarkdownView("Intro.\n\n" + markdown + "\n\n" + manyParagraphs(30)));
        ScrollPane document = (ScrollPane) view.lookup(".scroll-pane");
        TableView<?> table = table(view);
        Region block = (Region) table.getParent();
        ScrollBar bar = blockBar(block);

        assertTrue(block.getWidth() < SCENE_WIDTH, () -> "block width " + block.getWidth());
        assertTrue(table.getWidth() > block.getWidth(), "the table is wider than its block");
        TextFlow code = cellFlow(shownRows(table).get(0), 0);
        Text run = runs(code).get(0);
        assertEquals(lineHeight(run), textBounds(code).getHeight(), 1.0, "its longest word whole");
        assertTrue(isShown(bar), "a horizontal bar");
        assertEquals(sceneY(table) + table.getHeight(), sceneY(bar), 0.5, "below the table");
        assertEquals(table.getHeight() + bar.getHeight(), block.getHeight(), 0.5, "covering no row");
        assertAllRowsShown(table, shownRows(table));

        double tableX = sceneX(table);
        Event.fireEvent(run, wheel(run, 0, -40, false));
        assertTrue(document.getVvalue() > document.getVmin(), "the wheel scrolls the document");
        assertEquals(tableX, sceneX(table), 0.5, "and leaves the table where it is");
        Event.fireEvent(run, wheel(run, -40, 0, true));
        assertEquals(tableX - 40, sceneX(table), 0.5, "shift plus wheel moves the table left by 40 pixels");
        assertEquals(40, bar.getValue(), 0.5, "and the bar with it");
        Event.fireEvent(run, wheel(run, 4000, 0, true));
        assertEquals(tableX, sceneX(table), 0.5, "no further than its start");
    }

    @Test
    @DisplayName("the mouse wheel over a table scrolls the document, never the table")
    void wheelOverTableScrollsDocument() {
        MarkdownView view = layOut(new MarkdownView("Intro.\n\n| Name | Meaning |\n|---|---|\n| `a` | " + LONG_CELL
                + " |\n\n" + manyParagraphs(30)));
        ScrollPane document = (ScrollPane) view.lookup(".scroll-pane");
        TableView<?> table = table(view);
        Text run = runs(cellFlow(shownRows(table).get(0), 1)).get(0);
        double tableX = sceneX(table);

        Event.fireEvent(run, wheel(run, 0, -40, false));
        double scrolled = document.getVvalue();
        assertTrue(scrolled > document.getVmin(), "the document scrolls down");
        Event.fireEvent(run, wheel(run, -30, -20, false));
        assertTrue(document.getVvalue() > scrolled, "a touchpad's diagonal swipe scrolls it too");
        assertEquals(tableX, sceneX(table), 0.5, "a table that fits does not move sideways");
        Text header = runs((TextFlow) table.getColumns().get(0).getGraphic()).get(0);
        Event.fireEvent(header, wheel(header, 0, 80, false));
        assertTrue(document.getVvalue() < scrolled, "over the header too, and up again");
        assertAllRowsShown(table, shownRows(table));
    }

    @Test
    @DisplayName("a table in a list item or a quote is a table there, and a table of a header alone shows its header")
    void tablesInContainers() {
        MarkdownView view = layOut(new MarkdownView("- Patterns:\n\n  | P | On |\n  |---|---|\n  | `dd` | `31` |\n"
                + "- | first |\n  |---|\n\n> | quoted |\n> |---|\n> | `q` |"));

        List<TableView<?>> tables = tables(view);
        assertEquals(3, tables.size());
        List<Node> items = itemRows(view.lookup(".markdown-bullet-list"));
        TableView<?> patterns = tables.get(0);
        assertSame(items.get(0), patterns.getParent().getParent(), "in the first item");
        assertTrue(sceneX(patterns) >= sceneX(textFlows(items.get(0), ".markdown-paragraph").get(0)) - 0.5,
                "aligned with the item's text");
        assertEquals(List.of("P", "On"), columnTexts(patterns));
        assertEquals(List.of("dd", "31"), cellTexts(shownRows(patterns).get(0)));
        assertAllRowsShown(patterns, shownRows(patterns));
        TableView<?> first = tables.get(1);
        assertSame(items.get(1), first.getParent().getParent(), "in the second item");
        assertEquals(List.of("first"), columnTexts(first));
        Label marker = (Label) items.get(1).lookup(".markdown-list-marker");
        Text firstHeader = runs((TextFlow) first.getColumns().get(0).getGraphic()).get(0);
        assertEquals(sceneBaseline(firstHeader), sceneBaseline(marker), 0.5, "the marker on the header's line");
        assertEquals(0, first.getItems().size());
        assertAllRowsShown(first, List.of());
        assertTrue(first.lookupAll(".placeholder .label").isEmpty(), "no placeholder text below the header");
        TableView<?> quoted = tables.get(2);
        assertTrue(quoted.getParent().getParent().getStyleClass().contains("markdown-quote"));
        Text code = runs(cellFlow(shownRows(quoted).get(0), 0)).get(0);
        assertEquals("q", code.getText());
        assertEquals(themeColor(view, "-fx-code-text-color"), code.getFill(), "quoted code keeps its colour");
        assertEquals(themeColor(view, "-fx-text-secondary-color"),
                runs((TextFlow) quoted.getColumns().get(0).getGraphic()).get(0).getFill(), "quoted text the quote's");
    }

    @Test
    @DisplayName("a table takes the document's 13px in its header and cells, without zebra stripes")
    void tableLook() {
        MarkdownView view = layOut(new MarkdownView("| Name | Type |\n|---|---|\n| a | b |\n| c | d |\n| e | f |"));

        TableView<?> table = table(view);
        List<TableRow<?>> rows = shownRows(table);
        assertEquals(3, rows.size());
        for (TableRow<?> row : rows) {
            assertEquals(themeColor(view, "-fx-surface-color"), row.getBackground().getFills().get(0).getFill(),
                    "row " + row.getIndex());
            Text run = runs(cellFlow(row, 0)).get(0);
            assertEquals(13, run.getFont().getSize(), 0.01);
            assertEquals(themeColor(view, "-fx-text-color"), run.getFill());
        }
        Text header = runs((TextFlow) table.getColumns().get(0).getGraphic()).get(0);
        assertEquals(13, header.getFont().getSize(), 0.01);
        assertTrue(isBold(header.getFont()), () -> "the theme's bold header: " + header.getFont());
    }

    @Test
    @DisplayName("a click on a header sorts the rows by the column's visible text in natural order, the next"
            + " descending, the third back to the source order, with the theme's sort arrow")
    void headerClickSorts() {
        MarkdownView view = layOut(new MarkdownView("| Name | Rows |\n|---|--:|\n| `item10` | 3 |\n| **Item2** | 20 |\n"
                + "| item1 | 100 |\n| *item02* | 7 |"));
        TableView<?> table = table(view);
        TableColumn<?, ?> name = table.getColumns().get(0);

        clickHeader(table, 0, false);
        assertEquals(List.of("item1", "Item2", "item02", "item10"), shownColumn(table, 0),
                "ascending; Item2 and item02 are equal but for case and a zero, the text decides");
        assertEquals(List.of(name), table.getSortOrder());
        Region arrow = sortArrow(table, 0);
        assertNotNull(arrow, "the sort arrow shows");
        assertEquals(themeColor(view, "-fx-text-color"), arrow.getBackground().getFills().get(0).getFill());
        assertTrue(sceneBounds(name.getGraphic()).getMaxX() <= sceneBounds(arrow).getMinX() + 0.5,
                () -> "the header's text flow ends left of the arrow: " + sceneBounds(name.getGraphic()));
        clickHeader(table, 0, false);
        assertEquals(List.of("item10", "item02", "Item2", "item1"), shownColumn(table, 0), "descending");
        clickHeader(table, 0, false);
        assertEquals(List.of("item10", "Item2", "item1", "item02"), shownColumn(table, 0), "the source order");
        assertTrue(table.getSortOrder().isEmpty());
        assertNull(sortArrow(table, 0), "no sort arrow");
        clickHeader(table, 1, false);
        assertEquals(List.of("3", "7", "20", "100"), shownColumn(table, 1), "numbers by value");
        assertAllRowsShown(table, shownRows(table));
    }

    @Test
    @DisplayName("shift plus a click on another header sorts by that column too, rows equal in both in source order")
    void shiftClickSortsByTwoColumns() {
        MarkdownView view = layOut(new MarkdownView("| Group | Name |\n|---|---|\n| b | item2 |\n| A | item10 |\n"
                + "| b | item1 |\n| a | Item1 |\n| a | item10 |"));
        TableView<?> table = table(view);

        clickHeader(table, 0, false);
        assertEquals(List.of("A", "a", "a", "b", "b"), shownColumn(table, 0));
        assertEquals(List.of("item10", "Item1", "item10", "item2", "item1"), shownColumn(table, 1),
                "rows of one group in their source order");
        clickHeader(table, 1, true);
        assertEquals(List.of(table.getColumns().get(0), table.getColumns().get(1)), table.getSortOrder());
        assertEquals(List.of("A", "a", "a", "b", "b"), shownColumn(table, 0));
        assertEquals(List.of("item10", "Item1", "item10", "item1", "item2"), shownColumn(table, 1),
                "each group by name");
        assertNotNull(sortArrow(table, 0));
        assertNotNull(sortArrow(table, 1));
        clickHeader(table, 1, true);
        assertEquals(List.of("item10", "item10", "Item1", "item2", "item1"), shownColumn(table, 1), "names descending");
        clickHeader(table, 1, false);
        assertEquals(List.of(table.getColumns().get(1)), table.getSortOrder(), "a plain click sorts by one column");
        assertEquals(List.of("Item1", "item1", "item2", "item10", "item10"), shownColumn(table, 1));
    }

    @Test
    @DisplayName("the header's menu button sits at its right end in the theme's look, beside the last header's text,"
            + " and a column hidden through it leaves the others the table's width")
    void menuButtonHidesColumns() {
        MarkdownView view = layOut(new MarkdownView("| Key | Type | A long last header that wraps in its column |\n"
                + "|---|---|---|\n| `k` | **INT64** | " + LONG_CELL + " |\n| `j` | STRING | short |"));
        TableView<?> table = table(view);

        assertTrue(table.isTableMenuButtonVisible());
        Region button = (Region) table.lookup(".show-hide-columns-button");
        assertTrue(isShown(button), "the menu button shows");
        Bounds inside = sceneBounds(table);
        assertEquals(inside.getMaxX() - table.snappedRightInset(), sceneBounds(button).getMaxX(), 0.5,
                "at the header's right end");
        assertEquals(themeColor(view, "-fx-button-color"), button.getBackground().getFills().get(0).getFill());
        Region image = (Region) button.lookup(".show-hide-column-image");
        assertEquals(themeColor(view, "-fx-text-secondary-color"), image.getBackground().getFills().get(0).getFill());
        Node lastHeader = table.getColumns().get(2).getGraphic();
        assertTrue(sceneBounds(lastHeader).getMaxX() <= sceneBounds(button).getMinX() + 0.5,
                () -> "the last header's text flow ends left of the button: " + sceneBounds(lastHeader));
        assertEquals(List.of("Key", "Type", "A long last header that wraps in its column"), columnTexts(table),
                "the menu lists the columns by these texts");

        table.getColumns().get(1).setVisible(false);
        view.getScene().getRoot().applyCss();
        view.getScene().getRoot().layout();

        assertFalse(isTreeVisible(columnHeader(table, 1)), "its header is gone");
        for (TableRow<?> row : shownRows(table)) {
            assertEquals(2, shownCells(row).size(), "two cells per row");
        }
        assertEquals(List.of("k", "j"), shownColumn(table, 0));
        double inner = table.getWidth() - table.snappedLeftInset() - table.snappedRightInset();
        assertEquals(inner, table.getColumns().get(0).getWidth() + table.getColumns().get(2).getWidth(), 0.5,
                "the other columns fill the table");
        assertAllRowsShown(table, shownRows(table));

        table.getColumns().get(1).setVisible(true);
        view.getScene().getRoot().applyCss();
        view.getScene().getRoot().layout();

        assertEquals(List.of("INT64", "STRING"), shownColumn(table, 1), "shown again");
        assertEquals(inner, table.getColumns().get(0).getWidth() + table.getColumns().get(1).getWidth()
                + table.getColumns().get(2).getWidth(), 0.5);
        assertAllRowsShown(table, shownRows(table));
    }

    @Test
    @DisplayName("a column dragged by its edge keeps its new width, the columns right of it give or take the difference"
            + " down to their longest words, and the widths keep their shares through a sort and a narrower view")
    void resizedColumnKeepsItsWidth() {
        MarkdownView view = layOut(new MarkdownView("| Key | Type | Meaning |\n|---|---|---|\n| `k` | `INT64` | "
                + LONG_CELL + " |\n| `j` | `STRING` | short |"));
        TableView<?> table = table(view);
        double inner = table.getWidth() - table.snappedLeftInset() - table.snappedRightInset();
        double[] fitted = columnWidths(table);

        assertTrue(resizeColumn(table, 1, 30), "the header's resize handle drags the edge right of Type");
        relayOut(view);
        assertArrayEquals(new double[] {fitted[0], fitted[1] + 30, fitted[2] - 30}, columnWidths(table), 0.5,
                "Type takes 30 pixels from Meaning");
        double[] wider = columnWidths(table);
        assertTrue(resizeColumn(table, 1, -10));
        relayOut(view);
        assertArrayEquals(new double[] {wider[0], wider[1] - 10, wider[2] + 10}, columnWidths(table), 0.5,
                "and gives 10 back");
        double[] dragged = columnWidths(table);

        clickHeader(table, 1, false);
        assertArrayEquals(dragged, columnWidths(table), 0.5, "a sort keeps the widths");
        assertTrue(sceneBounds(table.getColumns().get(1).getGraphic()).getMaxX()
                <= sceneBounds(sortArrow(table, 1)).getMinX() + 0.5, "the sorted header's text left of its arrow");
        clickHeader(table, 0, false);
        double[] sortedByKey = columnWidths(table);
        TextFlow key = (TextFlow) table.getColumns().get(0).getGraphic();
        assertEquals(lineHeight(runs(key).get(0)), textBounds(key).getHeight(), 1.0,
                "Key, at its longest word, widens by the arrow: the word stays whole");
        assertTrue(sortedByKey[0] > dragged[0], () -> "Key " + dragged[0] + " -> " + sortedByKey[0]);
        assertTrue(sceneBounds(key).getMaxX() <= sceneBounds(sortArrow(table, 0)).getMinX() + 0.5);
        assertEquals(sortedByKey[1] / sortedByKey[2], dragged[1] / dragged[2], 0.02, "the others in their shares");
        clickHeader(table, 0, false);
        clickHeader(table, 0, false);
        assertArrayEquals(dragged, columnWidths(table), 0.5, "unsorted, the reader's widths again");
        view.setMaxWidth(360);
        relayOut(view);
        double narrow = table.getWidth() - table.snappedLeftInset() - table.snappedRightInset();
        double[] shares = columnWidths(table);
        assertEquals(narrow, shares[0] + shares[1] + shares[2], 0.5, "a narrower view: the columns fill it");
        assertEquals(dragged[0], shares[0], 0.5, "Key, at its longest word already, keeps it");
        assertEquals(dragged[1] / dragged[2], shares[1] / shares[2], 0.02, "Type and Meaning in their shares");
        assertTrue(shares[2] < dragged[2] - 25, "Meaning gives most");
        view.setMaxWidth(Double.MAX_VALUE);
        relayOut(view);
        assertArrayEquals(dragged, columnWidths(table), 1.0, "back at the full width");
        assertAllRowsShown(table, shownRows(table));

        assertTrue(resizeColumn(table, 0, 1000), "Key dragged far right");
        relayOut(view);
        double[] widest = columnWidths(table);
        // Meaning at its longest word wraps over many lines: the document outgrows the view, whose vertical bar
        // makes the table narrower.
        double now = table.getWidth() - table.snappedLeftInset() - table.snappedRightInset();
        assertEquals(now, widest[0] + widest[1] + widest[2], 0.5, "still exactly the table's width");
        for (TableRow<?> row : shownRows(table)) {
            for (int column = 1; column < 3; column++) {
                TextFlow flow = cellFlow(row, column);
                assertTrue(textBounds(flow).getWidth() <= flow.getWidth() + 0.5, "every word whole, column " + column);
            }
            Text type = runs(cellFlow(row, 1)).get(0);
            assertEquals(lineHeight(type), textBounds(cellFlow(row, 1)).getHeight(), 1.0, "Type on one line");
        }
        assertTrue(widest[1] < dragged[1] && widest[2] < dragged[2], "Type and Meaning gave all they could");
        assertAllRowsShown(table, shownRows(table));
        assertFalse(resizeColumn(table, 2, 50), "the last column's right edge is the table's");
    }

    @Test
    @DisplayName("a header dragged onto another column's header moves its column there, with its cells, and the columns"
            + " still fill the table")
    void draggedHeaderReordersColumns() {
        assumeFalse(Platform.isSupported(ConditionalFeature.INPUT_TOUCH),
                "JavaFX reorders columns with the mouse only where there is no touch input");
        MarkdownView view = layOut(new MarkdownView("| Key | Type | Meaning |\n|---|---|---|\n| `k` | `INT64` | "
                + LONG_CELL + " |\n| `j` | `STRING` | short |"));
        TableView<?> table = table(view);
        Node key = runs((TextFlow) table.getColumns().get(0).getGraphic()).get(0);
        Bounds keyBounds = sceneBounds(key);
        Bounds meaning = sceneBounds(columnHeader(table, 2));

        drag(key, new Point2D(keyBounds.getCenterX(), keyBounds.getCenterY()),
                new Point2D(meaning.getMaxX() - 10, meaning.getCenterY()));
        relayOut(view);

        assertEquals(List.of("Type", "Meaning", "Key"), columnTexts(table));
        List<TableRow<?>> rows = shownRows(table);
        List<String> firstRow = new ArrayList<>();
        for (TableCell<?, ?> cell : shownCells(rows.get(0))) {
            firstRow.add(textOf((TextFlow) cell.getGraphic()));
        }
        assertEquals(List.of("INT64", LONG_CELL, "k"), firstRow, "the cells left to right");
        assertTrue(table.getSortOrder().isEmpty(), "a drag does not sort");
        double inner = table.getWidth() - table.snappedLeftInset() - table.snappedRightInset();
        double[] widths = columnWidths(table);
        assertEquals(inner, widths[0] + widths[1] + widths[2], 0.5);
        Region button = (Region) table.lookup(".show-hide-columns-button");
        assertTrue(sceneBounds(table.getColumns().get(2).getGraphic()).getMaxX()
                <= sceneBounds(button).getMinX() + 0.5, "the new last header's text left of the menu button");
        assertAllRowsShown(table, rows);
    }

    @Test
    @DisplayName("every new text starts its tables fresh: in source order, unsorted, every column shown where the"
            + " author put it, at the widths the table fits them to")
    void newTextStartsTablesFresh() {
        String markdown = "| Key | Type | Meaning |\n|---|---|---|\n| `k` | `INT64` | " + LONG_CELL + " |\n"
                + "| `j` | `STRING` | short |";
        MarkdownView view = layOut(new MarkdownView(markdown));
        TableView<?> table = table(view);
        double[] fitted = columnWidths(table);
        clickHeader(table, 0, false);
        assertTrue(resizeColumn(table, 1, 25));
        moveColumn(table, 0, 1);
        table.getColumns().get(2).setVisible(false);
        relayOut(view);
        assertEquals(List.of("j", "k"), shownColumn(table, 1), "sorted by Key, now the second column");

        view.setMarkdown("Another node's documentation.");
        view.setMarkdown(markdown);
        relayOut(view);

        TableView<?> fresh = table(view);
        assertTrue(fresh.getSortOrder().isEmpty(), "unsorted");
        assertEquals(List.of("Key", "Type", "Meaning"), columnTexts(fresh), "in the author's order");
        for (TableColumn<?, ?> column : fresh.getColumns()) {
            assertTrue(column.isVisible(), column.getText() + " shown");
        }
        assertArrayEquals(fitted, columnWidths(fresh), 0.5, "fitted widths");
        assertEquals(List.of("k", "j"), shownColumn(fresh, 0), "the rows in source order");
        assertAllRowsShown(fresh, shownRows(fresh));
    }

    @Test
    @DisplayName("a click on a cell selects that cell alone, in the theme's selection colours with the focus frame, its"
            + " code in the selection's text colour too")
    void clickSelectsOneCell() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE));
        TableView<?> table = table(view);
        assertTrue(table.getSelectionModel().isCellSelectionEnabled(), "cell selection");
        assertEquals(SelectionMode.MULTIPLE, table.getSelectionModel().getSelectionMode());
        assertEquals("...\n...\n...", selection(table), "nothing selected at first");

        clickCell(table, 1, 0, false, false);
        relayOut(view);

        assertEquals("...\n#..\n...", selection(table));
        assertSame(table, view.getScene().getFocusOwner(), "the table takes the focus");
        assertEquals(1, table.getFocusModel().getFocusedCell().getRow(), "the focused cell");
        assertSame(table.getColumns().get(0), table.getFocusModel().getFocusedCell().getTableColumn());
        // The theme frames the focused cell of a focused table in a cell-selection table; a node is focused only in a
        // focused window, which a test without one has not, so the frame itself does not show here.
        assertTrue(cell(table, 1, 0).getPseudoClassStates().contains(PseudoClass.getPseudoClass("focused")));
        assertTrue(table.getPseudoClassStates().contains(PseudoClass.getPseudoClass("cell-selection")));
        assertEquals(themeColor(view, "-fx-selection-color"), fills(cell(table, 1, 0)).getLast(),
                "the selection colour");
        Text code = runs(cellFlow(shownRows(table).get(1), 0)).get(0);
        assertEquals(themeColor(view, "-fx-selection-text-color"), code.getFill(),
                "code in the selection's text colour");

        clickCell(table, 2, 1, false, false);
        relayOut(view);

        assertEquals("...\n...\n.#.", selection(table), "a click elsewhere moves the selection");
        assertFalse(fills(cell(table, 1, 0)).contains(themeColor(view, "-fx-selection-color")),
                () -> "deselected: " + fills(cell(table, 1, 0)));
        assertEquals(themeColor(view, "-fx-code-text-color"), code.getFill(), "the code colour again");
        Text type = runs(cellFlow(shownRows(table).get(2), 1)).get(0);
        assertEquals(themeColor(view, "-fx-selection-text-color"), type.getFill());
    }

    @Test
    @DisplayName("a click on a link in a cell follows the link and selects the cell")
    void linkInCellFollowsAndSelects() {
        MarkdownView view = layOut(new MarkdownView("| Name | Source |\n|---|---|\n| iris | [UCI](https://uci.edu) |"));
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        TableView<?> table = table(view);

        clickCell(table, 0, 1, false, false);

        assertEquals(List.of("https://uci.edu"), followed);
        assertEquals(".#", selection(table));
    }

    @Test
    @DisplayName("a drag over cells selects the rectangle from the pressed cell to the cell under the pointer, which"
            + " follows the pointer back and stops at the table's edges")
    void dragSelectsRectangle() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE));
        TableView<?> table = table(view);
        Node pressed = cellTarget(table, 0, 0);

        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_PRESSED, cellPoint(table, 0, 0), 1));
        assertEquals("#..\n...\n...", selection(table), "the press selects the pressed cell");
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_DRAGGED, cellPoint(table, 1, 1), 1));
        assertEquals("##.\n##.\n...", selection(table));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_DRAGGED, cellPoint(table, 2, 2), 1));
        assertEquals("###\n###\n###", selection(table));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_DRAGGED, cellPoint(table, 2, 0), 1));
        assertEquals("#..\n#..\n#..", selection(table), "back over the cells: the rectangle shrinks");
        Bounds tableBounds = sceneBounds(table);
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_DRAGGED,
                new Point2D(tableBounds.getMaxX() + 40, tableBounds.getMaxY() + 40), 1));
        assertEquals("###\n###\n###", selection(table), "beyond the bottom right corner: up to the last cell");
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_RELEASED,
                new Point2D(tableBounds.getMaxX() + 40, tableBounds.getMaxY() + 40), 1));
        assertSame(table, view.getScene().getFocusOwner(), "the table has the focus");

        Node middle = cellTarget(table, 1, 1);
        Event.fireEvent(middle, mouse(MouseEvent.MOUSE_PRESSED, cellPoint(table, 1, 1), 1));
        Event.fireEvent(middle, mouse(MouseEvent.MOUSE_DRAGGED,
                new Point2D(tableBounds.getMinX() - 40, cellPoint(table, 0, 0).getY()), 1));
        Event.fireEvent(middle, mouse(MouseEvent.MOUSE_RELEASED,
                new Point2D(tableBounds.getMinX() - 40, cellPoint(table, 0, 0).getY()), 1));
        assertEquals("##.\n##.\n...", selection(table), "up and left of the table: up to the first column");
        ScrollPane document = (ScrollPane) view.lookup(".scroll-pane");
        assertEquals(document.getVmin(), document.getVvalue(), "the document does not scroll");
    }

    @Test
    @DisplayName("shift plus a click selects the rectangle from the anchor, the cell clicked last without shift; the"
            + " shortcut key plus a click adds or removes one cell")
    void shiftAndShortcutClicks() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE));
        TableView<?> table = table(view);

        clickCell(table, 0, 1, false, false);
        clickCell(table, 2, 2, true, false);
        assertEquals(".##\n.##\n.##", selection(table));
        clickCell(table, 1, 0, true, false);
        assertEquals("##.\n##.\n...", selection(table), "from the same anchor");
        relayOut(view);
        assertEquals(List.of(themeColor(view, "-fx-selection-color")), fills(cell(table, 0, 0)),
                "a selected cell without the focus in the selection colour");

        clickCell(table, 2, 2, false, true);
        assertEquals("##.\n##.\n..#", selection(table), "the shortcut key adds a cell");
        clickCell(table, 0, 1, false, true);
        assertEquals("#..\n##.\n..#", selection(table), "and removes a selected one");
        clickCell(table, 2, 1, true, false);
        assertEquals(".#.\n.#.\n.#.", selection(table), "a rectangle from the cell clicked last, also when removed");
        clickCell(table, 0, 0, false, false);
        assertEquals("#..\n...\n...", selection(table), "a plain click selects one cell again");

        Node pressed = cellTarget(table, 1, 1);
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_PRESSED, cellPoint(table, 1, 1), 1, true, false));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_DRAGGED, cellPoint(table, 2, 1), 1, true, false));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_RELEASED, cellPoint(table, 2, 1), 1, true, false));
        assertEquals("##.\n##.\n##.", selection(table), "a drag after shift plus a press moves the far corner");
    }

    @Test
    @DisplayName("shift plus an arrow key moves the far corner of the rectangle from the anchor by a cell, across the"
            + " anchor and up to the table's edges")
    void shiftArrowKeysExtend() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE));
        TableView<?> table = table(view);

        clickCell(table, 1, 1, false, false);
        key(view, KeyCode.DOWN, true, false);
        assertEquals("...\n.#.\n.#.", selection(table));
        key(view, KeyCode.RIGHT, true, false);
        assertEquals("...\n.##\n.##", selection(table));
        key(view, KeyCode.RIGHT, true, false);
        assertEquals("...\n.##\n.##", selection(table), "the last column is the edge");
        key(view, KeyCode.UP, true, false);
        key(view, KeyCode.UP, true, false);
        assertEquals(".##\n.##\n...", selection(table), "past the anchor the rectangle turns upwards");
        key(view, KeyCode.LEFT, true, false);
        key(view, KeyCode.LEFT, true, false);
        key(view, KeyCode.LEFT, true, false);
        assertEquals("##.\n##.\n...", selection(table));
        assertEquals(0, table.getFocusModel().getFocusedCell().getRow(), "the focus on the far corner");
        assertSame(table.getColumns().get(0), table.getFocusModel().getFocusedCell().getTableColumn());

        key(view, KeyCode.DOWN, false, false);
        assertEquals("...\n#..\n...", selection(table), "a plain arrow key moves to one cell");
        key(view, KeyCode.DOWN, true, false);
        key(view, KeyCode.RIGHT, true, false);
        assertEquals("...\n##.\n##.", selection(table), "from there");
    }

    @Test
    @DisplayName("shift plus space extends the selection to the whole rows it touches, which shift plus an arrow key"
            + " then extends by whole rows; the shortcut key plus A selects every cell")
    void wholeRowsAndAllCells() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE + "\n| `d` | BOOLEAN | fourth |"));
        TableView<?> table = table(view);

        clickCell(table, 1, 1, false, false);
        key(view, KeyCode.SPACE, true, false);
        assertEquals("...\n###\n...\n...", selection(table), "one row");
        key(view, KeyCode.DOWN, true, false);
        assertEquals("...\n###\n###\n...", selection(table), "the next row whole");

        clickCell(table, 0, 0, false, false);
        clickCell(table, 2, 2, false, true);
        key(view, KeyCode.SPACE, true, false);
        assertEquals("###\n...\n###\n...", selection(table), "the rows of single cells, not the ones between");

        clickCell(table, 3, 2, false, false);
        key(view, KeyCode.UP, true, false);
        key(view, KeyCode.UP, true, false);
        key(view, KeyCode.SPACE, true, false);
        assertEquals("...\n###\n###\n###", selection(table), "the rows of a rectangle");
        key(view, KeyCode.UP, true, false);
        assertEquals("###\n###\n###\n###", selection(table), "growing on, upwards");

        clickCell(table, 2, 0, false, false);
        key(view, KeyCode.A, false, true);
        assertEquals("###\n###\n###\n###", selection(table), "the shortcut key plus A: every cell");
        table.getColumns().get(1).setVisible(false);
        relayOut(view);
        clickCell(table, 0, 0, false, false);
        key(view, KeyCode.SPACE, true, false);
        assertEquals("#-#\n.-.\n.-.\n.-.", selection(table), "whole rows of the shown columns");
    }

    @Test
    @DisplayName("one selection per view: a selection in a table clears another table's and a code block's, a"
            + " selection in a code block clears the table's, and a new text starts without one")
    void oneSelectionAcrossTablesAndCodeBlocks() {
        String markdown = "```\nfirst();\n```\n\n" + SELECTION_TABLE + "\n\nBetween.\n\n" + SELECTION_TABLE;
        MarkdownView view = layOut(new MarkdownView(markdown));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);
        List<TableView<?>> tables = tables(view);
        TableView<?> first = tables.get(0);
        TableView<?> second = tables.get(1);

        drag(code, caretPoint(code, 0, 0), caretPoint(code, 0, 5));
        clickCell(first, 1, 1, false, false);
        assertTrue(selectionHighlight(block).getElements().isEmpty(), "the code block's selection is gone");
        shortcutAt(block, KeyCode.C);
        assertEquals(List.of(), copied, "nothing left to copy there");

        clickCell(second, 0, 0, false, false);
        assertEquals("...\n...\n...", selection(first), "a selection in another table clears the first");
        key(view, KeyCode.A, false, true);
        assertEquals("...\n...\n...", selection(first), "all cells of the second table only");
        assertEquals("###\n###\n###", selection(second));

        drag(code, caretPoint(code, 0, 0), caretPoint(code, 0, 5));
        assertEquals("...\n...\n...", selection(second), "a selection in the code block clears the table's");
        shortcut(view, KeyCode.C);
        assertEquals(List.of("first"), copied);

        clickCell(first, 2, 2, false, false);
        view.setMarkdown("Another node's documentation.");
        view.setMarkdown(markdown);
        relayOut(view);
        for (TableView<?> table : tables(view)) {
            assertEquals("...\n...\n...", selection(table), "nothing selected in the new text");
        }
        clickCell(tables(view).get(1), 1, 0, false, false);
        Region newBlock = (Region) view.lookup(".markdown-code-block");
        drag(codeText(newBlock), caretPoint(codeText(newBlock), 0, 0), caretPoint(codeText(newBlock), 0, 3));
        assertEquals("...\n...\n...", selection(tables(view).get(1)), "the new text's blocks keep one selection");
    }

    @Test
    @DisplayName("a sort clears the table's selection, ascending, descending and back to the source order alike")
    void sortClearsSelection() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE));
        TableView<?> table = table(view);

        for (int click = 1; click <= 3; click++) {
            clickCell(table, 0, 0, false, false);
            key(view, KeyCode.DOWN, true, false);
            assertEquals("#..\n#..\n...", selection(table));
            clickHeader(table, 1, false);
            assertEquals("...\n...\n...", selection(table), "after header click " + click);
        }
        assertTrue(table.getSortOrder().isEmpty(), "back in the source order");
        clickCell(table, 2, 1, false, false);
        key(view, KeyCode.UP, true, false);
        assertEquals("...\n.#.\n.#.", selection(table), "selecting works on after a sort");
    }

    @Test
    @DisplayName("the shortcut key plus C in a table copies one selected cell's visible text, the Markdown resolved,"
            + " and nothing without a selection")
    void shortcutCopiesOneCell() {
        MarkdownView view = layOut(new MarkdownView("| Name | Note |\n|---|---|\n| `item_1` | **bold** and *it* |\n"
                + "| [UCI](https://uci.edu) | \\*plain\\* |"));
        List<String> copied = recordCopies(view);
        TableView<?> table = table(view);

        clickCell(table, 0, 0, false, false);
        shortcut(view, KeyCode.C);
        clickCell(table, 0, 1, false, false);
        shortcut(view, KeyCode.C);
        clickCell(table, 1, 0, false, false);
        shortcut(view, KeyCode.C);
        clickCell(table, 1, 1, false, false);
        shortcut(view, KeyCode.C);
        assertEquals(List.of("item_1", "bold and it", "UCI", "*plain*"), copied,
                "code without backticks, emphasis without asterisks, a link as its text, escapes resolved");

        clickHeader(table, 1, false);
        assertSame(table, view.getScene().getFocusOwner(), "the table keeps the focus");
        assertEquals("..\n..", selection(table));
        shortcut(view, KeyCode.C);
        assertEquals(4, copied.size(), "nothing copied without a selection");
    }

    @Test
    @DisplayName("several selected cells copy as their bounding rectangle: unselected cells empty, cells joined by"
            + " tabs, rows by line feeds, only the shown columns in their shown order and the rows in their shown"
            + " order")
    void shortcutCopiesRectangle() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE + "\n| `d` | BOOLEAN | fourth |"));
        List<String> copied = recordCopies(view);
        TableView<?> table = table(view);

        clickCell(table, 0, 0, false, false);
        clickCell(table, 2, 2, false, true);
        shortcut(view, KeyCode.C);
        clickCell(table, 1, 1, false, true);
        shortcut(view, KeyCode.C);
        assertEquals(List.of("a\t\t\n\t\t\n\t\tthird", "a\t\t\n\tSTRING\t\n\t\tthird"), copied,
                "a rectangle with holes, no line feed at the end");
        copied.clear();

        clickCell(table, 0, 0, false, false);
        clickCell(table, 1, 2, true, false);
        table.getColumns().get(1).setVisible(false);
        relayOut(view);
        shortcut(view, KeyCode.C);
        assertEquals(List.of("a\tfirst\nb\tsecond"), copied,
                "a hidden column is left out, though its cells stay selected");
        table.getColumns().get(1).setVisible(true);
        relayOut(view);
        clickCell(table, 3, 1, false, false);
        table.getColumns().get(1).setVisible(false);
        relayOut(view);
        shortcut(view, KeyCode.C);
        assertEquals(1, copied.size(), "nothing copied when only a hidden column's cell is selected");
        table.getColumns().get(1).setVisible(true);
        copied.clear();

        moveColumn(table, 0, 2);
        relayOut(view);
        assertEquals(List.of("Type", "Meaning", "Key"), columnTexts(table));
        clickCell(table, 0, 0, false, false);
        clickCell(table, 1, 2, true, false);
        shortcut(view, KeyCode.C);
        clickHeader(table, 1, false);
        assertEquals(List.of("first", "fourth", "second", "third"), shownColumn(table, 1), "sorted by Meaning");
        clickCell(table, 1, 0, false, false);
        clickCell(table, 2, 2, true, false);
        shortcut(view, KeyCode.C);
        assertEquals(List.of("INT64\tfirst\ta\nSTRING\tsecond\tb", "BOOLEAN\tfourth\td\nSTRING\tsecond\tb"), copied,
                "the columns as moved, the rows as sorted");
    }

    @Test
    @DisplayName("a table's context menu, on a cell or a header: Copy and Copy with header, greyed out without a"
            + " selection, Select column and Copy Markdown")
    void tableContextMenu() {
        String markdown = SELECTION_TABLE + "\n| `d` | BOOLEAN | fourth |\n\nAfter the table.";
        MarkdownView view = layOut(new MarkdownView(markdown));
        List<String> copied = recordCopies(view);
        TableView<?> table = table(view);

        List<MenuItem> onCell = contextMenuAt(view, cellTarget(table, 1, 1));
        assertEquals(List.of("Copy", "Copy with header", "Select column", "-", "Copy Markdown"), menuTexts(onCell));
        assertTrue(menuEntry(onCell, "Copy").isDisable(), "nothing selected: Copy greyed out");
        assertTrue(menuEntry(onCell, "Copy with header").isDisable());
        assertFalse(menuEntry(onCell, "Select column").isDisable());
        assertFalse(menuEntry(onCell, "Copy Markdown").isDisable());
        Node header = runs((TextFlow) table.getColumns().get(2).getGraphic()).get(0);
        List<MenuItem> onHeader = contextMenuAt(view, header);
        assertEquals(List.of("Copy", "Copy with header", "Select column", "-", "Copy Markdown"), menuTexts(onHeader));
        assertTrue(menuEntry(onHeader, "Copy").isDisable());

        clickCell(table, 0, 1, false, false);
        clickCell(table, 1, 2, true, false);
        List<MenuItem> selected = contextMenuAt(view, cellTarget(table, 3, 0));
        assertFalse(menuEntry(selected, "Copy").isDisable(), "a selection to copy");
        assertFalse(menuEntry(selected, "Copy with header").isDisable());
        menuEntry(selected, "Copy").fire();
        menuEntry(selected, "Copy with header").fire();
        menuEntry(selected, "Copy Markdown").fire();
        clickCell(table, 2, 0, false, false);
        menuEntry(contextMenuAt(view, header), "Copy with header").fire();
        assertEquals(List.of("INT64\tfirst\nSTRING\tsecond", "Type\tMeaning\nINT64\tfirst\nSTRING\tsecond", markdown,
                "Key\nc"), copied, "the titles of the copied rectangle's columns first");

        List<MenuItem> after = contextMenuAt(view, view.lookup(".markdown-paragraph"));
        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(after), "on text, the text selection's entries");
    }

    @Test
    @DisplayName("a code block's context menu: Copy, greyed out without a selection, copies the selected characters;"
            + " a source block's alike; Copy Markdown below")
    void codeBlockContextMenu() {
        String markdown = "```\nint total = price * count;\n```\n\n<div>amount INT64</div>\n\nAfter.";
        MarkdownView view = layOut(new MarkdownView(markdown));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        Text code = codeText(block);

        List<MenuItem> onCode = contextMenuAt(view, code);
        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(onCode));
        assertTrue(menuEntry(onCode, "Copy").isDisable(), "nothing selected: Copy greyed out");
        menuEntry(onCode, "Copy").fire();
        assertEquals(List.of(), copied);

        drag(code, caretPoint(code, 0, 4), caretPoint(code, 0, 9));
        List<MenuItem> selected = contextMenuAt(view, code);
        assertFalse(menuEntry(selected, "Copy").isDisable());
        menuEntry(selected, "Copy").fire();
        List<MenuItem> onButton = contextMenuAt(view, copyButton(block));
        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(onButton), "the copy button is in the block");
        menuEntry(onButton, "Copy Markdown").fire();

        Node html = view.lookup(".markdown-source-block");
        assertTrue(menuEntry(contextMenuAt(view, sourceText(html)), "Copy").isDisable(),
                "the code block's selection is not the source block's");
        Text htmlText = sourceText(html);
        drag(htmlText, caretPoint(htmlText, 0, 5), caretPoint(htmlText, 0, 11));
        assertTrue(menuEntry(contextMenuAt(view, code), "Copy").isDisable(), "one selection: the code block's is gone");
        menuEntry(contextMenuAt(view, html), "Copy").fire();
        assertEquals(List.of("total", markdown, "amount"), copied);

        assertEquals(List.of("Copy", "-", "Copy Markdown"),
                menuTexts(contextMenuAt(view, view.lookup(".markdown-paragraph"))), "on text, the text selection's entries");
    }

    @Test
    @DisplayName("only a primary click on a header sorts: a right click, which opens the header's menu, and a middle"
            + " click leave the order and the selection alone")
    void onlyPrimaryClickOnHeaderSorts() {
        MarkdownView view = layOut(new MarkdownView("| Name |\n|---|\n| b |\n| a |"));
        TableView<?> table = table(view);
        clickCell(table, 0, 0, false, false);
        Node header = runs((TextFlow) table.getColumns().get(0).getGraphic()).get(0);

        // As on Linux and macOS, where the press is the popup trigger and the release of the right button is none.
        clickWith(header, MouseButton.SECONDARY);
        clickWith(header, MouseButton.MIDDLE);
        relayOut(view);

        assertTrue(table.getSortOrder().isEmpty(), "not sorted");
        assertEquals(List.of("b", "a"), shownColumn(table, 0));
        assertEquals("#\n.", selection(table), "the selection stays for the menu's Copy");
        assertFalse(menuEntry(contextMenuAt(view, header), "Copy").isDisable());
        clickHeader(table, 0, false);
        assertEquals(List.of("a", "b"), shownColumn(table, 0), "a primary click sorts");
    }

    @Test
    @DisplayName("the labels Copy, Copy with header and Select column are English by default, and a change applies to"
            + " the menus shown later")
    void contextMenuLabels() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE + "\n\n```\na();\n```"));
        TableView<?> table = table(view);

        assertEquals("Copy", view.getCopyText());
        assertEquals("Copy with header", view.getCopyWithHeaderText());
        assertEquals("Select column", view.getSelectColumnText());

        view.setCopyText("Kopieren");
        view.copyWithHeaderTextProperty().set("Mit Titelzeile kopieren");
        view.setSelectColumnText("Spalte markieren");
        view.setCopyMarkdownText("Markdown kopieren");

        assertEquals("Kopieren", view.copyTextProperty().get());
        assertEquals("Mit Titelzeile kopieren", view.getCopyWithHeaderText());
        assertEquals("Spalte markieren", view.selectColumnTextProperty().get());
        assertEquals(List.of("Kopieren", "Mit Titelzeile kopieren", "Spalte markieren", "-", "Markdown kopieren"),
                menuTexts(contextMenuAt(view, cellTarget(table, 0, 0))));
        assertEquals(List.of("Kopieren", "-", "Markdown kopieren"),
                menuTexts(contextMenuAt(view, codeText(view.lookup(".markdown-code-block")))));
        view.setMarkdown(SELECTION_TABLE);
        relayOut(view);
        assertEquals(List.of("Kopieren", "Mit Titelzeile kopieren", "Spalte markieren", "-", "Markdown kopieren"),
                menuTexts(contextMenuAt(view, cellTarget(table(view), 1, 1))), "a new text's tables too");
    }

    @Test
    @DisplayName("Select column selects every cell of the column the menu was opened on, from a cell, from a header or,"
            + " opened from the keyboard, the focused cell's column")
    void selectColumnFromContextMenu() {
        MarkdownView view = layOut(new MarkdownView(SELECTION_TABLE + "\n| `d` | BOOLEAN | fourth |"));
        List<String> copied = recordCopies(view);
        TableView<?> table = table(view);

        clickCell(table, 3, 0, false, false);
        menuEntry(contextMenuAt(view, cellTarget(table, 1, 2)), "Select column").fire();
        assertEquals("..#\n..#\n..#\n..#", selection(table), "the cell's column");
        shortcut(view, KeyCode.C);
        Node keyHeader = runs((TextFlow) table.getColumns().get(0).getGraphic()).get(0);
        menuEntry(contextMenuAt(view, keyHeader), "Select column").fire();
        assertEquals("#..\n#..\n#..\n#..", selection(table), "the header's column");
        menuEntry(contextMenuAt(view, keyHeader), "Copy with header").fire();
        clickCell(table, 2, 0, true, false);
        assertEquals("#..\n#..\n#..\n...", selection(table), "the anchor is the column's first cell");

        clickCell(table, 1, 1, false, false);
        menuEntry(contextMenuAt(view, table), "Select column").fire();
        assertEquals(".#.\n.#.\n.#.\n.#.", selection(table),
                "a request at the focused table: the focused cell's column");
        assertEquals(List.of("first\nsecond\nthird\nfourth", "Key\na\nb\nc\nd"), copied);

        moveColumn(table, 2, 0);
        relayOut(view);
        clickHeader(table, 0, false);
        menuEntry(contextMenuAt(view, cellTarget(table, 0, 0)), "Select column").fire();
        assertEquals("#..\n#..\n#..\n#..", selection(table), "a moved column in a sorted table");
        shortcut(view, KeyCode.C);
        assertEquals("first\nfourth\nsecond\nthird", copied.getLast(), "its cells in the sorted order");
    }

    @Test
    @DisplayName("a link is a text run with the link class, a hand cursor and its address as tooltip, bold or code combined")
    void linkRuns() {
        MarkdownView view = layOut(new MarkdownView(
                "Read [the **docs** and `api`](https://example.com/docs \"The docs\") or https://example.com/x."));

        List<Text> runs = runs(textFlows(view, ".markdown-paragraph").get(0));
        assertEquals(8, runs.size(), () -> "runs: " + runs);
        assertRun(runs.get(0), "Read ");
        assertRun(runs.get(1), "the ", "markdown-link");
        assertRun(runs.get(2), "docs", "markdown-link", "markdown-strong");
        assertRun(runs.get(3), " and ", "markdown-link");
        assertRun(runs.get(4), "api", "markdown-link", "markdown-code");
        assertRun(runs.get(5), " or ");
        assertRun(runs.get(6), "https://example.com/x", "markdown-link");
        assertRun(runs.get(7), ".");
        for (int i : new int[] {1, 2, 3, 4}) {
            assertEquals(Cursor.HAND, runs.get(i).getCursor());
            assertEquals("https://example.com/docs", tooltip(runs.get(i)).getText(), "the address, not the title");
        }
        assertEquals("https://example.com/x", tooltip(runs.get(6)).getText());
        assertNull(tooltip(runs.get(0)));
        assertNull(tooltip(runs.get(5)));
        assertNotEquals(Cursor.HAND, runs.get(0).getCursor());
    }

    @Test
    @DisplayName("a click on a link passes its address as written to the handler, which may be set at any time")
    void linkClickReachesHandler() {
        MarkdownView view = layOut(new MarkdownView(
                "- [one](docs/One.md) and [two](<two words.md>)\n- [esc](a\\(b\\)) and <https://a.b/c\\_d>\n"
                        + "- https://x.y/__init__.py and https://a.b/c\\_e"));
        List<String> addresses = new ArrayList<>();
        Consumer<String> handler = address -> addresses.add(address);
        view.setOnLinkAction(handler);

        assertSame(handler, view.getOnLinkAction());
        assertSame(handler, view.onLinkActionProperty().get());
        for (Text run : linkRuns(view)) {
            click(run, MouseButton.PRIMARY, 1);
        }
        assertEquals(List.of("docs/One.md", "two words.md", "a(b)", "https://a.b/c\\_d", "https://x.y/__init__.py",
                "https://a.b/c\\_e"), addresses);
    }

    @Test
    @DisplayName("only a single primary click follows a link: no right click, no second click of a double click")
    void onlySinglePrimaryClicksFollowLinks() {
        MarkdownView view = layOut(new MarkdownView("A [link](/a)."));
        List<String> addresses = new ArrayList<>();
        view.setOnLinkAction(address -> addresses.add(address));
        Text link = linkRuns(view).get(0);

        click(link, MouseButton.SECONDARY, 1);
        click(link, MouseButton.PRIMARY, 1);
        click(link, MouseButton.PRIMARY, 2);

        assertEquals(List.of("/a"), addresses);
    }

    @Test
    @DisplayName("without a handler, a click on a link does nothing")
    void linkClickWithoutHandler() {
        MarkdownView view = layOut(new MarkdownView("A [link](/a) and https://a.b."));
        assertNull(view.getOnLinkAction());
        List<Text> links = linkRuns(view);
        assertEquals(2, links.size());

        for (Text link : links) {
            click(link, MouseButton.PRIMARY, 1);
        }
        view.setOnLinkAction(address -> fail("removed handler called with " + address));
        view.setOnLinkAction(null);
        click(links.get(0), MouseButton.PRIMARY, 1);
    }

    @Test
    @DisplayName("the theme shows links in the accent colour, also in a quote and around code, underlined only on hover")
    void themeStylesLinks() {
        MarkdownView view = layOut(new MarkdownView("A [link](/a) and [`code`](/b).\n\n> A [quoted link](/c)."));

        List<Text> links = linkRuns(view);
        Paint accent = themeColor(view, "-fx-accent-color");
        assertEquals(3, links.size());
        for (Text link : links) {
            assertEquals(accent, link.getFill(), () -> "fill of " + link.getText());
            assertFalse(link.isUnderline(), () -> link.getText() + " underlined without the pointer over it");
        }
        assertTrue(isMonospaced(links.get(1).getFont()), "code in a link stays monospace");
        Text plainInQuote = runs(textFlows(view, ".markdown-paragraph").get(1)).get(0);
        assertEquals(themeColor(view, "-fx-text-secondary-color"), plainInQuote.getFill());

        HoveredText hovered = new HoveredText("hovered");
        hovered.getStyleClass().addAll("markdown-text", "markdown-link");
        StackPane probe = new StackPane(hovered);
        probe.getStyleClass().add("markdown-view");
        StackPane root = (StackPane) view.getScene().getRoot();
        root.getChildren().add(probe);
        probe.applyCss();
        assertTrue(hovered.isUnderline(), "a link under the pointer is underlined");
        root.getChildren().remove(probe);
    }

    @Test
    @DisplayName("a long link text wraps with its paragraph")
    void longLinkWraps() {
        MarkdownView view = layOut(new MarkdownView("Before [" + "linked words ".repeat(40) + "](/a) after."));

        Text link = linkRuns(view).get(0);
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(0);
        assertTrue(paragraph.getWidth() <= SCENE_WIDTH, () -> "paragraph width " + paragraph.getWidth());
        assertTrue(link.getBoundsInParent().getHeight() > 3 * lineHeight(link),
                () -> "link height " + link.getBoundsInParent().getHeight());
    }

    @Test
    @DisplayName("the Oven Timer sample document renders with its headings and its list items")
    void ovenTimerDocument() throws IOException {
        MarkdownView view = layOut(new MarkdownView(sampleDocument("OvenTimer.md")));

        assertEquals(List.of("Oven Timer", "Tray in", "Tray out", "Settings", "While it counts"),
                texts(view, ".markdown-heading"));
        assertEquals(1, view.lookupAll(".markdown-heading-1").size());
        assertEquals(4, view.lookupAll(".markdown-list-item").size());
        assertEquals(List.of("\u2022", "\u2022", "\u2022", "\u2022"), markers(view));
        List<String> items = itemTexts(view);
        assertEquals("tray (optional) \u2014 the tray to watch.", items.get(0));
        assertEquals("Unit \u2014 seconds, minutes (the default) or hours.", items.get(3));
        List<String> paragraphs = texts(view, ".markdown-document > .markdown-paragraph");
        assertTrue(paragraphs.get(paragraphs.size() - 1).startsWith("The ring around the dial fills as the time"
                + " passes"));
    }

    @Test
    @DisplayName("the Order Import sample document renders with its nested lists and continuation lines")
    void orderImportDocument() throws IOException {
        MarkdownView view = layOut(new MarkdownView(sampleDocument("OrderImport.md")));

        assertEquals(List.of("Order Import", "Result", "Settings", "How the text is read"),
                texts(view, ".markdown-heading"));
        assertEquals(3, view.lookupAll(".markdown-document > .markdown-bullet-list").size());
        assertEquals(18, view.lookupAll(".markdown-list-item").size());
        List<Node> nested = new ArrayList<>();
        for (Node list : view.lookupAll(".markdown-bullet-list")) {
            if (listDepth(list) == 1) {
                nested.add(list);
            }
        }
        assertEquals(2, nested.size());
        assertEquals(2, itemRows(nested.get(0)).size());
        assertEquals(5, itemRows(nested.get(1)).size());
        List<String> items = itemTexts(view);
        assertTrue(items.contains("skip and note: the import passes over the row, writes its line number to the"
                + " import log, and tells at the end how many rows it passed over. No row is dropped unnoticed."),
                () -> "" + items);
        assertTrue(items.contains("a quote stands inside a field that does not begin with one (9\" tin), or text"
                + " follows a closing quote (\"rye\"loaf),"), () -> "" + items);
        assertTrue(items.contains("First line names the fields \u2014 on (the default): the first line gives the"
                + " field names. Off: the fields are called field1, field2, \u2026 and the first line is an order. A"
                + " name that repeats gets a number: bread, bread_1. A first line that ends in a delimiter, a,b,, has a"
                + " third field with the empty name."), () -> "" + items);
        List<String> paragraphs = texts(view, ".markdown-document > .markdown-paragraph");
        assertEquals("The progress bar follows the bytes read so far.", paragraphs.get(paragraphs.size() - 1));
    }

    @Test
    @DisplayName("the Batch Planner sample document renders with all seven code blocks")
    void batchPlannerDocument() throws IOException {
        MarkdownView view = layOut(new MarkdownView(sampleDocument("BatchPlanner.md")));

        List<String> headings = texts(view, ".markdown-heading");
        assertEquals(20, headings.size(), () -> "" + headings);
        assertEquals("Batch Planner", headings.get(0));
        assertEquals("The generated class", headings.get(8));
        assertEquals("Where it runs", headings.get(19));
        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        assertEquals(7, blocks.size());
        for (Node block : blocks) {
            assertTrue(block.getParent().getStyleClass().contains("markdown-document"), "a top-level block");
        }
        assertEquals("for (Order o : orders()) {\n    if (o.isMissing(\"loaves\")) continue;",
                firstLines(codeOf(blocks.get(0)), 2));
        assertEquals("// The three largest orders, a missing count last.",
                firstLines(codeOf(blocks.get(2)), 1));
        String generatedClass = codeOf(blocks.get(4));
        assertTrue(generatedClass.startsWith("// planner imports\nimport com.example.bakery.planner"),
                generatedClass);
        assertTrue(generatedClass.contains("\n\n// Your custom imports:\nimport java.util.Map;\n"), generatedClass);
        assertTrue(generatedClass.endsWith("// your code ends\n    }\n}"), generatedClass);
        assertEquals(39, generatedClass.split("\n", -1).length, "every line of the generated class, blank ones too");
        assertTrue(codeOf(blocks.get(6)).endsWith("if (!o.isMissing(\"loaves\")) out_total += o.get(\"loaves\","
                + " tLong);\n}"));
        List<String> copied = recordCopies(view);
        copyButton(blocks.get(4)).fire();
        assertEquals(List.of(generatedClass), copied, "the copy button copies the whole generated class");
        assertEquals(7, view.lookupAll(".markdown-code-copy").size(), "one copy button per code block, none on tables");
        List<String> paragraphs = texts(view, ".markdown-document > .markdown-paragraph");
        assertTrue(paragraphs.contains("Your code is the body of one method. It loops over the day's orders itself and"
                + " writes the plan lines it wants:"), () -> "" + paragraphs);
        List<TableView<?>> tables = tables(view);
        assertEquals(3, tables.size());
        assertEquals(List.of("Double-clicked", "Inserted"), columnTexts(tables.get(0)));
        assertEquals(List.of("Example", "Input", "What it shows"), columnTexts(tables.get(1)));
        assertEquals(List.of("Kind", "Java type", "Token", "Read", "Plan column"), columnTexts(tables.get(2)));
        assertEquals(List.of(3, 6, 14), List.of(tables.get(0).getItems().size(), tables.get(1).getItems().size(),
                tables.get(2).getItems().size()));
        List<TableRow<?>> kinds = shownRows(tables.get(2));
        assertEquals(List.of("COUNT8 \u00B7 COUNT16 \u00B7 COUNT32 \u00B7 COUNT64",
                "Byte \u00B7 Short \u00B7 Integer \u00B7 Long"),
                cellTexts(kinds.get(0)).subList(0, 2));
        assertEquals(List.of("blob", "\u2014", "\u2014", "no", "no"), cellTexts(kinds.get(13)));
        for (TableView<?> table : tables) {
            assertTrue(table.getParent().getParent().getStyleClass().contains("markdown-document"),
                    "a top-level table");
            assertAllRowsShown(table, shownRows(table));
        }
        for (String paragraph : texts(view, ".markdown-paragraph")) {
            assertFalse(paragraph.startsWith("|"), () -> "a table row left as paragraph text: " + paragraph);
        }
    }

    @Test
    @DisplayName("the Label Template sample document shows its seven tables as tables, two in list items")
    void labelTemplateDocument() throws IOException {
        MarkdownView view = layOut(new MarkdownView(sampleDocument("LabelTemplate.md")));

        List<String> headings = texts(view, ".markdown-heading");
        assertEquals(16, headings.size(), () -> "" + headings);
        assertEquals("Label Template", headings.get(0));
        assertEquals("Formats", headings.get(15));
        List<TableView<?>> tables = tables(view);
        List<List<String>> headers = new ArrayList<>();
        List<Integer> rowCounts = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        for (TableView<?> table : tables) {
            headers.add(columnTexts(table));
            rowCounts.add(shownRows(table).size());
            depths.add(listDepth(table));
            assertAllRowsShown(table, shownRows(table));
        }
        assertEquals(List.of(
                List.of("Double-clicked", "Inserted"),
                List.of("Example", "Mode", "What it shows"),
                List.of("Name", "What it holds"),
                List.of("Kind", "In the template", "Printed with {{\u2026}}", "For example"),
                List.of("Kind", "Printed with {{\u2026}}", "Parts"),
                List.of("Pattern", "On 2027-03-14, 2027-03-14T06:30:00, 06:30:00"),
                List.of("Instead of", "Write")), headers);
        assertEquals(List.of(4, 8, 7, 7, 4, 6, 3), rowCounts);
        assertEquals(List.of(0, 0, 0, 0, 0, 1, 1), depths, "the last two sit in list items");
        assertEquals(List.of("Markdown table", "Whole day", "a table generic over columns, | and <br> escaped"),
                cellTexts(shownRows(tables.get(1)).get(3)), "an escaped pipe in a code span is a pipe");
        assertEquals(List.of("HH:mm:ss", "06:30:00"), cellTexts(shownRows(tables.get(5)).get(5)));
        assertEquals(List.of("{{order.day:date}}, {{order.ready:time}}",
                "{{order.day}}, {{order.ready}}: a date and a time already"),
                cellTexts(shownRows(tables.get(6)).get(2)));
        Node itemOfLastTable = tables.get(6).getParent().getParent();
        assertEquals("A date held as text in a TEXT column, such as 14.03.2027, is best turned into a day column before"
                + " it reaches the template.", texts(itemOfLastTable, ".markdown-paragraph").get(1));
        List<Node> code = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        assertEquals(1, code.size());
        assertTrue(codeText(code.get(0)).getText().startsWith("DELIVERIES FOR {{shop.city}}\n"));
        List<String> paragraphs = texts(view, ".markdown-paragraph");
        for (String paragraph : paragraphs) {
            assertFalse(paragraph.startsWith("|"), () -> "a table row left as paragraph text: " + paragraph);
        }
        assertTrue(paragraphs.get(0).startsWith("Fills a label template once"), paragraphs.get(0));
        List<Text> links = linkRuns(view);
        assertEquals(1, links.size(), "its one link");
        assertEquals("Mustache", links.get(0).getText());
        assertEquals("https://mustache.github.io/", tooltip(links.get(0)).getText());
        assertSame(textFlows(view, ".markdown-paragraph").get(0), links.get(0).getParent());
        List<String> addresses = new ArrayList<>();
        view.setOnLinkAction(address -> addresses.add(address));
        click(links.get(0), MouseButton.PRIMARY, 1);
        assertEquals(List.of("https://mustache.github.io/"), addresses);
    }

    @Test
    @DisplayName("the Sample Bakes sample document turns the bare URLs of its five sources into links")
    void sampleBakesDocument() throws IOException {
        MarkdownView view = layOut(new MarkdownView(sampleDocument("SampleBakes.md")));
        List<String> addresses = new ArrayList<>();
        view.setOnLinkAction(address -> addresses.add(address));

        assertEquals(9, texts(view, ".markdown-heading").size());
        List<TableView<?>> tables = tables(view);
        assertEquals(5, tables.size(), "its five tables");
        assertEquals(List.of("Entry", "Rows", "Columns"), columnTexts(tables.get(0)));
        assertEquals(List.of("Rye Bread - Training", "37440", "14"), cellTexts(shownRows(tables.get(0)).get(1)));
        List<Text> links = linkRuns(view);
        for (Text link : links) {
            click(link, MouseButton.PRIMARY, 1);
            assertEquals(link.getText(), tooltip(link).getText());
            assertEquals(1, listDepth(link), "in a list item");
            assertEquals("Source: " + link.getText(), textOf((TextFlow) link.getParent()));
        }
        assertEquals(List.of(
                "https://example.org/archive/2/rye+bread",
                "https://example.org/archive/555/sourdough+starter+log",
                "https://example.org/archive/697/bakers+shift+notes+and+outcomes",
                "https://example.org/archive/144/mill+records+flour+deliveries",
                "https://example.org/archive/320/customer+tastings"), addresses);
    }

    @Test
    @DisplayName("the Recipe Lookup sample document shows its table as a table and its numbered list as a list")
    void recipeLookupDocument() throws IOException {
        MarkdownView view = layOut(new MarkdownView(sampleDocument("RecipeLookup.md")));

        assertEquals(List.of("Recipe Lookup", "Recipes in", "Lookups out", "Settings", "Kinds", "Lists and paths",
                "Missing values", "Invalid values", "Changes from version 1"), texts(view, ".markdown-heading"));
        TableView<?> table = table(view);
        assertEquals(List.of("Value", "Flag", "Whole", "Real", "Text", "JSON"), columnTexts(table));
        List<TableRow<?>> rows = shownRows(table);
        assertEquals(10, rows.size());
        assertEquals(List.of("an object or an array", "invalid", "invalid", "invalid", "its JSON text",
                "its JSON text"), cellTexts(rows.get(9)));
        assertAllRowsShown(table, rows);
        List<Node> orderedLists = new ArrayList<>(view.lookupAll(".markdown-ordered-list"));
        assertEquals(1, orderedLists.size());
        assertEquals(List.of("1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9."), markers(orderedLists.get(0)));
        List<Node> items = itemRows(orderedLists.get(0));
        assertEquals("Conversion is strict. Version 1 parsed text: \"7\" read as a number was 7 there. Now a text is"
                + " never parsed, so \"7\" is no Whole; the Convert menu turns it into a number afterwards.",
                texts(items.get(0), ".markdown-paragraph").get(0));
        assertEquals("Mistakes that can be seen without recipes are marked in the dialog, before the run: a path that"
                + " does not compile, an empty or duplicate name, a path that can match several values without List."
                + " Version 1 reported them when it ran.", texts(items.get(8), ".markdown-paragraph").get(0));
        List<Node> nested = new ArrayList<>();
        for (Node list : view.lookupAll(".markdown-bullet-list")) {
            if (listDepth(list) == 1) {
                nested.add(list);
            }
        }
        assertEquals(1, nested.size());
        assertEquals(5, itemRows(nested.get(0)).size(), "the five settings of a query");
    }

    @Test
    @DisplayName("the shortcut key plus A over the Batch Planner sample document selects its text, its seven code"
            + " blocks and its three tables, and plus C copies exactly the text a reader sees")
    void selectAllOverSampleDocument() throws IOException {
        MarkdownView view = layOut(new MarkdownView(sampleDocument("BatchPlanner.md")));
        List<String> copied = recordCopies(view);
        TextFlow first = textFlows(view, ".markdown-paragraph").get(0);

        clicks(runAt(first, 0), textPoint(first, 0), 1);
        shortcut(view, KeyCode.A);
        relayOut(view);
        for (Node block : view.lookupAll(".markdown-code-block")) {
            Bounds code = codeBounds(block);
            Bounds highlight = sceneBounds(selectionHighlight(block));
            assertEquals(code.getMinY(), highlight.getMinY(), 1.0, "every code block selected whole: its first line");
            assertEquals(code.getMaxY(), highlight.getMaxY(), 1.0, "to its last");
            assertEquals(code.getMinX(), highlight.getMinX(), 1.0, "from its start");
        }
        for (TableView<?> table : tables(view)) {
            assertTableLooksSelected(view, table, true);
        }
        shortcut(view, KeyCode.C);

        // The text as a reader sees it, written from the Markdown source by a converter of its own (headings and
        // paragraphs set apart by blank lines, list items by line feeds, code as written, tables tab-separated).
        String expected = sampleDocument("BatchPlanner.copied.txt").replace("\r\n", "\n");
        assertEquals(1, copied.size());
        String text = copied.getFirst();
        String[] expectedLines = expected.split("\n", -1);
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < Math.min(expectedLines.length, lines.length); i++) {
            int line = i;
            assertEquals(expectedLines[i], lines[i], () -> "line " + (line + 1));
        }
        assertEquals(expected, text);
    }

    /**
     * What a sample document must show, counted in its Markdown source: the title, the {@code ##} sections in order,
     * the list items (nested ones included), the fenced code blocks and the tables.
     */
    private record SampleDocument(String file, String title, List<String> sections, int listItems,
                                  int codeBlocks, int tables) {
    }

    @Test
    @DisplayName("the sample documents, one after another in one view, as a help pane shows its pages")
    void sampleDocumentsEndToEnd() throws IOException {
        List<SampleDocument> documents = List.of(
                new SampleDocument("OvenTimer.md", "Oven Timer",
                        List.of("Tray in", "Tray out", "Settings", "While it counts"), 4, 0, 0),
                new SampleDocument("OrderImport.md", "Order Import",
                        List.of("Result", "Settings", "How the text is read"), 18, 0, 0),
                new SampleDocument("BatchPlanner.md", "Batch Planner",
                        List.of("Trust: your code is a program", "Orders in", "Plan out", "Settings", "The code",
                                "Streaming and memory", "Book values", "The generated class", "Editor",
                                "Saved plans and examples", "How a run goes", "Kinds", "Kitchen values", "Log",
                                "Errors", "Stopping", "What your code sees", "Additional libraries", "Where it runs"),
                        59, 7, 3),
                new SampleDocument("LabelTemplate.md", "Label Template",
                        List.of("Orders in", "Labels out", "Settings", "Result", "Editor", "Saved templates",
                                "Preview", "The data model", "Kinds", "Dates and times", "Whole day", "Missing values",
                                "Errors", "Safety", "Formats"),
                        47, 1, 7),
                new SampleDocument("RecipeLookup.md", "Recipe Lookup",
                        List.of("Recipes in", "Lookups out", "Settings", "Kinds", "Lists and paths", "Missing values",
                                "Invalid values", "Changes from version 1"),
                        30, 0, 1),
                new SampleDocument("SampleBakes.md", "Sample Bakes",
                        List.of("Result", "Settings", "The data", "Rye Bread", "Sourdough Starter Log",
                                "Bakers' Shift Notes", "Mill Records (Flour Deliveries)", "Customer Tastings"),
                        17, 0, 5));
        MarkdownView view = layOut(new MarkdownView());
        ScrollPane outer = (ScrollPane) view.lookup(".scroll-pane");

        for (SampleDocument document : documents) {
            String name = document.file();
            // The reader had scrolled down the previous document before opening the next one.
            outer.setVvalue(outer.getVmax());
            view.setMarkdown(sampleDocument(name));
            view.getScene().getRoot().applyCss();
            view.getScene().getRoot().layout();

            Node title = view.lookup(".markdown-document").lookup(".markdown-heading");
            assertTrue(title.getStyleClass().contains("markdown-heading-1"), name + ": the first block is the title");
            assertEquals(List.of(document.title()), texts(view, ".markdown-heading-1"), name);
            assertEquals(document.sections(), texts(view, ".markdown-heading-2"), name);
            assertEquals(1 + document.sections().size(), view.lookupAll(".markdown-heading").size(),
                    name + ": no deeper headings");
            // From the top of the document's scroll pane, below the zoom bar.
            double titleTop = sceneY(title) - sceneY(outer);
            assertTrue(titleTop >= 0 && titleTop < 20, () -> name + ": the title at the top, " + titleTop);

            assertEquals(document.listItems(), view.lookupAll(".markdown-list-item").size(), name + ": list items");
            assertEquals(document.listItems(), markers(view).size(), name + ": one marker per item");

            List<Node> codeBlocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
            assertEquals(document.codeBlocks(), codeBlocks.size(), name + ": code blocks");
            for (Node block : codeBlocks) {
                assertFalse(codeOf(block).isBlank(), name + ": code in every block");
                copyButton(block);
            }

            List<TableView<?>> tables = tables(view);
            assertEquals(document.tables(), tables.size(), name + ": tables");
            assertTrue(view.lookupAll(".markdown-source-block").isEmpty(), name + ": no source blocks");
            List<String> cells = new ArrayList<>();
            for (TableView<?> table : tables) {
                assertFalse(table.getColumns().isEmpty(), name + ": a table's columns");
                assertAllRowsShown(table, shownRows(table));
                cells.addAll(columnTexts(table));
                for (TableRow<?> row : shownRows(table)) {
                    cells.addAll(cellTexts(row));
                }
            }

            // Nothing of the syntax the view renders is left for the reader to see.
            List<String> texts = texts(view, ".markdown-heading");
            texts.addAll(texts(view, ".markdown-paragraph"));
            texts.addAll(cells);
            for (String text : texts) {
                assertFalse(text.contains("**") || text.contains("`") || text.startsWith("#") || text.startsWith("|")
                        || text.startsWith("- "), () -> name + ": Markdown syntax shown as text: " + text);
            }
        }
    }

    /**
     * What a table of a sample document must show and do, read off its Markdown source: the document it is in, its
     * columns' titles, its number of rows, the styles of the runs in its header and cells, the column it is sorted by,
     * and what the shortcut key plus C copies after that sort from the rectangle of the first two shown rows (the one
     * row of a table of one) and the first two columns.
     */
    private record DocumentTable(String file, List<String> columns, int rows, Set<String> styles, int sortColumn,
                                 String copied) {
    }

    @Test
    @DisplayName("every table of the sample documents, one document after another in one view: its columns, rows and"
            + " formatted cells, sorted by a column, a rectangle of cells selected and copied")
    void sampleDocumentTablesEndToEnd() throws IOException {
        String code = "markdown-code";
        String strong = "markdown-strong";
        String emphasis = "markdown-emphasis";
        List<String> archiveColumns = List.of("Entry", "Rows", "Columns");
        List<DocumentTable> expectedTables = List.of(
                new DocumentTable("BatchPlanner.md", List.of("Double-clicked", "Inserted"), 3, Set.of(code),
                        1, "a kitchen value\tgetKitchenValue(\"ovens\", tLong)\n"
                                + "a column, outside every loop over the orders\to.get(\"loaves\", tLong)"),
                new DocumentTable("BatchPlanner.md", List.of("Example", "Input", "What it shows"), 6,
                        Set.of(strong, code), 0, "Baking calendar\tnone\nColumns to key/value lines\tany order book"),
                new DocumentTable("BatchPlanner.md",
                        List.of("Kind", "Java type", "Token", "Read", "Plan column"), 14, Set.of(code, emphasis), 0,
                        "blob\t\u2014\nBYTES\tbyte[]"),
                new DocumentTable("LabelTemplate.md", List.of("Double-clicked", "Inserted"), 4, Set.of(code),
                        1, "a value\t{{greeting}}\na column whose name is a plain name\t{{order.customer}}"),
                new DocumentTable("LabelTemplate.md", List.of("Example", "Mode", "What it shows"), 8,
                        Set.of(strong, code, emphasis), 1, "Box label\tPer order\nFile name\tPer order"),
                new DocumentTable("LabelTemplate.md", List.of("Name", "What it holds"), 7,
                        Set.of(code, emphasis, strong), 0, "columns\tthe book's columns, each with its name and its"
                                + " kind: {{#each columns as c}}{{c.name}}{{/each}}\norder\tPer order: the current"
                                + " order, its columns by their original names: {{order.customer}}, and"
                                + " {{order[\"Customer Name\"]}} for a name with a space, an accent or another"
                                + " character a plain name does not take"),
                new DocumentTable("LabelTemplate.md",
                        List.of("Kind", "In the template", "Printed with {{\u2026}}", "For example"), 7,
                        Set.of(code), 1, "FLAG\tboolean\nday, day-time, clock, moment (and their -micros and -nanos"
                                + " variants)\tdate and time, see the next section"),
                new DocumentTable("LabelTemplate.md", List.of("Kind", "Printed with {{\u2026}}", "Parts"),
                        4, Set.of(code), 0, "clock\t06:30:00\nday\t2027-03-14"),
                new DocumentTable("LabelTemplate.md",
                        List.of("Pattern", "On 2027-03-14, 2027-03-14T06:30:00, 06:30:00"), 6, Set.of(code), 1,
                        "HH:mm:ss\t06:30:00\nd. MMMM yyyy\t14. Mar 2027; with fr-CH 14. mars 2027"),
                new DocumentTable("LabelTemplate.md", List.of("Instead of", "Write"), 3, Set.of(code), 0,
                        "{{order.day:date}}, {{order.ready:time}}\t{{order.day}}, {{order.ready}}: a date and a time"
                                + " already\n{{order.day:text(\"dd.MM.yyyy\")}}\t{{order.day.format(\"dd.MM.yyyy\")}}"),
                new DocumentTable("RecipeLookup.md", List.of("Value", "Flag", "Whole", "Real", "Text", "JSON"),
                        10, Set.of(code), 0, "\"7\"\tinvalid\n2.50\tinvalid"),
                new DocumentTable("SampleBakes.md", archiveColumns, 2, Set.of(), 1,
                        "Rye Bread - Test\t12480\nRye Bread - Training\t37440"),
                new DocumentTable("SampleBakes.md", archiveColumns, 1, Set.of(), 1, "Starter Log - 10K\t10000"),
                new DocumentTable("SampleBakes.md", archiveColumns, 1, Set.of(), 1, "Shift Notes - Data\t4424"),
                new DocumentTable("SampleBakes.md", archiveColumns, 1, Set.of(), 1, "Mill Records - Original\t1000"),
                new DocumentTable("SampleBakes.md", archiveColumns, 2, Set.of(), 1,
                        "Customer Tastings - Harbour\t395\nCustomer Tastings - Town Square\t649"));
        List<String> files = List.of("OvenTimer.md", "OrderImport.md", "BatchPlanner.md", "LabelTemplate.md",
                "RecipeLookup.md", "SampleBakes.md");
        MarkdownView view = layOut(new MarkdownView());
        List<String> copied = recordCopies(view);
        Paint codeColor = themeColor(view, "-fx-code-text-color");

        int checked = 0;
        for (String file : files) {
            view.setMarkdown(sampleDocument(file));
            relayOut(view);
            List<DocumentTable> expectedInFile = new ArrayList<>();
            for (DocumentTable expected : expectedTables) {
                if (expected.file().equals(file)) {
                    expectedInFile.add(expected);
                }
            }
            List<TableView<?>> tables = tables(view);
            assertEquals(expectedInFile.size(), tables.size(), file + ": tables");

            for (int index = 0; index < tables.size(); index++) {
                TableView<?> table = tables.get(index);
                DocumentTable expected = expectedInFile.get(index);
                String where = file + ", table " + index;
                assertEquals(expected.columns(), columnTexts(table), where + ": columns");
                List<TableRow<?>> rows = shownRows(table);
                assertEquals(expected.rows(), rows.size(), where + ": rows");
                assertAllRowsShown(table, rows);

                // Formatted cells: every run of the header and the cells styled as its Markdown says.
                List<TextFlow> flows = new ArrayList<>();
                for (TableColumn<?, ?> column : table.getColumns()) {
                    flows.add((TextFlow) column.getGraphic());
                }
                for (TableRow<?> row : rows) {
                    for (int column = 0; column < table.getColumns().size(); column++) {
                        flows.add(cellFlow(row, column));
                    }
                }
                Set<String> styles = new HashSet<>();
                for (TextFlow flow : flows) {
                    for (Text run : runs(flow)) {
                        String text = run.getText();
                        assertFalse(text.contains("`") || text.contains("**") || text.contains("\\|"),
                                () -> where + ": Markdown syntax shown as text: " + text);
                        for (String styleClass : run.getStyleClass()) {
                            if (!styleClass.equals("markdown-text")) {
                                styles.add(styleClass);
                            }
                        }
                        boolean isCode = run.getStyleClass().contains(code);
                        assertEquals(isCode, isMonospaced(run.getFont()), () -> where + ": the font of " + text);
                        if (isCode) {
                            assertEquals(codeColor, run.getFill(), () -> where + ": the colour of " + text);
                        }
                        if (run.getStyleClass().contains(strong)) {
                            assertTrue(isBold(run.getFont()), () -> where + ": not bold: " + text);
                        }
                        if (run.getStyleClass().contains(emphasis)) {
                            String fontStyle = run.getFont().getStyle().toLowerCase(Locale.ROOT);
                            assertTrue(fontStyle.contains("italic") || fontStyle.contains("oblique"),
                                    () -> where + ": not italic: " + text);
                        }
                    }
                }
                assertEquals(expected.styles(), styles, where + ": the styles of its runs");

                // Sorting a column: ascending in natural order, the rows moving whole; descending; the source order.
                List<String> source = rowLines(table);
                clickHeader(table, expected.sortColumn(), false);
                assertEquals(List.of(table.getColumns().get(expected.sortColumn())), table.getSortOrder(), where);
                List<String> ascending = shownColumn(table, expected.sortColumn());
                for (int i = 1; i < ascending.size(); i++) {
                    String upper = ascending.get(i - 1);
                    String lower = ascending.get(i);
                    assertTrue(NaturalOrder.INSTANCE.compare(upper, lower) <= 0,
                            () -> where + ": " + upper + " sorted above " + lower);
                }
                List<String> sortedLines = rowLines(table);
                sortedLines.sort(null);
                List<String> sourceLines = new ArrayList<>(source);
                sourceLines.sort(null);
                assertEquals(sourceLines, sortedLines, where + ": every row moved with all its cells");

                // Selecting and copying a rectangle of the sorted rows, with and without the columns' titles.
                copied.clear();
                int lastRow = Math.min(1, expected.rows() - 1);
                drag(cellTarget(table, 0, 0), cellPoint(table, 0, 0), cellPoint(table, lastRow, 1));
                assertSame(table, view.getScene().getFocusOwner(), where + ": the table has the focus");
                shortcut(view, KeyCode.C);
                menuEntry(contextMenuAt(view, cellTarget(table, lastRow, 1)), "Copy with header").fire();
                assertEquals(List.of(expected.copied(), expected.columns().get(0) + "\t" + expected.columns().get(1)
                        + "\n" + expected.copied()), copied, where + ": the copied rectangle");
                for (TableView<?> other : tables) {
                    if (other != table) {
                        assertTrue(other.getSelectionModel().getSelectedCells().isEmpty(),
                                where + ": one selection in the view");
                    }
                }

                clickHeader(table, expected.sortColumn(), false);
                assertEquals(ascending.reversed(), shownColumn(table, expected.sortColumn()), where + ": descending");
                assertTrue(table.getSelectionModel().getSelectedCells().isEmpty(), where + ": the sort cleared it");
                clickHeader(table, expected.sortColumn(), false);
                assertTrue(table.getSortOrder().isEmpty(), where);
                assertEquals(source, rowLines(table), where + ": back in the source order");
                assertAllRowsShown(table, shownRows(table));
                checked++;
            }
        }
        assertEquals(expectedTables.size(), checked, "every table checked");
    }

    @Test
    @DisplayName("the deepest nesting the parser allows is styled and laid out without error")
    void deepestNestingLaysOut() {
        // A short innermost text: 100 levels indent far beyond the view's width, and a long paragraph wrapped at
        // almost no width takes seconds to lay out.
        MarkdownView view = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> layOut(new MarkdownView("- ".repeat(150) + "deep")));

        assertEquals(100, view.lookupAll(".markdown-bullet-list").size());
        TextFlow innermost = textFlows(view, ".markdown-paragraph").get(0);
        assertEquals("- ".repeat(50) + "deep", textOf(innermost));
    }

    @Test
    @DisplayName("emphasis nested thousands of levels deep renders without error, the deepest 100 levels as emphasis")
    void absurdlyDeepEmphasisRenders() {
        String markdown = "*a ".repeat(4000) + "z" + "* x".repeat(4000) + "*";

        MarkdownView view = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> layOut(new MarkdownView(markdown)));

        List<Text> runs = runs(textFlows(view, ".markdown-paragraph").get(0));
        assertEquals(3, runs.size());
        assertRun(runs.get(0), "*a ".repeat(3900));
        assertRun(runs.get(1), "a ".repeat(100) + "z" + " x".repeat(99), "markdown-emphasis");
        assertRun(runs.get(2), " x*".repeat(3901));
    }

    /**
     * The colour a theme token resolves to in the view's scene: a probe region beside the view takes the token as its
     * background, so the test does not repeat the theme's values.
     */
    private static Paint themeColor(MarkdownView view, String token) {
        Region probe = new Region();
        probe.setStyle("-fx-background-color: " + token + ";");
        StackPane root = (StackPane) view.getScene().getRoot();
        root.getChildren().add(probe);
        try {
            probe.applyCss();
            return probe.getBackground().getFills().get(0).getFill();
        } finally {
            root.getChildren().remove(probe);
        }
    }

    /**
     * The one table in a node.
     */
    private static TableView<?> table(Node root) {
        List<TableView<?>> tables = tables(root);
        assertEquals(1, tables.size(), "tables");
        return tables.get(0);
    }

    /**
     * The tables in a node, in document order.
     */
    private static List<TableView<?>> tables(Node root) {
        List<TableView<?>> tables = new ArrayList<>();
        for (Node node : root.lookupAll(".markdown-table")) {
            tables.add((TableView<?>) node);
        }
        return tables;
    }

    /**
     * The columns' plain texts, in the table's column order.
     */
    private static List<String> columnTexts(TableView<?> table) {
        List<String> texts = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) {
            texts.add(column.getText());
        }
        return texts;
    }

    /**
     * The texts of a row's cells, in the table's column order.
     */
    private static List<String> cellTexts(TableRow<?> row) {
        List<String> texts = new ArrayList<>();
        for (int column = 0; column < row.getTableView().getColumns().size(); column++) {
            texts.add(textOf(cellFlow(row, column)));
        }
        return texts;
    }

    /**
     * The rows as shown, top to bottom, each as its cells' texts in the table's column order joined by tabs.
     */
    private static List<String> rowLines(TableView<?> table) {
        List<String> lines = new ArrayList<>();
        for (TableRow<?> row : shownRows(table)) {
            lines.add(String.join("\t", cellTexts(row)));
        }
        return lines;
    }

    /**
     * The texts of a column's cells in the rows as shown, top to bottom; the column given by its index in the table's
     * columns.
     */
    private static List<String> shownColumn(TableView<?> table, int column) {
        List<String> texts = new ArrayList<>();
        for (TableRow<?> row : shownRows(table)) {
            texts.add(textOf(cellFlow(row, column)));
        }
        return texts;
    }

    /**
     * The widths of a table's columns, in the table's column order.
     */
    private static double[] columnWidths(TableView<?> table) {
        double[] widths = new double[table.getColumns().size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = table.getColumns().get(i).getWidth();
        }
        return widths;
    }

    /**
     * Drags a column's right edge by a distance, as the resize handle on the edge of its header does through the
     * table's public API.
     *
     * @return whether the table resized a column
     */
    private static <S> boolean resizeColumn(TableView<S> table, int column, double delta) {
        return table.resizeColumn(table.getColumns().get(column), delta);
    }

    /**
     * Moves a column to another place in the table's columns in one change, as a drag of its header does (a column
     * removed and added again in two changes would leave the sort order).
     */
    private static <S> void moveColumn(TableView<S> table, int from, int to) {
        List<TableColumn<S, ?>> columns = new ArrayList<>(table.getColumns());
        columns.add(to, columns.remove(from));
        table.getColumns().setAll(columns);
    }

    /**
     * Applies CSS to the view's scene and lays it out again, as the next pulse does.
     */
    private static void relayOut(MarkdownView view) {
        view.getScene().getRoot().applyCss();
        view.getScene().getRoot().layout();
    }

    /**
     * The cells a row shows, left to right.
     */
    private static List<TableCell<?, ?>> shownCells(TableRow<?> row) {
        List<TableCell<?, ?>> cells = new ArrayList<>();
        for (Node node : row.lookupAll(".table-cell")) {
            if (node instanceof TableCell<?, ?> cell && isTreeVisible(cell)) {
                cells.add(cell);
            }
        }
        cells.sort((first, second) -> Double.compare(sceneX(first), sceneX(second)));
        return cells;
    }

    /**
     * The header of a column, given by its index in the table's columns: the column header around the label that
     * shows the column's graphic.
     */
    private static Region columnHeader(TableView<?> table, int column) {
        Node graphic = table.getColumns().get(column).getGraphic();
        Parent label = graphic.getParent();
        assertNotNull(label, "the header's text flow is not shown");
        return (Region) label.getParent();
    }

    /**
     * Clicks a column's header with the primary button, as the reader does to sort: a press and a release over the
     * header's text without moving, then lays the view out again. The shift key adds the column to the sort.
     */
    private static void clickHeader(TableView<?> table, int column, boolean shiftDown) {
        Node text = runs((TextFlow) table.getColumns().get(column).getGraphic()).get(0);
        Bounds bounds = text.localToScene(text.getLayoutBounds());
        Point2D at = new Point2D(bounds.getCenterX(), bounds.getCenterY());
        Event.fireEvent(text, mouse(MouseEvent.MOUSE_PRESSED, at, 1, shiftDown));
        Event.fireEvent(text, mouse(MouseEvent.MOUSE_RELEASED, at, 1, shiftDown));
        Event.fireEvent(text, mouse(MouseEvent.MOUSE_CLICKED, at, 1, shiftDown));
        // Two passes, as two pulses of a shown window: a sort moves the rows, and a row the flow builds anew in the
        // first layout takes the theme's fonts and padding, and with them its height, only in the second.
        for (int pass = 0; pass < 2; pass++) {
            table.getScene().getRoot().applyCss();
            table.getScene().getRoot().layout();
        }
    }

    /**
     * The sort arrow shown in a column's header, {@code null} if it shows none.
     */
    private static Region sortArrow(TableView<?> table, int column) {
        Node arrow = columnHeader(table, column).lookup(".arrow");
        return arrow != null && isTreeVisible(arrow) && isShown(arrow) ? (Region) arrow : null;
    }

    /**
     * A table's selected cells as a grid: a line per row in the shown order, a character per column in the table's
     * column order, {@code #} for a selected cell, {@code .} for any other and {@code -} in a hidden column (whose
     * cells the selection model answers as selected whenever a cell of their row is).
     */
    private static <S> String selection(TableView<S> table) {
        StringBuilder grid = new StringBuilder();
        for (int row = 0; row < table.getItems().size(); row++) {
            if (row > 0) {
                grid.append('\n');
            }
            for (TableColumn<S, ?> column : table.getColumns()) {
                if (!column.isVisible()) {
                    grid.append('-');
                } else {
                    grid.append(table.getSelectionModel().isSelected(row, column) ? '#' : '.');
                }
            }
        }
        return grid.toString();
    }

    /**
     * The cell a table shows for a row, by its index in the shown order, and a column, by its index in the table's
     * columns.
     */
    private static TableCell<?, ?> cell(TableView<?> table, int row, int column) {
        TableColumn<?, ?> tableColumn = table.getColumns().get(column);
        for (TableRow<?> shown : shownRows(table)) {
            if (shown.getIndex() == row) {
                for (Node node : shown.lookupAll(".table-cell")) {
                    if (node instanceof TableCell<?, ?> cell && cell.getTableColumn() == tableColumn) {
                        return cell;
                    }
                }
            }
        }
        throw new AssertionError("no cell of column " + column + " in row " + row);
    }

    /**
     * What the pointer is over in the middle of a cell: the first text run of the cell, where there is one, as JavaFX
     * picks it; else the cell.
     */
    private static Node cellTarget(TableView<?> table, int row, int column) {
        TableCell<?, ?> cell = cell(table, row, column);
        if (cell.getGraphic() instanceof TextFlow flow && !flow.getChildren().isEmpty()) {
            return flow.getChildren().get(0);
        }
        return cell;
    }

    /**
     * The scene point in the middle of what the pointer is over in a cell.
     */
    private static Point2D cellPoint(TableView<?> table, int row, int column) {
        Bounds bounds = sceneBounds(cellTarget(table, row, column));
        return new Point2D(bounds.getCenterX(), bounds.getCenterY());
    }

    /**
     * Clicks a cell with the primary button, as JavaFX delivers a click on its text: a press, a release and a click,
     * with the shift key and the platform's shortcut key down as given.
     */
    private static void clickCell(TableView<?> table, int row, int column, boolean shiftDown, boolean shortcutDown) {
        Node target = cellTarget(table, row, column);
        Point2D at = cellPoint(table, row, column);
        Event.fireEvent(target, mouse(MouseEvent.MOUSE_PRESSED, at, 1, shiftDown, shortcutDown));
        Event.fireEvent(target, mouse(MouseEvent.MOUSE_RELEASED, at, 1, shiftDown, shortcutDown));
        Event.fireEvent(target, mouse(MouseEvent.MOUSE_CLICKED, at, 1, shiftDown, shortcutDown));
    }

    /**
     * Asserts whether a table looks selected whole, as the text selection shows a table it passes over: every shown
     * body cell and column header in the selection colour, their runs in the selection's text colour; or none of
     * them. Call it after a CSS pass.
     */
    private static void assertTableLooksSelected(MarkdownView view, TableView<?> table, boolean selected) {
        Paint selection = themeColor(view, "-fx-selection-color");
        Paint selectionText = themeColor(view, "-fx-selection-text-color");
        List<Region> parts = new ArrayList<>();
        for (TableRow<?> row : shownRows(table)) {
            parts.addAll(shownCells(row));
        }
        for (TableColumn<?, ?> column : table.getColumns()) {
            if (column.isVisible()) {
                parts.add(columnHeader(table, table.getColumns().indexOf(column)));
            }
        }
        for (Region part : parts) {
            assertEquals(selected, fills(part).contains(selection), () -> "background of " + part + ": " + fills(part));
            for (Node node : part.lookupAll(".markdown-text")) {
                assertEquals(selected, selectionText.equals(((Text) node).getFill()),
                        () -> "text in " + part + ": " + ((Text) node).getFill());
            }
        }
    }

    /**
     * The paints of a region's background fills, in their order; empty without a background.
     */
    private static List<Paint> fills(Region region) {
        List<Paint> paints = new ArrayList<>();
        if (region.getBackground() != null) {
            for (BackgroundFill fill : region.getBackground().getFills()) {
                paints.add(fill.getFill());
            }
        }
        return paints;
    }

    /**
     * The horizontal bar of a table's block, the one that scrolls a wide table sideways.
     */
    private static ScrollBar blockBar(Region block) {
        for (Node child : block.getChildrenUnmodifiable()) {
            if (child instanceof ScrollBar bar) {
                return bar;
            }
        }
        throw new AssertionError("no scroll bar in " + block);
    }

    private static Bounds sceneBounds(Node node) {
        return node.localToScene(node.getLayoutBounds());
    }

    /**
     * A table's rows as the reader sees them, in index order: the rows with an item, shown with all their parents.
     */
    private static List<TableRow<?>> shownRows(TableView<?> table) {
        List<TableRow<?>> rows = new ArrayList<>();
        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row && !row.isEmpty() && row.getIndex() >= 0 && isTreeVisible(row)) {
                rows.add(row);
            }
        }
        rows.sort((first, second) -> Integer.compare(first.getIndex(), second.getIndex()));
        return rows;
    }

    /**
     * The text flow of a row's cell in a column, given by its index in the table's columns.
     */
    private static TextFlow cellFlow(TableRow<?> row, int column) {
        TableColumn<?, ?> tableColumn = row.getTableView().getColumns().get(column);
        for (Node node : row.lookupAll(".table-cell")) {
            if (node instanceof TableCell<?, ?> cell && cell.getTableColumn() == tableColumn) {
                return (TextFlow) cell.getGraphic();
            }
        }
        throw new AssertionError("no cell of column " + column + " in row " + row.getIndex());
    }

    /**
     * Whether a node and all its parents are visible.
     */
    private static boolean isTreeVisible(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The scene bounds of a text flow's text: the union of its runs, which may reach past the flow's own bounds.
     */
    private static Bounds textBounds(TextFlow flow) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Text run : runs(flow)) {
            Bounds bounds = run.localToScene(run.getLayoutBounds());
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
        }
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private static void assertInside(Bounds inner, Node outer) {
        Bounds bounds = outer.localToScene(outer.getLayoutBounds());
        assertTrue(inner.getMinY() >= bounds.getMinY() - 0.5 && inner.getMaxY() <= bounds.getMaxY() + 0.5
                && inner.getMinX() >= bounds.getMinX() - 0.5 && inner.getMaxX() <= bounds.getMaxX() + 0.5,
                () -> inner + " outside " + bounds);
    }

    /**
     * Asserts that a table shows every one of its rows without scrolling: as tall as its header and the rows, no
     * vertical bar, the first row right under the header.
     */
    private static void assertAllRowsShown(TableView<?> table, List<TableRow<?>> rows) {
        assertEquals(table.getItems().size(), rows.size(), "every row shown");
        Region header = (Region) table.lookup(".column-header-background");
        double rowsHeight = 0;
        for (TableRow<?> row : rows) {
            rowsHeight += row.getHeight();
        }
        double inside = table.getHeight() - table.snappedTopInset() - table.snappedBottomInset();
        double horizontalBar = isShown(scrollBar(table, Orientation.HORIZONTAL))
                ? scrollBar(table, Orientation.HORIZONTAL).getHeight() : 0;
        assertEquals(header.getHeight() + rowsHeight + horizontalBar, inside, 1.0,
                "as tall as the header, the rows and a horizontal bar if there is one");
        assertFalse(isShown(scrollBar(table, Orientation.VERTICAL)), "no vertical bar");
        if (!rows.isEmpty()) {
            assertEquals(sceneY(header) + header.getHeight(), sceneY(rows.get(0)), 0.5,
                    "the first row under the header");
        }
    }

    private static String firstLines(String text, int count) {
        String[] lines = text.split("\n", -1);
        return String.join("\n", List.of(lines).subList(0, count));
    }

    /**
     * A document taller than the view, with a code block of a long line in its visible part.
     */
    private static String documentWithCodeBlock() {
        return "Intro.\n\n```java\n" + LONG_CODE_LINE + "\nshort();\n```\n\n" + manyParagraphs(30);
    }

    /**
     * A scroll event over the middle of a node, as the mouse wheel or a touchpad sends it; a positive delta scrolls
     * up or left.
     */
    private static ScrollEvent wheel(Node over, double deltaX, double deltaY, boolean shiftDown) {
        Bounds bounds = over.localToScene(over.getLayoutBounds());
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        return new ScrollEvent(ScrollEvent.SCROLL, x, y, x, y, shiftDown, false, false, false, false, false,
                deltaX, deltaY, 0, 0, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null);
    }

    /**
     * Replaces the view's clipboard with a recorder, so the test sees what the view copies and the system clipboard
     * stays untouched. Every test that copies calls this first.
     *
     * @return the texts copied from now on, in order
     */
    private static List<String> recordCopies(MarkdownView view) {
        List<String> copied = new ArrayList<>();
        view.setClipboard(text -> copied.add(text));
        return copied;
    }

    /**
     * A scene point just right of the gap before a character of a monospace text, a quarter of a character into it,
     * so a press there puts the caret before that character. A column past the line's end lies right of the line.
     */
    private static Point2D caretPoint(Text text, int line, int column) {
        // In the text's own coordinates, then into the scene: at another zoom than 1.0 the text is scaled.
        Bounds bounds = text.getLayoutBounds();
        return text.localToScene(bounds.getMinX() + (column + 0.25) * characterWidth(text),
                bounds.getMinY() + (line + 0.5) * lineHeight(text));
    }

    /**
     * The width of one character in a monospace text's font.
     */
    private static double characterWidth(Text text) {
        Text oneCharacter = new Text("x");
        oneCharacter.setFont(text.getFont());
        return oneCharacter.getLayoutBounds().getWidth();
    }

    /**
     * The selection highlight of a code or source block: exactly one.
     */
    private static Path selectionHighlight(Node block) {
        List<Node> highlights = new ArrayList<>(block.lookupAll(".markdown-selection"));
        assertEquals(1, highlights.size(), () -> "selection highlights in " + block);
        return (Path) highlights.get(0);
    }

    /**
     * A scene point a quarter of a character right of the gap before a character of a text flow, on that character's
     * line: a press there puts the caret before the character. The flow's text length gives a point just right of
     * its last character.
     */
    private static Point2D textPoint(TextFlow flow, int index) {
        int length = textOf(flow).length();
        if (index >= length) {
            Bounds last = characterBounds(flow, length - 1);
            return flow.localToScene(last.getMaxX() + 1, last.getCenterY());
        }
        Bounds character = characterBounds(flow, index);
        return flow.localToScene(character.getMinX() + 0.25 * character.getWidth(), character.getCenterY());
    }

    /**
     * The box of one character of a text flow, in the flow's coordinates.
     */
    private static Bounds characterBounds(TextFlow flow, int index) {
        Path shape = new Path(flow.getRangeShape(index, index + 1, false));
        shape.setStroke(null);
        shape.setFill(Color.BLACK);
        return shape.getLayoutBounds();
    }

    /**
     * The run of a text flow that holds a character, the node JavaFX picks for a press over it; the last run for the
     * flow's text length.
     */
    private static Text runAt(TextFlow flow, int index) {
        int start = 0;
        List<Text> runs = runs(flow);
        for (Text run : runs) {
            int end = start + run.getText().length();
            if (index < end) {
                return run;
            }
            start = end;
        }
        return runs.getLast();
    }

    /**
     * Whether a text flow shows a selection: a highlight with a shape, shown.
     */
    private static boolean isHighlighted(TextFlow flow) {
        Node highlight = flow.lookup(".markdown-selection");
        return highlight != null && isTreeVisible(highlight) && !((Path) highlight).getElements().isEmpty();
    }

    /**
     * The width of a text on one line in a font, measured by a text node of its own.
     */
    private static double textWidth(String text, Font font) {
        Text measure = new Text(text);
        measure.setFont(font);
        return measure.getLayoutBounds().getWidth();
    }

    /**
     * Whether a node lies inside another one, or is it.
     */
    private static boolean isInside(Node node, Node ancestor) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Presses the primary button over a node, moves the pointer to another point and releases it there, as JavaFX
     * delivers a drag: every event goes to the node under the press, wherever the pointer is.
     */
    private static void drag(Node pressed, Point2D from, Point2D to) {
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_PRESSED, from, 1));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_DRAGGED, to, 1));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_RELEASED, to, 1));
    }

    /**
     * Clicks the primary button over a node as often as given, as JavaFX delivers a double or triple click: a press,
     * a release and a click per click, each with the count of clicks so far.
     */
    private static void clicks(Node node, Point2D at, int count) {
        for (int clickCount = 1; clickCount <= count; clickCount++) {
            Event.fireEvent(node, mouse(MouseEvent.MOUSE_PRESSED, at, clickCount));
            Event.fireEvent(node, mouse(MouseEvent.MOUSE_RELEASED, at, clickCount));
            Event.fireEvent(node, mouse(MouseEvent.MOUSE_CLICKED, at, clickCount));
        }
    }

    /**
     * The click JavaFX sends after a press and a release on the same node with the pointer moved in between: not
     * still since the press.
     */
    private static MouseEvent movedClick(Point2D at) {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, at.getX(), at.getY(), at.getX(), at.getY(),
                MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null);
    }

    /**
     * Clicks the primary button over a node with the shift key down: a press, a release and a click.
     */
    private static void shiftClick(Node node, Point2D at) {
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_PRESSED, at, 1, true));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_RELEASED, at, 1, true));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_CLICKED, at, 1, true));
    }

    /**
     * Clicks a node with a button other than the primary one, without moving, in its middle: a press, a release and a
     * click, none of them a popup trigger.
     */
    private static void clickWith(Node node, MouseButton button) {
        Bounds bounds = sceneBounds(node);
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        boolean middle = button == MouseButton.MIDDLE;
        boolean secondary = button == MouseButton.SECONDARY;
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_PRESSED, x, y, x, y, button, 1, false, false, false,
                false, false, middle, secondary, false, false, true, null));
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_RELEASED, x, y, x, y, button, 1, false, false, false,
                false, false, false, false, false, false, true, null));
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_CLICKED, x, y, x, y, button, 1, false, false, false,
                false, false, false, false, false, false, true, null));
    }

    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at, int clickCount) {
        return mouse(type, at, clickCount, false);
    }

    /**
     * A mouse event of the primary button at a scene point; a drag is the only event not still since the press.
     */
    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at, int clickCount, boolean shiftDown) {
        return mouse(type, at, clickCount, shiftDown, false);
    }

    /**
     * A mouse event of the primary button at a scene point, with the shift key and the platform's shortcut key
     * (control, command on macOS) down as given.
     */
    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at, int clickCount, boolean shiftDown,
                                    boolean shortcutDown) {
        boolean buttonDown = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        boolean mac = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("mac");
        return new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), MouseButton.PRIMARY, clickCount,
                shiftDown, shortcutDown && !mac, false, shortcutDown && mac, buttonDown, false, false, false, false,
                type != MouseEvent.MOUSE_DRAGGED, null);
    }

    /**
     * Presses a key together with the platform's shortcut key (control, command on macOS) where the keyboard focus
     * is, as JavaFX delivers it.
     */
    private static void shortcut(MarkdownView view, KeyCode key) {
        Node focusOwner = view.getScene().getFocusOwner();
        assertNotNull(focusOwner, "no node has the focus");
        shortcutAt(focusOwner, key);
    }

    /**
     * Presses a key where the keyboard focus is, with the shift key and the platform's shortcut key down as given.
     */
    private static void key(MarkdownView view, KeyCode key, boolean shiftDown, boolean shortcutDown) {
        Node focusOwner = view.getScene().getFocusOwner();
        assertNotNull(focusOwner, "no node has the focus");
        boolean mac = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("mac");
        Event.fireEvent(focusOwner, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, shiftDown,
                shortcutDown && !mac, false, shortcutDown && mac));
    }

    /**
     * Presses a key together with the platform's shortcut key at a node, whether it has the focus or not.
     */
    private static void shortcutAt(Node target, KeyCode key) {
        boolean mac = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("mac");
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, !mac, false, mac));
    }

    /**
     * The copy button of a code block.
     */
    private static Button copyButton(Node block) {
        List<Node> buttons = new ArrayList<>(block.lookupAll(".markdown-code-copy"));
        assertEquals(1, buttons.size(), () -> "copy buttons in " + block);
        return (Button) buttons.get(0);
    }

    /**
     * The <em>Copy Markdown</em> entry of the view's context menu, as the menu is for text: <em>Copy</em>, a separator
     * and that entry. The menu belongs to the view's scroll pane, which fills the view, so a request anywhere in the
     * view reaches it; a test without a window cannot show it, so it fires the entry.
     */
    private static MenuItem copyMarkdownEntry(MarkdownView view) {
        ContextMenu menu = ((ScrollPane) view.lookup(".scroll-pane")).getContextMenu();
        assertNotNull(menu, "no context menu");
        assertEquals(3, menu.getItems().size(), () -> "entries: " + menu.getItems());
        assertEquals(view.getCopyText(), menu.getItems().get(0).getText(), "the text selection's Copy first");
        return menu.getItems().get(2);
    }

    /**
     * The entries of the view's context menu as a request at a node builds them: the view builds its menu for the
     * node a request comes from, then the view's scroll pane shows it. A test without a window cannot show it (a
     * popup needs a window), so it builds the entries for a node and fires them.
     */
    private static List<MenuItem> contextMenuAt(MarkdownView view, Node target) {
        view.prepareContextMenu(target);
        ContextMenu menu = ((ScrollPane) view.lookup(".scroll-pane")).getContextMenu();
        assertNotNull(menu, "no context menu");
        return menu.getItems();
    }

    /**
     * The labels of a menu's entries, {@code -} for a separator.
     */
    private static List<String> menuTexts(List<MenuItem> items) {
        List<String> texts = new ArrayList<>();
        for (MenuItem item : items) {
            texts.add(item instanceof SeparatorMenuItem ? "-" : item.getText());
        }
        return texts;
    }

    /**
     * The one entry of a menu with a label.
     */
    private static MenuItem menuEntry(List<MenuItem> items, String text) {
        MenuItem found = null;
        for (MenuItem item : items) {
            if (text.equals(item.getText())) {
                assertNull(found, () -> "two entries " + text);
                found = item;
            }
        }
        assertNotNull(found, () -> "no entry " + text + " in " + menuTexts(items));
        return found;
    }

    /**
     * The scroll pane inside a code block (the view's own is outside the block).
     */
    private static ScrollPane innerScrollPane(Node block) {
        return (ScrollPane) block.lookup(".scroll-pane");
    }

    private static ScrollBar scrollBar(Node pane, Orientation orientation) {
        for (Node node : pane.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == orientation) {
                return bar;
            }
        }
        throw new AssertionError("no " + orientation + " scroll bar in " + pane);
    }

    private static boolean isShown(Node node) {
        return node.isVisible() && node.getLayoutBounds().getWidth() > 0 && node.getLayoutBounds().getHeight() > 0;
    }

    /**
     * The height of one line of text in a text node's font.
     */
    private static double lineHeight(Text text) {
        // Measured in a text flow, as the view lays out its text. A lone Text measured on its own does not give a
        // stable answer: after a themed text field showed the same string in the same font, a new Text("x") reported
        // 19.0 instead of 17.29, so the measure depended on which test ran before. A text flow measured 17.29 either
        // way.
        Text oneLine = new Text("x");
        oneLine.setFont(text.getFont());
        return new TextFlow(oneLine).prefHeight(-1);
    }

    /**
     * The first run of a code block: its code, as one run, in a block the view does not colour. Its first character is
     * the code's first in a coloured block too, so its font and its place locate every character of the code.
     */
    private static Text codeText(Node block) {
        Node code = block.lookup(".markdown-code");
        assertNotNull(code, "no code text in " + block);
        return (Text) code;
    }

    /**
     * The code of a code block as it shows it, whatever its runs.
     */
    private static String codeOf(Node block) {
        return textOf(blockFlow(block));
    }

    /**
     * The box of a code block's code in the scene, all its runs.
     */
    private static Bounds codeBounds(Node block) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Text run : runs(blockFlow(block))) {
            Bounds bounds = sceneBounds(run);
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
        }
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * The text flow of a code or source block, {@code markdown-block-content}: the box that pads the runs of its text.
     */
    private static TextFlow blockFlow(Node block) {
        Node flow = block.lookup(".markdown-block-content");
        assertTrue(flow instanceof TextFlow, () -> "no text flow in " + block + ": " + flow);
        return (TextFlow) flow;
    }

    /**
     * The text node of a source block: its source lines, as one run.
     */
    private static Text sourceText(Node block) {
        Node text = block.lookup(".markdown-text");
        assertNotNull(text, "no source text in " + block);
        return (Text) text;
    }

    /**
     * A sample document of the test resources, the text of a made-up bakery app's help pages.
     */
    private static String sampleDocument(String name) throws IOException {
        try (InputStream in = MarkdownViewTest.class.getResourceAsStream("samples/" + name)) {
            assertNotNull(in, name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> texts(Node root, String selector) {
        List<String> texts = new ArrayList<>();
        for (TextFlow flow : textFlows(root, selector)) {
            texts.add(textOf(flow));
        }
        return texts;
    }

    /**
     * The text of each item's first paragraph, items in document order.
     */
    private static List<String> itemTexts(Node root) {
        List<String> texts = new ArrayList<>();
        for (Node item : root.lookupAll(".markdown-list-item")) {
            texts.add(textOf(textFlows(item, ".markdown-paragraph").get(0)));
        }
        return texts;
    }

    private static List<Node> itemRows(Node list) {
        List<Node> rows = new ArrayList<>();
        for (Node child : ((Parent) list).getChildrenUnmodifiable()) {
            if (child.getStyleClass().contains("markdown-list-item")) {
                rows.add(child);
            }
        }
        return rows;
    }

    /**
     * The vertical space between two nodes, one below the other, in scene coordinates.
     */
    private static double gap(Node upper, Node lower) {
        return lower.localToScene(lower.getLayoutBounds()).getMinY()
                - upper.localToScene(upper.getLayoutBounds()).getMaxY();
    }

    private static double sceneBaseline(Node node) {
        return node.localToScene(0, node.getLayoutBounds().getMinY() + node.getBaselineOffset()).getY();
    }

    /**
     * The number of lists a node sits in.
     */
    private static int listDepth(Node node) {
        int depth = 0;
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (isList(parent)) {
                depth++;
            }
        }
        return depth;
    }

    private static Node listAncestor(Node node) {
        Node parent = node.getParent();
        while (!isList(parent)) {
            parent = parent.getParent();
        }
        return parent;
    }

    private static boolean isList(Node node) {
        return node.getStyleClass().contains("markdown-bullet-list")
                || node.getStyleClass().contains("markdown-ordered-list");
    }

    private static double sceneX(Node node) {
        return node.localToScene(node.getLayoutBounds()).getMinX();
    }

    private static double sceneY(Node node) {
        return node.localToScene(node.getLayoutBounds()).getMinY();
    }

    private static List<String> markers(Node root) {
        List<String> markers = new ArrayList<>();
        for (Node marker : root.lookupAll(".markdown-list-marker")) {
            markers.add(((Labeled) marker).getText());
        }
        return markers;
    }

    private static boolean isBold(Font font) {
        return font.getStyle().toLowerCase(Locale.ROOT).contains("bold");
    }

    /**
     * Whether a narrow and a wide text of the same length are equally wide in the font.
     */
    private static boolean isMonospaced(Font font) {
        Text narrow = new Text("iiiiiiiiii");
        narrow.setFont(font);
        Text wide = new Text("MMMMMMMMMM");
        wide.setFont(font);
        return Math.abs(narrow.getLayoutBounds().getWidth() - wide.getLayoutBounds().getWidth()) < 0.5;
    }

    private static boolean hasBottomLine(TextFlow flow) {
        Border border = flow.getBorder();
        if (border == null) {
            return false;
        }
        for (BorderStroke stroke : border.getStrokes()) {
            if (stroke.getWidths().getBottom() > 0 && stroke.getBottomStroke() != Color.TRANSPARENT) {
                return true;
            }
        }
        return false;
    }

    /**
     * The link runs under a node, in document order.
     */
    private static List<Text> linkRuns(Node root) {
        List<Text> links = new ArrayList<>();
        for (Node node : root.lookupAll(".markdown-link")) {
            links.add((Text) node);
        }
        return links;
    }

    /**
     * The tooltip installed on a node: {@code Tooltip.install} keeps it in the node's properties under this key, the
     * only way to read it back from a node that is not a control.
     */
    private static Tooltip tooltip(Node node) {
        return (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
    }

    /**
     * Fires a mouse click at a node's middle, as JavaFX delivers one after a press and release on the node.
     */
    private static void click(Node node, MouseButton button, int clickCount) {
        Bounds bounds = node.localToScene(node.getLayoutBounds());
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_CLICKED, x, y, x, y, button, clickCount,
                false, false, false, false, button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY,
                false, false, true, null));
    }

    /**
     * Animation frames the test advances by hand, so a drag's scrolling runs on the test thread, step by step.
     */
    private static final class ManualFrames implements AnimationFrames {

        private DoubleConsumer step;

        @Override
        public void start(DoubleConsumer frameStep) {
            step = frameStep;
        }

        @Override
        public void stop() {
            step = null;
        }

        private boolean running() {
            return step != null;
        }

        /**
         * One frame, a given time after the last one.
         */
        private void advance(double seconds) {
            assertNotNull(step, "no frames running");
            step.accept(seconds);
        }
    }

    /**
     * A text node styled as if the pointer were over it: JavaFX sets {@code :hover} only from real mouse movement,
     * which a test without a window cannot make.
     */
    private static final class HoveredText extends Text {

        private HoveredText(String text) {
            super(text);
            pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
        }
    }

    /**
     * Asserts a run's text and its style classes: {@code markdown-text} and exactly the given ones.
     */
    private static void assertRun(Text run, String expectedText, String... expectedStyles) {
        assertEquals(expectedText, run.getText());
        Set<String> expectedClasses = new HashSet<>(List.of(expectedStyles));
        expectedClasses.add("markdown-text");
        assertEquals(expectedClasses, new HashSet<>(run.getStyleClass()), () -> "classes of run '" + expectedText + "'");
    }

    /**
     * Puts the view into a themed scene that is never shown, applies CSS and lays it out.
     */
    private static MarkdownView layOut(MarkdownView view) {
        StackPane root = new StackPane(view);
        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        ThemeManager.apply(scene);
        root.applyCss();
        root.layout();
        return view;
    }

    private static String manyParagraphs(int count) {
        StringBuilder markdown = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            markdown.append("## Section ").append(i).append("\n\nParagraph number ").append(i).append(".\n\n");
        }
        return markdown.toString();
    }

    private static List<TextFlow> textFlows(Node root, String selector) {
        List<TextFlow> flows = new ArrayList<>();
        for (Node node : root.lookupAll(selector)) {
            flows.add((TextFlow) node);
        }
        return flows;
    }

    /**
     * The text runs of a flow, in order: its {@code Text} children (a selection lays a layer of its own over them).
     */
    private static List<Text> runs(TextFlow flow) {
        List<Text> runs = new ArrayList<>();
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                runs.add(run);
            }
        }
        return runs;
    }

    private static String textOf(TextFlow flow) {
        StringBuilder text = new StringBuilder();
        for (Text run : runs(flow)) {
            text.append(run.getText());
        }
        return text.toString();
    }
}
