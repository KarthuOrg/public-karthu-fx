package org.karthu.fx.markdown;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.karthu.fx.font.MonospaceFont;
import org.karthu.fx.markdown.parser.Block;
import org.karthu.fx.markdown.parser.BlockQuote;
import org.karthu.fx.markdown.parser.BulletList;
import org.karthu.fx.markdown.parser.CodeBlock;
import org.karthu.fx.markdown.parser.CodeSpan;
import org.karthu.fx.markdown.parser.Emphasis;
import org.karthu.fx.markdown.parser.HardBreak;
import org.karthu.fx.markdown.parser.Heading;
import org.karthu.fx.markdown.parser.Image;
import org.karthu.fx.markdown.parser.Inline;
import org.karthu.fx.markdown.parser.Link;
import org.karthu.fx.markdown.parser.ListItem;
import org.karthu.fx.markdown.parser.MarkdownDocument;
import org.karthu.fx.markdown.parser.OrderedList;
import org.karthu.fx.markdown.parser.Paragraph;
import org.karthu.fx.markdown.parser.PlainText;
import org.karthu.fx.markdown.parser.SoftBreak;
import org.karthu.fx.markdown.parser.SourceBlock;
import org.karthu.fx.markdown.parser.Strong;
import org.karthu.fx.markdown.parser.Table;
import org.karthu.fx.markdown.parser.ThematicBreak;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Turns a parsed document into JavaFX nodes: one node per block, styled through the view's style classes.
 */
final class MarkdownRenderer {

    /**
     * The marker of a bullet list item.
     */
    private static final String BULLET = "\u2022";

    /**
     * The space between a table cell's image icon and its alt text.
     */
    private static final String NO_BREAK_SPACE = "\u00a0";

    /**
     * The pseudo-class of a loose list.
     */
    private static final PseudoClass LOOSE = PseudoClass.getPseudoClass("loose");

    /**
     * The space between a code block's edges and its copy button. A margin in a {@code StackPane} is no CSS
     * property, so it is set here.
     */
    private static final Insets COPY_BUTTON_MARGIN = new Insets(4);

    /**
     * What a click on a link does: it gets the link's address.
     */
    private final Consumer<String> linkAction;

    /**
     * What copying in a block does: it gets a code block's code from its copy button, or the text of a table's
     * selected cells.
     */
    private final Consumer<String> copyAction;

    /**
     * The tooltip and accessible text of the copy buttons, which follow its changes.
     */
    private final ObservableValue<String> copyCodeText;

    /**
     * What a table does when cells become selected: it is the block with the view's one selection now.
     */
    private final Consumer<SelectionHolder> selectionAction;

    /**
     * Which pieces of a code block the view colours.
     */
    private final CodeColoring coloring;

    /**
     * The view's selection across the document, which gets every leaf block with text as its segments: the text
     * flows of headings and paragraphs, code and source blocks, the blocks of tables.
     */
    private final DocumentSelection documentSelection;

    /**
     * The view's images, which create the block of an image alone in its paragraph and load it.
     */
    private final DocumentImages images;

    /**
     * The quotes and list items around the block being rendered, the outermost first.
     */
    private final List<BlockContainer> containers = new ArrayList<>();

    /**
     * Creates a renderer, which renders every document of a view.
     *
     * @param linkAction        called with a link's address when the link is clicked
     * @param copyAction        called with a code block's code when its copy button is pressed, and with the text of
     *                          a table's selected cells when the reader copies them
     * @param copyCodeText      the copy buttons' tooltip, bound so that a change applies to the buttons shown
     * @param selectionAction   called with a table when cells become selected in it
     * @param documentSelection the selection across the document, given the segments in document order
     * @param coloring          which pieces of a code block the view colours
     * @param images            the view's images, which create and load the blocks of images
     */
    MarkdownRenderer(Consumer<String> linkAction, Consumer<String> copyAction, ObservableValue<String> copyCodeText,
                     Consumer<SelectionHolder> selectionAction, DocumentSelection documentSelection,
                     CodeColoring coloring, DocumentImages images) {
        this.linkAction = linkAction;
        this.copyAction = copyAction;
        this.copyCodeText = copyCodeText;
        this.selectionAction = selectionAction;
        this.documentSelection = documentSelection;
        this.coloring = coloring;
        this.images = images;
    }

    /**
     * Renders the document's blocks and hands their segments to the document selection, which the view has
     * reset for them.
     *
     * @param document the parsed document
     * @return one node per top-level block, in source order
     */
    List<Node> render(MarkdownDocument document) {
        containers.clear();
        return renderBlocks(document.blocks());
    }

    /**
     * Renders blocks, of the document or of a container block, one node per block.
     */
    private List<Node> renderBlocks(List<Block> blocks) {
        List<Node> nodes = new ArrayList<>();
        for (Block block : blocks) {
            nodes.add(renderBlock(block));
        }
        return nodes;
    }

    private Node renderBlock(Block block) {
        Node node = switch (block) {
            case Heading heading -> textSegment(heading.content(), true,
                    "markdown-heading", "markdown-heading-" + heading.level());
            case Paragraph paragraph -> paragraph(paragraph);
            case BulletList list -> bulletList(list);
            case OrderedList list -> orderedList(list);
            case BlockQuote quote -> quote(quote);
            case ThematicBreak thematicBreak -> separator();
            case CodeBlock code -> codeBlock(code);
            case Table table -> tableBlock(table);
            case SourceBlock source -> sourceBlock(source);
        };
        if (!(block instanceof BulletList || block instanceof OrderedList || block instanceof BlockQuote)) {
            // A leaf block: the first one in a list item takes the item's marker in copied text.
            for (BlockContainer container : containers) {
                container.leafRendered(node);
            }
        }
        return node;
    }

    /**
     * A paragraph: an image block if its only content is one image, or one link whose only content is one image,
     * apart from white space around it, the block following the link and a segment of the document selection as the
     * paragraph would be; else a text segment.
     */
    private Node paragraph(Paragraph paragraph) {
        Inline only = onlyInline(paragraph.content());
        Link link = null;
        if (only instanceof Link onlyLink) {
            link = onlyLink;
            only = onlyInline(onlyLink.content());
        }
        if (only instanceof Image image) {
            ImageBlock block = images.block(image.destination(), image.title(), image.alt(), containers,
                    !inTightItem());
            if (link != null) {
                block.linkTo(link.destination(), linkAction);
            }
            documentSelection.add(block);
            return block;
        }
        return textSegment(paragraph.content(), !inTightItem(), "markdown-paragraph");
    }

    /**
     * The one inline among white space: text of blanks only and line breaks.
     *
     * @return the inline, {@code null} if there is none or more than one
     */
    private static Inline onlyInline(List<Inline> content) {
        Inline only = null;
        for (Inline inline : content) {
            boolean whiteSpace = inline instanceof SoftBreak || inline instanceof HardBreak
                    || (inline instanceof PlainText plainText && plainText.text().isBlank());
            if (!whiteSpace) {
                if (only != null) {
                    return null;
                }
                only = inline;
            }
        }
        return only;
    }

    /**
     * Whether the block being rendered lies directly in a tight list's item, where a paragraph is no paragraph of
     * its own in copied text (HTML writes no {@code <p>} there).
     */
    private boolean inTightItem() {
        return !containers.isEmpty() && containers.getLast() instanceof BlockContainer.Item item && item.tight();
    }

    /**
     * A table in its block, which scrolls it sideways when it is wider than the document and is the table's segment
     * of the document selection.
     */
    private Node tableBlock(Table table) {
        MarkdownTable tableView = new MarkdownTable(table, (flow, content) -> addCellRuns(flow, content), copyAction,
                selectionAction);
        TableBlock block = new TableBlock(tableView, containers);
        documentSelection.add(block);
        return block;
    }

    /**
     * A block the view does not render, an HTML block or indented code: its source lines as written, in a monospace
     * block that keeps them whole and scrolls sideways like a code block, in the text colour and without a copy
     * button.
     */
    private Node sourceBlock(SourceBlock source) {
        MonospaceBlock block = new MonospaceBlock(String.join("\n", source.lines()), List.of(), documentSelection,
                containers, "markdown-source-block");
        documentSelection.add(block);
        return block;
    }

    /**
     * A fenced code block: its code in a monospace block, a text flow of {@code markdown-code} runs, which keeps its
     * lines whole and scrolls sideways, and a copy button in its top right corner. The code ends without the line feed
     * of its last line, which would show as an empty line at the bottom. A block the view colours (see
     * {@link CodeColoring}) is a run per piece its lexer finds and per plain stretch between them; any other block is
     * one run.
     */
    private Node codeBlock(CodeBlock code) {
        String literal = code.literal();
        String text = literal.endsWith("\n") ? literal.substring(0, literal.length() - 1) : literal;
        MonospaceBlock block = new MonospaceBlock(text, coloring.pieces(code.info(), text), documentSelection,
                containers, "markdown-code-block", RunStyle.CODE.styleClass());
        block.getChildren().add(copyButton(text));
        documentSelection.add(block);
        return block;
    }

    /**
     * A code block's copy button, which copies the code as the block shows it: the theme's icon button with the
     * clipboard icon, laid over the block's top right corner. It is the block's child after the block's scroll pane,
     * not part of what the pane scrolls, so it stays in place when the code scrolls sideways.
     */
    private Button copyButton(String code) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", "markdown-code-copy");
        button.setGraphic(FontIcon.of(BootstrapIcons.CLIPBOARD));
        // Bound, not copied: a new label applies to the buttons already shown. A binding's listener refers to its
        // target weakly, so the view's property does not keep the buttons of a replaced document alive.
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(copyCodeText);
        button.setTooltip(tooltip);
        button.accessibleTextProperty().bind(copyCodeText);
        button.setOnAction(event -> copyAction.accept(code));
        StackPane.setAlignment(button, Pos.TOP_RIGHT);
        StackPane.setMargin(button, COPY_BUTTON_MARGIN);
        return button;
    }

    /**
     * A block quote: a box of the quote's blocks, which the theme indents and marks with a bar on its left.
     */
    private Node quote(BlockQuote quote) {
        VBox box = new VBox();
        box.getStyleClass().add("markdown-quote");
        containers.add(new BlockContainer.Quote());
        box.getChildren().addAll(renderBlocks(quote.blocks()));
        containers.removeLast();
        return box;
    }

    private static Node separator() {
        Separator separator = new Separator();
        separator.getStyleClass().add("markdown-separator");
        return separator;
    }

    private Node bulletList(BulletList list) {
        VBox box = listBox("markdown-bullet-list", list.tight(), list.items());
        for (ListItem item : list.items()) {
            box.getChildren().add(listItem(BULLET, "-", list.tight(), item));
        }
        return box;
    }

    /**
     * An ordered list: the items count on from the list's start number, each marker the number and the delimiter.
     */
    private Node orderedList(OrderedList list) {
        VBox box = listBox("markdown-ordered-list", list.tight(), List.of());
        long number = list.start();
        for (ListItem item : list.items()) {
            String marker = number + String.valueOf(list.delimiter());
            box.getChildren().add(listItem(marker, marker, list.tight(), item));
            number++;
        }
        return box;
    }

    /**
     * The box of a list's item rows. A loose list (CommonMark: blank lines between its items or their blocks) gets
     * the {@code loose} pseudo-class, so the theme spaces its items and their blocks like paragraphs. A list with a
     * task item gets {@code markdown-task-list}, so the theme can make room for the check boxes.
     *
     * @param items the items whose tasks show check boxes
     */
    private static VBox listBox(String styleClass, boolean tight, List<ListItem> items) {
        VBox box = new VBox();
        box.getStyleClass().add(styleClass);
        box.pseudoClassStateChanged(LOOSE, !tight);
        for (ListItem item : items) {
            if (item.task() != ListItem.Task.NONE) {
                box.getStyleClass().add("markdown-task-list");
                break;
            }
        }
        return box;
    }

    /**
     * An item row: the marker as shown, and the item's blocks, rendered inside the item as their container, whose
     * copied text starts with the marker as written for copying ({@code -} for a bullet).
     *
     * <p>A task item of a bullet list shows its check box in place of the bullet. Its copied marker carries the task
     * marker as GFM writes it ({@code - [ ]}, {@code - [x]}), so the copy keeps the state.
     */
    private Node listItem(String markerText, String copiedMarker, boolean tight, ListItem item) {
        TaskBox taskBox = null;
        String copied = copiedMarker;
        if (item.task() != ListItem.Task.NONE && markerText.equals(BULLET)) {
            taskBox = new TaskBox(item.task() == ListItem.Task.DONE);
            copied = copiedMarker + (item.task() == ListItem.Task.DONE ? " [x]" : " [ ]");
        }
        Label marker = null;
        if (taskBox == null) {
            marker = new Label(markerText);
            marker.getStyleClass().add("markdown-list-marker");
        }
        containers.add(new BlockContainer.Item(copied, tight));
        List<Node> blocks = renderBlocks(item.blocks());
        containers.removeLast();
        return new ListItemBox(marker, taskBox, blocks);
    }

    /**
     * The text flow of a heading or a paragraph, with one {@link Text} per style run (see
     * {@link #addRuns(TextFlow, List)}), and a segment of the document selection.
     *
     * @param spaced whether copied text sets it apart from its neighbours by a blank line
     */
    private TextSegment textSegment(List<Inline> content, boolean spaced, String... styleClasses) {
        TextSegment flow = new TextSegment(containers, spaced);
        flow.getStyleClass().addAll(styleClasses);
        addRuns(flow, content);
        documentSelection.add(flow);
        return flow;
    }

    /**
     * Adds the inlines of a heading or a paragraph to its text flow, one {@link Text} per style run: the inline tree
     * is flattened into runs of equal format (styles and link address), each run carrying a style class per style it
     * has. An image is a node of its own between the runs, an {@link InlineImage}, which follows the link around it.
     */
    private void addRuns(TextFlow flow, List<Inline> content) {
        for (StyledRun run : styledRuns(content)) {
            switch (run) {
                case TextRun text -> flow.getChildren().add(textRun(text.text().toString(), text.format()));
                case ImageRun imageRun -> {
                    Image image = imageRun.image();
                    InlineImage inline = images.inline(image.destination(), image.title(), image.alt());
                    if (imageRun.format().address() != null) {
                        inline.linkTo(imageRun.format().address(), linkAction);
                    }
                    flow.getChildren().add(inline);
                }
            }
        }
    }

    /**
     * Adds the inlines of a table's cell, header or body, to its text flow, as {@link #addRuns} does a paragraph's,
     * but an image as text: an image icon followed by the image's alt text, both {@code markdown-image-cell}, which
     * the theme styles like a link, so the table stays compact and its column widths measure the image as the text it
     * is. The image is never loaded to show it. Its title is the tooltip of both, and a click on either opens the
     * image at full size; inside a link both are the link's runs. Both carry the image ({@link CellImage}), so the
     * context menu opens it too.
     */
    private void addCellRuns(TextFlow flow, List<Inline> content) {
        for (StyledRun run : styledRuns(content)) {
            switch (run) {
                case TextRun text -> flow.getChildren().add(textRun(text.text().toString(), text.format()));
                case ImageRun image -> addCellImage(flow, image.image(), image.format());
            }
        }
    }

    /**
     * Adds an image of a table's cell to its text flow as an image icon and its alt text, see {@link #addCellRuns}.
     *
     * @param format the format of the inlines around the image, a link's address among them
     */
    private void addCellImage(TextFlow flow, Image image, Format format) {
        FontIcon icon = FontIcon.of(BootstrapIcons.IMAGE);
        icon.getStyleClass().add("markdown-image-cell");
        if (format.address() != null) {
            icon.getStyleClass().add(RunStyle.LINK.styleClass());
        }
        List<Text> parts = new ArrayList<>(List.of(icon));
        String altText = cellAltText(image);
        if (altText != null) {
            Text alt = textRun(altText, format);
            alt.getStyleClass().add("markdown-image-cell");
            parts.add(alt);
        }
        CellImage cellImage = new CellImage(image.destination(), image.alt());
        for (Text part : parts) {
            cellImage.markShownBy(part);
            if (format.address() != null) {
                makeLink(part, format.address());
            } else {
                opensOnClick(part, cellImage);
                if (!image.title().isEmpty()) {
                    Tooltip.install(part, new Tooltip(image.title()));
                }
            }
            flow.getChildren().add(part);
        }
    }

    /**
     * The run of text an image of a table's cell shows after its icon: a no-break space and the alt text, so the alt
     * text never wraps away from its icon, and the column's longest word, which ends at white space, is the icon and
     * the alt text's first word together.
     *
     * @return the run's text, {@code null} for an image without alt text, which shows its icon alone
     */
    private static String cellAltText(Image image) {
        return image.alt().isEmpty() ? null : NO_BREAK_SPACE + image.alt();
    }

    /**
     * The texts the view's search runs through in a table's cell, header or body: the text the cell's flow shows,
     * derived from its inlines as {@link #addCellRuns} builds the flow's runs, without building them, so a cell never
     * drawn is searched too. An image's icon is a part of the flow no match takes, so the cell's text is split there:
     * a cell without images is one text, and each image starts a new one with the text the flow shows after the icon,
     * the no-break space and the alt text first. A position in a text counts the flow's characters from the cell's
     * start, or from just after the icon before the text.
     *
     * @param content the cell's inlines
     * @return the texts, one more than the cell has images, empty ones included
     */
    static List<String> cellSearchTexts(List<Inline> content) {
        List<String> texts = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (StyledRun run : styledRuns(content)) {
            switch (run) {
                case TextRun textRun -> text.append(textRun.text());
                case ImageRun imageRun -> {
                    texts.add(text.toString());
                    text.setLength(0);
                    String altText = cellAltText(imageRun.image());
                    if (altText != null) {
                        text.append(altText);
                    }
                }
            }
        }
        texts.add(text.toString());
        return texts;
    }

    /**
     * Makes the icon or the alt text of an image in a table's body cell open the image at full size, as a link's run
     * follows the link: a hand cursor, and a single click with the primary button without moving since the press
     * opens it. In a column header, which the same runs fill, a click sorts the column and opens nothing; the context
     * menu still opens the image there.
     */
    private void opensOnClick(Text part, CellImage image) {
        part.setCursor(Cursor.HAND);
        part.setOnMouseClicked(event -> {
            if (StillClick.matches(event) && Ancestors.nearest(part, TableColumnHeader.class, null) == null) {
                event.consume();
                images.open(image);
            }
        });
    }

    /**
     * The runs of inlines, see {@link #addRuns}.
     */
    private static List<StyledRun> styledRuns(List<Inline> content) {
        List<StyledRun> runs = new ArrayList<>();
        collectRuns(content, new Format(EnumSet.noneOf(RunStyle.class), null), runs);
        return runs;
    }

    /**
     * A {@link Text} of one format: the classes of its styles, the monospace font for code, a link's behaviour.
     */
    private Text textRun(String content, Format format) {
        Text text = new Text(content);
        text.getStyleClass().add("markdown-text");
        for (RunStyle style : format.styles()) {
            text.getStyleClass().add(style.styleClass());
        }
        if (format.styles().contains(RunStyle.CODE)) {
            text.setStyle(MonospaceFont.style());
        }
        if (format.address() != null) {
            makeLink(text, format.address());
        }
        return text;
    }

    /**
     * The text inlines show, without their Markdown: code without its backticks, emphasis without its asterisks, a
     * link as its text, an image as its alt text, a soft break as a space.
     *
     * @param content the inlines
     * @return the visible text
     */
    static String visibleText(List<Inline> content) {
        StringBuilder visible = new StringBuilder();
        for (StyledRun run : styledRuns(content)) {
            switch (run) {
                case TextRun text -> visible.append(text.text());
                case ImageRun image -> visible.append(image.image().alt());
            }
        }
        return visible.toString();
    }

    /**
     * Makes a run a link: a hand cursor, the address as its tooltip, and a single click with the primary button that
     * hands the address to the link action. Being a run of the paragraph's text flow, a long link wraps with it.
     * The second click of a double click is ignored, so a double click follows a link once. A click after the
     * pointer moved since the press (JavaFX sends one when the release is on the pressed node) ends a drag that
     * selects text, so it follows nothing.
     */
    private void makeLink(Text text, String address) {
        text.setCursor(Cursor.HAND);
        Tooltip.install(text, new Tooltip(address));
        text.setOnMouseClicked(event -> {
            if (StillClick.matches(event)) {
                event.consume();
                linkAction.accept(address);
            }
        });
    }

    /**
     * Flattens inlines into runs. The parser limits how deep inlines nest, so the recursion stays shallow.
     *
     * @param format the format of the inlines around the content
     */
    private static void collectRuns(List<Inline> content, Format format, List<StyledRun> runs) {
        for (Inline inline : content) {
            switch (inline) {
                case PlainText plainText -> appendRun(plainText.text(), format, runs);
                case CodeSpan codeSpan -> appendRun(codeSpan.code(), format.with(RunStyle.CODE), runs);
                case Emphasis emphasis -> collectRuns(emphasis.content(), format.with(RunStyle.EMPHASIS), runs);
                case Strong strong -> collectRuns(strong.content(), format.with(RunStyle.STRONG), runs);
                case Link link -> collectRuns(link.content(), format.link(link.destination()), runs);
                case Image image -> runs.add(new ImageRun(image, format));
                case SoftBreak softBreak -> appendRun(" ", format, runs);
                case HardBreak hardBreak -> appendRun("\n", format, runs);
            }
        }
    }

    /**
     * Appends text to the last run if it is text of the same format, else starts a new run.
     */
    private static void appendRun(String text, Format format, List<StyledRun> runs) {
        if (!runs.isEmpty() && runs.getLast() instanceof TextRun last && last.format().equals(format)) {
            last.text().append(text);
        } else {
            runs.add(new TextRun(new StringBuilder(text), format));
        }
    }

    /**
     * A style a run of text can have, with the style class that shows it.
     */
    private enum RunStyle {
        STRONG("markdown-strong"),
        EMPHASIS("markdown-emphasis"),
        CODE("markdown-code"),
        LINK("markdown-link");

        private final String styleClass;

        RunStyle(String styleClass) {
            this.styleClass = styleClass;
        }

        private String styleClass() {
            return styleClass;
        }
    }

    /**
     * The format of a run: its styles and the link it belongs to. Adjacent text of equal format is one run.
     *
     * @param styles  the run's styles
     * @param address the address of the link around the run, {@code null} outside a link
     */
    private record Format(Set<RunStyle> styles, String address) {

        /**
         * This format with one more style.
         */
        private Format with(RunStyle style) {
            Set<RunStyle> combined = EnumSet.noneOf(RunStyle.class);
            combined.addAll(styles);
            combined.add(style);
            return new Format(combined, address);
        }

        /**
         * This format inside a link to an address: with the link style and the link's address.
         */
        private Format link(String linkAddress) {
            return new Format(with(RunStyle.LINK).styles(), linkAddress);
        }
    }

    /**
     * A run of inlines: text of one format, or an image, a run of its own.
     */
    private sealed interface StyledRun permits TextRun, ImageRun {
    }

    /**
     * Text of one format, collected until the format changes.
     *
     * @param text   the text
     * @param format the format of the text
     */
    private record TextRun(StringBuilder text, Format format) implements StyledRun {
    }

    /**
     * An image, a run of its own.
     *
     * @param image  the image
     * @param format the format of the inlines around the image, a link's address among them
     */
    private record ImageRun(Image image, Format format) implements StyledRun {
    }
}
