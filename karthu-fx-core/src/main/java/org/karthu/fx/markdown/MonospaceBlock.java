package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.karthu.fx.font.MonospaceFont;

import java.util.ArrayList;
import java.util.List;

/**
 * A block of monospace text that keeps its lines whole: the text, never wrapped, in a scroll pane of its own that
 * scrolls sideways when a line is wider than the block, with a horizontal bar as needed and never a vertical one. The
 * view's code blocks are such blocks, and so are its source blocks, which show text the view cannot render.
 *
 * <p><b>Nodes.</b> The block (its style class given) holds the scroll pane, whose content is a text flow
 * ({@code markdown-block-content}) that pads the text's runs, so the text scrolls through the padding and the bar sits
 * at the block's bottom edge. The runs ({@code markdown-text}, the text style classes given and the monospace family)
 * hold the text in order: one run of its whole text, or in a block coloured by its language a run per piece, which also
 * carries the {@code code-} classes of the piece's category, and a run per plain stretch between pieces. The scroll
 * pane gives the flow its preferred width, that of its longest line, so the flow never wraps a line. A tab advances to
 * the flow's next tab stop, every four spaces of its first run's font, so every four columns of the code (a lone
 * {@code Text}, as the text was before, measures its tab in spaces of JavaFX's default font instead).
 *
 * <p><b>Height.</b> The block is as tall as its text, plus the bar when a line is wider than the block, so the bar
 * never covers the last line and the block has nothing to scroll vertically.
 *
 * <p><b>Wheel.</b> A vertical scroll over the block scrolls the document around it: an event filter passes the
 * vertical part of every scroll event on to the block's parent, so the scroll pane around the block gets it as if the
 * block were not there. The horizontal part stays with the block: a touchpad's sideways swipe, and shift plus wheel,
 * which JavaFX delivers as a horizontal scroll ({@code deltaX}, with {@code deltaY} 0): on Windows and Linux its
 * Glass toolkit turns the wheel into {@code deltaX} while shift is down, on macOS the system does.
 *
 * <p><b>Overlay.</b> The block is a {@link StackPane} whose first child is the scroll pane: a node added after it
 * lies over the text, placed by {@link StackPane#setAlignment(javafx.scene.Node, Pos)} (a copy button in the top
 * right corner), and stays in place when the text scrolls sideways.
 *
 * <p><b>Selection.</b> The text is a segment of the view's selection across the document ({@link DocumentSelection}):
 * it can be selected with the mouse as in a text field, without being one, alone or as part of a selection that starts
 * or ends elsewhere in the document. The characters under the pointer are found with {@link TextFlow#getHitInfo} from
 * the pointer's scene position (so a block scrolled sideways selects what it shows), the flow's padding included. A
 * run's own selection does not show inside a text flow, so the selected characters show as in a paragraph: a
 * {@link HighlightLayer} over the runs, there from the start, fills their shape with the highlight
 * ({@code markdown-selection}) and shows them on it in the selection's text colour. A press with the primary button on
 * the text and the drag after it go to the document selection, which consumes them, so neither scroll pane takes the
 * focus or pans, and which gives the block the keyboard focus (the block stays out of the focus traversal). With the
 * focus, the shortcut key plus C copies the document selection if it reaches into the block, else nothing; plus A
 * selects the block's whole text. A double click selects a word as code has it, a triple click a line: rules on the
 * block's text, whatever its runs.
 *
 * <p><b>Search.</b> The block's text is one text of the view's search, its matches shown in the two layers of its
 * flow's {@link SearchMarks}, below the selection's layer. A match the search goes to scrolls the block sideways into
 * view ({@link #revealSideways}).
 *
 * <p><b>Source lines.</b> The block knows the source line it starts on and that of its text's first line
 * ({@link Lines}): its text's lines follow one another in the source, so the line feeds before a character count the
 * source lines from there.
 */
final class MonospaceBlock extends StackPane implements Searchable {

    private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SELECT_ALL = new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN);

    /**
     * The block's text, which its runs hold in order.
     */
    private final String text;

    private final BlockFlow flow;
    private final ScrollPane scrollPane;

    /**
     * The view's selection across the document, which the block's text is a segment of.
     */
    private final DocumentSelection selection;

    private final List<BlockContainer> containers;

    /**
     * The source lines of the block and of its text's first line.
     */
    private final Lines lines;

    /**
     * Creates a block.
     *
     * @param text             the text, its lines separated by line feeds
     * @param pieces           the coloured pieces of the text, in order, not empty, inside the text and not
     *                         overlapping; none for a text in one colour
     * @param selection        the view's selection across the document, which takes the presses and drags on the text
     * @param containers       the quotes and list items around the block, the outermost first
     * @param lines            the source lines of the block and of its text's first line
     * @param blockStyleClass  the block's style class, such as {@code markdown-code-block}
     * @param textStyleClasses the style classes of the text's runs beside {@code markdown-text}
     */
    MonospaceBlock(String text, List<CodePiece> pieces, DocumentSelection selection, List<BlockContainer> containers,
                   Lines lines, String blockStyleClass, String... textStyleClasses) {
        this.text = text;
        this.selection = selection;
        this.containers = List.copyOf(containers);
        this.lines = lines;
        getStyleClass().add(blockStyleClass);
        flow = new BlockFlow(runs(text, pieces, textStyleClasses));
        flow.getStyleClass().add("markdown-block-content");
        flow.setCursor(Cursor.TEXT);
        // Handlers on the text's flow run before the block's scroll pane sees the events: its behaviour would take the
        // focus on the press, its skin pan on the drag where touch input is supported.
        flow.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> selection.pressed(event, this));
        flow.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> selection.dragged(event));
        scrollPane = new SidewaysScrollPane(flow);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToHeight(true);
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> WheelPassing.passVerticalPartOn(this, event));
        getChildren().add(scrollPane);
        addEventHandler(KeyEvent.KEY_PRESSED, event -> keyPressed(event));
    }

    /**
     * The runs of a block's text: one per piece, with the style classes of the piece's category and its fallback
     * chain, and one per plain stretch before, between and after the pieces; the whole text as one run without
     * pieces, the empty text too.
     */
    private static List<Text> runs(String text, List<CodePiece> pieces, String... textStyleClasses) {
        if (pieces.isEmpty()) {
            return List.of(run(text, textStyleClasses));
        }
        List<Text> runs = new ArrayList<>();
        int position = 0;
        for (CodePiece piece : pieces) {
            if (piece.start() > position) {
                runs.add(run(text.substring(position, piece.start()), textStyleClasses));
            }
            Text run = run(text.substring(piece.start(), piece.end()), textStyleClasses);
            run.getStyleClass().addAll(piece.category().styleClasses());
            runs.add(run);
            position = piece.end();
        }
        if (position < text.length()) {
            runs.add(run(text.substring(position), textStyleClasses));
        }
        return runs;
    }

    /**
     * A run of a block's text: {@code markdown-text}, the given style classes and the monospace family, set inline as
     * on inline code.
     */
    private static Text run(String text, String... styleClasses) {
        Text run = new Text(text);
        run.getStyleClass().add("markdown-text");
        run.getStyleClass().addAll(styleClasses);
        run.setStyle(MonospaceFont.style());
        return run;
    }

    /**
     * The shortcut key plus C copies the document selection if it reaches into the block, else nothing; plus A
     * selects the block's whole text. Both are consumed, so the key goes nowhere else while the block has the focus.
     */
    private void keyPressed(KeyEvent event) {
        if (COPY.match(event)) {
            event.consume();
            if (isSelected()) {
                selection.copySelection();
            }
        } else if (SELECT_ALL.match(event)) {
            event.consume();
            selection.selectAll(this);
        }
    }

    /**
     * Whether characters of the block are selected: the document selection reaches into it.
     */
    private boolean isSelected() {
        return flow.hasSelection();
    }

    @Override
    public Node node() {
        return this;
    }

    @Override
    public int length() {
        return text.length();
    }

    /**
     * The character gap nearest to a point, where a text field puts its caret; from scene coordinates, so a block
     * scrolled sideways finds the character that shows there. A point above or below the text counts as one on its
     * first or last line, a point left or right of a line as one at its start or end.
     */
    @Override
    public int positionAt(double sceneX, double sceneY) {
        return Math.clamp(hit(sceneX, sceneY).getInsertionIndex(), 0, length());
    }

    /**
     * The word around the character under a point, a word as code has it: a run of letters, digits and underscores,
     * so {@code max_rows} is one word and {@code java.util.Comparator} three (the platform's word iterator would join
     * them across the dots, as in prose); else a run of spaces and tabs, else the one character.
     */
    @Override
    public Range wordAt(double sceneX, double sceneY) {
        if (text.isEmpty()) {
            return new Range(0, 0);
        }
        int index = Math.clamp(hit(sceneX, sceneY).getCharIndex(), 0, text.length() - 1);
        CharacterKind kind = CharacterKind.of(text.charAt(index));
        int start = index;
        int end = index + 1;
        if (kind != CharacterKind.OTHER) {
            while (start > 0 && CharacterKind.of(text.charAt(start - 1)) == kind) {
                start--;
            }
            while (end < text.length() && CharacterKind.of(text.charAt(end)) == kind) {
                end++;
            }
        }
        return new Range(start, end);
    }

    /**
     * The line around the character under a point, without its line feed: what a reader copies to take one line out
     * of an example.
     */
    @Override
    public Range paragraphAt(double sceneX, double sceneY) {
        int index = Math.clamp(hit(sceneX, sceneY).getCharIndex(), 0, text.length());
        int lineFeed = text.indexOf('\n', index);
        return new Range(text.lastIndexOf('\n', index - 1) + 1, lineFeed < 0 ? text.length() : lineFeed);
    }

    /**
     * Selects the characters from {@code start} (inclusive) to {@code end} (exclusive); an empty range selects
     * nothing and shows no highlight.
     */
    @Override
    public void select(int start, int end) {
        flow.select(start, end);
    }

    /**
     * The characters of a range as written: lines of code separated by line feeds.
     */
    @Override
    public String text(int start, int end) {
        return text.substring(start, end);
    }

    /**
     * One text, the block's text as it shows it: its positions are the block's.
     */
    @Override
    public List<String> searchTexts() {
        return List.of(text);
    }

    @Override
    public void showSearchMatches(List<Match> matches, int current) {
        flow.showSearchMatches(matches, current);
    }

    @Override
    public Bounds searchMatchBounds(Match match) {
        return flow.localToScene(SearchMarks.boundsOf(flow, new Range(match.start(), match.end())));
    }

    /**
     * Scrolls the block sideways, if its text is wider than the block and the match does not show whole, as
     * {@link SidewaysReveal} has it. The scroll pane's content is the flow, so the flow's coordinates are the
     * content's.
     */
    @Override
    public void revealSideways(Match match) {
        double visibleWidth = scrollPane.getViewportBounds().getWidth();
        double range = flow.getLayoutBounds().getWidth() - visibleWidth;
        double span = scrollPane.getHmax() - scrollPane.getHmin();
        if (visibleWidth <= 0 || range <= 0 || span <= 0) {
            return;
        }
        Bounds bounds = SearchMarks.boundsOf(flow, new Range(match.start(), match.end()));
        double offset = (scrollPane.getHvalue() - scrollPane.getHmin()) / span * range;
        double wanted = SidewaysReveal.offset(bounds, offset, visibleWidth);
        if (wanted == offset) {
            return;
        }
        scrollPane.setHvalue(scrollPane.getHmin() + Math.clamp(wanted / range, 0, 1) * span);
    }

    @Override
    public List<BlockContainer> containers() {
        return containers;
    }

    /**
     * Not spaced: a line feed sets the block apart from its neighbours, a blank line from a heading or a paragraph.
     */
    @Override
    public boolean spaced() {
        return false;
    }

    /**
     * The source line of the text's line at the height, the first or the last line above or below the text.
     */
    @Override
    public int sourceLineAt(double sceneY) {
        int index = Math.clamp(hit(flow.localToScene(0, 0).getX(), sceneY).getCharIndex(), 0, text.length());
        return lines.text() + lineFeedsBefore(index);
    }

    /**
     * The block's line if the whole block lies below the height, else the source line of the text's first line whose
     * top does.
     */
    @Override
    public int sourceLineFrom(double sceneY) {
        if (SourceLines.startsFrom(this, sceneY)) {
            return lines.block();
        }
        int line = 0;
        int lineStart = 0;
        while (lineStart >= 0) {
            if (SourceLines.isAtOrBelow(flow.localToScene(0, lineTop(lineStart)).getY(), sceneY)) {
                return lines.text() + line;
            }
            int lineFeed = text.indexOf('\n', lineStart);
            lineStart = lineFeed < 0 ? -1 : lineFeed + 1;
            line++;
        }
        return -1;
    }

    /**
     * The source line of the text's line where the match starts.
     */
    @Override
    public int sourceLine(Match match) {
        return lines.text() + lineFeedsBefore(match.start());
    }

    /**
     * The number of line feeds in the text before a position: the index of the text's line the position lies in.
     */
    private int lineFeedsBefore(int position) {
        int count = 0;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    /**
     * The top of the line a position starts, in the flow's coordinates: the top of the caret there.
     */
    private double lineTop(int lineStart) {
        double top = Double.POSITIVE_INFINITY;
        for (PathElement element : flow.caretShape(lineStart, true)) {
            if (element instanceof MoveTo move) {
                top = Math.min(top, move.getY());
            } else if (element instanceof LineTo line) {
                top = Math.min(top, line.getY());
            }
        }
        return top;
    }

    /**
     * What of the text lies under a point: the character, and the character gap nearest to it (its insertion index).
     */
    private HitInfo hit(double sceneX, double sceneY) {
        Point2D point = flow.sceneToLocal(sceneX, sceneY);
        return flow.getHitInfo(point);
    }

    /**
     * The baseline of the text's first line, from the insets on the way to the flow and the flow's baseline, which
     * counts its padding. A list item places its marker by this baseline while it lays out its blocks, before the
     * block lays out its own children, so the positions of these are not there yet the first time.
     */
    @Override
    public double getBaselineOffset() {
        return snappedTopInset() + scrollPane.getInsets().getTop() + flow.getBaselineOffset();
    }

    /**
     * What a double click takes together with its neighbours: word characters with word characters, spaces with
     * spaces; any other character stands alone.
     */
    private enum CharacterKind {
        WORD,
        SPACE,
        OTHER;

        /**
         * The kind of a character. Both halves of a surrogate pair count as word characters, so a double click never
         * splits one.
         */
        static CharacterKind of(char character) {
            if (Character.isLetterOrDigit(character) || character == '_' || Character.isSurrogate(character)) {
                return WORD;
            }
            if (character == ' ' || character == '\t') {
                return SPACE;
            }
            return OTHER;
        }
    }

    /**
     * The flow of a block's text: its runs, and over them, its last child, the layer that shows the selection, there
     * from the start, its copies of the runs made on the first selection. The layer holds the selected range, the
     * block's only record of it. The search's layers, made on the first match, lie between the runs and it.
     */
    /**
     * The source lines of a block.
     *
     * @param block the 1-based source line the block starts on: a code block's opening fence, a source block's first
     *              line
     * @param text  the 1-based source line of the text's first line: the line below a code block's fence, a source
     *              block's first line
     */
    record Lines(int block, int text) {
    }

    private static final class BlockFlow extends TextFlow {

        /**
         * The columns from one tab stop to the next: four, as the editors the examples come from show code, and the
         * closest to the about three and a half columns a tab took while the text was a lone {@code Text}.
         */
        private static final int TAB_SIZE = 4;

        private final HighlightLayer layer;
        private final SearchMarks searchMarks = new SearchMarks(this);

        BlockFlow(List<Text> runs) {
            // A value set here, not in the theme: an application's stylesheet may still set -fx-tab-size.
            setTabSize(TAB_SIZE);
            getChildren().addAll(runs);
            layer = new HighlightLayer(this, HighlightPath.SELECTION);
            getChildren().add(layer);
        }

        /**
         * Shows the search's matches in the block's text, below the selection.
         */
        void showSearchMatches(List<Match> matches, int current) {
            List<Range> ranges = new ArrayList<>(matches.size());
            for (Match match : matches) {
                ranges.add(new Range(match.start(), match.end()));
            }
            searchMarks.show(ranges, current, layer);
        }

        /**
         * Shows the characters from {@code start} (inclusive) to {@code end} (exclusive) selected; an empty range
         * selects nothing and shows no highlight.
         */
        void select(int start, int end) {
            layer.show(start, end);
        }

        /**
         * Whether characters of the text are selected.
         */
        boolean hasSelection() {
            return layer.isShowing();
        }

        /**
         * Lays out the runs, then the search's and the selection's layers over them.
         */
        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            searchMarks.layOverFlow();
            layer.layOverFlow();
        }
    }

    /**
     * The block's scroll pane, as tall as its content plus the horizontal bar when the content is wider than the
     * pane: a plain scroll pane asks for its content's height only, and the bar, once shown, then covers the last
     * line and leaves the pane room to scroll vertically. Its height depends on its width, hence the horizontal
     * content bias, which makes the boxes around it pass the width they give it.
     */
    private static final class SidewaysScrollPane extends ScrollPane {

        SidewaysScrollPane(Node content) {
            super(content);
        }

        @Override
        public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override
        protected double computePrefHeight(double width) {
            double height = super.computePrefHeight(width);
            if (width >= 0 && getContent() != null
                    && getContent().prefWidth(-1) > width - snappedLeftInset() - snappedRightInset()) {
                height += horizontalBarHeight();
            }
            return height;
        }

        /**
         * The preferred height of the skin's horizontal bar, 0 before the skin exists.
         */
        private double horizontalBarHeight() {
            for (Node node : lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.HORIZONTAL) {
                    return bar.prefHeight(-1);
                }
            }
            return 0;
        }
    }
}
