package org.karthu.fx.sidebar;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * The icon buttons of a side bar's header, made alike wherever they come from: the side bar's own menu and hide
 * buttons, and a navigation tree's Expand All and Collapse All.
 */
final class HeaderButtons {

    private HeaderButtons() {
    }

    /**
     * An icon button of the header: the theme's {@code icon-button} with a style class of its own, an icon, an
     * accessible text and a tooltip that follow their texts, and an action.
     *
     * @param styleClass     the button's own style class, part of the package's style contract
     * @param ikon           the icon
     * @param accessibleText the text a screen reader names the button by
     * @param tooltipText    the tooltip's text, which may name a key beside the accessible text
     * @param action         what a click does
     * @return the button
     */
    static Button iconButton(String styleClass, Ikon ikon, ObservableValue<String> accessibleText,
                             ObservableValue<String> tooltipText, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", styleClass);
        button.setGraphic(FontIcon.of(ikon));
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(tooltipText);
        button.setTooltip(tooltip);
        button.accessibleTextProperty().bind(accessibleText);
        button.setOnAction(event -> action.run());
        return button;
    }
}
