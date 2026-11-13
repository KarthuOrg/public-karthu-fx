package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;

/**
 * Controller for date-picker-showcase.fxml.
 * Sets up DatePicker instances with various states.
 */
public class DatePickerShowcaseController {

    @FXML
    private DatePicker normalDate;

    @FXML
    private DatePicker emptyDate;

    @FXML
    private DatePicker editableDate;

    @FXML
    private DatePicker disabledDate;

    @FXML
    private void initialize() {
        normalDate.setValue(LocalDate.now());

        // emptyDate left without a value

        editableDate.setValue(LocalDate.now());
        editableDate.setEditable(true);

        disabledDate.setValue(LocalDate.now());
        disabledDate.setDisable(true);
    }
}
