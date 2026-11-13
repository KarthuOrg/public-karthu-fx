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
 * The highlight behind selected characters ({@code markdown-selection}): a path in the shape of the selected text,
 * filled with the selection colour from CSS ({@code -fx-fill}), outside the layout and the mouse's reach. A
 * {@code Text} has no CSS property for the colour of its selected characters, so the highlight carries it,
 * {@code -fx-highlight-text-fill} as a text field's skin names it, and the view gives it to the selected characters:
 * a text flow's {@link SelectionLayer}, in a paragraph, a heading, a code or a source block, takes it as the fill of
 * its copies of the runs. A selected image, block or inline, shows a highlight of its own over what it shows
 * ({@link ImageNode#showSelected(boolean)}), which the theme makes see-through.
 */
final class SelectionHighlight extends Path {

    private static final CssMetaData<SelectionHighlight, Paint> HIGHLIGHT_TEXT_FILL =
            new CssMetaData<>("-fx-highlight-text-fill", StyleConverter.getPaintConverter(), Color.WHITE) {

                @Override
                public boolean isSettable(SelectionHighlight highlight) {
                    return !highlight.highlightTextFill.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(SelectionHighlight highlight) {
                    return highlight.highlightTextFill;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA = cssMetaData();

    // The colour of the selected characters; documented at highlightTextFillProperty().
    private final StyleableObjectProperty<Paint> highlightTextFill =
            new SimpleStyleableObjectProperty<>(HIGHLIGHT_TEXT_FILL, this, "highlightTextFill", Color.WHITE);

    /**
     * Creates an empty highlight.
     */
    SelectionHighlight() {
        getStyleClass().add("markdown-selection");
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
     * The colour of the selected characters, {@code -fx-highlight-text-fill}; white, as a {@code Text}'s selection
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
