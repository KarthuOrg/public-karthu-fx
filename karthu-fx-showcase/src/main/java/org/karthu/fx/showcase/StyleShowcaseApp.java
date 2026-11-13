package org.karthu.fx.showcase;

import java.io.IOException;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.karthu.fx.config.ConfigService;
import org.karthu.fx.config.WindowState;
import org.karthu.fx.form.DirectoryMemory;
import org.karthu.fx.theme.StageIcons;
import org.karthu.fx.showcase.controller.StyleShowcaseController;
import org.karthu.fx.theme.ThemeManager;
import org.karthu.fx.showcase.viewmodel.StyleShowcaseViewModel;

/**
 * Application for previewing and testing stylesheet styles.
 * Acts as the MVVM composition root.
 */
public class StyleShowcaseApp extends Application {

    private ConfigService configService;
    private Stage primaryStage;

    @Override
    public void init() {
        configService = new ConfigService();
        configService.initialize();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;

        // The path fields' last used directory, kept on the showcase's own Preferences node across restarts.
        DirectoryMemory.setShared(DirectoryMemory.preferences(Preferences.userNodeForPackage(StyleShowcaseApp.class)));

        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/karthu/fx/showcase/style-showcase.fxml")
        );
        Parent root = loader.load();

        StyleShowcaseController controller = loader.getController();
        controller.initViewModel(viewModel);

        Scene scene = new Scene(root);
        ThemeManager.apply(scene);

        configService.getConfig().windowState().applyTo(primaryStage);

        primaryStage.setTitle("Karthu FX Showcase");
        Image appIcon = new Image(getClass().getResourceAsStream("/org/karthu/fx/showcase/img/app-icon.png"));
        StageIcons.setIcon(primaryStage, appIcon);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (primaryStage != null && configService != null) {
            configService.setWindowState(WindowState.fromStage(primaryStage));
        }
        if (configService != null) {
            configService.shutdown();
        }
    }

    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        launch(args);
    }
}
