package org.karthu.fx.showcase.controller.showcase;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.karthu.fx.components.KeyValueEditor;
import org.karthu.fx.components.KeyValueEntry;

import java.util.List;

/**
 * Showcase page for the KeyValueEditor: the "Request Header" example with a masked row, an editor whose options
 * are switched by CheckBoxes, and an untitled editor.
 */
public class KeyValueEditorShowcaseController {

    @FXML private VBox requestHeaderContainer;
    @FXML private VBox optionsContainer;
    @FXML private VBox untitledContainer;

    @FXML
    private void initialize() {
        KeyValueEditor headers = new KeyValueEditor();
        headers.setTitle("Request Header");
        headers.setAddText("Add header");
        headers.getEntries().addAll(
                new KeyValueEntry(true, "key1", "value1", false),
                new KeyValueEntry(true, "secret", "s3cr3t-token", true),
                new KeyValueEntry(true, "", "", false));

        Label snapshotLabel = new Label();
        snapshotLabel.getStyleClass().add("showcase-page-description");
        snapshotLabel.setWrapText(true);
        snapshotLabel.setText(describe(headers.snapshot()));
        headers.getEntries().addListener(
                (ListChangeListener<KeyValueEntry>) change ->
                        snapshotLabel.setText(describe(headers.snapshot())));
        // Edits inside a row change the entries' properties, not the list: refresh on every row edit too.
        headers.addEventHandler(KeyEvent.KEY_RELEASED,
                event -> snapshotLabel.setText(describe(headers.snapshot())));
        headers.addEventHandler(MouseEvent.MOUSE_RELEASED,
                event -> snapshotLabel.setText(describe(headers.snapshot())));
        requestHeaderContainer.getChildren().addAll(headers, snapshotLabel);

        KeyValueEditor options = new KeyValueEditor();
        options.setTitle("Environment");
        options.setAddText("Add variable");
        options.getEntries().addAll(
                new KeyValueEntry("PATH", "/usr/bin"),
                new KeyValueEntry("path", "/opt/bin"),
                new KeyValueEntry(true, "API_TOKEN", "t0k3n", true),
                new KeyValueEntry("", "value without key"));
        FlowPane switches = new FlowPane(16, 8,
                option("Reveal allowed", options.revealAllowedProperty()),
                option("Lock column", options.lockColumnVisibleProperty()),
                option("CheckBox column", options.enabledColumnVisibleProperty()),
                option("Duplicate keys allowed", options.duplicateKeysAllowedProperty()),
                option("Ignore key case", options.ignoreKeyCaseProperty()),
                option("Editable", options.editableProperty()));
        optionsContainer.getChildren().addAll(switches, options);

        KeyValueEditor untitled = new KeyValueEditor();
        untitled.setTitle("");
        untitled.setKeyPromptText("Name");
        untitled.setValuePromptText("Value");
        untitled.getEntries().addAll(
                new KeyValueEntry("JAVA_HOME", "/usr/lib/jvm/temurin-25"),
                new KeyValueEntry(false, "DEBUG", "true", false));
        untitledContainer.getChildren().add(untitled);
    }

    private static CheckBox option(String text, BooleanProperty property) {
        CheckBox box = new CheckBox(text);
        box.selectedProperty().bindBidirectional(property);
        return box;
    }

    private static String describe(List<KeyValueEntry.Snapshot> snapshot) {
        StringBuilder text = new StringBuilder("snapshot(): ");
        if (snapshot.isEmpty()) {
            text.append("(empty)");
        }
        for (int i = 0; i < snapshot.size(); i++) {
            KeyValueEntry.Snapshot pair = snapshot.get(i);
            if (i > 0) {
                text.append(", ");
            }
            text.append(pair.enabled() ? "" : "(off) ")
                    .append(pair.key())
                    .append('=')
                    .append(pair.masked() ? "•••" : pair.value());
        }
        return text.toString();
    }
}
