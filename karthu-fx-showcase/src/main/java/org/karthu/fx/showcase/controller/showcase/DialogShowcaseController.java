package org.karthu.fx.showcase.controller.showcase;

import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Window;

import org.karthu.fx.dialog.CustomDialog;

/**
 * Controller for dialog-showcase.fxml.
 * Wires buttons to open various custom dialog types.
 */
public class DialogShowcaseController {

    @FXML
    private Button infoBtn;

    @FXML
    private Button warningBtn;

    @FXML
    private Button errorBtn;

    @FXML
    private Button confirmBtn;

    @FXML
    private Button inputBtn;

    @FXML
    private Label resultLabel;

    @FXML
    private Label inputResultLabel;

    @FXML
    private void initialize() {
        infoBtn.setOnAction(event -> {
            Window owner = infoBtn.getScene().getWindow();
            CustomDialog.info(owner, "Information", "Operation Completed",
                    "The file has been saved successfully.");
            resultLabel.setText("Information dialog closed.");
        });

        warningBtn.setOnAction(event -> {
            Window owner = warningBtn.getScene().getWindow();
            CustomDialog.warning(owner, "Warning",
                    "You have unsaved changes that will be lost if you continue.");
            resultLabel.setText("Warning dialog closed.");
        });

        errorBtn.setOnAction(event -> {
            Window owner = errorBtn.getScene().getWindow();
            CustomDialog.error(owner, "Error", "Connection Failed",
                    "Could not connect to the server. Please check your network settings.");
            resultLabel.setText("Error dialog closed.");
        });

        confirmBtn.setOnAction(event -> {
            Window owner = confirmBtn.getScene().getWindow();
            boolean confirmed = CustomDialog.confirm(owner, "Confirm", "Delete Item",
                    "Are you sure you want to delete this item? This action cannot be undone.");
            if (confirmed) {
                resultLabel.setText("Confirmation: Yes pressed.");
            } else {
                resultLabel.setText("Confirmation: Cancelled.");
            }
        });

        inputBtn.setOnAction(event -> {
            Window owner = inputBtn.getScene().getWindow();
            Optional<String> result = CustomDialog.input(owner, "Input",
                    "Enter a Name", "default value");
            if (result.isPresent()) {
                inputResultLabel.setText("Entered: " + result.get());
            } else {
                inputResultLabel.setText("Input cancelled.");
            }
        });
    }
}
