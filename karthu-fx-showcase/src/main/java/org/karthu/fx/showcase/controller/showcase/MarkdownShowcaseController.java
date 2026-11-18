package org.karthu.fx.showcase.controller.showcase;

import javafx.application.ColorScheme;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.karthu.fx.form.ComboInput;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.markdown.MarkdownView;
import org.karthu.fx.markdown.SearchDirection;
import org.karthu.fx.theme.ThemeManager;
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
 * the right, showing every change of the text as a new document, a divider between them to make the view narrow, and
 * below them the address of the last clicked link. Above the view a bar as an application builds its own: a search
 * field connected to the view and the choice of the search's direction. Above them a <i>Zoom bar</i> switch shows and
 * hides the bar, and a read-out shows the zoom as the application reads it. The sample holds a code block of each
 * built-in language, tables and images of every kind. Its images lie in a folder beside the page, the view's base
 * address, and two switches above the editor play animated GIFs (<i>Play animations</i>) and load images from the
 * internet (<i>Images from the internet</i>, off at first, so the page sends no request by itself). <i>Insert a line at
 * the top</i> beside them gives the view a new version of its text, which keeps the reader's place. Below them the
 * <i>Margins</i> switch gives the view the margins of two sliders, live, and a read-out shows them. The <i>Code
 * colours</i> card below holds the CSS of the view's code colours in the scheme in effect, which the view takes at every
 * change, and a <i>Defaults</i> button.
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
     * The text of the <i>Code colours</i> card in Nebula Dark, at first and after <i>Defaults</i>: one rule for the
     * page's view with the 23 colour variables of coloured code as the theme sets them, the nine base ones with their
     * colours, the fourteen fine ones with the variable they fall back to. Applied, it changes nothing.
     */
    static final String DARK_CODE_COLORS = """
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

    /**
     * The text of the <i>Code colours</i> card in Nebula Light: the same rule with the light scheme's nine base colours.
     * Applied, it changes nothing either.
     */
    static final String LIGHT_CODE_COLORS = """
            .markdown-view {
                -fx-code-comment-color: #5E6475;
                -fx-code-keyword-color: #7A3EC0;
                -fx-code-string-color: #875A10;
                -fx-code-number-color: #A84A10;
                -fx-code-type-color: #15703F;
                -fx-code-function-color: #1F56B3;
                -fx-code-variable-color: #B0284D;
                -fx-code-property-color: #3B4CC0;
                -fx-code-tag-color: #B0284D;
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
    @FXML private CheckBox marginsSwitch;
    @FXML private Slider leftMarginSlider;
    @FXML private Slider rightMarginSlider;
    @FXML private Label marginsReadOut;
    @FXML private SplitPane editorRow;
    @FXML private VBox viewColumn;
    @FXML private HBox searchBar;
    @FXML private TextArea sourceArea;
    @FXML private Label linkLabel;
    @FXML private TextArea codeColorsArea;

    /**
     * The stylesheet the <i>Code colours</i> card gave the view last, a {@code data:} address of its text.
     */
    private String codeColorsSheet;

    /** The page's view. */
    private MarkdownView view;

    /** How many lines <i>Insert a line at the top</i> has inserted, which numbers the next one. */
    private int insertedLines;

    /**
     * Gives the <i>Code colours</i> card the colours of a new scheme while it still holds the old scheme's, so that it
     * keeps the theme's look; a text changed by hand stays. The theme holds it weakly, through the page.
     */
    private final ChangeListener<ColorScheme> schemeListener = (observable, oldScheme, newScheme) -> {
        if (codeColorsArea.getText().equals(codeColors(oldScheme))) {
            codeColorsArea.setText(codeColors(newScheme));
        }
    };

    @FXML
    private void initialize() {
        view = new MarkdownView();
        view.setPrefWidth(420);
        VBox.setVgrow(view, Priority.ALWAYS);
        viewColumn.getChildren().add(view);
        buildSearchBar(view);
        // The showcase opens nothing: it shows the address the application would get.
        view.setOnLinkAction(address -> linkLabel.setText("Last clicked link: " + address));

        // The sample's images: relative addresses resolve against their folder. The two switches start as the view's
        // defaults, animations on and the internet off.
        view.setImageBase(imageBase());
        animationSwitch.selectedProperty().bindBidirectional(view.imageAnimationProperty());
        remoteImagesSwitch.selectedProperty().bindBidirectional(view.remoteImagesProperty());

        // The editor's text is another document at every change, shown from the top; not a binding, which would
        // refuse the new versions Insert a line at the top gives the view.
        sourceArea.setText(loadSample());
        view.setMarkdown(sourceArea.getText());
        sourceArea.textProperty().addListener((observable, oldText, newText) -> view.setMarkdown(newText));

        zoomBarSwitch.selectedProperty().bindBidirectional(view.zoomBarVisibleProperty());
        view.zoomProperty().addListener(observable -> zoomReadOut.setText(describeZoom(view.getZoom())));
        zoomReadOut.setText(describeZoom(view.getZoom()));

        marginsSwitch.selectedProperty().addListener(observable -> applyMargins());
        leftMarginSlider.valueProperty().addListener(observable -> applyMargins());
        rightMarginSlider.valueProperty().addListener(observable -> applyMargins());
        applyMargins();

        codeColorsArea.textProperty().addListener(
                (observable, oldText, newText) -> applyCodeColors(view, newText));
        codeColorsArea.setText(codeColors(ThemeManager.getColorScheme()));
        ThemeManager.colorSchemeProperty().addListener(new WeakChangeListener<>(schemeListener));
    }

    /**
     * The bar above the view, as an application builds its own: a search field connected to the view with one call,
     * which keeps its text and the view's search equal, counts the matches and goes through them, and beside it the
     * choice of the search's direction, bound to the view's both ways.
     */
    private void buildSearchBar(MarkdownView view) {
        SearchInput searchField = new SearchInput();
        searchField.setId("searchField");
        searchField.search(view);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ComboInput<SearchDirection> direction = new ComboInput<>();
        direction.setId("searchDirection");
        direction.getItems().setAll(SearchDirection.FORWARD, SearchDirection.BACKWARD);
        direction.setDisplayText(MarkdownShowcaseController::describeDirection);
        direction.setPrefWidth(150);
        direction.valueProperty().bindBidirectional(view.searchDirectionProperty());

        searchBar.getChildren().addAll(searchField, direction);
    }

    /** The label of a search direction in the choice above the view. */
    private static String describeDirection(SearchDirection direction) {
        if (direction == SearchDirection.BACKWARD) {
            return "Backward";
        }
        return "Forward";
    }

    /**
     * <i>Insert a line at the top</i>: gives the view a new version of its text, a numbered line more at the top, as
     * an application gives one after a reload of its file. The view keeps the place the reader is reading, and the
     * pictures it shows. The view takes the text first: the editor's listener then sets the same text, which changes
     * nothing, where setting the editor first would show the text as another document, from the top.
     */
    @FXML
    private void insertLineAtTop() {
        insertedLines++;
        String changed = "**Update " + insertedLines + ".** A line inserted at the top with `updateMarkdown`.\n\n"
                + sourceArea.getText();
        view.updateMarkdown(changed);
        sourceArea.setText(changed);
    }

    /**
     * The <i>Margins</i> switch and its sliders: on, the view takes the sliders' values in whole pixels at every change,
     * as an application gives it the margins of its document; off, not a number, the theme's padding, and the sliders
     * wait disabled.
     */
    private void applyMargins() {
        boolean on = marginsSwitch.isSelected();
        leftMarginSlider.setDisable(!on);
        rightMarginSlider.setDisable(!on);
        long left = Math.round(leftMarginSlider.getValue());
        long right = Math.round(rightMarginSlider.getValue());
        view.setLeftMargin(on ? left : Double.NaN);
        view.setRightMargin(on ? right : Double.NaN);
        marginsReadOut.setText(on ? String.format(Locale.ROOT, "Margins %d / %d px", left, right)
                : "Margins: the theme's padding");
    }

    /**
     * <i>Defaults</i>: brings back the card's first text in the scheme in effect, and with it the theme's look.
     */
    @FXML
    private void restoreCodeColors() {
        codeColorsArea.setText(codeColors(ThemeManager.getColorScheme()));
    }

    /** The <i>Code colours</i> card's text that leaves the look of a scheme as the theme sets it. */
    private static String codeColors(ColorScheme scheme) {
        if (scheme == ColorScheme.LIGHT) {
            return LIGHT_CODE_COLORS;
        }
        return DARK_CODE_COLORS;
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
