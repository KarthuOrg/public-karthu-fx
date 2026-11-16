package org.karthu.fx.markdown;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The images of a view's document at work, beside the rendering, the selection and the zoom: it creates the nodes
 * of the images ({@link ImageNode}), blocks and inline images alike, loads each through the application's loader and
 * decodes it in the background ({@link ImageWork}), and tells the nodes what their size depends on beyond the width
 * of their text: the output scale of the view's window and the visible height at the current zoom.
 *
 * <p><b>Loading.</b> The loader is read when the document is rendered, and called on a background thread with the
 * image's address. The node keeps its bytes, which are decoded on the decoding thread; the decode computes the
 * bitmap at the size the image shows at in screen pixels, by the node's last limits, with an {@link AreaAverage},
 * and the node shows it on the JavaFX Application Thread. The whole decoded image is let go once the bitmap is
 * computed. A new text discards the nodes of the old one, whose work then does nothing.
 *
 * <p><b>Animated GIFs.</b> A decode tells an animated GIF from its bytes ({@link GifFrames}). While animations play,
 * the node shows the decoded GIF itself, which plays and which JavaFX scales; else the decode reads the GIF's first
 * frame alone and goes on as for any image. A change of the setting decodes the animated GIFs shown anew from their
 * bytes ({@link #setAnimation(boolean)}).
 *
 * <p><b>Order.</b> The decoding thread takes the images in or near the visible area first, those within one visible
 * height above or below it, then the rest in document order ({@link DecodeQueue}). A node notes its nearness at each
 * layout and at each scroll of the view.
 *
 * <p><b>Reading position.</b> An image that arrives, the bitmap or the error box of a load in place of what the node
 * showed before, above the line at the top edge of the visible area keeps that line in place: the view notes it
 * before the change and scrolls it back after the next layout, as for a zoom. An image in or below the visible area
 * pushes the text below it down.
 *
 * <p><b>Reloading.</b> When where images come from changes, or the pixel limit, {@link #reload()} loads every
 * node's image anew through the loader of that moment. A node keeps what it shows until the new result arrives, and
 * shows the results of its latest load only: an older load's result that arrives later is dropped, a decode of it
 * that still waits does nothing, and so does a recompute of a bitmap while a newer load is under way. A new result
 * above the top edge keeps the reader's place as an image that arrives does.
 *
 * <p><b>A new version of the text.</b> For the view's text update, {@link #forget(boolean)} notes what each old node
 * showed, its picture or its error box, by the image's address as written, and every node the renderer creates next
 * starts with what the old one of its address showed ({@link ImageNode#startWith}), then loads as any new node does:
 * its first result replaces that in place, as a reload's does. The notes go once the text is rendered
 * ({@link #rendered()}), so no old picture outlives the rendering but in a node that shows it.
 *
 * <p><b>Limits.</b> A change of the output scale, the visible height or the zoom asks every node for a new layout,
 * which computes its shown size anew and scales its last bitmap to it. A node whose bitmap no longer has the size in
 * screen pixels is noted as outdated, and once the sizes have settled ({@link ImageWork#settling}) each outdated node
 * decodes its bytes again for a new bitmap.
 */
final class DocumentImages {

    /**
     * The number of the next node, of all views: nodes are numbered in document order.
     */
    private static final AtomicLong NEXT_IMAGE = new AtomicLong();

    /**
     * The view's loader property, read at each image.
     */
    private final ObservableValue<ImageLoader> loader;

    /**
     * The notes of the error boxes and the pixel limit, the view's properties.
     */
    private final ImageNotes notes;

    /**
     * The tooltip and accessible text of a block image's open button, the view's text property.
     */
    private final ObservableValue<String> openImageText;

    /**
     * The view's scroll pane, whose visible area the images near it are decoded first for.
     */
    private final ScrollPane scrollPane;

    /**
     * Keeps the line at the top of the visible area in place through the next layout.
     */
    private final Runnable keepReadingPosition;

    /**
     * The box that scales the document, whose zoom the visible height is divided by: the scroll pane's content.
     */
    private final ZoomBox zoomBox;

    /**
     * The nodes of the shown document, in document order.
     */
    private final List<ImageNode> nodes = new ArrayList<>();

    /**
     * The nodes whose bitmap no longer has the size in screen pixels, in the order they were noted.
     */
    private final Set<ImageNode> outdated = new LinkedHashSet<>();

    /**
     * What the old text's nodes showed, by their address as written, while the view renders a new version of the
     * text ({@link #forget(boolean)}); empty otherwise, so the old pictures are not kept beyond the rendering.
     */
    private Map<String, ImageNode.Shown> shownBefore = Map.of();

    /**
     * Where the work runs: in the background, or at once in the view's tests.
     */
    private ImageWork work = ImageWork.background();

    /**
     * The wait for the sizes to settle before outdated bitmaps are computed anew, of the work.
     */
    private ImageWork.Settling settling = work.settling(() -> recomputeOutdated());

    /**
     * Where failures are reported: the SLF4J logger named after the view's class, a recorder in tests. Read on
     * background threads.
     */
    private volatile Logger logger = LoggerFactory.getLogger(MarkdownView.class);

    /**
     * Whether animated GIFs play, the view's property: else their first frame shows. Set on the JavaFX thread, read by
     * the decoding thread.
     */
    private volatile boolean animation = true;

    /**
     * The output scale the view assumes in its tests; 0 for the scale of the view's window.
     */
    private double assumedOutputScale;

    /**
     * The height of the view's visible area, in the scene's pixels; 0 before the view is laid out.
     */
    private double visibleHeight;

    /**
     * The window the view is in, whose output scale it follows; {@code null} while it has none.
     */
    private Window window;

    /**
     * What opens an image at full size: the view's full size window, a recorder in tests.
     */
    private Consumer<ImageWindowRequest> fullSize = request -> {
    };

    private final ChangeListener<Number> outputScaleListener = (observable, oldScale, newScale) -> limitsChanged();

    private final ChangeListener<Window> windowListener = (observable, oldWindow, newWindow) -> follow(newWindow);

    /**
     * Creates the images of a view.
     *
     * @param view                the view, whose window's output scale the images follow
     * @param loader              the view's loader property
     * @param notes               the notes of an image that cannot be shown and the pixel limit
     * @param openImageText       the tooltip of a block image's open button
     * @param scrollPane          the view's scroll pane
     * @param zoomBox             the box that scales the document, the scroll pane's content
     * @param keepReadingPosition keeps the line at the top of the visible area in place through the next layout
     */
    DocumentImages(Node view, ObservableValue<ImageLoader> loader, ImageNotes notes,
                   ObservableValue<String> openImageText, ScrollPane scrollPane, ZoomBox zoomBox,
                   Runnable keepReadingPosition) {
        this.loader = loader;
        this.notes = notes;
        this.openImageText = openImageText;
        this.scrollPane = scrollPane;
        this.zoomBox = zoomBox;
        this.keepReadingPosition = keepReadingPosition;
        view.sceneProperty().addListener((observable, oldScene, newScene) -> sceneChanged(oldScene, newScene));
        sceneChanged(null, view.getScene());
        scrollPane.vvalueProperty().addListener(observable -> scrolled());
    }

    /**
     * Replaces where the work runs.
     *
     * @param work the work from now on, for the images of the next text
     */
    void setWork(ImageWork work) {
        this.work = work;
        settling = work.settling(() -> recomputeOutdated());
    }

    /**
     * Where the work runs now, which the view's full size window shares.
     *
     * @return the work
     */
    ImageWork work() {
        return work;
    }

    /**
     * Replaces where failures are reported.
     *
     * @param logger the logger from now on
     */
    void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Replaces what opens an image at full size.
     *
     * @param window what gets each request to open an image from now on
     */
    void setWindow(Consumer<ImageWindowRequest> window) {
        fullSize = window;
    }

    /**
     * Sets whether animated GIFs play in the document, or show their first frame, and switches the animated GIFs
     * shown, in place: each decodes the bytes it keeps anew, at the size it shows at, without a new load. A decode
     * still waiting reads the setting when it runs; one under way read the old setting, so another follows it.
     *
     * @param on {@code true} to play them
     */
    void setAnimation(boolean on) {
        if (on == animation) {
            return;
        }
        animation = on;
        for (ImageNode node : nodes) {
            ImageNode.Loaded loaded = node.loaded();
            if (loaded != null && node.wants(loaded.load()) && loaded.animated()) {
                queueDecode(node);
            }
        }
    }

    /**
     * The tooltip and accessible text of a block image's open button.
     *
     * @return the view's text property, followed when it changes
     */
    ObservableValue<String> openImageText() {
        return openImageText;
    }

    /**
     * Opens the image of a node at full size, with the bytes the node keeps, if it shows its image: a placeholder or
     * an error box opens nothing.
     *
     * @param node the node
     */
    void open(ImageNode node) {
        ImageNode.Loaded loaded = node.loaded();
        if (!node.showsPicture() || loaded == null) {
            return;
        }
        fullSize.accept(ImageWindowRequest.of(node.alt(), node.address(), loaded.bytes()));
    }

    /**
     * Opens an image of a table cell at full size, which the cell never loads: loads its file through the loader of
     * this moment first, in the background. Without a file the window opens all the same and says so.
     *
     * @param image the image of the cell
     */
    void open(CellImage image) {
        ImageLoader imageLoader = loader.getValue();
        ImageWork imageWork = work;
        Consumer<ImageWindowRequest> window = fullSize;
        imageWork.load(() -> {
            byte[] bytes = fetch(imageLoader, image.address());
            imageWork.show(() -> window.accept(ImageWindowRequest.of(image.alt(), image.address(), bytes)));
        });
    }

    /**
     * Sets the output scale the view assumes instead of its window's.
     *
     * @param scale screen pixels per document pixel; 0 for the window's
     */
    void setAssumedOutputScale(double scale) {
        assumedOutputScale = scale;
        limitsChanged();
    }

    /**
     * Sets the height of the view's visible area, which the view knows in its layout before its scroll pane lays the
     * document out.
     *
     * @param height the height in the scene's pixels
     */
    void setVisibleHeight(double height) {
        if (height != visibleHeight) {
            visibleHeight = height;
            limitsChanged();
        }
    }

    /**
     * The horizontal output scale of the view's window, screen pixels per document pixel: 1.0 while the view has no
     * window.
     *
     * @return the scale
     */
    double outputScaleX() {
        if (assumedOutputScale > 0) {
            return assumedOutputScale;
        }
        return window == null ? 1.0 : window.getOutputScaleX();
    }

    /**
     * The vertical output scale of the view's window.
     *
     * @return the scale
     */
    double outputScaleY() {
        if (assumedOutputScale > 0) {
            return assumedOutputScale;
        }
        return window == null ? 1.0 : window.getOutputScaleY();
    }

    /**
     * The visible height at the current zoom: the visible area's height divided by the zoom, in the document's own
     * pixels; 0, no limit, before the view is laid out.
     *
     * @return the height
     */
    double maxHeight() {
        return visibleHeight / zoomBox.zoom();
    }

    /**
     * The limits of an image node's size at a width of its content: that width, the visible height at the zoom, the
     * zoom and the output scale.
     *
     * @param contentWidth the width the text wraps at; -1 for none
     * @return the limits
     */
    ImageSize.Limits limits(double contentWidth) {
        return new ImageSize.Limits(contentWidth, maxHeight(), zoomBox.zoom(), outputScaleX(), outputScaleY());
    }

    /**
     * Whether a node lies in the visible area of the view, or within one visible height above or below it, in the
     * current layout and scroll position.
     *
     * @param node the node
     * @return {@code true} if it does; {@code false} before the view is laid out
     */
    boolean isNear(ImageNode node) {
        NearArea area = NearArea.of(scrollPane);
        return area != null && node.getScene() != null && area.contains(node);
    }

    /**
     * Notes a node whose bitmap no longer has the size in screen pixels its image shows at: once the sizes have
     * settled, it gets a new one.
     *
     * @param node the node
     */
    void bitmapOutdated(ImageNode node) {
        outdated.add(node);
        settling.restart();
    }

    /**
     * Asks every node for a new layout: something its shown size depends on has changed.
     */
    void limitsChanged() {
        for (ImageNode node : nodes) {
            node.requestLayout();
        }
    }

    /**
     * Creates the block of an image alone in its paragraph and starts its work.
     *
     * @param address    the image's address as written
     * @param title      the image's title, empty if none
     * @param alt        the image's alt text
     * @param containers the quotes and list items around the block, the outermost first
     * @param spaced     whether copied text sets the block apart by blank lines, as the paragraph it stands for
     * @return the block
     */
    ImageBlock block(String address, String title, String alt, List<BlockContainer> containers, boolean spaced) {
        ImageBlock block = new ImageBlock(address, title, alt, this, NEXT_IMAGE.getAndIncrement(), containers,
                spaced);
        add(block);
        return block;
    }

    /**
     * Creates the node of an image inside a sentence and starts its work.
     *
     * @param address the image's address as written
     * @param title   the image's title, empty if none
     * @param alt     the image's alt text
     * @return the node, for the text flow of its paragraph or heading
     */
    InlineImage inline(String address, String title, String alt) {
        InlineImage image = new InlineImage(address, title, alt, this, NEXT_IMAGE.getAndIncrement());
        add(image);
        return image;
    }

    /**
     * Keeps a new node of an image among those of the shown document and starts its work, showing first what the old
     * text's node of its address showed, if the new text is a new version of the old one.
     */
    private void add(ImageNode node) {
        nodes.add(node);
        ImageNode.Shown before = shownBefore.get(node.address());
        if (before != null) {
            node.startWith(before);
        }
        start(node);
    }

    /**
     * Loads the images of the shown document anew, in place, through the loader of this moment: where they come from
     * has changed. The document is not rebuilt; each node keeps what it shows until its new result arrives.
     */
    void reload() {
        for (ImageNode node : nodes) {
            start(node);
        }
    }

    /**
     * Forgets the nodes of the shown document, before the view renders a new one: their work does nothing from now
     * on. For a new version of the same text, the nodes created next, until {@link #rendered()}, start with what the
     * old text's node of their address showed: the first of that address that showed its picture or its error box.
     *
     * @param newVersion whether the next text is a new version of the shown one, whose images keep what they show
     */
    void forget(boolean newVersion) {
        Map<String, ImageNode.Shown> shown = new HashMap<>();
        for (ImageNode node : nodes) {
            ImageNode.Shown nodeShows = newVersion ? node.shown() : null;
            if (nodeShows != null) {
                shown.putIfAbsent(node.address(), nodeShows);
            }
            node.discard();
        }
        nodes.clear();
        outdated.clear();
        shownBefore = shown;
    }

    /**
     * Notes that the new text is rendered, all its nodes created: lets go what the old text's nodes showed.
     */
    void rendered() {
        shownBefore = Map.of();
    }

    /**
     * Loads a node's image through the loader of this moment and hands its bytes to the node, which keeps them, and
     * to a decode.
     */
    private void start(ImageNode node) {
        ImageLoader imageLoader = loader.getValue();
        ImageWork imageWork = work;
        // Each step checks that the node still wants this load: a new text or a reload replaces it.
        Object load = node.newLoad();
        imageWork.load(() -> {
            if (!node.wants(load)) {
                return;
            }
            byte[] bytes = fetch(imageLoader, node.address());
            if (bytes == null) {
                imageWork.show(() -> arrive(node, load, () -> node.showError(notes.missing(), load)));
                return;
            }
            // Told here, in the background, and kept with the bytes: a switch of the animations reads it on the
            // JavaFX Application Thread.
            boolean animated = GifFrames.isAnimated(bytes);
            imageWork.show(() -> {
                if (node.wants(load)) {
                    node.setLoaded(load, bytes, animated);
                    queueDecode(node);
                }
            });
        });
    }

    /**
     * Computes a new bitmap for each outdated node that still shows an image of the wrong size.
     */
    private void recomputeOutdated() {
        List<ImageNode> waiting = new ArrayList<>(outdated);
        outdated.clear();
        for (ImageNode node : waiting) {
            if (!node.isDiscarded() && !node.bitmapFits()) {
                queueDecode(node);
            }
        }
    }

    /**
     * Queues a decode of a node's bytes, unless one waits already: the decode reads the node's limits when it
     * runs, so one serves every change of the size before it.
     */
    private void queueDecode(ImageNode node) {
        if (!node.queueDecode()) {
            return;
        }
        node.setNear(isNear(node));
        ImageWork imageWork = work;
        int limit = notes.pixelLimit().get();
        imageWork.decode(node, () -> decode(node, imageWork, limit));
    }

    /**
     * Decodes a node's bytes on the decoding thread and computes the bitmap for the node's last limits: the image
     * shrunk by an area average to its size in screen pixels, or the image itself where it shows at its full size or
     * beyond, or where the node has not been laid out yet. An animated GIF is the decoded GIF itself while animations
     * play, else its first frame alone, decoded and shrunk like any image. Shows the bitmap, the unsupported box, or
     * the too-large box for an image of more pixels than the limit. Nothing if a newer load than the bytes' has
     * started: its bytes queue a decode of their own.
     */
    private void decode(ImageNode node, ImageWork imageWork, int limit) {
        node.decodeStarted();
        ImageNode.Loaded loaded = node.loaded();
        if (loaded == null || !node.wants(loaded.load())) {
            return;
        }
        Object load = loaded.load();
        boolean animated = loaded.animated();
        boolean plays = animated && animation;
        // A still animated GIF is its first frame alone, decoded like any image: JavaFX cannot read the pixels of an
        // animated image, and the other frames would take memory for nothing.
        byte[] file = animated && !plays ? GifFrames.firstFrame(loaded.bytes()) : loaded.bytes();
        Image image;
        try {
            image = ImageFiles.decode(file);
        } catch (IOException undecodable) {
            logger.debug("The image '{}' cannot be decoded: {}", node.address(), undecodable.getMessage());
            imageWork.show(() -> arrive(node, load, () -> node.showError(notes.unsupported(), load)));
            return;
        }
        int pixelWidth = (int) image.getWidth();
        int pixelHeight = (int) image.getHeight();
        if (ImageFiles.aboveLimit(image, limit)) {
            logger.debug("The image '{}' has {} by {} pixels, more than the limit of {}", node.address(), pixelWidth,
                    pixelHeight, limit);
            imageWork.show(() -> arrive(node, load, () -> node.showError(notes.tooLarge(), load)));
            return;
        }
        Image bitmap = image;
        ImageSize.Limits limits = node.limits();
        if (limits != null && !plays) {
            ImageSize.Pixels pixels = limits.pixels(pixelWidth, pixelHeight);
            if (pixels.width() < pixelWidth || pixels.height() < pixelHeight) {
                bitmap = AreaAverage.shrink(image, pixels.width(), pixels.height());
            }
        }
        Image shown = bitmap;
        imageWork.show(() -> arrive(node, load,
                () -> node.showBitmap(shown, pixelWidth, pixelHeight, load, plays)));
    }

    /**
     * Changes what a node shows with a result of a load, if the node still wants that load, keeping the line at the
     * top of the visible area in place if the image arrives above it: a result of a load the node does not show yet,
     * in place of the placeholder or of an older load's result, may change its height there. A new bitmap of the load
     * it shows keeps its height.
     */
    private void arrive(ImageNode node, Object load, Runnable change) {
        if (!node.wants(load)) {
            return;
        }
        if (!node.shows(load) && isAboveTopEdge(node)) {
            keepReadingPosition.run();
        }
        change.run();
    }

    /**
     * Whether a node starts above the top edge of the visible area, in the current layout and scroll position.
     */
    private boolean isAboveTopEdge(ImageNode node) {
        if (node.getScene() == null || scrollPane.getViewportBounds().getHeight() <= 0) {
            return false;
        }
        double top = zoomBox.sceneToLocal(node.localToScene(0, 0)).getY();
        return top < VerticalScroll.offset(scrollPane);
    }

    /**
     * The bytes the loader returns for an address; {@code null} if it has none: no loader, an {@link IOException},
     * {@code null} or no bytes, logged at debug level, or a runtime exception, a fault of the loader, logged as a
     * warning with the address.
     */
    private byte[] fetch(ImageLoader imageLoader, String address) {
        if (imageLoader == null) {
            logger.debug("No image loader for the image '{}'", address);
            return null;
        }
        byte[] bytes;
        try {
            bytes = imageLoader.load(address);
        } catch (IOException missing) {
            logger.debug("No image at '{}': {}", address, missing.getMessage());
            return null;
        } catch (RuntimeException failure) {
            logger.warn("The image loader failed on the address '{}', so the image shows as missing", address, failure);
            return null;
        }
        if (bytes == null || bytes.length == 0) {
            logger.debug("No image at '{}': the loader returned no bytes", address);
            return null;
        }
        return bytes;
    }

    /**
     * Notes for every node whether it lies near the visible area now, so the decodes still queued go in the new
     * order.
     */
    private void scrolled() {
        for (ImageNode node : nodes) {
            node.setNear(isNear(node));
        }
    }

    /**
     * Follows the window of the view's new scene.
     */
    private void sceneChanged(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.windowProperty().removeListener(windowListener);
        }
        if (newScene != null) {
            newScene.windowProperty().addListener(windowListener);
        }
        follow(newScene == null ? null : newScene.getWindow());
    }

    /**
     * Follows the output scale of a window.
     */
    private void follow(Window newWindow) {
        if (window != null) {
            window.outputScaleXProperty().removeListener(outputScaleListener);
            window.outputScaleYProperty().removeListener(outputScaleListener);
        }
        window = newWindow;
        if (window != null) {
            window.outputScaleXProperty().addListener(outputScaleListener);
            window.outputScaleYProperty().addListener(outputScaleListener);
        }
        limitsChanged();
    }
}
