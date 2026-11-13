package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

/**
 * Controller for accordion-showcase.fxml.
 * Expands the first pane by default.
 */
public class AccordionShowcaseController {

    @FXML
    private Accordion accordion;

    @FXML
    private TitledPane pane1;

    @FXML
    private void initialize() {
        accordion.setExpandedPane(pane1);
    }
}
