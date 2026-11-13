package org.karthu.fx.showcase.controller.showcase;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.util.Duration;

/**
 * Controller for progress-showcase.fxml.
 * Creates animated progress bar and indicator.
 */
public class ProgressShowcaseController {

    @FXML
    private ProgressBar animatedBar;

    @FXML
    private ProgressIndicator animatedIndicator;

    @FXML
    private void initialize() {
        // Animate progress bar from 0 to 1 over 3 seconds, looping indefinitely
        Timeline barTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(animatedBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(3), new KeyValue(animatedBar.progressProperty(), 1.0))
        );
        barTimeline.setCycleCount(Timeline.INDEFINITE);
        barTimeline.setAutoReverse(true);
        barTimeline.play();

        // Animate progress indicator from 0 to 1 over 3 seconds, looping indefinitely
        Timeline indicatorTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(animatedIndicator.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(3), new KeyValue(animatedIndicator.progressProperty(), 1.0))
        );
        indicatorTimeline.setCycleCount(Timeline.INDEFINITE);
        indicatorTimeline.setAutoReverse(true);
        indicatorTimeline.play();
    }
}
