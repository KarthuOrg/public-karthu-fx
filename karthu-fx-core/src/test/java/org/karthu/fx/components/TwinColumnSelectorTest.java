package org.karthu.fx.components;

import javafx.application.Platform;
import javafx.util.StringConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwinColumnSelectorTest {

    private TwinColumnSelector<String> selector;

    @BeforeAll
    static void initToolkit() {
        // Controls cannot be instantiated without the JavaFX toolkit. No
        // Stage/Scene is shown, so the controls may live on the test thread.
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
    }

    @BeforeEach
    void createSelector() {
        selector = new TwinColumnSelector<>();
    }

    @Test
    @DisplayName("setItems partitions all into available and selected")
    void setItemsPartitions() {
        selector.setItems(List.of("a", "b", "c", "d"), List.of("b", "d"));

        assertEquals(List.of("a", "c"), selector.getAvailableItems());
        assertEquals(List.of("b", "d"), selector.getSelectedItems());
    }

    @Test
    @DisplayName("setItems preserves the selected order exactly as given")
    void setItemsPreservesSelectedOrder() {
        selector.setItems(List.of("a", "b", "c", "d"), List.of("d", "a"));

        assertEquals(List.of("d", "a"), selector.getSelectedItems());
        assertEquals(List.of("b", "c"), selector.getAvailableItems());
    }

    @Test
    @DisplayName("selected entries not part of all are dropped")
    void setItemsDropsUnknownSelected() {
        selector.setItems(List.of("a", "b"), List.of("b", "zzz"));

        assertEquals(List.of("b"), selector.getSelectedItems());
        assertEquals(List.of("a"), selector.getAvailableItems());
    }

    @Test
    @DisplayName("moveRight appends the selection at the end and clears it")
    void moveRightAppendsAndClearsSelection() {
        selector.setItems(List.of("a", "b", "c", "d", "x"), List.of("x"));
        selector.availableListView().getSelectionModel().selectIndices(0, 2); // a, c

        selector.moveRight();

        assertEquals(List.of("b", "d"), selector.getAvailableItems());
        assertEquals(List.of("x", "a", "c"), selector.getSelectedItems());
        assertTrue(selector.availableListView().getSelectionModel().isEmpty());
    }

    @Test
    @DisplayName("a multi-selection moves in display order, not click order")
    void multiSelectMovesInDisplayOrder() {
        selector.setItems(List.of("a", "b", "c", "d"), List.of());
        // Select out of order: d first, then a.
        selector.availableListView().getSelectionModel().select(3);
        selector.availableListView().getSelectionModel().select(0);

        selector.moveRight();

        assertEquals(List.of("a", "d"), selector.getSelectedItems());
    }

    @Test
    @DisplayName("moveAllRight moves only the currently filtered items")
    void moveAllRightRespectsFilter() {
        selector.setItems(List.of("apple", "banana", "apricot", "cherry"), List.of());
        selector.availableFilterField().setText("ap");

        selector.moveAllRight();

        assertEquals(List.of("apple", "apricot"), selector.getSelectedItems());
        assertEquals(List.of("banana", "cherry"), selector.getAvailableItems());
    }

    @Test
    @DisplayName("moveLeft and moveAllLeft mirror the right-moves")
    void leftMovesMirror() {
        selector.setItems(List.of("a", "b", "c"), List.of("a", "b", "c"));
        selector.selectedListView().getSelectionModel().select(1); // b

        selector.moveLeft();
        assertEquals(List.of("b"), selector.getAvailableItems());
        assertEquals(List.of("a", "c"), selector.getSelectedItems());

        selector.selectedFilterField().setText("c");
        selector.moveAllLeft();
        assertEquals(List.of("b", "c"), selector.getAvailableItems());
        assertEquals(List.of("a"), selector.getSelectedItems());
    }

    @Test
    @DisplayName("filtering is view-only and case-insensitive")
    void filterIsViewOnlyAndCaseInsensitive() {
        selector.setItems(List.of("Apple", "banana", "APRICOT"), List.of());
        selector.availableFilterField().setText("ap");

        assertEquals(2, selector.availableListView().getItems().size());
        assertEquals(3, selector.getAvailableItems().size(), "model must stay untouched");

        selector.availableFilterField().setText("");
        assertEquals(3, selector.availableListView().getItems().size());
    }

    @Test
    @DisplayName("a converter change re-applies the filter predicate")
    void converterChangeReappliesFilter() {
        selector.setItems(List.of("a", "b"), List.of());
        selector.availableFilterField().setText("x");
        assertEquals(0, selector.availableListView().getItems().size());

        selector.setStringConverter(new StringConverter<>() {
            @Override
            public String toString(String object) {
                return "x-" + object;
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        assertEquals(2, selector.availableListView().getItems().size(),
                "all items now match 'x' through the new converter");
    }

    @Test
    @DisplayName("reset restores the partition captured at the last setItems")
    void resetRestoresPartition() {
        selector.setItems(List.of("a", "b", "c"), List.of("c"));
        selector.availableListView().getSelectionModel().selectAll();
        selector.moveRight();
        assertEquals(List.of("c", "a", "b"), selector.getSelectedItems());

        selector.reset();

        assertEquals(List.of("a", "b"), selector.getAvailableItems());
        assertEquals(List.of("c"), selector.getSelectedItems());
    }

    @Test
    @DisplayName("moves and setItems are no-op safe on empty input")
    void noOpSafety() {
        assertDoesNotThrow(() -> {
            selector.moveRight();
            selector.moveAllRight();
            selector.moveLeft();
            selector.moveAllLeft();
            selector.moveUp();
            selector.moveDown();
            selector.reset();
            selector.setItems(null, null);
        });
        assertEquals(0, selector.getAvailableItems().size());
        assertEquals(0, selector.getSelectedItems().size());

        selector.setItems(List.of("a"), List.of());
        selector.availableFilterField().setText("zzz");
        selector.moveAllRight(); // everything filtered out — nothing moves
        assertEquals(List.of("a"), selector.getAvailableItems());
    }

    @Test
    @DisplayName("move buttons disable when their action would be a no-op")
    void disableStates() {
        selector.setItems(List.of("a", "b"), List.of());

        assertTrue(selector.moveRightButtonNode().isDisabled(), "no selection yet");
        selector.availableListView().getSelectionModel().select(0);
        assertFalse(selector.moveRightButtonNode().isDisabled());

        assertFalse(selector.moveAllRightButtonNode().isDisabled());
        selector.availableFilterField().setText("zzz");
        assertTrue(selector.moveAllRightButtonNode().isDisabled(), "filtered view is empty");

        assertTrue(selector.moveLeftButtonNode().isDisabled(), "selected list is empty");
        assertTrue(selector.moveAllLeftButtonNode().isDisabled());
    }

    @Test
    @DisplayName("reorder buttons are hidden by default and reorder when enabled")
    void reorderableUpDown() {
        assertFalse(selector.moveUpButtonNode().isVisible());
        assertFalse(selector.moveUpButtonNode().isManaged());
        assertFalse(selector.moveDownButtonNode().isVisible());

        selector.setReorderable(true);
        assertTrue(selector.moveUpButtonNode().isVisible());

        selector.setItems(List.of("a", "b", "c"), List.of("a", "b", "c"));
        selector.selectedListView().getSelectionModel().select(1); // b
        selector.moveUp();
        assertEquals(List.of("b", "a", "c"), selector.getSelectedItems());

        // Boundary no-op at the top.
        selector.selectedListView().getSelectionModel().clearAndSelect(0);
        selector.moveUp();
        assertEquals(List.of("b", "a", "c"), selector.getSelectedItems());

        selector.selectedListView().getSelectionModel().clearAndSelect(1); // a
        selector.moveDown();
        assertEquals(List.of("b", "c", "a"), selector.getSelectedItems());

        // Boundary no-op at the bottom.
        selector.selectedListView().getSelectionModel().clearAndSelect(2);
        selector.moveDown();
        assertEquals(List.of("b", "c", "a"), selector.getSelectedItems());
    }

    @Test
    @DisplayName("header captions are uppercased and counts follow the source lists")
    void headerCaptionsAndCounts() {
        selector.setAvailableHeader("Pool");
        selector.setItems(List.of("a", "b", "c"), List.of("c"));

        javafx.scene.control.Label caption = (javafx.scene.control.Label) selector.lookup(
                ".twin-header .twin-header-caption");
        assertEquals("POOL", caption.getText());

        selector.availableListView().getSelectionModel().select(0);
        selector.moveRight();
        // Counts come from the source lists even under an active filter.
        selector.availableFilterField().setText("zzz");
        assertEquals(1, selector.getAvailableItems().size());
        assertEquals(2, selector.getSelectedItems().size());
    }
}
