package org.karthu.fx.showcase.controller.showcase;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Window;

import org.karthu.fx.settings.SettingCategory;
import org.karthu.fx.settings.SettingEntry;
import org.karthu.fx.settings.SettingGroup;
import org.karthu.fx.settings.SettingsDialog;
import org.karthu.fx.settings.SettingsDialogResult;
import org.karthu.fx.settings.SettingsModel;

/**
 * Controller for settings-dialog-showcase.fxml.
 * Demonstrates the SettingsDialog component with sample settings. The page keeps the applied values while the
 * showcase runs, as a snapshot of the model; nothing is written to disk.
 */
public class SettingsDialogShowcaseController {

    @FXML private Button openDialogBtn;
    @FXML private Label resultLabel;

    /** The values applied last, or null before the first Apply or OK. */
    private Map<String, Object> appliedValues;

    @FXML
    private void initialize() {
        openDialogBtn.setOnAction(event -> openSettingsDialog());
    }

    private void openSettingsDialog() {
        Window owner = openDialogBtn.getScene().getWindow();

        // --- Build sample settings model ---

        // Appearance > General
        SettingEntry<String> density = SettingEntry.ofChoice(
                "appearance.density", "Density",
                "Compact", List.of("Compact", "Cozy", "Comfortable"));
        SettingEntry<Integer> fontSize = SettingEntry.ofInteger(
                "appearance.font-size", "Font Size",
                "Font size in pixels for the main editor area",
                13, 8, 32, 1);
        SettingEntry<String> fontFamily = SettingEntry.ofChoice(
                "appearance.font-family", "Font Family",
                "Segoe UI", List.of("Segoe UI", "Consolas", "JetBrains Mono", "Fira Code", "Cascadia Code"));
        SettingEntry<Boolean> smoothScrolling = SettingEntry.ofBoolean(
                "appearance.smooth-scrolling", "Smooth Scrolling", true);
        SettingEntry<Boolean> showLineNumbers = SettingEntry.ofBoolean(
                "appearance.line-numbers", "Show Line Numbers", true);

        SettingGroup generalGroup = new SettingGroup("General", List.of(
                density, fontFamily, fontSize));
        SettingGroup editorGroup = new SettingGroup("Editor", List.of(
                smoothScrolling, showLineNumbers));

        SettingCategory appearance = new SettingCategory(
                "appearance", "Appearance",
                List.of(generalGroup, editorGroup), List.of());

        // Network > Proxy
        SettingEntry<String> proxyHost = SettingEntry.ofString(
                "network.proxy.host", "Proxy Host", "");
        SettingEntry<Integer> proxyPort = SettingEntry.ofInteger(
                "network.proxy.port", "Proxy Port",
                8080, 1, 65535, 1);
        SettingEntry<Boolean> useProxy = SettingEntry.ofBoolean(
                "network.proxy.enabled", "Use Proxy", false);

        SettingGroup proxyGroup = new SettingGroup("Proxy Settings", List.of(
                useProxy, proxyHost, proxyPort));

        SettingCategory proxy = new SettingCategory(
                "network.proxy", "Proxy",
                List.of(proxyGroup), List.of());

        // Network > Connection
        SettingEntry<Integer> timeout = SettingEntry.ofInteger(
                "network.connection.timeout", "Connection Timeout (s)",
                "Maximum time to wait for a connection before failing",
                30, 5, 300, 5);
        SettingEntry<Integer> retries = SettingEntry.ofInteger(
                "network.connection.retries", "Max Retries",
                3, 0, 10, 1);

        SettingGroup connectionGroup = new SettingGroup("Connection", List.of(
                timeout, retries));

        SettingCategory connection = new SettingCategory(
                "network.connection", "Connection",
                List.of(connectionGroup), List.of());

        // Network (parent with children)
        SettingCategory network = new SettingCategory(
                "network", "Network",
                List.of(), List.of(proxy, connection));

        // Security
        SettingEntry<String> apiKey = SettingEntry.ofSecret(
                "security.api-key", "API Key", "");
        SettingEntry<String> apiSecret = SettingEntry.ofSecret(
                "security.api-secret", "API Secret", "");
        SettingEntry<Boolean> rememberCredentials = SettingEntry.ofBoolean(
                "security.remember-credentials", "Remember Credentials",
                "Store credentials locally for automatic login",
                false);
        SettingEntry<String> authMethod = SettingEntry.ofChoice(
                "security.auth-method", "Authentication Method",
                "Token", List.of("Token", "OAuth2", "Basic Auth", "Certificate"));
        SettingEntry<LocalDate> licenseExpiry = SettingEntry.ofDateTime(
                "security.license-expiry", "License Expiry Date",
                "Date when the current license expires",
                LocalDate.of(2027, 1, 1));

        SettingGroup credentialsGroup = new SettingGroup("Credentials", List.of(
                authMethod, apiKey, apiSecret, rememberCredentials));
        SettingGroup licenseGroup = new SettingGroup("License", List.of(
                licenseExpiry));

        SettingCategory security = new SettingCategory(
                "security", "Security",
                List.of(credentialsGroup, licenseGroup), List.of());

        // Build the model (null ResourceBundle — raw keys used as labels)
        SettingsModel model = new SettingsModel(
                List.of(appearance, network, security), null);

        // Start from the values applied last
        if (appliedValues != null) {
            model.restore(appliedValues);
        }

        // --- Show the dialog ---
        SettingsDialogResult dialogResult = new SettingsDialog.Builder()
                .owner(owner)
                .title("Settings")
                .model(model)
                .onApply(() -> {
                    appliedValues = model.snapshot();
                    resultLabel.setText("Apply — settings kept for this run");
                })
                .prefSize(900, 600)
                .build()
                .showAndWait();

        if (dialogResult == SettingsDialogResult.OK) {
            appliedValues = model.snapshot();
            resultLabel.setText("OK — Density: " + density.getValue()
                    + ", Font: " + fontFamily.getValue()
                    + ", Size: " + fontSize.getValue()
                    + ", License: " + licenseExpiry.getValue());
        } else {
            resultLabel.setText("Cancelled — no changes applied.");
        }
    }
}
