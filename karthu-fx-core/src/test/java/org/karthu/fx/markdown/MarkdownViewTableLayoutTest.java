package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The Markdown view comes to rest after layout: a document with many tables, in a themed scene that is never shown,
 * is laid out pass after pass as pulses would do it, until a pass requests no layout of any node and leaves none to
 * be laid out. A view that never comes to rest keeps the JavaFX Application Thread busy with one layout pass after
 * the other while nothing changes.
 *
 * <p>The scroll pane asks the document's height at two widths in one pass, with and without its vertical bar, so
 * a table whose height has a side effect on its columns would be laid out again in every pass.
 */
class MarkdownViewTableLayoutTest {

    /**
     * How many passes the view may take to come to rest: the first lays everything out, the second follows up on
     * what the first changed, the third finds nothing to do.
     */
    private static final int PASSES = 3;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @ParameterizedTest(name = "zoom {0}, zoom bar shown {1}")
    @DisplayName("a document with many tables, taller than the view, comes to rest within a few layout passes")
    @CsvSource({"1.0, true", "1.0, false", "1.25, true", "1.25, false"})
    void manyTablesComeToRest(double zoom, boolean zoomBar) {
        MarkdownView view = new MarkdownView();
        view.setZoomBarVisible(zoomBar);
        view.setMarkdown(documentWithTables());
        view.setZoom(zoom);
        StackPane root = new StackPane(view);
        Scene scene = new Scene(root, 1200, 800);
        ThemeManager.apply(scene);
        LayoutRequests requests = new LayoutRequests();
        List<String> passes = new ArrayList<>();
        for (int pass = 1; pass <= PASSES; pass++) {
            requests.watch(root);
            requests.clear();
            root.applyCss();
            root.layout();
            Map<String, Integer> dirty = new TreeMap<>();
            collectDirty(root, dirty);
            if (requests.count() == 0 && dirty.isEmpty()) {
                return;
            }
            passes.add("pass " + pass + ": " + requests.count() + " layout requests, still to be laid out " + dirty);
        }
        fail("the view never comes to rest within " + PASSES + " passes:\n" + String.join("\n", passes));
    }

    /**
     * Counts the layout requests of every node watched: each time a node's {@code needsLayout} becomes true.
     */
    private static final class LayoutRequests {

        private final Set<Parent> watched = Collections.newSetFromMap(new IdentityHashMap<>());
        private int count;

        /**
         * Watches every node under a root that is not watched yet, the root too.
         */
        void watch(Node node) {
            if (node instanceof Parent parent) {
                if (watched.add(parent)) {
                    parent.needsLayoutProperty().addListener((observable, was, now) -> {
                        if (now) {
                            count++;
                        }
                    });
                }
                for (Node child : parent.getChildrenUnmodifiable()) {
                    watch(child);
                }
            }
        }

        void clear() {
            count = 0;
        }

        int count() {
            return count;
        }
    }

    /**
     * Counts the nodes under a root that still need a layout, by their class names.
     */
    private static void collectDirty(Node node, Map<String, Integer> dirty) {
        if (node instanceof Parent parent) {
            if (parent.isNeedsLayout()) {
                dirty.merge(parent.getClass().getName(), 1, Integer::sum);
            }
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectDirty(child, dirty);
            }
        }
    }

    // --- A made-up document ---------------------------------------------------------------------------------------

    private static final String[] WORDS = {"engine", "table", "column", "value", "layout", "pulse", "width",
        "height", "render", "thread", "scene", "graph", "node", "bundle", "module", "layer", "port", "flow", "data",
        "chunk", "cache", "state", "row", "cell", "header", "theme", "font", "padding", "border", "reader", "index"};

    /**
     * A document of 20 sections, each a heading, a paragraph or two and a table of 3 to 6 columns and 2 to 15 rows:
     * code names in the first column, then words, numbers and now and then a cell long enough to wrap; the columns
     * aligned left, centred and right.
     */
    private static String documentWithTables() {
        Random random = new Random(42);
        StringBuilder text = new StringBuilder("# A document with tables\n\n");
        text.append(paragraph(random)).append("\n\n");
        for (int t = 0; t < 20; t++) {
            text.append("## Section ").append(t + 1).append("\n\n");
            text.append(paragraph(random)).append("\n\n");
            if (t % 3 == 0) {
                text.append(paragraph(random)).append("\n\n");
            }
            int columns = 3 + t % 4;
            int rows = 2 + (t * 5) % 14;
            StringBuilder header = new StringBuilder("|");
            StringBuilder delimiter = new StringBuilder("|");
            for (int c = 0; c < columns; c++) {
                header.append(' ').append(words(random, 1 + random.nextInt(3))).append(" |");
                delimiter.append(c == 0 ? " --- |" : (c % 3 == 1 ? " :---: |" : " ---: |"));
            }
            text.append(header).append('\n').append(delimiter).append('\n');
            for (int r = 0; r < rows; r++) {
                text.append('|');
                for (int c = 0; c < columns; c++) {
                    String cell;
                    if (c == 0) {
                        cell = "`" + WORDS[random.nextInt(WORDS.length)] + "-" + r + "`";
                    } else if (random.nextInt(5) == 0) {
                        cell = words(random, 12 + random.nextInt(14));
                    } else if (random.nextInt(3) == 0) {
                        cell = String.valueOf(random.nextInt(100000));
                    } else {
                        cell = words(random, 1 + random.nextInt(5));
                    }
                    text.append(' ').append(cell).append(" |");
                }
                text.append('\n');
            }
            text.append('\n');
        }
        text.append(paragraph(random)).append('\n');
        return text.toString();
    }

    private static String paragraph(Random random) {
        String text = words(random, 40 + random.nextInt(40));
        return Character.toUpperCase(text.charAt(0)) + text.substring(1) + ".";
    }

    /**
     * Words, now and then one in bold or as code.
     */
    private static String words(Random random, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                text.append(' ');
            }
            String word = WORDS[random.nextInt(WORDS.length)];
            int style = random.nextInt(25);
            if (style == 0) {
                text.append("**").append(word).append("**");
            } else if (style == 1) {
                text.append('`').append(word).append('`');
            } else {
                text.append(word);
            }
        }
        return text.toString();
    }
}
