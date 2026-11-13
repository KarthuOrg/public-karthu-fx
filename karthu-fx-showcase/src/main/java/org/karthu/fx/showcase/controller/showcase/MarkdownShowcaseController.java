package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import org.karthu.fx.markdown.MarkdownView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Showcase page for the MarkdownView: a text area with a sample document on the left, the view with its zoom bar on
 * the right, following every change of the text, a divider between them to make the view narrow, and below them the
 * address of the last clicked link. Above them a <i>Zoom bar</i> switch shows and hides the bar, and a read-out shows
 * the zoom as the application reads it. The sample holds a code block of each built-in language and images of every
 * kind. Its images lie in a folder beside the page, the view's base address, and two switches above the editor play
 * animated GIFs (<i>Play animations</i>) and load images from the internet (<i>Images from the internet</i>, off at
 * first, so the page sends no request by itself). The <i>Code colours</i> card below holds the CSS of the view's code
 * colours, which the view takes at every change, and a <i>Defaults</i> button.
 */
public class MarkdownShowcaseController {

    private static final Logger log = LoggerFactory.getLogger(MarkdownShowcaseController.class);

    private static final String SAMPLE = "/org/karthu/fx/showcase/showcase/markdown-sample.md";

    /**
     * One of the sample's images. The folder it lies in is the view's base address, found from the address of a file,
     * since a folder inside a jar has an address only where the jar holds an entry for it.
     */
    private static final String SAMPLE_IMAGE = "/org/karthu/fx/showcase/showcase/images/badge.png";

    /**
     * The text of the <i>Code colours</i> card at first and after <i>Defaults</i>: one rule for the page's view with
     * the 23 colour variables of coloured code as the theme sets them, the nine base ones with their colours, the
     * fourteen fine ones with the variable they fall back to. Applied, it changes nothing.
     */
    static final String DEFAULT_CODE_COLORS = """
            .markdown-view {
                -fx-code-comment-color: #8a90a8;
                -fx-code-keyword-color: #c88bff;
                -fx-code-string-color: #e0a84c;
                -fx-code-number-color: #ff9e64;
                -fx-code-type-color: #4cd38a;
                -fx-code-function-color: #5d9bff;
                -fx-code-variable-color: #ff8fa3;
                -fx-code-property-color: #7b8cff;
                -fx-code-tag-color: #ff8fa3;
                -fx-code-doc-color: -fx-code-comment-color;
                -fx-code-annotation-color: -fx-code-keyword-color;
                -fx-code-literal-color: -fx-code-number-color;
                -fx-code-constant-color: -fx-code-literal-color;
                -fx-code-escape-color: -fx-code-string-color;
                -fx-code-attribute-color: -fx-code-number-color;
                -fx-code-operator-color: -fx-code-text-color;
                -fx-code-punctuation-color: -fx-code-text-color;
                -fx-code-heading-color: -fx-code-function-color;
                -fx-code-link-color: -fx-code-property-color;
                -fx-code-code-color: -fx-code-string-color;
                -fx-code-list-color: -fx-code-keyword-color;
                -fx-code-quote-color: -fx-code-comment-color;
                -fx-code-emphasis-color: -fx-code-text-color;
            }
            """;

    @FXML private CheckBox zoomBarSwitch;
    @FXML private CheckBox animationSwitch;
    @FXML private CheckBox remoteImagesSwitch;
    @FXML private Label zoomReadOut;
    @FXML private SplitPane editorRow;
    @FXML private TextArea sourceArea;
    @FXML private Label linkLabel;
    @FXML private TextArea codeColorsArea;

    /**
     * The stylesheet the <i>Code colours</i> card gave the view last, a {@code data:} address of its text.
     */
    private String codeColorsSheet;

    @FXML
    private void initialize() {
        MarkdownView view = new MarkdownView();
        view.setPrefWidth(420);
        editorRow.getItems().add(view);
        // The showcase opens nothing: it shows the address the application would get.
        view.setOnLinkAction(address -> linkLabel.setText("Last clicked link: " + address));

        // The sample's images: relative addresses resolve against their folder. The two switches start as the view's
        // defaults, animations on and the internet off.
        view.setImageBase(imageBase());
        animationSwitch.selectedProperty().bindBidirectional(view.imageAnimationProperty());
        remoteImagesSwitch.selectedProperty().bindBidirectional(view.remoteImagesProperty());

        sourceArea.setText(loadSample());
        view.markdownProperty().bind(sourceArea.textProperty());

        zoomBarSwitch.selectedProperty().bindBidirectional(view.zoomBarVisibleProperty());
        view.zoomProperty().addListener(observable -> zoomReadOut.setText(describeZoom(view.getZoom())));
        zoomReadOut.setText(describeZoom(view.getZoom()));

        codeColorsArea.textProperty().addListener(
                (observable, oldText, newText) -> applyCodeColors(view, newText));
        codeColorsArea.setText(DEFAULT_CODE_COLORS);
    }

    /**
     * <i>Defaults</i>: brings back the card's first text, and with it the theme's look.
     */
    @FXML
    private void restoreCodeColors() {
        codeColorsArea.setText(DEFAULT_CODE_COLORS);
    }

    /**
     * Gives the view the card's text as its own stylesheet, in place of the one before: a {@code data:} address of the
     * text, so no file is written. A stylesheet of the view reaches only the view and its code, nothing else of the
     * page. JavaFX skips what it cannot parse, with a warning in its log, and keeps the rest.
     */
    private void applyCodeColors(MarkdownView view, String css) {
        if (codeColorsSheet != null) {
            view.getStylesheets().remove(codeColorsSheet);
        }
        codeColorsSheet = "data:text/css;base64,"
                + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        view.getStylesheets().add(codeColorsSheet);
    }

    /**
     * The read-out of the zoom, as the application reads it and as the bar shows it: {@code Zoom 1.25 (125%)}. The
     * view's zoom is whole percent, so the factor is written from the percent, never from the double's digits.
     */
    private static String describeZoom(double zoom) {
        long percent = Math.round(zoom * 100);
        return String.format(Locale.ROOT, "Zoom %d.%02d (%d%%)", percent / 100, percent % 100, percent);
    }

    /**
     * The address of the folder of the sample's images, a {@code file:} address while the showcase runs from its
     * classes and a {@code jar:} one from its jar, {@code null} if the images are missing.
     */
    private static String imageBase() {
        URL image = MarkdownShowcaseController.class.getResource(SAMPLE_IMAGE);
        if (image == null) {
            log.warn("The Markdown sample's images not found: {}", SAMPLE_IMAGE);
            return null;
        }
        String address = image.toExternalForm();
        return address.substring(0, address.lastIndexOf('/') + 1);
    }

    private static String loadSample() {
        try (InputStream in = MarkdownShowcaseController.class.getResourceAsStream(SAMPLE)) {
            if (in == null) {
                log.warn("Markdown sample not found: {}", SAMPLE);
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read the Markdown sample: {}", SAMPLE, e);
            return "";
        }
    }
}
