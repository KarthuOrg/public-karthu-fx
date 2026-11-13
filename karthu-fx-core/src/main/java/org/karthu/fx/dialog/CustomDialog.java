package org.karthu.fx.dialog;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;
import org.karthu.fx.theme.ThemeManager;

/**
 * Custom dialog system with theme and stage icon support.
 * Replaces the default JavaFX Alert dialogs with a customizable alternative.
 *
 * <p>Use the {@link Builder} for full control, or the static convenience methods
 * ({@link #info}, {@link #warning}, {@link #error}, {@link #confirm}, {@link #input})
 * for common patterns.</p>
 */
public class CustomDialog {

    /**
     * The type of dialog, which determines the icon and default button layout.
     */
    public enum DialogType {
        /** A question with Yes and No buttons. */
        CONFIRMATION,
        /** A message with an OK button. */
        INFORMATION,
        /** A warning with an OK button. */
        WARNING,
        /** An error with an OK button. */
        ERROR,
        /** A text input field with OK and Cancel buttons. */
        INPUT
    }

    /**
     * The result of the dialog interaction.
     */
    public enum DialogResult {
        /** The user clicked Yes. */
        YES,
        /** The user clicked No. */
        NO,
        /** The user clicked OK, or pressed Enter in the input field. */
        OK,
        /** The user clicked Cancel or closed the dialog without choosing. */
        CANCEL
    }

    private final Stage dialogStage;
    private final TextField inputField;

    private DialogResult result = DialogResult.CANCEL;
    private String inputValue = null;

    private CustomDialog(Builder builder) {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.initModality(builder.modality);
        dialogStage.setResizable(true);
        dialogStage.setTitle(builder.title);

        if (builder.owner != null) {
            dialogStage.initOwner(builder.owner);
            // Inherit stage icons from owner
            if (builder.owner instanceof Stage ownerStage) {
                dialogStage.getIcons().addAll(ownerStage.getIcons());
            }
        }

        // Root container
        VBox root = new VBox();
        root.getStyleClass().add("custom-dialog");
        root.setSpacing(0);

        // Header section
        HBox header = new HBox(10);
        header.getStyleClass().add("custom-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15));

        Node icon = createIcon(builder.type);
        Label titleLabel = new Label(builder.title);
        titleLabel.getStyleClass().add("custom-dialog-title");

        header.getChildren().addAll(icon, titleLabel);

        // Content section
        VBox content = new VBox(10);
        content.getStyleClass().add("custom-dialog-content");
        content.setPadding(new Insets(12, 15, 12, 15));

        Label headerLabel = new Label(builder.headerText);
        headerLabel.getStyleClass().add("custom-dialog-header-text");
        headerLabel.setWrapText(true);
        content.getChildren().add(headerLabel);

        if (builder.contentText != null && !builder.contentText.isEmpty()) {
            if (builder.selectableContent) {
                TextArea selectableTextArea = new TextArea(builder.contentText);
                selectableTextArea.setEditable(false);
                selectableTextArea.setWrapText(true);
                selectableTextArea.getStyleClass().add("custom-dialog-selectable-content");
                selectableTextArea.setPrefRowCount(Math.max(countLines(builder.contentText), 10));
                VBox.setVgrow(selectableTextArea, Priority.ALWAYS);
                content.getChildren().add(selectableTextArea);
            } else {
                Label contentLabel = new Label(builder.contentText);
                contentLabel.getStyleClass().add("custom-dialog-content-text");
                contentLabel.setWrapText(true);
                content.getChildren().add(contentLabel);
            }
        }

        VBox.setVgrow(content, Priority.ALWAYS);

        // Input field for INPUT type
        inputField = new TextField();
        inputField.getStyleClass().add("custom-dialog-input");
        inputField.setPromptText(builder.inputPrompt);
        if (builder.inputDefaultValue != null) {
            inputField.setText(builder.inputDefaultValue);
        }

        if (builder.type == DialogType.INPUT) {
            content.getChildren().add(inputField);
        }

        // Button bar
        HBox buttonBar = new HBox(10);
        buttonBar.getStyleClass().add("custom-dialog-button-bar");
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(8, 15, 8, 15));

        createButtons(builder, buttonBar);

        // Assemble dialog
        root.getChildren().addAll(header, content, buttonBar);

        // Create scene and apply theme
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);

        dialogStage.setScene(scene);
        dialogStage.setMinWidth(builder.minWidth);
        dialogStage.setMinHeight(builder.minHeight);
        if (builder.prefWidth > 0) {
            dialogStage.setWidth(builder.prefWidth);
        }
        if (builder.prefHeight > 0) {
            dialogStage.setHeight(builder.prefHeight);
        }

        // Center on owner
        if (builder.owner != null) {
            Window owner = builder.owner;
            dialogStage.setOnShown(e -> {
                dialogStage.setX(owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2);
                dialogStage.setY(owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2);
            });
        }

        // Handle input field enter key
        if (builder.type == DialogType.INPUT) {
            inputField.setOnAction(e -> {
                result = DialogResult.OK;
                inputValue = inputField.getText();
                dialogStage.close();
            });
        }
    }

    private FontIcon createIcon(DialogType type) {
        BootstrapIcons iconCode = switch (type) {
            case INFORMATION -> BootstrapIcons.INFO_CIRCLE;
            case WARNING -> BootstrapIcons.EXCLAMATION_TRIANGLE;
            case ERROR -> BootstrapIcons.X_CIRCLE;
            case CONFIRMATION -> BootstrapIcons.QUESTION_CIRCLE;
            case INPUT -> BootstrapIcons.PENCIL;
        };
        FontIcon icon = FontIcon.of(iconCode, 24);
        icon.getStyleClass().add("custom-dialog-icon");
        icon.getStyleClass().add(getIconStyleClass(type));
        return icon;
    }

    private String getIconStyleClass(DialogType type) {
        return switch (type) {
            case INFORMATION -> "custom-dialog-icon-information";
            case WARNING -> "custom-dialog-icon-warning";
            case ERROR -> "custom-dialog-icon-error";
            case CONFIRMATION -> "custom-dialog-icon-confirmation";
            case INPUT -> "custom-dialog-icon-input";
        };
    }

    private void createButtons(Builder builder, HBox buttonBar) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (builder.type == DialogType.CONFIRMATION) {
            Button noButton = createButton(builder.noButtonText, null, () -> {
                result = DialogResult.NO;
                dialogStage.close();
            });
            Button yesButton = createButton(builder.yesButtonText, "primary-button", () -> {
                result = DialogResult.YES;
                dialogStage.close();
            });
            buttonBar.getChildren().addAll(spacer, noButton, yesButton);
        } else if (builder.type == DialogType.INPUT) {
            Button cancelButton = createButton(builder.cancelButtonText, null, () -> {
                result = DialogResult.CANCEL;
                dialogStage.close();
            });
            Button okButton = createButton(builder.okButtonText, "primary-button", () -> {
                result = DialogResult.OK;
                inputValue = inputField.getText();
                dialogStage.close();
            });
            buttonBar.getChildren().addAll(spacer, cancelButton, okButton);
        } else {
            Button okButton = createButton(builder.okButtonText, "primary-button", () -> {
                result = DialogResult.OK;
                dialogStage.close();
            });
            buttonBar.getChildren().addAll(spacer, okButton);
        }
    }

    private Button createButton(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("custom-dialog-button");
        if (styleClass != null) {
            button.getStyleClass().add(styleClass);
        }
        button.setMinWidth(80);
        button.setOnAction(e -> action.run());
        return button;
    }

    private static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        int lines = 1;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                lines++;
            }
        }
        return Math.min(lines + 1, 15);
    }

    /**
     * Shows the dialog without blocking (non-modal behavior).
     */
    public void show() {
        dialogStage.show();
    }

    /**
     * Shows the dialog and waits for user response.
     *
     * @return the dialog result
     */
    public DialogResult showAndWait() {
        dialogStage.showAndWait();
        return result;
    }

    /**
     * Shows the dialog and waits for user response.
     *
     * @return true if user clicked Yes or OK, false otherwise
     */
    public boolean showAndWaitForConfirmation() {
        dialogStage.showAndWait();
        return result == DialogResult.YES || result == DialogResult.OK;
    }

    /**
     * Shows the dialog and returns the input value.
     *
     * @return optional containing the input value if OK was clicked
     */
    public Optional<String> showAndWaitForInput() {
        dialogStage.showAndWait();
        if (result == DialogResult.OK && inputValue != null) {
            return Optional.of(inputValue);
        }
        return Optional.empty();
    }

    // --- Static convenience methods ---

    /**
     * Shows a confirmation dialog with Yes/No buttons.
     *
     * @param owner   the owner window, or null for none
     * @param title   the dialog title
     * @param message the question shown
     * @return true if the user clicked Yes
     */
    public static boolean confirm(Window owner, String title, String message) {
        return new Builder(DialogType.CONFIRMATION)
                .owner(owner)
                .title(title)
                .headerText(message)
                .build()
                .showAndWaitForConfirmation();
    }

    /**
     * Shows a confirmation dialog with header and content text.
     *
     * @param owner   the owner window, or null for none
     * @param title   the dialog title
     * @param header  the header text, the question
     * @param content the content text below it
     * @return true if the user clicked Yes
     */
    public static boolean confirm(Window owner, String title, String header, String content) {
        return new Builder(DialogType.CONFIRMATION)
                .owner(owner)
                .title(title)
                .headerText(header)
                .contentText(content)
                .build()
                .showAndWaitForConfirmation();
    }

    /**
     * Shows an information dialog with an OK button.
     *
     * @param owner   the owner window, or null for none
     * @param title   the dialog title
     * @param message the message shown
     */
    public static void info(Window owner, String title, String message) {
        new Builder(DialogType.INFORMATION)
                .owner(owner)
                .title(title)
                .headerText(message)
                .build()
                .showAndWait();
    }

    /**
     * Shows an information dialog with header and content text.
     *
     * @param owner   the owner window, or null for none
     * @param title   the dialog title
     * @param header  the header text
     * @param content the content text below it
     */
    public static void info(Window owner, String title, String header, String content) {
        new Builder(DialogType.INFORMATION)
                .owner(owner)
                .title(title)
                .headerText(header)
                .contentText(content)
                .build()
                .showAndWait();
    }

    /**
     * Shows a warning dialog with an OK button.
     *
     * @param owner   the owner window, or null for none
     * @param title   the dialog title
     * @param message the message shown
     */
    public static void warning(Window owner, String title, String message) {
        new Builder(DialogType.WARNING)
                .owner(owner)
                .title(title)
                .headerText(message)
                .build()
                .showAndWait();
    }

    /**
     * Shows an error dialog with an OK button.
     *
     * @param owner   the owner window, or null for none
     * @param title   the dialog title
     * @param message the message shown
     */
    public static void error(Window owner, String title, String message) {
        new Builder(DialogType.ERROR)
                .owner(owner)
                .title(title)
                .headerText(message)
                .build()
                .showAndWait();
    }

    /**
     * Shows an error dialog with header and content text.
     *
     * @param owner   the owner window, or null for none
     * @param title   the dialog title
     * @param header  the header text
     * @param content the content text below it
     */
    public static void error(Window owner, String title, String header, String content) {
        new Builder(DialogType.ERROR)
                .owner(owner)
                .title(title)
                .headerText(header)
                .contentText(content)
                .build()
                .showAndWait();
    }

    /**
     * Shows an input dialog and returns the entered value.
     *
     * @param owner        the owner window, or null for none
     * @param title        the dialog title
     * @param message      the message shown above the input field
     * @param defaultValue the input field's initial text
     * @return optional containing the input value if OK was clicked
     */
    public static Optional<String> input(Window owner, String title, String message, String defaultValue) {
        return new Builder(DialogType.INPUT)
                .owner(owner)
                .title(title)
                .headerText(message)
                .inputDefaultValue(defaultValue)
                .build()
                .showAndWaitForInput();
    }

    /**
     * Builder for creating customized dialogs.
     */
    public static class Builder {

        private final DialogType type;
        private Window owner;
        private String title = "Dialog";
        private String headerText = "";
        private String contentText = "";
        private String yesButtonText = "Yes";
        private String noButtonText = "No";
        private String okButtonText = "OK";
        private String cancelButtonText = "Cancel";
        private String inputPrompt = "";
        private String inputDefaultValue = "";
        private boolean selectableContent = false;
        private double minWidth = 400;
        private double minHeight = 200;
        private double prefWidth = 0;
        private double prefHeight = 0;
        private Modality modality = Modality.APPLICATION_MODAL;

        /**
         * Creates a builder for a dialog of the given type.
         *
         * @param type the dialog type, which determines the icon and the buttons
         */
        public Builder(DialogType type) {
            this.type = type;
        }

        /**
         * Sets the owner window. The dialog is centered on it and inherits its stage icons.
         *
         * @param owner the owner window, or null for none (the default)
         * @return this builder
         */
        public Builder owner(Window owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the dialog title, shown in the window frame and the dialog header.
         *
         * @param title the title (default "Dialog")
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the header text, the dialog's main message.
         *
         * @param headerText the header text (default empty)
         * @return this builder
         */
        public Builder headerText(String headerText) {
            this.headerText = headerText;
            return this;
        }

        /**
         * Sets the content text shown below the header text. A null or empty text shows none.
         *
         * @param contentText the content text (default empty)
         * @return this builder
         */
        public Builder contentText(String contentText) {
            this.contentText = contentText;
            return this;
        }

        /**
         * Sets the text of the Yes button of a {@link DialogType#CONFIRMATION} dialog.
         *
         * @param text the button text (default "Yes")
         * @return this builder
         */
        public Builder yesButtonText(String text) {
            this.yesButtonText = text;
            return this;
        }

        /**
         * Sets the text of the No button of a {@link DialogType#CONFIRMATION} dialog.
         *
         * @param text the button text (default "No")
         * @return this builder
         */
        public Builder noButtonText(String text) {
            this.noButtonText = text;
            return this;
        }

        /**
         * Sets the text of the OK button of every dialog type except {@link DialogType#CONFIRMATION}.
         *
         * @param text the button text (default "OK")
         * @return this builder
         */
        public Builder okButtonText(String text) {
            this.okButtonText = text;
            return this;
        }

        /**
         * Sets the text of the Cancel button of an {@link DialogType#INPUT} dialog.
         *
         * @param text the button text (default "Cancel")
         * @return this builder
         */
        public Builder cancelButtonText(String text) {
            this.cancelButtonText = text;
            return this;
        }

        /**
         * Sets the prompt text of the input field of an {@link DialogType#INPUT} dialog.
         *
         * @param prompt the prompt text (default empty)
         * @return this builder
         */
        public Builder inputPrompt(String prompt) {
            this.inputPrompt = prompt;
            return this;
        }

        /**
         * Sets the initial text of the input field of an {@link DialogType#INPUT} dialog.
         *
         * @param defaultValue the initial text (default empty)
         * @return this builder
         */
        public Builder inputDefaultValue(String defaultValue) {
            this.inputDefaultValue = defaultValue;
            return this;
        }

        /**
         * Shows the content text in a read-only text area, so that the user can select and copy it.
         *
         * @param selectable true for a selectable text area, false for a label (the default)
         * @return this builder
         */
        public Builder selectableContent(boolean selectable) {
            this.selectableContent = selectable;
            return this;
        }

        /**
         * Sets the minimum dialog size.
         *
         * @param width  the minimum width (default 400)
         * @param height the minimum height (default 200)
         * @return this builder
         */
        public Builder minSize(double width, double height) {
            this.minWidth = width;
            this.minHeight = height;
            return this;
        }

        /**
         * Sets the initial dialog size. A value of 0 or less keeps the size the content needs.
         *
         * @param width  the initial width (default 0)
         * @param height the initial height (default 0)
         * @return this builder
         */
        public Builder prefSize(double width, double height) {
            this.prefWidth = width;
            this.prefHeight = height;
            return this;
        }

        /**
         * Sets the dialog's modality.
         *
         * @param modality the modality (default {@link Modality#APPLICATION_MODAL})
         * @return this builder
         */
        public Builder modality(Modality modality) {
            this.modality = modality;
            return this;
        }

        /**
         * Builds the dialog. It is shown by one of its {@code show} methods.
         *
         * @return the configured dialog
         */
        public CustomDialog build() {
            return new CustomDialog(this);
        }
    }
}
