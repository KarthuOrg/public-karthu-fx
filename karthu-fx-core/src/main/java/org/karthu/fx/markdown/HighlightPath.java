package org.karthu.fx.markdown;

import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A highlight behind characters: a path in the shape of the highlighted text, filled with its colour from CSS
 * ({@code -fx-fill}), outside the layout and the mouse's reach. The selection's highlight carries the style class
 * {@code markdown-selection}, the search's {@code markdown-search-match} for all matches and
 * {@code markdown-search-current} for the current one. A {@code Text} has no CSS property for the colour of
 * highlighted characters, so the highlight carries it, {@code -fx-highlight-text-fill} as a text field's skin names
 * it, and the view gives it to the highlighted characters: a text flow's {@link HighlightLayer}, in a paragraph, a
 * heading, a code or a source block, takes it as the fill of its copies of the runs. A selected image, block or inline,
 * shows a selection highlight of its own over what it shows ({@link ImageNode#showSelected(boolean)}), which the theme
 * makes see-through.
 */
final class HighlightPath extends Path {

    private static final CssMetaData<HighlightPath, Paint> HIGHLIGHT_TEXT_FILL =
            new CssMetaData<>("-fx-highlight-text-fill", StyleConverter.getPaintConverter(), Color.WHITE) {

                @Override
                public boolean isSettable(HighlightPath highlight) {
                    return !highlight.highlightTextFill.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(HighlightPath highlight) {
                    return highlight.highlightTextFill;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA = cssMetaData();

    // The colour of the highlighted characters; documented at highlightTextFillProperty().
    private final StyleableObjectProperty<Paint> highlightTextFill =
            new SimpleStyleableObjectProperty<>(HIGHLIGHT_TEXT_FILL, this, "highlightTextFill", Color.WHITE);

    /**
     * Creates an empty highlight.
     *
     * @param styleClass its style class, such as {@code markdown-selection}
     */
    HighlightPath(String styleClass) {
        getStyleClass().add(styleClass);
        setStroke(null);
        setManaged(false);
        setMouseTransparent(true);
    }

    private static List<CssMetaData<? extends Styleable, ?>> cssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> metaData = new ArrayList<>(Path.getClassCssMetaData());
        metaData.add(HIGHLIGHT_TEXT_FILL);
        return Collections.unmodifiableList(metaData);
    }

    /**
     * The colour of the highlighted characters, {@code -fx-highlight-text-fill}; white, as a {@code Text}'s selection
     * fill is, until CSS sets it.
     *
     * @return the colour property
     */
    StyleableObjectProperty<Paint> highlightTextFillProperty() {
        return highlightTextFill;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA;
    }
}
