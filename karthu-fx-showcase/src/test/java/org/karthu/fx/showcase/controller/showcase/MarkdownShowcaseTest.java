package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.karthu.fx.form.ComboInput;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.ImageLoader;
import org.karthu.fx.markdown.MarkdownView;
import org.karthu.fx.markdown.SearchDirection;
import org.karthu.fx.theme.ThemeManager;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <i>Markdown</i> page of the group <i>Data Display</i>, loaded from its FXML and laid out with the theme in a
 * scene that is never shown: the view with its zoom bar beside the source, the <i>Zoom bar</i> switch, the read-out
 * of the zoom, the <i>Margins</i> switch with its sliders, the <i>Code colours</i> card, the sample's images with the
 * <i>Play animations</i> and <i>Images from the internet</i> switches, the search field and the direction above the
 * view, and <i>Insert a line at the top</i>.
 *
 * <p>The page's view loads its images on its own background threads and shows them on the JavaFX Application Thread.
 * So every test runs on that thread ({@link OnFxThread}), where no image arrives while it works, but for the tests
 * that wait for the images ({@link WaitsForImages}): they run on the test's thread and reach the page only through
 * {@link #onFx(Callable)}. No test opens a connection to the internet: the internet switch stays off, or the view's
 * loader is wrapped first so that it refuses internet addresses.
 */
@ExtendWith(MarkdownShowcaseTest.OnFxThread.class)
class MarkdownShowcaseTest {

    /** The address of the sample's image from the internet. */
    private static final String INTERNET_IMAGE = "https://www.w3.org/Icons/w3c_home.png";

    /** The alt texts of the sample's images that show a picture. */
    private static final String SCREENSHOT = "The showcase on a 4K screen at 150 %";
    private static final String DIALOG = "A dialog on a 4K screen at 150 %";
    private static final String SIDE_BAR = "The showcase's side bar";
    private static final String RECORDING = "An export in progress";
    private static final String BADGE = "karthu-fx for JavaFX 25";
    private static final String ICON = "info";

    /** The sample's image files, beside the page, and their size in pixels. */
    private static final Map<String, List<Integer>> IMAGE_FILES = Map.of(
            "screenshot-4k.png", List.of(3840, 2160),
            "dialog.png", List.of(690, 273),
            "sidebar.png", List.of(330, 2841),
            "progress.gif", List.of(486, 77),
            "badge.png", List.of(186, 30),
            "icon-info.png", List.of(24, 24));

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        // The first control has the toolkit set its default stylesheet on its own thread: wait for it.
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("the Zoom bar switch shows and hides the view's zoom bar, and follows the view")
    void zoomBarSwitch() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        CheckBox zoomBar = assertInstanceOf(CheckBox.class, root.lookup("#zoomBarSwitch"));
        Node bar = view.lookup(".markdown-zoom-bar");
        assertNotNull(bar);

        assertEquals("Zoom bar", zoomBar.getText());
        assertTrue(zoomBar.isSelected());
        assertTrue(bar.isVisible());

        zoomBar.setSelected(false);
        assertFalse(view.isZoomBarVisible());
        assertFalse(bar.isVisible());

        view.setZoomBarVisible(true);
        assertTrue(zoomBar.isSelected(), "the switch follows the view");
        assertTrue(bar.isVisible());
    }

    @Test
    @DisplayName("the read-out shows the zoom as a factor and in percent, set by the application or by the bar")
    void readOutFollowsTheZoom() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        Label readOut = assertInstanceOf(Label.class, root.lookup("#zoomReadOut"));
        assertEquals("Zoom 1.00 (100%)", readOut.getText());

        view.setZoom(1.25);
        assertEquals("Zoom 1.25 (125%)", readOut.getText());

        view.setZoom(1.3);
        assertInstanceOf(Button.class, view.lookup(".markdown-zoom-in")).fire();
        assertEquals("Zoom 1.50 (150%)", readOut.getText());

        assertInstanceOf(Button.class, view.lookup(".markdown-zoom-reset")).fire();
        assertEquals("Zoom 1.00 (100%)", readOut.getText());

        view.setZoom(0.5);
        assertEquals("Zoom 0.50 (50%)", readOut.getText());
        view.setZoom(1.15);
        assertEquals("Zoom 1.15 (115%)", readOut.getText());
    }

    @Test
    @DisplayName("the Margins switch gives the view the sliders' margins in whole pixels, which follow the sliders while"
            + " it is on; off, the theme's padding holds and the sliders are disabled")
    void marginsSwitch() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        CheckBox margins = assertInstanceOf(CheckBox.class, root.lookup("#marginsSwitch"));
        Slider left = assertInstanceOf(Slider.class, root.lookup("#leftMarginSlider"));
        Slider right = assertInstanceOf(Slider.class, root.lookup("#rightMarginSlider"));
        Label readOut = assertInstanceOf(Label.class, root.lookup("#marginsReadOut"));

        assertEquals("Margins", margins.getText());
        assertFalse(margins.isSelected());
        assertTrue(Double.isNaN(view.getLeftMargin()), "the theme's padding");
        assertTrue(Double.isNaN(view.getRightMargin()), "the theme's padding");
        assertTrue(left.isDisabled());
        assertTrue(right.isDisabled());
        assertEquals("Margins: the theme's padding", readOut.getText());

        margins.setSelected(true);

        assertEquals(80, view.getLeftMargin());
        assertEquals(40, view.getRightMargin());
        assertFalse(left.isDisabled());
        assertFalse(right.isDisabled());
        assertEquals("Margins 80 / 40 px", readOut.getText());

        left.setValue(120.4);
        right.setValue(250.6);

        assertEquals(120, view.getLeftMargin());
        assertEquals(251, view.getRightMargin());
        assertEquals("Margins 120 / 251 px", readOut.getText());

        margins.setSelected(false);

        assertTrue(Double.isNaN(view.getLeftMargin()));
        assertTrue(Double.isNaN(view.getRightMargin()));
        assertTrue(left.isDisabled());
        assertEquals("Margins: the theme's padding", readOut.getText());
    }

    @Test
    @DisplayName("the divider beside the view narrows it until its bar hides the slider and Reset, the group staying")
    void dividerNarrowsTheView() throws IOException {
        Parent root = layOut();
        SplitPane editorRow = assertInstanceOf(SplitPane.class, root.lookup("#editorRow"));
        MarkdownView view = view(root);
        assertTrue(editorRow.getItems().contains(view.getParent()), "the view under its bar, beside the divider");
        Node slider = view.lookup(".markdown-zoom-slider");
        Node reset = view.lookup(".markdown-zoom-reset");
        assertTrue(slider.isVisible());
        assertTrue(reset.isVisible());

        // The reader drags the divider as far right as it goes: the view keeps its minimum width.
        editorRow.setDividerPositions(1.0);
        root.layout();

        assertTrue(view.getWidth() < 100, "the view is " + view.getWidth() + " px wide");
        assertFalse(slider.isVisible());
        assertFalse(reset.isVisible());
        assertTrue(view.lookup(".markdown-zoom-group").isVisible());
    }

    @Test
    @DisplayName("the page's description tells about the zoom bar")
    void descriptionMentionsTheZoom() throws IOException {
        List<String> descriptions = new ArrayList<>();
        for (Node node : layOut().lookupAll(".showcase-page-description")) {
            descriptions.add(((Label) node).getText());
        }

        assertTrue(anyContains(descriptions, "zoom bar"), descriptions.toString());
        assertTrue(anyContains(descriptions, "50 to 200 %"), descriptions.toString());
    }

    @Test
    @DisplayName("the page's description tells about code coloured by its language and the Code colours card")
    void descriptionMentionsTheCodeColors() throws IOException {
        VBox page = assertInstanceOf(VBox.class, layOut());
        Label description = assertInstanceOf(Label.class, page.getChildren().get(1));
        assertTrue(description.getStyleClass().contains("showcase-page-description"));

        assertTrue(description.getText().contains("names a language the view knows"), description.getText());
        assertTrue(description.getText().contains("Code colours card"), description.getText());
    }

    @Test
    @DisplayName("in the showcase's scroll pane, which fits the page to its height, the view keeps its height")
    void viewKeepsItsHeightInAShortWindow() throws IOException {
        URL url = MarkdownShowcaseTest.class.getResource("/org/karthu/fx/showcase/showcase/markdown-showcase.fxml");
        assertNotNull(url);
        Parent page = FXMLLoader.load(url);
        // As the showcase hosts a page: a scroll pane that fits it to its width and height, here a short window.
        ScrollPane host = new ScrollPane(page);
        host.setFitToWidth(true);
        host.setFitToHeight(true);
        Scene scene = new Scene(host, 1400, 700);
        ThemeManager.apply(scene);
        host.applyCss();
        host.layout();

        SplitPane editorRow = assertInstanceOf(SplitPane.class, page.lookup("#editorRow"));
        assertTrue(editorRow.getHeight() >= 680, "the row with the view is " + editorRow.getHeight() + "px high");
        assertTrue(view(page).getHeight() >= 670, "the view is " + view(page).getHeight() + "px high");
        assertTrue(page.getLayoutBounds().getHeight() > 700, "the page is higher than the window and scrolls");
        // The Code colours card below the editor row, its text area showing its whole rule.
        TextArea css = codeColors(page);
        double rowBottom = editorRow.localToScene(editorRow.getLayoutBounds()).getMaxY();
        assertTrue(css.localToScene(css.getLayoutBounds()).getMinY() > rowBottom, "the card is below the editor row");
        assertTrue(css.getHeight() >= 300, "the text area is " + css.getHeight() + "px high");
    }

    @Test
    @DisplayName("the live source has a block of each of the seven languages, showing the categories its language marks")
    void liveSourceShowsEveryLanguage() throws IOException {
        Parent root = layOut();
        String source = assertInstanceOf(TextArea.class, root.lookup("#sourceArea")).getText();
        List<String> languages = fenceLanguages(source);
        List<Node> blocks = new ArrayList<>(view(root).lookupAll(".markdown-code-block"));
        assertEquals(languages.size(), blocks.size(), "a code block per fence: " + languages);

        // What each built-in lexer marks.
        Map<String, Set<CodeCategory>> marked = Map.of(
                "java", EnumSet.of(CodeCategory.COMMENT, CodeCategory.DOC, CodeCategory.STRING, CodeCategory.ESCAPE,
                        CodeCategory.NUMBER, CodeCategory.KEYWORD, CodeCategory.LITERAL, CodeCategory.ANNOTATION,
                        CodeCategory.CONSTANT, CodeCategory.TYPE, CodeCategory.FUNCTION, CodeCategory.OPERATOR,
                        CodeCategory.PUNCTUATION),
                "json", EnumSet.of(CodeCategory.PROPERTY, CodeCategory.STRING, CodeCategory.ESCAPE,
                        CodeCategory.NUMBER, CodeCategory.LITERAL, CodeCategory.PUNCTUATION),
                "sql", EnumSet.of(CodeCategory.COMMENT, CodeCategory.STRING, CodeCategory.KEYWORD,
                        CodeCategory.LITERAL, CodeCategory.TYPE, CodeCategory.VARIABLE, CodeCategory.FUNCTION,
                        CodeCategory.NUMBER, CodeCategory.OPERATOR, CodeCategory.PUNCTUATION),
                "xml", EnumSet.of(CodeCategory.COMMENT, CodeCategory.TAG, CodeCategory.ATTRIBUTE, CodeCategory.STRING,
                        CodeCategory.ESCAPE),
                "yaml", EnumSet.of(CodeCategory.COMMENT, CodeCategory.PROPERTY, CodeCategory.STRING,
                        CodeCategory.ESCAPE, CodeCategory.NUMBER, CodeCategory.LITERAL, CodeCategory.VARIABLE,
                        CodeCategory.PUNCTUATION),
                "bash", EnumSet.of(CodeCategory.COMMENT, CodeCategory.STRING, CodeCategory.VARIABLE,
                        CodeCategory.KEYWORD, CodeCategory.FUNCTION, CodeCategory.NUMBER, CodeCategory.OPERATOR,
                        CodeCategory.PUNCTUATION),
                "markdown", EnumSet.of(CodeCategory.HEADING, CodeCategory.EMPHASIS, CodeCategory.CODE,
                        CodeCategory.LINK, CodeCategory.LIST, CodeCategory.QUOTE, CodeCategory.PUNCTUATION));
        Set<CodeCategory> shown = EnumSet.noneOf(CodeCategory.class);
        for (Map.Entry<String, Set<CodeCategory>> language : marked.entrySet()) {
            Set<CodeCategory> best = EnumSet.noneOf(CodeCategory.class);
            for (int index = 0; index < blocks.size(); index++) {
                if (languages.get(index).equals(language.getKey())) {
                    Set<CodeCategory> categories = categories(blocks.get(index));
                    if (categories.size() > best.size()) {
                        best = categories;
                    }
                }
            }
            assertTrue(best.containsAll(language.getValue()), language.getKey() + " shows " + best);
            shown.addAll(best);
        }
        assertEquals(EnumSet.allOf(CodeCategory.class), shown, "every category on the page");
    }

    @Test
    @DisplayName("the Code colours card holds one rule for the page's view: all 23 variables, the base ones with their"
            + " default colours, the fine ones with their references")
    void codeColorsCardHoldsEveryVariable() throws IOException {
        String css = codeColors(layOut()).getText();

        assertTrue(css.startsWith(".markdown-view {\n"), css);
        assertTrue(css.endsWith("}\n"), css);
        for (CodeCategory category : CodeCategory.values()) {
            assertEquals(1, occurrences(css, "    " + category.colorVariable() + ": "),
                    category.colorVariable() + " in " + css);
        }
        // The defaults: a base colour, a fine one by its fallback, one by the code text.
        assertTrue(css.contains("    -fx-code-keyword-color: #c88bff;\n"), css);
        assertTrue(css.contains("    -fx-code-tag-color: #ff8fa3;\n"), css);
        assertTrue(css.contains("    -fx-code-annotation-color: -fx-code-keyword-color;\n"), css);
        assertTrue(css.contains("    -fx-code-constant-color: -fx-code-literal-color;\n"), css);
        assertTrue(css.contains("    -fx-code-emphasis-color: -fx-code-text-color;\n"), css);
    }

    @Test
    @DisplayName("a keyword colour changed in the card reaches the view's keyword runs at once")
    void changedKeywordColorReachesTheView() throws IOException {
        Parent root = layOut();
        TextArea css = codeColors(root);
        List<Text> keywords = runs(view(root), "code-keyword");
        assertFalse(keywords.isEmpty(), "the sample has keywords");
        for (Text keyword : keywords) {
            assertEquals(Color.web("#c88bff"), keyword.getFill(), "the default, " + keyword.getText());
        }

        css.setText(css.getText().replace("-fx-code-keyword-color: #c88bff;", "-fx-code-keyword-color: #ff0000;"));
        root.applyCss();

        for (Text keyword : keywords) {
            assertEquals(Color.web("#ff0000"), keyword.getFill(), keyword.getText());
        }
    }

    @Test
    @DisplayName("a fine category follows its base changed in the card until it is set itself")
    void fineCategoryFollowsItsBase() throws IOException {
        Parent root = layOut();
        TextArea css = codeColors(root);
        Text keyword = runs(view(root), "code-keyword").get(0);
        Text annotation = runs(view(root), "code-annotation").get(0);

        css.setText(css.getText().replace("-fx-code-keyword-color: #c88bff;", "-fx-code-keyword-color: #ff0000;"));
        root.applyCss();
        assertEquals(Color.web("#ff0000"), annotation.getFill(), "the annotation follows the keyword");

        css.setText(css.getText().replace("-fx-code-annotation-color: -fx-code-keyword-color;",
                "-fx-code-annotation-color: #00ff00;"));
        root.applyCss();
        assertEquals(Color.web("#00ff00"), annotation.getFill(), "set itself");
        assertEquals(Color.web("#ff0000"), keyword.getFill(), "the keyword unchanged");
    }

    @Test
    @DisplayName("the card's first text leaves the theme's look, Defaults restores the text and the look")
    void defaultsRestoresTextAndLook() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        TextArea css = codeColors(root);
        String first = css.getText();
        Map<CodeCategory, Paint> withFirstText = fills(view);
        // The theme's look: the view without the card's stylesheet.
        List<String> sheets = new ArrayList<>(view.getStylesheets());
        assertEquals(1, sheets.size(), "the card's stylesheet: " + sheets);
        assertTrue(sheets.get(0).startsWith("data:text/css"), "loaded from a data: address, no file");
        view.getStylesheets().clear();
        root.applyCss();
        Map<CodeCategory, Paint> theme = fills(view);
        view.getStylesheets().setAll(sheets);
        root.applyCss();
        assertEquals(theme, withFirstText, "the first text sets the theme's colours");

        css.setText(".markdown-view { -fx-code-keyword-color: #ff0000; -fx-code-string-color: #00ff00; }");
        root.applyCss();
        assertEquals(Color.web("#ff0000"), fills(view).get(CodeCategory.KEYWORD));
        Button defaults = assertInstanceOf(Button.class, root.lookup("#codeColorsDefaults"));
        assertEquals("Defaults", defaults.getText());
        defaults.fire();
        root.applyCss();

        assertEquals(first, css.getText());
        assertEquals(theme, fills(view), "the theme's look again");
        assertEquals(1, view.getStylesheets().size(), "one stylesheet of the card at a time");
    }

    @Test
    @DisplayName("invalid CSS in the card leaves the view working in the theme's colours")
    void invalidCssLeavesTheViewWorking() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        TextArea source = assertInstanceOf(TextArea.class, root.lookup("#sourceArea"));
        Map<CodeCategory, Paint> theme = fills(view);

        codeColors(root).setText("this is { not css ;;; -fx-code-keyword-color: }} .markdown-view {");
        root.applyCss();
        root.layout();
        assertEquals(theme, fills(view));

        source.setText("Text.\n\n```java\nint x = 1;\n```");
        root.applyCss();
        root.layout();
        assertEquals(theme.get(CodeCategory.KEYWORD), runs(view, "code-keyword").get(0).getFill(),
                "a new text renders and is coloured");
        assertEquals("int", runs(view, "code-keyword").get(0).getText());
    }

    @Test
    @DisplayName("the view's base address is the folder of the sample's images, which holds every image the sample"
            + " shows, an SVG file and no missing.png")
    void imageBaseIsTheFolderOfTheImages() throws IOException {
        String base = view(layOut()).getImageBase();
        assertNotNull(base, "the page sets a base address");
        assertTrue(base.endsWith("/images/"), base);

        for (Map.Entry<String, List<Integer>> file : IMAGE_FILES.entrySet()) {
            Image image = new Image(URI.create(base).resolve(file.getKey()).toString());
            assertFalse(image.isError(), file.getKey() + " decodes");
            assertEquals(file.getValue().get(0), (int) image.getWidth(), file.getKey());
            assertEquals(file.getValue().get(1), (int) image.getHeight(), file.getKey());
        }
        try (InputStream svg = URI.create(base).resolve("diagram.svg").toURL().openStream()) {
            assertTrue(new String(svg.readAllBytes(), StandardCharsets.UTF_8).contains("<svg"), "an SVG file");
        }
        assertThrows(IOException.class, () -> URI.create(base).resolve("missing.png").toURL().openStream().close(),
                "the missing image has no file");
    }

    @Test
    @WaitsForImages
    @DisplayName("the sample shows images of every kind: a 4K screenshot shrunk to the text, a tall image shrunk to the"
            + " visible height, a dialog, an animated GIF playing, inline icons, a linked image, a table's images, and"
            + " the boxes of a missing image, an SVG file and an image from the internet while the switch is off")
    void sampleShowsEveryKindOfImage() throws Exception {
        Parent root = onFx(() -> layOut());
        MarkdownView view = onFx(() -> view(root));
        assertTrue(until(root, () -> showPictures(view, SCREENSHOT, DIALOG, SIDE_BAR, RECORDING, BADGE, ICON)
                        && errorBoxes(view).size() == 3),
                () -> "the images arrive in time: " + describeImages(view));

        onFx(() -> {
            // The 4K screenshot: a bitmap the view computed, shrunk to the width the text wraps at.
            ImageView screenshot = picture(view, SCREENSHOT);
            double textWidth = paragraphWidth(view);
            double shownWidth = sceneBounds(screenshot).getWidth();
            assertTrue(shownWidth <= textWidth + 0.5 && shownWidth > 100, shownWidth + " for a text " + textWidth);
            assertInstanceOf(WritableImage.class, screenshot.getImage(), "shrunk by the view's area average");
            assertEquals(Math.round(shownWidth), Math.round(screenshot.getImage().getWidth()),
                    "as many pixels as it shows on a screen of scale 1.0");
            assertTrue(screenshot.getParent().getStyleClass().contains("markdown-image-block"));

            // The tall image: shrunk to the visible height.
            ImageView sideBar = picture(view, SIDE_BAR);
            double visible = ((ScrollPane) view.lookup(".scroll-pane")).getViewportBounds().getHeight();
            assertTrue(sceneBounds(sideBar).getHeight() <= visible + 0.5,
                    sceneBounds(sideBar).getHeight() + " in a visible height of " + visible);
            assertTrue(sceneBounds(sideBar).getHeight() > visible / 2, "not smaller than it must be");

            // The animated GIF plays: JavaFX's own image with all its pixels.
            ImageView recording = picture(view, RECORDING);
            assertEquals(Boolean.TRUE, plays(view, RECORDING), "the GIF itself, playing");
            assertEquals(486, (int) recording.getImage().getWidth());

            // Inline icons: in a sentence and in a heading.
            List<String> inlineIn = new ArrayList<>();
            for (Node inline : view.lookupAll(".markdown-image-inline")) {
                ImageView icon = (ImageView) inline.lookup(".markdown-image");
                assertEquals(ICON, icon.getAccessibleText());
                inlineIn.add(inline.getParent().getStyleClass().contains("markdown-heading") ? "heading" : "text");
            }
            assertTrue(inlineIn.contains("heading") && inlineIn.contains("text"), inlineIn.toString());

            // The table's images: an icon and the alt text each.
            List<String> cellTexts = new ArrayList<>();
            for (Node cell : view.lookupAll(".markdown-image-cell")) {
                if (cell instanceof Text run && !(cell instanceof FontIcon)) {
                    cellTexts.add(run.getText().replace('\u00a0', ' ').strip());
                }
            }
            assertTrue(cellTexts.contains("The badge") && cellTexts.contains("The side bar"), cellTexts.toString());

            // The boxes: missing, SVG, and the image from the internet while the switch is off.
            Map<String, List<String>> boxes = errorBoxes(view);
            assertEquals(List.of("A missing image", "Image not found"), boxes.get("missing.png"));
            assertEquals(List.of("A diagram in SVG", "Image format not supported"), boxes.get("diagram.svg"));
            assertEquals(List.of("The W3C logo", "Image not found"), boxes.get(INTERNET_IMAGE), "not loaded");
            return null;
        });
    }

    @Test
    @WaitsForImages
    @DisplayName("a still click on the linked image follows its link: the page shows the address its handler receives")
    void linkedImageFollowsItsLink() throws Exception {
        Parent root = onFx(() -> layOut());
        MarkdownView view = onFx(() -> view(root));
        assertTrue(until(root, () -> showPictures(view, BADGE)), () -> describeImages(view));

        String shown = onFx(() -> {
            ImageView badge = picture(view, BADGE);
            Point2D center = new Point2D(sceneBounds(badge).getCenterX(), sceneBounds(badge).getCenterY());
            Event.fireEvent(badge, click(MouseEvent.MOUSE_PRESSED, center));
            Event.fireEvent(badge, click(MouseEvent.MOUSE_RELEASED, center));
            Event.fireEvent(badge, click(MouseEvent.MOUSE_CLICKED, center));
            return assertInstanceOf(Label.class, root.lookup("#linkLabel")).getText();
        });

        assertEquals("Last clicked link: https://example.com/karthu-fx", shown);
    }

    @Test
    @WaitsForImages
    @DisplayName("the Play animations switch is on and follows the view: switched off, the GIF shows its first frame as"
            + " a still image, switched on, it plays again")
    void playAnimationsSwitch() throws Exception {
        Parent root = onFx(() -> layOut());
        MarkdownView view = onFx(() -> view(root));
        CheckBox animations = onFx(() -> assertInstanceOf(CheckBox.class, root.lookup("#animationSwitch")));
        assertEquals("Play animations", onFx(() -> animations.getText()));
        assertTrue(onFx(() -> animations.isSelected() && view.isImageAnimation()), "on at first");
        assertTrue(until(root, () -> Boolean.TRUE.equals(plays(view, RECORDING))), "the GIF plays");

        onFx(() -> {
            animations.setSelected(false);
            return null;
        });
        assertFalse(onFx(() -> view.isImageAnimation()));
        assertTrue(until(root, () -> Boolean.FALSE.equals(plays(view, RECORDING))), "its first frame, still");

        onFx(() -> {
            view.setImageAnimation(true);
            return null;
        });
        assertTrue(onFx(() -> animations.isSelected()), "the switch follows the view");
        assertTrue(until(root, () -> Boolean.TRUE.equals(plays(view, RECORDING))), "playing again");
    }

    @Test
    @DisplayName("the Images from the internet switch is off and follows the view, and the sample's internet image"
            + " waits for it")
    void imagesFromTheInternetSwitch() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        CheckBox internet = assertInstanceOf(CheckBox.class, root.lookup("#remoteImagesSwitch"));
        assertEquals("Images from the internet", internet.getText());
        assertFalse(internet.isSelected());
        assertFalse(view.isRemoteImages(), "off at first: the page sends no request by itself");
        String source = assertInstanceOf(TextArea.class, root.lookup("#sourceArea")).getText();
        assertTrue(source.contains("](" + INTERNET_IMAGE), "the sample has an image from the internet");

        // No connection in a test: from here on the loader refuses every internet address itself.
        ImageLoader own = view.getImageLoader();
        List<String> refused = new CopyOnWriteArrayList<>();
        view.setImageLoader(address -> {
            if (address.startsWith("http:") || address.startsWith("https:")) {
                refused.add(address);
                throw new IOException("no internet in a test");
            }
            return own.load(address);
        });
        internet.setSelected(true);
        assertTrue(view.isRemoteImages(), "the switch switches the view");
        view.setRemoteImages(false);
        assertFalse(internet.isSelected(), "the switch follows the view");
    }

    @Test
    @DisplayName("the page's description tells about images and their two switches")
    void descriptionMentionsTheImages() throws IOException {
        VBox page = assertInstanceOf(VBox.class, layOut());
        String description = assertInstanceOf(Label.class, page.getChildren().get(1)).getText();

        assertTrue(description.contains("image"), description);
        assertTrue(description.contains("Play animations"), description);
        assertTrue(description.contains("Images from the internet"), description);
    }

    @Test
    @DisplayName("the page's description tells about the search field, its direction and Insert a line at the top")
    void descriptionMentionsTheSearchAndTheUpdate() throws IOException {
        VBox page = assertInstanceOf(VBox.class, layOut());
        String description = assertInstanceOf(Label.class, page.getChildren().get(1)).getText();

        assertTrue(description.contains("search field"), description);
        assertTrue(description.contains("Backward"), description);
        assertTrue(description.contains("Insert a line at the top"), description);
        assertTrue(description.contains("updateMarkdown"), description);
    }

    @Test
    @DisplayName("the sample tells how to search it and how to try a new version of its text")
    void sampleTellsAboutTheSearchAndTheUpdate() throws IOException {
        String source = assertInstanceOf(TextArea.class, layOut().lookup("#sourceArea")).getText();

        assertTrue(source.contains("\n## Search\n"), "a section on the search");
        assertTrue(source.contains("Insert a line at the top"), "how to try a new version of the text");
    }

    @Test
    @DisplayName("a search field in a bar above the view searches it: the field's text is the view's search, both"
            + " ways")
    void searchFieldSearchesTheView() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        SearchInput search = assertInstanceOf(SearchInput.class, root.lookup("#searchField"));
        Bounds field = sceneBounds(search);
        Bounds shown = sceneBounds(view);
        assertTrue(field.getMaxY() <= shown.getMinY(), "the field " + field + " above the view " + shown);
        assertTrue(field.getMinX() >= shown.getMinX() - 0.5 && field.getMaxX() <= shown.getMaxX() + 0.5,
                "the field " + field + " within the view's width " + shown);

        search.setText("table");
        assertEquals("table", view.getSearchText());
        assertTrue(view.getSearchMatchCount() > 1, "the sample has matches: " + view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex(), "forward from the first match");

        view.clearSearch();
        assertEquals("", search.getText(), "the field follows the view");
    }

    @Test
    @DisplayName("the sample's own example finds what it promises: utf in its text, its code and a table")
    void sampleExampleFindsTextCodeAndTables() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        SearchInput search = assertInstanceOf(SearchInput.class, root.lookup("#searchField"));

        search.setText("utf");

        assertEquals(7, view.getSearchMatchCount(), "one in the text, three in the code, three in a table");
    }

    @Test
    @DisplayName("the choice of the direction beside the search field, forward at first, sets the view's direction and"
            + " follows it")
    void directionChoiceSetsTheViewsDirection() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        SearchInput search = assertInstanceOf(SearchInput.class, root.lookup("#searchField"));
        assertInstanceOf(ComboInput.class, root.lookup("#searchDirection"));
        @SuppressWarnings("unchecked")
        ComboInput<SearchDirection> direction = (ComboInput<SearchDirection>) root.lookup("#searchDirection");
        Bounds choice = sceneBounds(direction);
        assertTrue(choice.getMinX() >= sceneBounds(search).getMaxX(), "beside the field, on its right");
        assertTrue(choice.getMaxY() <= sceneBounds(view).getMinY(), "above the view");

        assertEquals(List.of(SearchDirection.FORWARD, SearchDirection.BACKWARD), new ArrayList<>(direction.getItems()));
        assertEquals("Forward", direction.getDisplayText().apply(SearchDirection.FORWARD));
        assertEquals("Backward", direction.getDisplayText().apply(SearchDirection.BACKWARD));
        assertEquals(SearchDirection.FORWARD, direction.getValue());

        search.setText("table");
        int matches = view.getSearchMatchCount();
        direction.setValue(SearchDirection.BACKWARD);
        assertEquals(SearchDirection.BACKWARD, view.getSearchDirection());
        view.nextSearchMatch();
        assertEquals(matches - 1, view.getSearchMatchIndex(), "backward from the first match to the last");

        view.setSearchDirection(SearchDirection.FORWARD);
        assertEquals(SearchDirection.FORWARD, direction.getValue(), "the choice follows the view");
    }

    @Test
    @DisplayName("Insert a line at the top gives the view a new version of its text, a line more at the top, which the"
            + " source shows too, and the view stays at the heading the reader was reading")
    void insertLineKeepsThePlace() throws IOException {
        Parent root = layOut();
        MarkdownView view = view(root);
        TextArea source = assertInstanceOf(TextArea.class, root.lookup("#sourceArea"));
        Button insert = assertInstanceOf(Button.class, root.lookup("#insertLineButton"));
        assertEquals("Insert a line at the top", insert.getText());
        String before = source.getText();

        // The reader has scrolled down to the tables: their heading a little below the top of the visible area.
        ScrollPane pane = assertInstanceOf(ScrollPane.class, view.lookup(".scroll-pane"));
        scrollTo(pane, heading(view, "Tables"), 40);
        root.layout();
        double readAt = sceneBounds(heading(view, "Tables")).getMinY();
        assertEquals(sceneBounds(pane).getMinY() + 40, readAt, 2.0, "scrolled there");

        insert.fire();
        root.applyCss();
        root.layout();

        String line = "**Update 1.** A line inserted at the top with `updateMarkdown`.";
        assertEquals(line + "\n\n" + before, source.getText());
        assertEquals(source.getText(), view.getMarkdown());
        assertEquals(readAt, sceneBounds(heading(view, "Tables")).getMinY(), 1.0, "the heading stays where it was");

        insert.fire();
        assertTrue(source.getText().startsWith("**Update 2.** A line inserted at the top with `updateMarkdown`.\n\n"
                + line + "\n\n"), "a second line above the first");
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /**
     * Runs every test of the class on the JavaFX Application Thread, but for those that wait for images
     * ({@link WaitsForImages}): the page's view shows what its background threads load there, so a test that runs on
     * that thread never meets an image arriving while it works.
     */
    static final class OnFxThread implements InvocationInterceptor {

        @Override
        public void interceptTestMethod(Invocation<Void> invocation,
                                        ReflectiveInvocationContext<Method> invocationContext,
                                        ExtensionContext extensionContext) throws Throwable {
            if (invocationContext.getExecutable().isAnnotationPresent(WaitsForImages.class)) {
                invocation.proceed();
                return;
            }
            CompletableFuture<Void> done = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    invocation.proceed();
                    done.complete(null);
                } catch (Throwable failure) {
                    done.completeExceptionally(failure);
                }
            });
            try {
                done.get(60, TimeUnit.SECONDS);
            } catch (ExecutionException failure) {
                throw failure.getCause();
            }
        }
    }

    /**
     * Marks a test that waits for the view's images: it runs on the test's thread and reaches the page through
     * {@link #onFx(Callable)} only.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface WaitsForImages {
    }

    /**
     * Runs code on the JavaFX Application Thread and waits for its result, at most 10 s.
     */
    private static <T> T onFx(Callable<T> code) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(code.call());
            } catch (Exception | AssertionError failure) {
                result.completeExceptionally(failure);
            }
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException failure) {
            if (failure.getCause() instanceof AssertionError assertion) {
                throw assertion;
            }
            throw failure;
        }
    }

    /**
     * Lays the page out on the JavaFX Application Thread again and again until a condition holds there, at most 20 s,
     * while the view's images arrive.
     */
    private static boolean until(Parent root, Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            boolean holds = onFx(() -> {
                root.applyCss();
                root.layout();
                return condition.call();
            });
            if (holds) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    /** Whether the view shows a picture for each of the alt texts. */
    private static boolean showPictures(MarkdownView view, String... alts) {
        for (String alt : alts) {
            ImageView picture = pictureOrNull(view, alt);
            if (picture == null || picture.getImage() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the picture of an alt text plays an animation ({@code TRUE}) or shows a still image ({@code FALSE}),
     * {@code null} while it shows none. JavaFX gives no pixel reader for an animated image, so a picture whose image
     * has none plays.
     */
    private static Boolean plays(MarkdownView view, String alt) {
        ImageView picture = pictureOrNull(view, alt);
        if (picture == null || picture.getImage() == null) {
            return null;
        }
        return picture.getImage().getPixelReader() == null;
    }

    /** The picture the view shows for an alt text, the first in document order. */
    private static ImageView picture(MarkdownView view, String alt) {
        ImageView picture = pictureOrNull(view, alt);
        assertNotNull(picture, "a picture of " + alt);
        return picture;
    }

    private static ImageView pictureOrNull(MarkdownView view, String alt) {
        for (Node node : view.lookupAll(".markdown-image")) {
            if (node instanceof ImageView picture && alt.equals(picture.getAccessibleText())) {
                return picture;
            }
        }
        return null;
    }

    /** The view's error boxes by their tooltip, the image's address: the texts of their labels. */
    private static Map<String, List<String>> errorBoxes(MarkdownView view) {
        Map<String, List<String>> boxes = new HashMap<>();
        for (Node box : view.lookupAll(".markdown-image-error")) {
            Tooltip tooltip = (Tooltip) box.getProperties().get("javafx.scene.control.Tooltip");
            List<String> labels = new ArrayList<>();
            for (Node label : box.lookupAll(".label")) {
                labels.add(((Label) label).getText());
            }
            boxes.put(tooltip == null ? "" : tooltip.getText(), labels);
        }
        return boxes;
    }

    /** What the view shows for its images, for a failure's message. */
    private static String describeImages(MarkdownView view) {
        try {
            return onFx(() -> {
                List<String> shown = new ArrayList<>();
                for (Node node : view.lookupAll(".markdown-image")) {
                    shown.add(node.getAccessibleText() + (((ImageView) node).getImage() == null ? " (empty)" : ""));
                }
                return "pictures " + shown + ", boxes " + errorBoxes(view);
            });
        } catch (Exception failure) {
            return failure.toString();
        }
    }

    /** The width the view's paragraphs wrap at: the widest paragraph's flow. */
    private static double paragraphWidth(MarkdownView view) {
        double width = 0;
        for (Node paragraph : view.lookupAll(".markdown-paragraph")) {
            width = Math.max(width, ((Region) paragraph).getWidth());
        }
        return width;
    }

    private static Bounds sceneBounds(Node node) {
        return node.localToScene(node.getLayoutBounds());
    }

    /**
     * Scrolls a scroll pane so that a node of its content lies a distance below the top of the visible area, as far as
     * the scroll range allows.
     */
    private static void scrollTo(ScrollPane pane, Node node, double below) {
        Node content = pane.getContent();
        double top = content.sceneToLocal(sceneBounds(node)).getMinY();
        double range = content.getLayoutBounds().getHeight() - pane.getViewportBounds().getHeight();
        pane.setVvalue(Math.max(0, Math.min(1, (top - below) / range)));
    }

    /** The view's heading of a text. */
    private static Node heading(MarkdownView view, String text) {
        for (Node heading : view.lookupAll(".markdown-heading")) {
            if (flowText(heading).equals(text)) {
                return heading;
            }
        }
        throw new AssertionError("no heading " + text);
    }

    /** The text of a heading's or a paragraph's flow: its runs one after the other. */
    private static String flowText(Node flow) {
        StringBuilder text = new StringBuilder();
        for (Node run : ((Parent) flow).getChildrenUnmodifiable()) {
            if (run instanceof Text piece) {
                text.append(piece.getText());
            }
        }
        return text.toString();
    }

    /** A mouse event of the primary button at a scene point, still since the press. */
    private static MouseEvent click(EventType<MouseEvent> type, Point2D at) {
        return new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), MouseButton.PRIMARY, 1, false, false,
                false, false, type == MouseEvent.MOUSE_PRESSED, false, false, false, false, true, null);
    }

    /**
     * The fill of a run of each category the view shows, the first run whose finest category it is.
     */
    private static Map<CodeCategory, Paint> fills(MarkdownView view) {
        Map<CodeCategory, Paint> fills = new EnumMap<>(CodeCategory.class);
        for (CodeCategory category : CodeCategory.values()) {
            List<Text> runs = runs(view, category.styleClass());
            if (!runs.isEmpty()) {
                fills.put(category, runs.get(0).getFill());
            }
        }
        return fills;
    }

    /**
     * The runs of the view's coloured code blocks whose finest category is the one of a style class: they carry the
     * class and no finer one, so a keyword run is no annotation's or list marker's.
     */
    private static List<Text> runs(MarkdownView view, String styleClass) {
        List<Text> runs = new ArrayList<>();
        for (Node node : view.lookupAll("." + styleClass)) {
            Text run = assertInstanceOf(Text.class, node);
            CodeCategory finest = finestCategory(run);
            if (finest != null && finest.styleClass().equals(styleClass)) {
                runs.add(run);
            }
        }
        return runs;
    }

    /**
     * The languages of a Markdown text's fenced code blocks in document order, the first word of each opening fence's
     * info string in lower case, empty for none. A fence ends at a line of the same fence characters.
     */
    private static List<String> fenceLanguages(String markdown) {
        List<String> languages = new ArrayList<>();
        String open = null;
        for (String line : markdown.split("\n", -1)) {
            String trimmed = line.strip();
            if (open == null && (trimmed.startsWith("```") || trimmed.startsWith("~~~"))) {
                open = trimmed.substring(0, 3);
                String info = trimmed.substring(3).strip();
                int end = info.indexOf(' ');
                languages.add((end < 0 ? info : info.substring(0, end)).toLowerCase(Locale.ROOT));
            } else if (open != null && trimmed.startsWith(open) && trimmed.replace(open.substring(0, 1), "").isEmpty()) {
                open = null;
            }
        }
        return languages;
    }

    /**
     * The categories a code block shows: the finest category of each of its runs.
     */
    private static Set<CodeCategory> categories(Node block) {
        Set<CodeCategory> categories = EnumSet.noneOf(CodeCategory.class);
        for (Node node : block.lookupAll(".markdown-text")) {
            CodeCategory category = finestCategory(assertInstanceOf(Text.class, node));
            if (category != null) {
                categories.add(category);
            }
        }
        return categories;
    }

    /**
     * The finest category a run carries the class of: of the categories whose classes it carries, the one with the
     * longest fallback chain. {@code null} for a plain run.
     */
    private static CodeCategory finestCategory(Text run) {
        CodeCategory finest = null;
        int finestChain = 0;
        for (CodeCategory category : CodeCategory.values()) {
            int chain = 0;
            for (CodeCategory step = category; step != null; step = step.fallback().orElse(null)) {
                chain++;
            }
            if (run.getStyleClass().contains(category.styleClass()) && chain > finestChain) {
                finest = category;
                finestChain = chain;
            }
        }
        return finest;
    }

    /** The text area of the <i>Code colours</i> card. */
    private static TextArea codeColors(Parent root) {
        return assertInstanceOf(TextArea.class, root.lookup("#codeColorsArea"));
    }

    private static int occurrences(String text, String part) {
        int count = 0;
        int from = text.indexOf(part);
        while (from >= 0) {
            count++;
            from = text.indexOf(part, from + part.length());
        }
        return count;
    }

    private static boolean anyContains(List<String> texts, String part) {
        for (String text : texts) {
            if (text.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /** Loads the page and lays it out in the theme, in a scene that is never shown. */
    private static Parent layOut() throws IOException {
        URL url = MarkdownShowcaseTest.class.getResource("/org/karthu/fx/showcase/showcase/markdown-showcase.fxml");
        assertNotNull(url);
        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root, 1400, 900);
        ThemeManager.apply(scene);
        root.applyCss();
        root.layout();
        return root;
    }

    /** The page's Markdown view, the one beside the source. */
    private static MarkdownView view(Parent root) {
        return assertInstanceOf(MarkdownView.class, root.lookup(".markdown-view"));
    }
}
