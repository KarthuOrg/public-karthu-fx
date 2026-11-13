package org.karthu.fx.markdown;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.control.Skinnable;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.util.function.IntConsumer;

/**
 * The zoom bar's list of zoom levels: the presets 50 to 200 % in a popup under the zoom group, in the theme's popup
 * list look (the popup carries {@code combo-box-popup}, the style class of a combo box's popup, so the theme's rules
 * for {@code .combo-box-popup .list-view} draw it), the current zoom selected, a value in between none.
 *
 * <p><b>Keys and mouse.</b> Up and Down move the selection, from a value in between to the next preset in their
 * direction, and change no zoom; Enter chooses the selected preset, Escape closes the list, a click on a preset
 * chooses it. Choosing closes the list and hands the preset to the bar. A shown popup takes the keys of its owner's
 * window, so they work wherever the focus is in the window: the list is the popup's focus owner.
 *
 * <p><b>Open.</b> The list is open from {@link #open} to {@link #close}, or until the popup hides by itself (a click
 * outside it, the window losing the focus). The popup itself shows only in a shown window: elsewhere, as in the view's
 * tests, the list is open without a popup, and the keys and clicks reach the list where the tests fire them.
 *
 * <p><b>Size.</b> The list is as wide as the zoom group and as tall as all its rows, so it never scrolls: its skin
 * adds up the heights of its cells as a combo box's list does.
 */
final class ZoomLevelsPopup {

    private final ListView<Integer> list = new ListView<>(FXCollections.observableArrayList(ZoomPercent.PRESETS));
    private final PopupControl popup = new PopupControl();
    private final ReadOnlyBooleanWrapper open = new ReadOnlyBooleanWrapper(this, "open");
    private final IntConsumer chooser;

    /**
     * The zoom in percent when the list opened: where Up and Down start from a value in between.
     */
    private int openedAt = ZoomPercent.NORMAL;

    /**
     * Creates the list.
     *
     * @param chooser what choosing a preset does: the bar sets the zoom, in percent
     */
    ZoomLevelsPopup(IntConsumer chooser) {
        this.chooser = chooser;
        list.setSkin(new RowsSkin(list));
        list.setCellFactory(view -> new LevelCell());
        list.addEventFilter(KeyEvent.KEY_PRESSED, event -> keyPressed(event));
        popup.getStyleClass().add("combo-box-popup");
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.setSkin(new Skin<Skinnable>() {
            @Override
            public Skinnable getSkinnable() {
                return popup;
            }

            @Override
            public Node getNode() {
                return list;
            }

            @Override
            public void dispose() {
                // Nothing to release: the list stays the popup's content.
            }
        });
        popup.setOnHidden(event -> open.set(false));
    }

    /**
     * Whether the list is open, see the class comment.
     *
     * @return the open flag
     */
    ReadOnlyBooleanProperty openProperty() {
        return open.getReadOnlyProperty();
    }

    /**
     * Returns whether the list is open.
     *
     * @return true while it is open
     */
    boolean isOpen() {
        return open.get();
    }

    /**
     * The list of presets: what the view's tests operate, since the popup shows only in a shown window.
     *
     * @return the list
     */
    ListView<Integer> list() {
        return list;
    }

    /**
     * Opens the list under a node, as wide as it, the zoom selected if it is a preset.
     *
     * @param percent the zoom in percent
     * @param anchor  the node the list opens under: the zoom group
     */
    void open(int percent, Node anchor) {
        openedAt = percent;
        select(percent);
        open.set(true);
        if (isInShownWindow(anchor)) {
            Bounds bounds = anchor.localToScreen(anchor.getLayoutBounds());
            list.setPrefWidth(anchor.getLayoutBounds().getWidth());
            popup.show(anchor, bounds.getMinX(), bounds.getMaxY());
            // The popup takes the keys of its owner's window and hands them to its own focus owner.
            list.requestFocus();
        }
    }

    /**
     * Shows the zoom in the open list: selected if it is a preset, nothing selected otherwise.
     *
     * @param percent the zoom in percent
     */
    void showZoom(int percent) {
        if (open.get()) {
            openedAt = percent;
            select(percent);
        }
    }

    /**
     * Closes the list if it is open.
     */
    void close() {
        open.set(false);
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    private void select(int percent) {
        int index = ZoomPercent.PRESETS.indexOf(percent);
        if (index < 0) {
            list.getSelectionModel().clearSelection();
        } else {
            list.getSelectionModel().clearAndSelect(index);
            list.getFocusModel().focus(index);
        }
    }

    /**
     * Up and Down move the selection, Enter chooses, Escape closes; all four are consumed, so neither the list's own
     * keys nor the window's (a default button, a dialog's Escape) see them.
     */
    private void keyPressed(KeyEvent event) {
        if (event.isAltDown() || event.isControlDown() || event.isMetaDown() || event.isShiftDown()) {
            return;
        }
        KeyCode code = event.getCode();
        if (code == KeyCode.UP || code == KeyCode.DOWN) {
            event.consume();
            move(code == KeyCode.DOWN);
        } else if (code == KeyCode.ENTER) {
            event.consume();
            Integer selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                choose(selected);
            }
        } else if (code == KeyCode.ESCAPE) {
            event.consume();
            close();
        }
    }

    /**
     * Moves the selection one preset down the list (larger) or up (smaller); with nothing selected, to the next
     * preset from the zoom in that direction.
     */
    private void move(boolean larger) {
        Integer selected = list.getSelectionModel().getSelectedItem();
        int from = selected != null ? selected : openedAt;
        int to = ZoomPercent.step(from, larger ? 1 : -1);
        if (to != from || selected == null) {
            select(to);
        }
    }

    /**
     * Whether a node lies in a window that shows: only there can the popup show.
     */
    private static boolean isInShownWindow(Node node) {
        Scene scene = node.getScene();
        if (scene == null) {
            return false;
        }
        Window window = scene.getWindow();
        return window != null && window.isShowing();
    }

    private void choose(int percent) {
        close();
        chooser.accept(percent);
    }

    /**
     * A preset in the list, shown as the percent field shows it ({@code 125%}); a click on it chooses it.
     */
    private final class LevelCell extends ListCell<Integer> {

        private LevelCell() {
            addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && !isEmpty() && getItem() != null) {
                    event.consume();
                    choose(getItem());
                }
            });
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : ZoomPercent.format(item));
        }
    }

    /**
     * The list's skin: as tall as all its rows, as a combo box's list is, where a list view would ask for 400 pixels.
     * The heights are the cells' own, measured by the list's flow, so they follow the theme's padding and font.
     */
    private static final class RowsSkin extends ListViewSkin<Integer> {

        private RowsSkin(ListView<Integer> list) {
            super(list);
        }

        @Override
        protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset,
                                           double leftInset) {
            VirtualFlow<ListCell<Integer>> flow = getVirtualFlow();
            // One pixel more, as a combo box's list has, so rounding never brings up a scroll bar.
            double height = 1;
            for (int i = 0; i < getSkinnable().getItems().size(); i++) {
                height += flow.getCell(i).getLayoutBounds().getHeight();
            }
            return topInset + height + bottomInset;
        }
    }
}
