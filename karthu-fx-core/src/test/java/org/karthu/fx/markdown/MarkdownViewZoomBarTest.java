package org.karthu.fx.markdown;

import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.elsewhere;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.manyParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;

/**
 * The Markdown view's zoom bar seen from outside: its parts found by their documented style classes and operated as a
 * user does, through events on them.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; the tests apply CSS and lay it out themselves. No
 * window opens, so the list of zoom levels never shows as a popup: the tests operate the list the bar opens through
 * the bar's seam for tests.
 */
class MarkdownViewZoomBarTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    // --- The bar ---------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("the bar is visible by default, on top of the view and outside its scroll pane, a 1px line in the"
            + " control border colour below it")
    void barOnTopWithALine() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(30)));
        Region bar = bar(view);

        assertTrue(view.isZoomBarVisible());
        assertTrue(bar.isVisible());
        assertFalse(isInside(bar, scrollPane(view)), "outside the scroll pane");
        Bounds barBounds = sceneBounds(bar);
        Bounds viewBounds = sceneBounds(view);
        assertEquals(viewBounds.getMinY() + view.snappedTopInset(), barBounds.getMinY(), 0.01, "at the top");
        assertEquals(viewBounds.getMinX() + view.snappedLeftInset(), barBounds.getMinX(), 0.01);
        assertEquals(viewBounds.getMaxX() - view.snappedRightInset(), barBounds.getMaxX(), 0.01, "the view's width");
        assertEquals(barBounds.getMaxY(), sceneBounds(scrollPane(view)).getMinY(), 0.01, "the document below it");
        assertNotNull(bar.getBorder(), "a line below the bar");
        assertEquals(1, bar.getBorder().getStrokes().getFirst().getWidths().getBottom(), 0.01);
        assertEquals(0, bar.getBorder().getStrokes().getFirst().getWidths().getTop(), 0.01);
        assertEquals(tokenColor(view, "-fx-control-border"), bar.getBorder().getStrokes().getFirst().getBottomStroke());

        scrollPane(view).setVvalue(scrollPane(view).getVmax());
        relayOut(view);

        assertEquals(barBounds, sceneBounds(bar), "the bar does not scroll");
    }

    @Test
    @DisplayName("the bar has the tool bar's density: 4px padding and spacing, from the left the zoom group, the slider"
            + " and Reset, as high as a text field")
    void barDensityAndOrder() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Region bar = bar(view);
        Bounds group = sceneBounds(part(view, "markdown-zoom-group"));
        Bounds slider = sceneBounds(part(view, "markdown-zoom-slider"));
        Bounds reset = sceneBounds(part(view, "markdown-zoom-reset"));

        assertEquals(4, bar.getPadding().getTop(), 0.01);
        assertEquals(4, bar.getPadding().getLeft(), 0.01);
        assertEquals(4, bar.getPadding().getBottom(), 0.01);
        assertEquals(sceneBounds(bar).getMinX() + 4, group.getMinX(), 0.01);
        assertEquals(group.getMaxX() + 4, slider.getMinX(), 0.01, "4px apart");
        assertEquals(slider.getMaxX() + 4, reset.getMinX(), 0.01, "4px apart");
        TextField textField = new TextField("100%");
        ((StackPane) view.getParent()).getChildren().add(textField);
        relayOut(view);
        assertEquals(textField.prefHeight(-1), group.getHeight(), 0.5, "the group as high as a text field");
        assertEquals(textField.prefHeight(-1), reset.getHeight(), 0.5, "Reset as high as a text field");
        ((StackPane) view.getParent()).getChildren().remove(textField);
    }

    @Test
    @DisplayName("hidden, the bar is neither shown nor laid out: the document fills the view; the preferred height"
            + " includes the bar while it is visible")
    void hiddenBar() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(30)));
        Region bar = bar(view);
        double barHeight = bar.getHeight();
        double framed = view.snappedTopInset() + 360 + view.snappedBottomInset();

        assertTrue(barHeight > 20, () -> "a bar of a control's height: " + barHeight);
        assertEquals(framed + barHeight, view.prefHeight(-1), 0.5);

        view.setZoomBarVisible(false);
        relayOut(view);

        assertFalse(bar.isVisible());
        assertFalse(bar.isManaged());
        assertEquals(sceneBounds(view).getMinY() + view.snappedTopInset(), sceneBounds(scrollPane(view)).getMinY(),
                0.01, "the document at the top");
        assertEquals(framed, view.prefHeight(-1), 0.5);

        view.setZoomBarVisible(true);
        relayOut(view);

        assertTrue(bar.isVisible());
        assertEquals(sceneBounds(bar).getMaxY(), sceneBounds(scrollPane(view)).getMinY(), 0.01);
    }

    // --- Minus, plus and Reset ------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{1} from {0}% gives {2}%")
    @DisplayName("minus and plus go to the next preset in their direction, also from a value in between")
    @CsvSource({
            "100, plus, 125",
            "100, minus, 75",
            "130, plus, 150",
            "130, minus, 125",
            "75, minus, 50",
            "150, plus, 200",
            "51, minus, 50",
            "199, plus, 200",
            "50, plus, 75",
            "200, minus, 150"
    })
    void minusAndPlusGoToTheNextPreset(int from, String button, int expected) {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(from / 100.0);

        click(part(view, button.equals("plus") ? "markdown-zoom-in" : "markdown-zoom-out"));

        assertEquals(expected / 100.0, view.getZoom());
        assertEquals(expected + "%", field(view).getText());
    }

    @Test
    @DisplayName("minus is disabled at 50 %, plus at 200 %, and a click on them then does nothing")
    void minusAndPlusAtTheLimits() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Button zoomOut = (Button) part(view, "markdown-zoom-out");
        Button zoomIn = (Button) part(view, "markdown-zoom-in");

        assertFalse(zoomOut.isDisabled());
        assertFalse(zoomIn.isDisabled());

        view.setZoom(0.5);
        assertTrue(zoomOut.isDisabled());
        assertFalse(zoomIn.isDisabled());
        click(zoomOut);
        assertEquals(0.5, view.getZoom());

        view.setZoom(2.0);
        assertFalse(zoomOut.isDisabled());
        assertTrue(zoomIn.isDisabled());
        click(zoomIn);
        assertEquals(2.0, view.getZoom());

        view.setZoom(1.99);
        assertFalse(zoomIn.isDisabled());
    }

    @Test
    @DisplayName("at the limits only the icon of minus or plus is dimmed: the group's dividers, their borders, keep full"
            + " contrast")
    void dividersKeepFullContrastAtTheLimits() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Button zoomOut = (Button) part(view, "markdown-zoom-out");
        Button zoomIn = (Button) part(view, "markdown-zoom-in");
        Paint border = tokenColor(view, "-fx-control-border");

        view.setZoom(0.5);
        relayOut(view);

        assertTrue(zoomOut.isDisabled());
        assertEquals(1.0, zoomOut.getOpacity(), "minus, whose border is the divider, at full contrast");
        assertEquals(border, zoomOut.getBorder().getStrokes().getFirst().getRightStroke(), "the divider's colour");
        assertEquals(0.5, zoomOut.getGraphic().getOpacity(), "its icon dimmed as a disabled icon button's");
        assertEquals(1.0, zoomIn.getGraphic().getOpacity(), "plus enabled, its icon not dimmed");

        view.setZoom(2.0);
        relayOut(view);

        assertTrue(zoomIn.isDisabled());
        assertEquals(1.0, zoomIn.getOpacity(), "plus, whose border is the divider, at full contrast");
        assertEquals(border, zoomIn.getBorder().getStrokes().getFirst().getLeftStroke(), "the divider's colour");
        assertEquals(0.5, zoomIn.getGraphic().getOpacity(), "its icon dimmed as a disabled icon button's");
        assertEquals(1.0, zoomOut.getGraphic().getOpacity(), "minus enabled again, its icon not dimmed");
    }

    @Test
    @DisplayName("Reset sets 100 % and is disabled at 100 %")
    void resetSetsTheNormalSize() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Button reset = (Button) part(view, "markdown-zoom-reset");

        assertTrue(reset.isDisabled());

        view.setZoom(1.37);
        assertFalse(reset.isDisabled());
        click(reset);

        assertEquals(1.0, view.getZoom());
        assertTrue(reset.isDisabled());
        assertEquals("100%", field(view).getText());
    }

    @Test
    @DisplayName("plus keeps the line at the top of the visible area at the top")
    void plusKeepsTheTopLine() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(20)));
        ScrollPane scrollPane = scrollPane(view);
        scrollPane.setVvalue(0.5);
        relayOut(view);
        double top = sceneBounds(scrollPane.lookup(".viewport")).getMinY();
        Node atTop = paragraphAt(view, top + 1);

        click(part(view, "markdown-zoom-in"));
        relayOut(view);

        assertEquals(1.25, view.getZoom());
        Bounds after = sceneBounds(atTop);
        assertTrue(after.getMinY() <= top + 1 && after.getMaxY() > top + 1, () -> "still at the top: " + after);
    }

    // --- The percent field -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("the field shows the zoom as whole percent followed by % without a space, and is as wide as 200%")
    void fieldShowsThePercent() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        TextField field = field(view);
        double width = field.getWidth();

        assertEquals("100%", field.getText());
        view.setZoom(1.25);
        assertEquals("125%", field.getText());
        view.setZoom(0.5);
        relayOut(view);
        assertEquals("50%", field.getText());
        assertEquals(width, field.getWidth(), 0.01, "the same width for every zoom");

        Text widest = new Text("200%");
        widest.setFont(field.getFont());
        double room = field.getWidth() - field.snappedLeftInset() - field.snappedRightInset();
        assertTrue(widest.getLayoutBounds().getWidth() <= room, () -> "200% fits: " + room);
        assertTrue(widest.getLayoutBounds().getWidth() + 4 > room, () -> "not wider than 200%: " + room);
    }

    @ParameterizedTest(name = "\"{0}\" gives {1}%")
    @DisplayName("a typed value is taken on Enter: a decimal number with a dot or a comma, perhaps a % after spaces,"
            + " rounded half up and limited to 50 to 200; anything else brings back the zoom (137 %)")
    @CsvSource(value = {
            "130| 130",
            "130%| 130",
            "130 %| 130",
            " 80 | 80",
            "99.5| 100",
            "99,5| 100",
            "99.4| 99",
            "49| 50",
            "201| 200",
            "-5| 50",
            "abc| 137",
            "''| 137",
            "%| 137",
            "1e2| 137",
            "13 0| 137"
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void typedValueOnEnter(String typed, String expected) {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.37);
        TextField field = field(view);
        int percent = Integer.parseInt(expected.strip());

        typeInto(field, typed);
        assertEquals(1.37, view.getZoom(), "typing changes no zoom");
        Event.fireEvent(field, key(KeyCode.ENTER));

        assertEquals(percent / 100.0, view.getZoom());
        assertEquals(percent + "%", field.getText());
    }

    @Test
    @DisplayName("a typed value is taken when the focus leaves the field")
    void typedValueOnFocusLoss() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Button elsewhere = elsewhere(view);
        TextField field = field(view);

        typeInto(field, "150");
        assertEquals(1.0, view.getZoom());
        elsewhere.requestFocus();

        assertEquals(1.5, view.getZoom());
        assertEquals("150%", field.getText());
    }

    @Test
    @DisplayName("Escape brings back the zoom without taking the text; without an edit it is not the field's")
    void escapeAbandonsTheEdit() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.37);
        TextField field = field(view);
        List<KeyEvent> passed = new ArrayList<>();
        view.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, passed::add);

        typeInto(field, "150");
        Event.fireEvent(field, key(KeyCode.ESCAPE));

        assertEquals(1.37, view.getZoom());
        assertEquals("137%", field.getText());
        assertTrue(passed.isEmpty(), "consumed: it abandoned an edit");

        Event.fireEvent(field, key(KeyCode.ESCAPE));
        assertEquals(1, passed.size(), "nothing to abandon: Escape goes on, as to a dialog");
    }

    @Test
    @DisplayName("a zoom the application sets during an edit leaves the typed text alone until it is taken or"
            + " abandoned")
    void applicationZoomDuringAnEdit() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        TextField field = field(view);
        Slider slider = (Slider) part(view, "markdown-zoom-slider");

        typeInto(field, "150");
        view.setZoom(1.8);

        assertEquals("150", field.getText());
        assertEquals(180, slider.getValue(), 1e-9, "the other parts show the zoom");
        Event.fireEvent(field, key(KeyCode.ENTER));
        assertEquals(1.5, view.getZoom());

        typeInto(field, "90");
        view.setZoom(1.8);
        Event.fireEvent(field, key(KeyCode.ESCAPE));
        assertEquals(1.8, view.getZoom());
        assertEquals("180%", field.getText(), "abandoned: the zoom the application set");
    }

    @Test
    @DisplayName("a part of the bar the user operates during an edit shows its zoom in the field at once")
    void barDuringAnEdit() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        TextField field = field(view);

        typeInto(field, "150");
        click(part(view, "markdown-zoom-in"));

        assertEquals(1.25, view.getZoom(), "plus steps from the zoom, not from the typed text");
        assertEquals("125%", field.getText());
    }

    @ParameterizedTest(name = "{1} from {0}% gives {2}%")
    @DisplayName("Up and Down in the field go to the next larger or smaller preset at once, also from a value in"
            + " between, and are the field's at the ends too")
    @CsvSource({
            "100, UP, 125",
            "100, DOWN, 75",
            "130, UP, 150",
            "130, DOWN, 125",
            "199, UP, 200",
            "200, UP, 200",
            "50, DOWN, 50"
    })
    void upAndDownStep(int from, KeyCode code, int expected) {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(from / 100.0);
        List<KeyEvent> passed = new ArrayList<>();
        view.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, passed::add);

        Event.fireEvent(field(view), key(code));

        assertEquals(expected / 100.0, view.getZoom());
        assertEquals(expected + "%", field(view).getText());
        assertTrue(passed.isEmpty(), "consumed");
    }

    // --- The list of zoom levels ------------------------------------------------------------------------------------

    @Test
    @DisplayName("the chevron opens the list of presets, the zoom selected, and leaves the focus where it is; the group"
            + " shows the focus border while it is open; a second click closes it")
    void chevronOpensTheList() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.25);
        Button elsewhere = elsewhere(view);
        elsewhere.requestFocus();
        Node group = part(view, "markdown-zoom-group");
        ListView<Integer> list = levels(view);

        assertFalse(hasPseudoClass(group, "focus-within"));
        click(part(view, "markdown-zoom-levels"));

        assertTrue(hasPseudoClass(group, "focus-within"), "the focus border while the list is open");
        assertEquals(List.of(50, 75, 100, 125, 150, 200), list.getItems());
        assertEquals(125, list.getSelectionModel().getSelectedItem());
        assertSame(elsewhere, view.getScene().getFocusOwner(), "the focus stays where it was");

        click(part(view, "markdown-zoom-levels"));

        assertFalse(hasPseudoClass(group, "focus-within"), "closed");
        assertEquals(1.25, view.getZoom());
    }

    @Test
    @DisplayName("a zoom in between marks no preset in the list")
    void valueInBetweenMarksNone() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.3);

        click(part(view, "markdown-zoom-levels"));

        assertTrue(levels(view).getSelectionModel().isEmpty());
    }

    @Test
    @DisplayName("Alt+Down in the field opens the list, consumed; Down alone steps")
    void altDownOpensTheList() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Node group = part(view, "markdown-zoom-group");
        List<KeyEvent> passed = new ArrayList<>();
        view.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, passed::add);

        Event.fireEvent(field(view), altKey(KeyCode.DOWN));

        assertTrue(hasPseudoClass(group, "focus-within"), "open");
        assertTrue(passed.isEmpty(), "consumed");
        assertEquals(100, levels(view).getSelectionModel().getSelectedItem());
        assertEquals(1.0, view.getZoom());
    }

    @Test
    @DisplayName("in the open list the arrow keys move without zooming, Enter chooses and closes the list")
    void arrowsMoveEnterChooses() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.25);
        ListView<Integer> list = levels(view);
        click(part(view, "markdown-zoom-levels"));

        Event.fireEvent(list, key(KeyCode.DOWN));
        assertEquals(150, list.getSelectionModel().getSelectedItem());
        Event.fireEvent(list, key(KeyCode.DOWN));
        Event.fireEvent(list, key(KeyCode.DOWN));
        assertEquals(200, list.getSelectionModel().getSelectedItem(), "stays at the last preset");
        Event.fireEvent(list, key(KeyCode.UP));
        assertEquals(150, list.getSelectionModel().getSelectedItem());
        assertEquals(1.25, view.getZoom(), "moving changes no zoom");

        Event.fireEvent(list, key(KeyCode.ENTER));

        assertEquals(1.5, view.getZoom());
        assertEquals("150%", field(view).getText());
        assertFalse(hasPseudoClass(part(view, "markdown-zoom-group"), "focus-within"), "closed");
    }

    @ParameterizedTest(name = "{0} from 130%")
    @DisplayName("from a zoom in between, the arrow keys move to the next preset in their direction")
    @CsvSource({"DOWN, 150", "UP, 125"})
    void arrowsFromAValueInBetween(KeyCode code, int expected) {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.3);
        ListView<Integer> list = levels(view);
        click(part(view, "markdown-zoom-levels"));

        Event.fireEvent(list, key(code));

        assertEquals(expected, list.getSelectionModel().getSelectedItem());
    }

    @Test
    @DisplayName("Escape closes the list and changes nothing")
    void escapeClosesTheList() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        ListView<Integer> list = levels(view);
        click(part(view, "markdown-zoom-levels"));
        Event.fireEvent(list, key(KeyCode.DOWN));

        Event.fireEvent(list, key(KeyCode.ESCAPE));

        assertEquals(1.0, view.getZoom());
        assertFalse(hasPseudoClass(part(view, "markdown-zoom-group"), "focus-within"), "closed");
    }

    @Test
    @DisplayName("a click on a preset chooses it and closes the list; the list shows the presets as the field does")
    void clickChooses() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        ListView<Integer> list = levels(view);
        click(part(view, "markdown-zoom-levels"));
        List<String> texts = new ArrayList<>();
        ListCell<Integer> seventyFive = null;
        for (int i = 0; i < list.getItems().size(); i++) {
            ListCell<Integer> cell = list.getCellFactory().call(list);
            cell.updateListView(list);
            cell.updateIndex(i);
            texts.add(cell.getText());
            if (list.getItems().get(i) == 75) {
                seventyFive = cell;
            }
        }
        assertEquals(List.of("50%", "75%", "100%", "125%", "150%", "200%"), texts);

        Event.fireEvent(seventyFive, new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null));

        assertEquals(0.75, view.getZoom());
        assertEquals("75%", field(view).getText());
        assertFalse(hasPseudoClass(part(view, "markdown-zoom-group"), "focus-within"), "closed");
    }

    @Test
    @DisplayName("hiding the bar closes the list")
    void hidingTheBarClosesTheList() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        click(part(view, "markdown-zoom-levels"));

        view.setZoomBarVisible(false);

        assertFalse(hasPseudoClass(part(view, "markdown-zoom-group"), "focus-within"), "closed");
    }

    @Test
    @DisplayName("the list has the theme's popup list look: the current preset in the selection colour, as tall as its"
            + " six rows, no scroll bar")
    void listLook() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.25);
        ListView<Integer> list = levels(view);
        click(part(view, "markdown-zoom-levels"));
        // A shown popup takes its owner's stylesheets; without a window the test gives them to it.
        ThemeManager.apply(list.getScene());
        list.applyCss();
        double height = list.prefHeight(-1);
        list.resize(140, height);
        list.layout();

        assertTrue(height < 250, () -> "as tall as its rows, not a list view's 400: " + height);
        double rows = 0;
        ListCell<?> selected = null;
        for (Node node : list.lookupAll(".list-cell")) {
            ListCell<?> cell = (ListCell<?>) node;
            if (cell.isVisible() && !cell.isEmpty()) {
                rows += cell.getHeight();
                if (Integer.valueOf(125).equals(cell.getItem())) {
                    selected = cell;
                }
            }
        }
        assertEquals(height, rows + list.snappedTopInset() + list.snappedBottomInset() + 1, 0.5, "six rows");
        assertNotNull(selected);
        assertEquals(tokenColor(view, "-fx-selection-color"), selected.getBackground().getFills().getFirst().getFill());
        for (Node node : list.lookupAll(".scroll-bar")) {
            assertFalse(node.isVisible(), "no scroll bar");
        }
    }

    // --- The slider ------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("the slider goes from 50 to 200, shows the zoom in percent, without tick marks or labels")
    void sliderShowsTheZoom() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Slider slider = (Slider) part(view, "markdown-zoom-slider");

        assertEquals(50, slider.getMin());
        assertEquals(200, slider.getMax());
        assertEquals(100, slider.getValue());
        assertFalse(slider.isShowTickMarks());
        assertFalse(slider.isShowTickLabels());

        view.setZoom(1.37);
        assertEquals(137, slider.getValue());
        view.setZoom(0.5);
        assertEquals(50, slider.getValue());
    }

    @Test
    @DisplayName("a press on the slider's track and a drag of its thumb set the zoom in whole percent")
    void sliderSetsTheZoom() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        Slider slider = (Slider) part(view, "markdown-zoom-slider");
        Node track = slider.lookup(".track");
        Bounds trackBounds = sceneBounds(track);

        mouse(track, MouseEvent.MOUSE_PRESSED, trackBounds.getMinX() + trackBounds.getWidth() * 0.37,
                trackBounds.getCenterY());
        mouse(track, MouseEvent.MOUSE_RELEASED, trackBounds.getMinX() + trackBounds.getWidth() * 0.37,
                trackBounds.getCenterY());

        double pressed = view.getZoom();
        assertTrue(pressed > 0.8 && pressed < 1.3, () -> "about a third of the way: " + pressed);
        assertEquals(Math.round(pressed * 100), pressed * 100, 1e-9, "whole percent");
        assertEquals(pressed * 100, slider.getValue(), 1e-9, "the slider at the zoom");
        assertEquals(Math.round(pressed * 100) + "%", field(view).getText());

        relayOut(view);
        Node thumb = slider.lookup(".thumb");
        Bounds thumbBounds = sceneBounds(thumb);
        mouse(thumb, MouseEvent.MOUSE_PRESSED, thumbBounds.getCenterX(), thumbBounds.getCenterY());
        mouse(thumb, MouseEvent.MOUSE_DRAGGED, thumbBounds.getCenterX() + 23.3, thumbBounds.getCenterY());

        double dragged = view.getZoom();
        assertTrue(dragged > pressed, () -> "larger: " + dragged);
        assertEquals(Math.round(dragged * 100), dragged * 100, 1e-9, "whole percent while dragging");
        assertEquals(dragged * 100, slider.getValue(), 1e-9);
        mouse(thumb, MouseEvent.MOUSE_RELEASED, thumbBounds.getCenterX() + 23.3, thumbBounds.getCenterY());
    }

    @ParameterizedTest(name = "at {0}%")
    @DisplayName("the slider's track is filled in the accent colour from its left end up to the middle of the thumb")
    @ValueSource(ints = {50, 75, 125, 200})
    void sliderTrackFilledUpToTheThumb(int percent) {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(percent / 100.0);
        relayOut(view);
        Slider slider = (Slider) part(view, "markdown-zoom-slider");
        Region fill = (Region) slider.lookup(".track > .track-fill");
        Bounds track = sceneBounds(slider.lookup(".track"));
        Bounds thumb = sceneBounds(slider.lookup(".thumb"));

        assertNotNull(fill);
        assertEquals(tokenColor(view, "-fx-accent-color"), fill.getBackground().getFills().getFirst().getFill());
        Bounds filled = sceneBounds(fill);
        assertEquals(track.getMinX(), filled.getMinX(), 0.5, "from the track's left end");
        assertEquals(thumb.getCenterX(), filled.getMaxX(), 1, "to the middle of the thumb");
        assertEquals(track.getHeight(), filled.getHeight(), 0.5, "as high as the track");

        view.setZoom(1.62);
        Bounds moved = sceneBounds(slider.lookup(".thumb"));
        assertEquals(moved.getCenterX(), sceneBounds(fill).getMaxX(), 1, "it follows the thumb at once");
    }

    // --- Focus -----------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("only the field is in the tab order; a click on minus, plus, the chevron, the slider or Reset leaves"
            + " the focus where it is")
    void onlyTheFieldTakesTheFocus() {
        MarkdownView view = layOut(new MarkdownView("Text"));
        view.setZoom(1.25);
        Button elsewhere = elsewhere(view);

        assertTrue(field(view).isFocusTraversable());
        for (String part : List.of("markdown-zoom-out", "markdown-zoom-in", "markdown-zoom-levels",
                "markdown-zoom-slider", "markdown-zoom-reset")) {
            assertFalse(part(view, part).isFocusTraversable(), part);
        }

        for (String part : List.of("markdown-zoom-out", "markdown-zoom-in", "markdown-zoom-levels",
                "markdown-zoom-reset")) {
            view.setZoom(1.25);
            elsewhere.requestFocus();
            click(part(view, part));
            assertSame(elsewhere, view.getScene().getFocusOwner(), part);
        }
        Slider slider = (Slider) part(view, "markdown-zoom-slider");
        Bounds track = sceneBounds(slider.lookup(".track"));
        mouse(slider.lookup(".track"), MouseEvent.MOUSE_PRESSED, track.getCenterX(), track.getCenterY());
        mouse(slider.lookup(".track"), MouseEvent.MOUSE_RELEASED, track.getCenterX(), track.getCenterY());
        Bounds thumb = sceneBounds(slider.lookup(".thumb"));
        mouse(slider.lookup(".thumb"), MouseEvent.MOUSE_PRESSED, thumb.getCenterX(), thumb.getCenterY());
        mouse(slider.lookup(".thumb"), MouseEvent.MOUSE_RELEASED, thumb.getCenterX(), thumb.getCenterY());
        assertSame(elsewhere, view.getScene().getFocusOwner(), "the slider");
    }

    // --- Narrow widths ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("narrow widths: the slider shrinks from 120px down to 60px, below that it is hidden, then Reset; the"
            + " zoom group always stays at its width, cut off by the bar's edge; the bar is one line at every width")
    void narrowWidths() {
        MarkdownView wide = layOut(new MarkdownView("Text"));
        double groupWidth = part(wide, "markdown-zoom-group").getLayoutBounds().getWidth();
        double resetWidth = part(wide, "markdown-zoom-reset").getLayoutBounds().getWidth();
        double barHeight = bar(wide).getHeight();
        // The scene width at which the bar has a given room inside its 4px padding (and the view's 1px frame).
        double frame = 2 + 8;

        assertEquals(120, part(wide, "markdown-zoom-slider").getLayoutBounds().getWidth(), 0.01, "its preferred width");

        MarkdownView shrunk = layOut(new MarkdownView("Text"), frame + groupWidth + 4 + 90 + 4 + resetWidth);
        assertEquals(90, part(shrunk, "markdown-zoom-slider").getLayoutBounds().getWidth(), 0.5, "shrunk");
        assertTrue(part(shrunk, "markdown-zoom-reset").isVisible());

        MarkdownView smallest = layOut(new MarkdownView("Text"), frame + groupWidth + 4 + 60 + 4 + resetWidth);
        assertTrue(part(smallest, "markdown-zoom-slider").isVisible());
        assertEquals(60, part(smallest, "markdown-zoom-slider").getLayoutBounds().getWidth(), 0.5, "at its minimum");

        MarkdownView noSlider = layOut(new MarkdownView("Text"), frame + groupWidth + 4 + 59 + 4 + resetWidth);
        assertFalse(part(noSlider, "markdown-zoom-slider").isVisible(), "hidden below 60px");
        assertTrue(part(noSlider, "markdown-zoom-reset").isVisible());
        assertEquals(sceneBounds(part(noSlider, "markdown-zoom-group")).getMaxX() + 4,
                sceneBounds(part(noSlider, "markdown-zoom-reset")).getMinX(), 0.5, "Reset right after the group");

        MarkdownView noReset = layOut(new MarkdownView("Text"), frame + groupWidth + 4 + resetWidth - 1);
        assertFalse(part(noReset, "markdown-zoom-slider").isVisible());
        assertFalse(part(noReset, "markdown-zoom-reset").isVisible(), "then Reset");
        assertTrue(part(noReset, "markdown-zoom-group").isVisible());

        MarkdownView cut = layOut(new MarkdownView("Text"), frame + groupWidth - 30);
        Node group = part(cut, "markdown-zoom-group");
        assertTrue(group.isVisible(), "the group always stays");
        assertEquals(groupWidth, group.getLayoutBounds().getWidth(), 0.01, "at its own width");
        assertTrue(sceneBounds(group).getMaxX() > sceneBounds(bar(cut)).getMaxX(), "beyond the bar's edge");
        assertNotNull(bar(cut).getClip(), "cut off there");
        assertEquals(barHeight, bar(cut).getHeight(), 0.01, "one line");
        assertEquals(barHeight, bar(noReset).getHeight(), 0.01, "one line");
    }

    // --- The viewport cache ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("no viewport inside the view caches its content as a bitmap: the view's own and those of code and"
            + " source blocks, also of blocks a new text brings")
    void noViewportCaches() {
        String blocks = "Text\n\n```\ncode\n```\n\n    indented code\n\n<div>html</div>\n";
        MarkdownView view = layOut(new MarkdownView(blocks));

        assertEquals(4, view.lookupAll(".viewport").size(), "the view's, a code block's, two source blocks'");
        for (Node viewport : view.lookupAll(".viewport")) {
            assertFalse(viewport.isCache(), "a viewport without bitmap cache");
        }

        view.setMarkdown(blocks + "\n```\nmore code\n```\n");
        relayOut(view);

        assertEquals(5, view.lookupAll(".viewport").size());
        for (Node viewport : view.lookupAll(".viewport")) {
            assertFalse(viewport.isCache(), "a viewport without bitmap cache");
        }
    }

    // --- Texts -----------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("the texts are English by default: the tooltips and accessible texts of minus, plus and the chevron,"
            + " the field's accessible text and Reset's label")
    void defaultTexts() {
        MarkdownView view = layOut(new MarkdownView("Text"));

        assertEquals("Zoom out", view.getZoomOutText());
        assertEquals("Zoom in", view.getZoomInText());
        assertEquals("Zoom levels", view.getZoomLevelsText());
        assertEquals("Reset", view.getResetZoomText());
        assertTexts(view, "Zoom out", "Zoom in", "Zoom levels", "Reset");
    }

    @Test
    @DisplayName("the texts can be replaced, and a change applies to the bar at once")
    void replacedTexts() {
        MarkdownView view = layOut(new MarkdownView("Text"));

        view.setZoomOutText("Verkleinern");
        view.setZoomInText("Vergr\u00f6\u00dfern");
        view.setZoomLevelsText("Zoomstufen");
        view.setResetZoomText("Zur\u00fccksetzen");

        assertTexts(view, "Verkleinern", "Vergr\u00f6\u00dfern", "Zoomstufen", "Zur\u00fccksetzen");
        assertEquals("Zoomstufen", view.zoomLevelsTextProperty().get());
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    private static Region bar(MarkdownView view) {
        return (Region) part(view, "markdown-zoom-bar");
    }

    /**
     * A part of the view by its style class; fails if there is none.
     */
    private static Node part(MarkdownView view, String styleClass) {
        Node node = view.lookup("." + styleClass);
        assertNotNull(node, () -> "no ." + styleClass);
        return node;
    }

    private static TextField field(MarkdownView view) {
        return (TextField) part(view, "markdown-zoom-field");
    }

    /**
     * Types a text into a field as the keyboard does: the field takes the focus, its text is selected and replaced
     * by the typed characters; an empty text is a Backspace over the selection.
     */
    private static void typeInto(TextField field, String text) {
        field.requestFocus();
        field.selectAll();
        if (text.isEmpty()) {
            Event.fireEvent(field, key(KeyCode.BACK_SPACE));
        } else {
            Event.fireEvent(field, new KeyEvent(KeyEvent.KEY_TYPED, text, "", KeyCode.UNDEFINED, false, false, false,
                    false));
        }
    }

    /**
     * A key press without modifiers.
     */
    private static KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    /**
     * A key press with the Alt key down.
     */
    private static KeyEvent altKey(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, true, false);
    }

    /**
     * The bar's list of zoom levels, through the bar's seam for tests: the list shows as a popup only in a shown
     * window.
     */
    private static ListView<Integer> levels(MarkdownView view) {
        return ((ZoomBar) bar(view)).levelsList();
    }

    /**
     * A mouse event of the primary button at a scene point, fired at a node.
     */
    private static void mouse(Node node, EventType<MouseEvent> type, double sceneX, double sceneY) {
        boolean down = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        Event.fireEvent(node, new MouseEvent(type, sceneX, sceneY, sceneX, sceneY, MouseButton.PRIMARY, 1, false,
                false, false, false, down, false, false, false, false, true, null));
    }

    /**
     * Clicks a node with the primary button in its middle, without moving: a press, a release and a click.
     */
    private static void click(Node node) {
        Bounds bounds = sceneBounds(node);
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_PRESSED, x, y, x, y, MouseButton.PRIMARY, 1, false,
                false, false, false, true, false, false, false, false, true, null));
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_RELEASED, x, y, x, y, MouseButton.PRIMARY, 1, false,
                false, false, false, false, false, false, false, false, true, null));
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_CLICKED, x, y, x, y, MouseButton.PRIMARY, 1, false,
                false, false, false, false, false, false, false, false, true, null));
    }

    /**
     * Checks the bar's texts: minus's, plus's and the chevron's tooltip and accessible text, the field's accessible
     * text (the chevron's) and Reset's label.
     */
    private static void assertTexts(MarkdownView view, String zoomOut, String zoomIn, String zoomLevels, String reset) {
        Button zoomOutButton = (Button) part(view, "markdown-zoom-out");
        Button zoomInButton = (Button) part(view, "markdown-zoom-in");
        Button levels = (Button) part(view, "markdown-zoom-levels");
        assertEquals(zoomOut, zoomOutButton.getTooltip().getText());
        assertEquals(zoomOut, zoomOutButton.getAccessibleText());
        assertEquals(zoomIn, zoomInButton.getTooltip().getText());
        assertEquals(zoomIn, zoomInButton.getAccessibleText());
        assertEquals(zoomLevels, levels.getTooltip().getText());
        assertEquals(zoomLevels, levels.getAccessibleText());
        assertEquals(zoomLevels, field(view).getAccessibleText());
        assertEquals(reset, ((Button) part(view, "markdown-zoom-reset")).getText());
    }

    /**
     * The paragraph of the document at a height of the scene.
     */
    private static Node paragraphAt(MarkdownView view, double sceneY) {
        for (Node node : view.lookupAll(".markdown-paragraph")) {
            Bounds bounds = sceneBounds(node);
            if (bounds.getMinY() <= sceneY && bounds.getMaxY() > sceneY) {
                return node;
            }
        }
        throw new AssertionError("no paragraph at " + sceneY);
    }

    private static boolean isInside(Node node, Node ancestor) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    /**
     * The colour of a theme token in the view's scene: a region filled with it, styled there.
     */
    private static Paint tokenColor(MarkdownView view, String token) {
        Region swatch = new Region();
        swatch.setStyle("-fx-background-color: " + token + ";");
        StackPane root = (StackPane) view.getScene().getRoot();
        root.getChildren().add(swatch);
        swatch.applyCss();
        Paint color = swatch.getBackground().getFills().getFirst().getFill();
        root.getChildren().remove(swatch);
        return color;
    }

    private static boolean hasPseudoClass(Node node, String name) {
        return node.getPseudoClassStates().contains(PseudoClass.getPseudoClass(name));
    }
}
