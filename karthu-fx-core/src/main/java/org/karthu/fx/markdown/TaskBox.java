package org.karthu.fx.markdown;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/**
 * The check box of a GFM task list item, which shows whether the task is done and nothing more: the reader cannot
 * tick it, and the view never changes its source.
 *
 * <p>A region of its own, not a {@code CheckBox}: a control reacts to clicks, takes the keyboard focus, and the theme's
 * check box rules give it hover colours and a hand cursor. The theme draws this box from the same colour tokens
 * ({@code markdown-task-box}, with a region {@code mark} inside for the tick, {@code :checked} while done). It consumes
 * no mouse event, so a press on it starts a selection as a press beside the text does. For a screen reader it is a
 * check box, selected while done. It names its state in words, the view's text for an open or a done task, as its
 * tooltip and its accessible text; the tooltip is why the box is not mouse transparent.
 */
final class TaskBox extends Region {

    /**
     * The pseudo-class of a done task, on the box and on its item's row.
     */
    static final PseudoClass CHECKED = PseudoClass.getPseudoClass("checked");

    private final Region mark = new Region();
    private final boolean checked;

    /**
     * Creates the box of a task.
     *
     * @param checked whether the task is done
     * @param text    the text that names the state, bound so that a change applies to the box at once
     */
    TaskBox(boolean checked, ObservableValue<String> text) {
        this.checked = checked;
        getStyleClass().add("markdown-task-box");
        mark.getStyleClass().add("mark");
        getChildren().add(mark);
        pseudoClassStateChanged(CHECKED, checked);
        setAccessibleRole(AccessibleRole.CHECK_BOX);
        accessibleTextProperty().bind(text);
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(text);
        Tooltip.install(this, tooltip);
    }

    /**
     * Whether the task is done.
     *
     * @return {@code true} for a ticked box
     */
    boolean isChecked() {
        return checked;
    }

    @Override
    protected void layoutChildren() {
        // The tick in the middle of the box, as large as its preferred size.
        double width = snapSizeX(mark.prefWidth(-1));
        double height = snapSizeY(mark.prefHeight(-1));
        mark.resizeRelocate(snapPositionX((getWidth() - width) / 2), snapPositionY((getHeight() - height) / 2), width,
                height);
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        if (attribute == AccessibleAttribute.SELECTED) {
            return checked;
        }
        return super.queryAccessibleAttribute(attribute, parameters);
    }
}
