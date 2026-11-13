package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 * Controller for spinner-showcase.fxml.
 * Sets up Spinner instances with various value factories.
 */
public class SpinnerShowcaseController {

    @FXML
    private Spinner<Integer> intSpinner;

    @FXML
    private Spinner<Double> doubleSpinner;

    @FXML
    private Spinner<Integer> editableSpinner;

    @FXML
    private Spinner<Integer> disabledSpinner;

    @FXML
    private void initialize() {
        intSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 50));

        doubleSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, 2.5, 0.5));

        editableSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 25));
        editableSpinner.setEditable(true);

        disabledSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 75));
        disabledSpinner.setDisable(true);
    }
}
