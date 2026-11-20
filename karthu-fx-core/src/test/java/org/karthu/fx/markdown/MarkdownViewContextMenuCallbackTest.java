package org.karthu.fx.markdown;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.PickResult;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.textFlows;

/**
 * The Markdown view's context menu callback seen from outside: the menu is requested by a context menu event fired at
 * a node of the view, as a right click does, and its entries are read from the view's scroll pane, which would show it.
 * A filter on the scroll pane consumes the request after the view has prepared the menu, so no menu is ever shown.
 *
 * <p>The view sits in a themed scene that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewContextMenuCallbackTest {

    /**
     * A document with every kind of block that has a line of its own: a heading on line 1, a paragraph on line 3, a
     * list item on line 5 (marker alone, its text on line 6), a table with its header on line 8 and its rows on lines
     * 10 and 11, and a code block with its fence on line 13 and its code lines on lines 14 to 16.
     */
    private static final String DOCUMENT = "# Title\n\nA paragraph.\n\n-\n  item text\n\n"
            + "| Name | Value |\n|---|---|\n| Row 0 | 00 |\n| Row 1 | 01 |\n\n"
            + "```\ncode line 0\ncode line 1\ncode line 2\n```\n";

    private static final int CODE_LINE_LENGTH = "code line 0\n".length();

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("without a callback the menu holds the built-in entries alone")
    void withoutCallback() {
        MarkdownView view = layOut(new MarkdownView(DOCUMENT));

        assertEquals(null, view.getContextMenuCallback());
        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(requestAt(view, paragraphs(view).get(0))));
        assertEquals(List.of("Copy", "Copy with header", "Select column", "-", "Copy Markdown"),
                menuTexts(requestAt(view, row(view, 0))));
    }

    @Test
    @DisplayName("the callback's entries show after the built-in ones, anew on every opening")
    void addedEntries() {
        MarkdownView view = layOut(new MarkdownView(DOCUMENT));
        view.setContextMenuCallback(new ContextMenuCallback() {
            @Override
            public void contextMenuRequested(ObservableList<MenuItem> items, ContextMenuPlace place) {
                items.add(new SeparatorMenuItem());
                items.add(new MenuItem("Open in Editor"));
            }
        });

        assertEquals(List.of("Copy", "-", "Copy Markdown", "-", "Open in Editor"),
                menuTexts(requestAt(view, paragraphs(view).get(0))));
        assertEquals(List.of("Copy", "Copy with header", "Select column", "-", "Copy Markdown", "-", "Open in Editor"),
                menuTexts(requestAt(view, row(view, 0))), "a second opening, on a table");

        view.setContextMenuCallback(null);
        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(requestAt(view, paragraphs(view).get(0))),
                "the callback removed");
    }

    @Test
    @DisplayName("the callback may remove and reorder the built-in entries")
    void reorderedEntries() {
        MarkdownView view = layOut(new MarkdownView(DOCUMENT));
        view.setContextMenuCallback(new ContextMenuCallback() {
            @Override
            public void contextMenuRequested(ObservableList<MenuItem> items, ContextMenuPlace place) {
                MenuItem last = items.removeLast();
                items.removeLast();
                items.addFirst(last);
            }
        });

        assertEquals(List.of("Copy Markdown", "Copy"), menuTexts(requestAt(view, paragraphs(view).get(0))));
        view.setContextMenuCallback(null);
        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(requestAt(view, paragraphs(view).get(0))));
    }

    @Test
    @DisplayName("the place carries the line of the heading, paragraph, list item, table row or code line clicked")
    void placeLines() {
        MarkdownView view = layOut(new MarkdownView(DOCUMENT));
        List<ContextMenuPlace> places = recordPlaces(view);
        Label marker = (Label) view.lookup(".markdown-list-marker");
        TextFlow code = codeFlow(view);

        requestAt(view, textFlows(view, ".markdown-heading").get(0));
        requestAt(view, paragraphs(view).get(0));
        requestAt(view, marker);
        requestAt(view, paragraphs(view).get(1));
        requestAt(view, row(view, -1));
        requestAt(view, row(view, 1));
        requestAt(view, code, sceneCharacterBounds(code, CODE_LINE_LENGTH * 2).getCenterY());

        assertEquals(List.of(OptionalInt.of(1), OptionalInt.of(3), OptionalInt.of(5), OptionalInt.of(6),
                OptionalInt.of(8), OptionalInt.of(11), OptionalInt.of(16)), lines(places));
    }

    @Test
    @DisplayName("the place has no line where the click hit no block, as in the margins")
    void placeWithoutBlock() {
        MarkdownView view = layOut(new MarkdownView(DOCUMENT));
        view.setLeftMargin(80);
        List<ContextMenuPlace> places = recordPlaces(view);
        Node document = view.lookup(".markdown-document");

        requestAt(view, document, sceneBounds(paragraphs(view).get(0)).getCenterY());

        assertEquals(List.of(OptionalInt.empty()), lines(places));
    }

    @Test
    @DisplayName("a place is made with a line or without one, never with null")
    void placeValues() {
        assertEquals(OptionalInt.of(7), new ContextMenuPlace(OptionalInt.of(7)).sourceLine());
        assertEquals(new ContextMenuPlace(OptionalInt.empty()), new ContextMenuPlace(OptionalInt.empty()));
        assertThrows(NullPointerException.class, () -> new ContextMenuPlace(null));
    }

    @Test
    @DisplayName("the callback works on the menu's own entry list")
    void ownList() {
        MarkdownView view = layOut(new MarkdownView(DOCUMENT));
        List<ObservableList<MenuItem>> lists = new ArrayList<>();
        view.setContextMenuCallback(new ContextMenuCallback() {
            @Override
            public void contextMenuRequested(ObservableList<MenuItem> items, ContextMenuPlace place) {
                lists.add(items);
            }
        });

        requestAt(view, paragraphs(view).get(0));

        assertSame(scrollPane(view).getContextMenu().getItems(), lists.getFirst());
    }

    // --- Requests --------------------------------------------------------------------------------------------------

    /**
     * Requests the context menu at a node's centre, as a right click there does.
     */
    private static List<MenuItem> requestAt(MarkdownView view, Node target) {
        return requestAt(view, target, sceneBounds(target).getCenterY());
    }

    /**
     * Requests the context menu at a node, at a height in the scene, as a right click there does, and returns the
     * entries the scroll pane would show.
     */
    private static List<MenuItem> requestAt(MarkdownView view, Node target, double sceneY) {
        ScrollPane pane = scrollPane(view);
        EventHandler<ContextMenuEvent> noShow = new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                event.consume();
            }
        };
        double sceneX = sceneBounds(target).getCenterX();
        pane.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, noShow);
        try {
            Event.fireEvent(target, new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, sceneX, sceneY,
                    sceneX, sceneY, false, new PickResult(target, sceneX, sceneY)));
        } finally {
            pane.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, noShow);
        }
        return new ArrayList<>(pane.getContextMenu().getItems());
    }

    private static List<ContextMenuPlace> recordPlaces(MarkdownView view) {
        List<ContextMenuPlace> places = new ArrayList<>();
        view.setContextMenuCallback(new ContextMenuCallback() {
            @Override
            public void contextMenuRequested(ObservableList<MenuItem> items, ContextMenuPlace place) {
                places.add(place);
            }
        });
        return places;
    }

    private static List<OptionalInt> lines(List<ContextMenuPlace> places) {
        List<OptionalInt> lines = new ArrayList<>();
        for (ContextMenuPlace place : places) {
            lines.add(place.sourceLine());
        }
        return lines;
    }

    private static List<String> menuTexts(List<MenuItem> items) {
        List<String> texts = new ArrayList<>();
        for (MenuItem item : items) {
            texts.add(item instanceof SeparatorMenuItem ? "-" : item.getText());
        }
        return texts;
    }

    // --- The view's parts ------------------------------------------------------------------------------------------

    private static TextFlow codeFlow(MarkdownView view) {
        Node block = view.lookup(".markdown-code-block");
        assertNotNull(block, "code block");
        return (TextFlow) block.lookup(".markdown-block-content");
    }

    /**
     * The table's row at a shown index, as the table lays it out; -1 for the header row.
     */
    private static Node row(MarkdownView view, int index) {
        TableView<?> table = (TableView<?>) view.lookup(".markdown-table");
        if (index < 0) {
            Node header = table.lookup(".column-header");
            assertNotNull(header, "column header");
            return header;
        }
        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row && row.getIndex() == index && row.isVisible()) {
                return row;
            }
        }
        throw new AssertionError("no row " + index);
    }
}
