package org.karthu.fx.components;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A toggle switch component that looks like a modern switch slider.
 * When selected, the thumb moves to the right. When deselected, it moves left and is grayed out.
 */
public class ToggleSwitch extends StackPane {

    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private static final double THUMB_MARGIN = 3.0;  // Equal margin on left (off) and right (on)
    private static final double THUMB_TRAVEL = 18.0; // 36 (track) - 12 (thumb) - 2*3 (margins)

    private final Region track;
    private final Region thumb;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    /**
     * Creates a switch that is off.
     */
    public ToggleSwitch() {
        this(false);
    }

    /**
     * Creates a switch in the given state, shown without animation.
     *
     * @param initialState true for on, false for off
     */
    public ToggleSwitch(boolean initialState) {
        getStyleClass().add("toggle-switch");

        track = new Region();
        track.getStyleClass().add("toggle-switch-track");

        thumb = new Region();
        thumb.getStyleClass().add("toggle-switch-thumb");

        getChildren().addAll(track, thumb);

        // Set initial state without animation
        selected.set(initialState);
        updateState(false);

        // Listen for state changes
        selected.addListener((obs, wasSelected, isSelected) -> updateState(true));

        // Handle clicks
        setOnMouseClicked(e -> setSelected(!isSelected()));
    }

    private void updateState(boolean animate) {
        boolean isOn = selected.get();
        pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isOn);
        track.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isOn);
        thumb.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isOn);

        double targetX = isOn ? (THUMB_MARGIN + THUMB_TRAVEL) : THUMB_MARGIN;

        if (animate) {
            TranslateTransition transition = new TranslateTransition(Duration.millis(150), thumb);
            transition.setToX(targetX);
            transition.play();
        } else {
            thumb.setTranslateX(targetX);
        }
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    /**
     * Whether the switch is on. A click toggles it; a change moves the thumb with a short
     * animation.
     *
     * @return the selected property
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }
}
