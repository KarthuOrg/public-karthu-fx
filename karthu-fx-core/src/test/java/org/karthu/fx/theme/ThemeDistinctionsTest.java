package org.karthu.fx.theme;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TooltipSkin;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.markdown.MarkdownView;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The places Nebula Light tells apart that share one colour in Nebula Dark, seen from outside: the resolved colours of
 * themed nodes in scenes that are never shown. The expected light colours are the values of the colour design, the
 * expected dark ones the colours those places had before Nebula Light told them apart.
 */
class ThemeDistinctionsTest {

    private static final Color DARK_SURFACE = Color.web("#24273c");
    private static final Color DARK_BORDER = Color.web("#42486e");

    private static final Color LIGHT_WINDOW = Color.web("#E4E5E8");
    private static final Color LIGHT_FIELD = Color.web("#F3F1E8");
    private static final Color LIGHT_BORDER = Color.web("#C2C4CC");
    private static final Color LIGHT_RAISED = Color.web("#DCDDE1");
    private static final Color LIGHT_TEXT = Color.web("#1F2230");

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @AfterEach
    void backToTheDefault() {
        ThemeManager.setAppearance(Appearance.DARK);
    }

    @Test
    @DisplayName("an editable field lies on warm ivory in Light, apart from the grey window, and on the surface in Dark")
    void fieldOnIvory() {
        TextField field = new TextField("text");
        VBox root = themed(Appearance.LIGHT, field);

        assertEquals(LIGHT_WINDOW, fill(root, 0), "the window");
        assertEquals(LIGHT_BORDER, fill(field, 0), "the field's border");
        assertEquals(LIGHT_FIELD, fill(field, 1), "the field's background");

        ThemeManager.setAppearance(Appearance.DARK);
        root.applyCss();

        assertEquals(DARK_BORDER, fill(field, 0), "the field's border");
        assertEquals(DARK_SURFACE, fill(field, 1), "the field's background");
    }

    /**
     * A disabled control keeps its structure in both schemes: the field's own colours, dimmed as a disabled button.
     */
    @Test
    @DisplayName("a disabled field keeps the field's ivory and border in Light, dimmed by half")
    void disabledFieldDimmed() {
        TextField field = new TextField("text");
        field.setDisable(true);
        themed(Appearance.LIGHT, field);

        assertEquals(LIGHT_BORDER, fill(field, 0), "the field's border");
        assertEquals(LIGHT_FIELD, fill(field, 1), "the field's background");
        Text text = (Text) field.lookup(".text");
        assertEquals(LIGHT_TEXT, text.getFill(), "the text");
        assertEquals(0.5, field.getOpacity(), "dimmed");
    }

    @Test
    @DisplayName("the Markdown view's content lies on the raised grey in Light, darker than the window, on the surface"
            + " in Dark")
    void markdownViewRaised() {
        MarkdownView view = new MarkdownView();
        VBox root = themed(Appearance.LIGHT, view);

        assertEquals(LIGHT_BORDER, fill(view, 0), "the view's border");
        assertEquals(LIGHT_RAISED, fill(view, 1), "the view's content area");

        ThemeManager.setAppearance(Appearance.DARK);
        root.applyCss();

        assertEquals(DARK_BORDER, fill(view, 0), "the view's border");
        assertEquals(DARK_SURFACE, fill(view, 1), "the view's content area");
    }

    /**
     * The danger button stays filled. In Light its outline takes the danger family's pale reds, and its fill turns
     * darker under the pointer and darker still while pressed; in Dark the fill turns lighter under the pointer, as
     * before.
     */
    @Test
    @DisplayName("a danger button in Light: the red fill with white text, darker on hover and pressed, in the danger"
            + " outlines")
    void dangerButton() {
        PointedButton atRest = new PointedButton();
        PointedButton hovered = new PointedButton();
        hovered.hover();
        PointedButton pressed = new PointedButton();
        pressed.press();
        VBox root = themed(Appearance.LIGHT, atRest, hovered, pressed);

        assertEquals(Color.web("#E0B4B0"), fill(atRest, 0), "the outline at rest");
        assertEquals(Color.web("#B3261E"), fill(atRest, 1), "the fill at rest");
        assertEquals(Color.WHITE, atRest.getTextFill(), "the text");
        assertEquals(Color.web("#D99A95"), fill(hovered, 0), "the outline on hover");
        assertEquals(Color.web("#9C1F18"), fill(hovered, 1), "the fill on hover");
        assertEquals(Color.web("#D99A95"), fill(pressed, 0), "the outline while pressed");
        assertEquals(Color.web("#8A1D17"), fill(pressed, 1), "the fill while pressed");

        ThemeManager.setAppearance(Appearance.DARK);
        root.applyCss();

        assertEquals(DARK_BORDER, fill(atRest, 0), "the outline at rest");
        assertEquals(Color.web("#e56a86"), fill(atRest, 1), "the fill at rest");
        assertEquals(Color.web("#1b1d2d"), atRest.getTextFill(), "the text");
        assertEquals(DARK_BORDER, fill(hovered, 0), "the outline on hover");
        assertEquals(swatch(root, "derive(#e56a86, 15%)"), fill(hovered, 1), "the fill on hover, lighter");
        assertEquals(DARK_BORDER, fill(pressed, 0), "the outline while pressed");
        assertEquals(swatch(root, "derive(#e56a86, -12%)"), fill(pressed, 1), "the fill while pressed, darker");
    }

    /**
     * Links keep the accent in Dark, as before, and take a deeper blue than the accent in Light, which reads as text
     * on the window and on the Markdown view's raised content.
     */
    @Test
    @DisplayName("a hyperlink, the shortcuts editor's fix and a link of the Markdown view take the accent in Dark and"
            + " the deep link blue in Light")
    void links() {
        Hyperlink hyperlink = new Hyperlink("A hyperlink");
        MarkdownView view = new MarkdownView("A [link](https://example.org) in a paragraph.");
        VBox root = themed(Appearance.DARK, hyperlink, view);
        root.layout();
        Text link = (Text) view.lookup(".markdown-link");

        assertEquals(Color.web("#7b8cff"), hyperlink.getTextFill(), "the hyperlink");
        assertEquals(Color.web("#7b8cff"), link.getFill(), "the Markdown view's link");

        ThemeManager.setAppearance(Appearance.LIGHT);
        root.applyCss();

        assertEquals(Color.web("#1F56B3"), hyperlink.getTextFill(), "the hyperlink");
        assertEquals(Color.web("#1F56B3"), link.getFill(), "the Markdown view's link");
    }

    /**
     * A tooltip cannot be styled without showing it, so the label its skin shows stands in for it: the label carries
     * the tooltip's style class, and the theme's rule for the class colours it as in the tooltip's own window. In
     * Dark it keeps JavaFX's own translucent grey with white text and JavaFX's shadow, as before; in Light it lies on
     * the ivory of popups with the dark text and a soft shadow in the ink. Its content, here a region as the tooltip's
     * graphic that shows the colour JavaFX derives a content's text colours from ({@code -fx-background}), finds the
     * tooltip's own colour: JavaFX's opaque grey in Dark, the ivory in Light.
     */
    @Test
    @DisplayName("a tooltip keeps JavaFX's translucent grey, white text and shadow in Dark and lies on the ivory of"
            + " popups with the dark text and a soft shadow in Light; its content derives its text from its colour")
    void tooltip() {
        Region content = new Region();
        content.setStyle("-fx-background-color: -fx-background;");
        Tooltip tooltip = new Tooltip("A tip");
        tooltip.setGraphic(content);
        Label tip = (Label) new TooltipSkin(tooltip).getNode();
        VBox root = themed(Appearance.DARK, tip);
        root.applyCss();

        assertEquals(Color.rgb(30, 30, 30, 0.8), fill(tip, 0), "the background");
        assertEquals(Color.WHITE, tip.getTextFill(), "the text");
        assertEquals(Color.rgb(30, 30, 30), fill(content, 0), "the colour the content derives its text from");
        assertTooltipShadow(Color.rgb(0, 0, 0, 0.5), tip.getEffect());

        ThemeManager.setAppearance(Appearance.LIGHT);
        root.applyCss();

        assertEquals(LIGHT_FIELD, fill(tip, 0), "the background");
        assertEquals(LIGHT_TEXT, tip.getTextFill(), "the text");
        assertEquals(LIGHT_FIELD, fill(content, 0), "the colour the content derives its text from");
        assertTooltipShadow(Color.rgb(31, 34, 48, 0.18), tip.getEffect());
    }

    /**
     * A context menu cannot be styled without showing it, so a region with its style class stands in for it: the
     * theme's rule for the class draws it.
     */
    @Test
    @DisplayName("shadows keep their offsets and radii, in the ink at 18 percent in Light and in black at 20 percent in"
            + " Dark; the focused slider's stronger shadow too")
    void shadows() {
        Region menu = new Region();
        menu.getStyleClass().add("context-menu");
        FocusedSlider slider = new FocusedSlider();
        VBox root = themed(Appearance.LIGHT, menu, slider);

        assertShadow(Color.rgb(31, 34, 48, 0.18), 8, 2, menu.getEffect());
        assertShadow(Color.rgb(31, 34, 48, 0.27), 4, 1, slider.lookup(".thumb").getEffect());

        ThemeManager.setAppearance(Appearance.DARK);
        root.applyCss();

        assertShadow(Color.rgb(0, 0, 0, 0.2), 8, 2, menu.getEffect());
        assertShadow(Color.rgb(0, 0, 0, 0.3), 4, 1, slider.lookup(".thumb").getEffect());
    }

    /**
     * Nebula Dark looks as before: each token that tells places apart for Nebula Light holds, in Dark, the colour
     * those places had before, given here as they had it, a value or a computation of one.
     */
    @Test
    @DisplayName("in Dark every token added for Light holds the colour its places had before")
    void darkAsBefore() {
        String[][] tokens = {
                {"-fx-field-color", "#24273c"},
                {"-fx-raised-color", "#24273c"},
                {"-fx-text-placeholder-color", "#8a90a8"},
                {"-fx-text-disabled-color", "#8a90a8"},
                {"-fx-accent-tint-text-color", "#7b8cff"},
                {"-fx-row-divider-color", "#42486e"},
                {"-fx-selection-neutral-color", "#2d334d"},
                {"-fx-shadow-color", "rgba(0, 0, 0, 0.2)"},
                {"-fx-shadow-strong-color", "rgba(0, 0, 0, 0.3)"},
                {"-fx-status-normal-hover", "derive(#4cd38a, 15%)"},
                {"-fx-status-normal-pressed", "derive(#4cd38a, -12%)"},
                {"-fx-status-running-hover", "derive(#3fd0a0, 15%)"},
                {"-fx-status-running-pressed", "derive(#3fd0a0, -12%)"},
                {"-fx-status-paused-hover", "derive(#e0a84c, 15%)"},
                {"-fx-status-paused-pressed", "derive(#e0a84c, -12%)"},
                {"-fx-status-error-hover", "derive(#e56a86, 15%)"},
                {"-fx-status-error-pressed", "derive(#e56a86, -12%)"},
                {"-fx-status-info-hover", "derive(#5d9bff, 15%)"},
                {"-fx-status-info-pressed", "derive(#5d9bff, -12%)"},
                {"-fx-danger-border", "#42486e"},
                {"-fx-danger-border-hover", "#42486e"},
                {"-fx-danger-text-hover-color", "#e56a86"},
                {"-fx-error-border", "#e56a86"},
                {"-fx-keycap-border", "#42486e"},
                {"-fx-keycap-color", "#2d334d"},
        };
        VBox root = themed(Appearance.DARK);

        for (String[] token : tokens) {
            assertEquals(swatch(root, token[1]), swatch(root, token[0]), token[0]);
        }
    }

    private static void assertShadow(Color color, double radius, double offsetY, Effect effect) {
        DropShadow shadow = (DropShadow) effect;
        assertEquals(color, shadow.getColor(), "the shadow's colour");
        assertEquals(radius, shadow.getRadius(), "the shadow's radius");
        assertEquals(0, shadow.getOffsetX(), "the shadow's horizontal offset");
        assertEquals(offsetY, shadow.getOffsetY(), "the shadow's vertical offset");
    }

    /**
     * A tooltip's shadow in JavaFX's own shape, in both schemes: a three-pass box of radius 10, no spread, 3 pixels
     * down.
     */
    private static void assertTooltipShadow(Color color, Effect effect) {
        assertShadow(color, 10, 3, effect);
        DropShadow shadow = (DropShadow) effect;
        assertEquals(BlurType.THREE_PASS_BOX, shadow.getBlurType(), "the shadow's blur");
        assertEquals(0, shadow.getSpread(), "the shadow's spread");
    }

    /**
     * A scene of the nodes with the appearance chosen and the theme applied, never shown, styled.
     */
    private static VBox themed(Appearance appearance, Region... nodes) {
        ThemeManager.setAppearance(appearance);
        VBox root = new VBox(nodes);
        ThemeManager.apply(new Scene(root));
        root.applyCss();
        return root;
    }

    private static Paint fill(Region region, int layer) {
        return region.getBackground().getFills().get(layer).getFill();
    }

    /**
     * The colour a value resolves to in the scene of the root: a region with the value as its background, added to
     * the root and styled.
     */
    private static Paint swatch(VBox root, String value) {
        Region swatch = new Region();
        swatch.setStyle("-fx-background-color: " + value + ";");
        root.getChildren().add(swatch);
        root.applyCss();
        return fill(swatch, 0);
    }

    /**
     * A danger button that shows the pointer over it or a press, without a pointer: the scene is never shown.
     */
    private static final class PointedButton extends Button {

        PointedButton() {
            super("Delete");
            getStyleClass().add("danger-button");
        }

        void hover() {
            setHover(true);
        }

        void press() {
            setPressed(true);
        }
    }

    /**
     * A slider that shows the keyboard's focus without a window: the scene is never shown.
     */
    private static final class FocusedSlider extends Slider {

        FocusedSlider() {
            setFocused(true);
        }
    }
}
