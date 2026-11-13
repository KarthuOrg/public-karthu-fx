package org.karthu.fx.theme.skin;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollPaneSkin;

/**
 * The theme's skin of every scroll pane: JavaFX's own, with the bitmap cache of its viewport switched off, so the text
 * inside a scroll pane is drawn with the theme's LCD smoothing.
 *
 * <p>JavaFX's scroll pane skin caches its viewport (the node with the style class {@code viewport}) as a bitmap, so
 * that a scroll step moves the bitmap and draws only the strip that comes into view. Into that bitmap JavaFX draws text
 * in gray wherever nothing opaque lies behind it, whatever the text asks for, and the theme's viewports are
 * transparent: labels, text flows and the Markdown view's text inside a scroll pane lost the theme's LCD smoothing. The
 * skin sets the cache once, when it is created, and never again; this skin switches it off right after. Scrolling then
 * draws what the viewport shows at every step, as list, table and tree views do: they draw their rows in a virtual
 * flow, which caches nothing, and keep JavaFX's skins.
 *
 * <p>{@code base.css} names this skin with {@code -fx-skin} on {@code .scroll-pane}, so JavaFX creates it by reflection
 * for every scroll pane of a themed scene, those inside JavaFX's own controls included. The package is exported to
 * {@code javafx.controls} alone and is not part of the library's API; {@code META-INF/native-image} registers the
 * constructor for a native image.
 */
public final class UncachedScrollPaneSkin extends ScrollPaneSkin {

    /**
     * Creates the skin for a scroll pane: JavaFX's skin, its viewport without bitmap cache.
     *
     * @param control the scroll pane
     */
    public UncachedScrollPaneSkin(ScrollPane control) {
        super(control);
        // The viewport is a private node of the skin, found by the style class JavaFX gives it.
        for (Node child : getChildren()) {
            if (child.getStyleClass().contains("viewport")) {
                child.setCache(false);
            }
        }
    }
}
