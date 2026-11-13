package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;

/**
 * Images in table cells seen from outside: an image icon followed by the alt text, runs of the cell's text styled like
 * a link, never a picture, so the table stays compact and its columns keep the widths text gives them.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown, its images' work runs at once on the test's thread.
 */
class MarkdownViewTableImagesTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("an image in a cell shows as an image icon followed by its alt text, never as a picture, and is not"
            + " loaded")
    void iconAndAltText() {
        List<String> loaded = new ArrayList<>();
        MarkdownView view = viewRecordingLoads(loaded);
        view.setMarkdown("| Icon | Name |\n|---|---|\n| ![The logo](logo.png \"Our logo\") | x |");
        layOut(view);

        Node table = view.lookup(".markdown-table");
        assertNull(table.lookup(".markdown-image"), "no picture");
        assertNull(table.lookup(".markdown-image-inline"), "no inline image");
        assertNull(table.lookup(".markdown-image-placeholder"), "no placeholder");
        List<Node> parts = new ArrayList<>(table.lookupAll(".markdown-image-cell"));
        assertEquals(2, parts.size(), () -> "the icon and the alt text: " + parts);
        FontIcon icon = (FontIcon) parts.get(0);
        Text alt = (Text) parts.get(1);
        assertEquals(BootstrapIcons.IMAGE, icon.getIconCode(), "an image icon");
        assertEquals("The logo", alt.getText().replace('\u00a0', ' ').strip(), "the alt text");
        assertTrue(alt.getStyleClass().contains("markdown-text"), "a run of the cell's text");
        assertTrue(sceneBounds(icon).getMaxX() <= sceneBounds(alt).getMinX() + 0.5, "the icon first");
        assertTrue(sceneBounds(icon).getMaxY() > sceneBounds(alt).getMinY()
                && sceneBounds(icon).getMinY() < sceneBounds(alt).getMaxY(), "on one line");
        assertEquals("Our logo", tooltip(alt).getText(), "the title as the tooltip");
        assertEquals("Our logo", tooltip(icon).getText(), "the title as the tooltip");
        assertEquals(List.of(), loaded, "a cell's image is not loaded");
    }

    @Test
    @DisplayName("the icon and the alt text are styled like a link, in a quote too")
    void styledLikeALink() {
        MarkdownView view = viewRecordingLoads(new ArrayList<>());
        view.setMarkdown("| Icon |\n|---|\n| ![The logo](logo.png) |\n\n> | Icon |\n> |---|\n> | ![Quoted](q.png) |");
        layOut(view);

        Paint accent = themeColor(view, "-fx-accent-color");
        List<Node> parts = new ArrayList<>(view.lookupAll(".markdown-image-cell"));
        assertEquals(4, parts.size());
        for (Node part : parts) {
            assertEquals(accent, ((Text) part).getFill(), () -> "the accent colour: " + part);
        }
    }

    @Test
    @DisplayName("an image in a cell inside a link follows the link, its icon and its alt text")
    void linkedImageInACell() {
        MarkdownView view = viewRecordingLoads(new ArrayList<>());
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown("| Icon |\n|---|\n| [![The logo](logo.png)](https://example.com/home) |");
        layOut(view);

        List<Node> parts = new ArrayList<>(view.lookupAll(".markdown-image-cell"));
        for (Node part : parts) {
            assertTrue(part.getStyleClass().contains("markdown-link"), () -> "a link's run: " + part);
            click(part);
        }
        assertEquals(List.of("https://example.com/home", "https://example.com/home"), followed);
    }

    @Test
    @DisplayName("the column widths treat an image in a cell as text: the column holds the icon and the alt text")
    void columnWidthsTreatItAsText() {
        MarkdownView view = viewRecordingLoads(new ArrayList<>());
        view.setMarkdown("| I | Text |\n|---|---|\n| ![Averyveryverylongword](logo.png) | "
                + "words that wrap in a narrow column ".repeat(6) + "|");
        layOut(view);

        TableView<?> table = (TableView<?>) view.lookup(".markdown-table");
        List<Node> parts = new ArrayList<>(table.lookupAll(".markdown-image-cell"));
        double needed = parts.get(0).getLayoutBounds().getWidth() + parts.get(1).getLayoutBounds().getWidth();
        assertTrue(table.getColumns().get(0).getWidth() >= needed, () -> "the column holds the icon and the alt"
                + " text: " + table.getColumns().get(0).getWidth() + " for " + needed);
        Bounds cell = sceneBounds(parts.get(1).getParent());
        assertTrue(sceneBounds(parts.get(1)).getMaxX() <= cell.getMaxX() + 0.5, "the alt text is not cut off");
    }

    @Test
    @DisplayName("a column of images sorts by their alt texts, and an image in a header names its column by its alt text")
    void sortsByAltText() {
        MarkdownView view = viewRecordingLoads(new ArrayList<>());
        view.setMarkdown("| ![Icon](h.png) |\n|---|\n| ![b](a.png) |\n| ![c](b.png) |\n| ![a](c.png) |");
        layOut(view);
        TableView<?> table = (TableView<?>) view.lookup(".markdown-table");
        assertEquals("Icon", table.getColumns().get(0).getText(), "the header's alt text");

        clickHeader(table);

        assertEquals(List.of("a", "b", "c"), shownAltTexts(table), "sorted by the alt texts, not the addresses");
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * A view whose images' work runs at once, with a loader that notes each address it is asked for and has no file,
     * and a window switch that opens nothing.
     */
    private static MarkdownView viewRecordingLoads(List<String> loaded) {
        MarkdownView view = new MarkdownView();
        MarkdownTestSupport.openNoWindow(view);
        view.setImageWork(new ImmediateWork());
        view.setOutputScale(1.0);
        view.setImageLoader(address -> {
            loaded.add(address);
            throw new IOException("No test image " + address);
        });
        return view;
    }

    /**
     * The alt texts of the images in the table's first column, in the rows as shown, top to bottom.
     */
    private static List<String> shownAltTexts(TableView<?> table) {
        List<TableRow<?>> rows = new ArrayList<>();
        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row && !row.isEmpty() && row.getIndex() >= 0 && row.isVisible()) {
                rows.add(row);
            }
        }
        rows.sort((first, second) -> Integer.compare(first.getIndex(), second.getIndex()));
        List<String> alts = new ArrayList<>();
        TableColumn<?, ?> column = table.getColumns().get(0);
        for (TableRow<?> row : rows) {
            for (Node node : row.lookupAll(".table-cell")) {
                if (node instanceof TableCell<?, ?> cell && cell.getTableColumn() == column) {
                    for (Node part : ((TextFlow) cell.getGraphic()).getChildren()) {
                        if (part.getStyleClass().contains("markdown-image-cell") && !(part instanceof FontIcon)) {
                            alts.add(((Text) part).getText().replace('\u00a0', ' ').strip());
                        }
                    }
                }
            }
        }
        return alts;
    }

    /**
     * Clicks the first column's header with the primary button, as the reader does to sort, on its first run, then
     * lays the view out again.
     */
    private static void clickHeader(TableView<?> table) {
        Node run = ((TextFlow) table.getColumns().get(0).getGraphic()).getChildren().get(0);
        Bounds bounds = sceneBounds(run);
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        Event.fireEvent(run, mouse(MouseEvent.MOUSE_PRESSED, x, y));
        Event.fireEvent(run, mouse(MouseEvent.MOUSE_RELEASED, x, y));
        Event.fireEvent(run, mouse(MouseEvent.MOUSE_CLICKED, x, y));
        table.getScene().getRoot().applyCss();
        table.getScene().getRoot().layout();
    }

    /**
     * Fires a still click with the primary button at a node's middle.
     */
    private static void click(Node node) {
        Bounds bounds = sceneBounds(node);
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_CLICKED, bounds.getCenterX(), bounds.getCenterY()));
    }

    /**
     * A mouse event of the primary button at a scene point, still since the press.
     */
    private static MouseEvent mouse(EventType<MouseEvent> type, double x, double y) {
        boolean down = type == MouseEvent.MOUSE_PRESSED;
        return new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false, false, down, false,
                false, false, false, true, null);
    }

    /**
     * A colour token of the theme, resolved in the view's scene.
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
     * The tooltip installed on a node: {@code Tooltip.install} keeps it in the node's properties under this key, the
     * one place JavaFX keeps it.
     */
    private static Tooltip tooltip(Node node) {
        return (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
    }
}
