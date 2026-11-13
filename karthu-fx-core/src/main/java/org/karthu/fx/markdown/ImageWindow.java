package org.karthu.fx.markdown;

import javafx.geometry.Dimension2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.karthu.fx.theme.ThemeManager;

import java.util.List;

/**
 * A view's full size window: one decorated stage per view, not modal, owned by the view's window, so it closes with
 * it, in the theme and with the application's stage icon, as karthu-fx's dialogs are built. It shows its content
 * ({@link ImageWindowContent}); opening another image of the view shows it in the open window.
 *
 * <p><b>Size.</b> When it opens, the window is as large as the image at 100 % plus its bar, at most 90 % of the visual
 * bounds of the screen the view's window is on and at least large enough for its bar ({@link #firstSize}), centred
 * over the view's window. While it stays open it keeps the size the reader gives it.
 *
 * <p>The view's tests never show it: they take the requests to open through the view's window switch, and test the
 * content on its own. A guard holds them to it: while the system property {@value #NO_WINDOWS} is {@code true}, as the
 * build sets it for every test run, the window refuses to open with an {@link IllegalStateException}.
 */
final class ImageWindow {

    /**
     * The system property that keeps the window from opening while it is {@code true}: the build sets it for the
     * tests, which must never show a window.
     */
    static final String NO_WINDOWS = "karthu.fx.test.noWindows";

    /**
     * How much of the screen's visual bounds the window takes at most when it opens.
     */
    private static final double SCREEN_SHARE = 0.9;

    /**
     * The smallest size the window opens at, its content's: room for the bar and a small image.
     */
    private static final double MIN_WIDTH = 320;
    private static final double MIN_HEIGHT = 240;

    /**
     * The content's size when it shows a note instead of an image.
     */
    private static final Dimension2D NOTE_SIZE = new Dimension2D(480, 360);

    private final MarkdownView view;
    private final ImageWindowContent content;

    /**
     * The stage, made at the first opening; {@code null} before.
     */
    private Stage stage;

    /**
     * Creates the full size window of a view, without its stage yet.
     *
     * @param view  the view, whose window owns the stage and whose labels the content shows
     * @param notes the view's notes of an image that cannot be shown and its pixel limit
     * @param work  where the decoding and the shrinking run
     */
    ImageWindow(MarkdownView view, ImageNotes notes, ImageWork work) {
        this.view = view;
        content = new ImageWindowContent(view, notes, work, () -> stage.hide());
    }

    /**
     * The size of the window's content when it opens: the image at 100 % plus the bar, at most 90 % of the screen's
     * visual bounds, at least {@value #MIN_WIDTH} by {@value #MIN_HEIGHT} and at least as wide as the bar asks to be,
     * so that its labels show whole whatever their language.
     *
     * @param imageWidth   the image's width at 100 %, in the screen's pixels of the scene
     * @param imageHeight  the image's height at 100 %
     * @param barWidth     the width the window's bar asks for
     * @param barHeight    the height of the window's bar
     * @param visualBounds the visual bounds of the screen
     * @return the content's size
     */
    static Dimension2D firstSize(double imageWidth, double imageHeight, double barWidth, double barHeight,
                                 Rectangle2D visualBounds) {
        double width = Math.min(imageWidth, SCREEN_SHARE * visualBounds.getWidth());
        double height = Math.min(imageHeight + barHeight, SCREEN_SHARE * visualBounds.getHeight());
        return new Dimension2D(Math.max(width, Math.max(barWidth, MIN_WIDTH)), Math.max(height, MIN_HEIGHT));
    }

    /**
     * Shows the image of a request in the window, opening the window once the image is decoded, or bringing it to the
     * front with its new title. Refused while the system property {@value #NO_WINDOWS} is {@code true}, as the build
     * sets it for every test run: a test that clicks an image of a view without the view's window switch fails at
     * once, on the thread that clicked, instead of showing a window on the desktop.
     *
     * @param request the request
     * @throws IllegalStateException while {@value #NO_WINDOWS} is {@code true}
     */
    void open(ImageWindowRequest request) {
        if (Boolean.getBoolean(NO_WINDOWS)) {
            throw new IllegalStateException("The Markdown view's full size image window does not open while the"
                    + " system property " + NO_WINDOWS + " is true: a test opened an image of a view without"
                    + " replacing its window (the view's package-private setImageWindow)");
        }
        content.show(request, () -> showStage(request.title()));
    }

    /**
     * The window's stage, made at the first call: owned by the view's window of that moment.
     *
     * @return the stage
     */
    Stage stage() {
        if (stage == null) {
            stage = createStage();
        }
        return stage;
    }

    private Stage createStage() {
        Stage created = new Stage(StageStyle.DECORATED);
        created.initModality(Modality.NONE);
        Window owner = ownerWindow();
        if (owner != null) {
            created.initOwner(owner);
            if (owner instanceof Stage ownerStage) {
                created.getIcons().setAll(ownerStage.getIcons());
            }
        }
        Scene scene = new Scene(content);
        ThemeManager.apply(scene);
        created.setScene(scene);
        // Closed, the window lets go of its image.
        created.setOnHidden(event -> content.clear());
        // On a screen of another scale 100 % is another size.
        created.outputScaleXProperty().addListener(observable -> content.outputScaleChanged());
        return created;
    }

    /**
     * Brings the window to the front with a title; opening it first, sized to its content and centred over the view's
     * window.
     */
    private void showStage(String title) {
        Stage window = stage();
        window.setTitle(title);
        if (!window.isShowing()) {
            Screen screen = screen();
            Dimension2D size = openingSize(screen);
            content.setPrefSize(size.getWidth(), size.getHeight());
            window.sizeToScene();
            window.setOnShown(event -> centre(window, screen));
            window.show();
        }
        window.toFront();
    }

    /**
     * The content's size when the window opens on a screen, by the image it shows at 100 % on that screen.
     */
    private Dimension2D openingSize(Screen screen) {
        content.applyCss();
        double barWidth = content.barWidth();
        double barHeight = content.barHeight();
        if (!content.showsImage()) {
            return firstSize(NOTE_SIZE.getWidth(), NOTE_SIZE.getHeight() - barHeight, barWidth, barHeight,
                    screen.getVisualBounds());
        }
        return firstSize(content.imagePixelWidth() / screen.getOutputScaleX(),
                content.imagePixelHeight() / screen.getOutputScaleY(), barWidth, barHeight, screen.getVisualBounds());
    }

    /**
     * Centres the window over the view's window, or on its screen if the view has none.
     */
    private void centre(Stage window, Screen screen) {
        Window owner = window.getOwner();
        if (owner == null) {
            window.centerOnScreen();
            return;
        }
        Rectangle2D bounds = screen.getVisualBounds();
        double x = owner.getX() + (owner.getWidth() - window.getWidth()) / 2;
        double y = owner.getY() + (owner.getHeight() - window.getHeight()) / 2;
        // Its title bar always on the screen.
        window.setX(Math.clamp(x, bounds.getMinX(), Math.max(bounds.getMinX(), bounds.getMaxX() - window.getWidth())));
        window.setY(Math.clamp(y, bounds.getMinY(), Math.max(bounds.getMinY(),
                bounds.getMaxY() - window.getHeight())));
    }

    /**
     * The screen the view's window is on, the primary screen if it has none.
     */
    private Screen screen() {
        Window owner = stage.getOwner();
        if (owner != null) {
            List<Screen> screens = Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(),
                    owner.getHeight());
            if (!screens.isEmpty()) {
                return screens.getFirst();
            }
        }
        return Screen.getPrimary();
    }

    /**
     * The window the view is in, {@code null} while it has none.
     */
    private Window ownerWindow() {
        Scene scene = view.getScene();
        return scene == null ? null : scene.getWindow();
    }
}
