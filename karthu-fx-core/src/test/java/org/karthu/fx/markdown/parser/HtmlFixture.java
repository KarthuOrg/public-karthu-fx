package org.karthu.fx.markdown.parser;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes the document model as the HTML CommonMark's reference renderer produces, so parser tests can state their
 * expectations as CommonMark specification examples do.
 *
 * <p>Like the reference renderer (commonmark.js), it puts a line break before and after each block tag unless the
 * output already ends in one, and leaves out the {@code <p>} tags of a paragraph directly inside an item of a tight
 * list.
 *
 * <p>A table is written as cmark-gfm, GFM's reference renderer, writes it, so GFM's table examples paste verbatim.
 *
 * <p>Test fixture only. Later block and inline kinds get their case here as they join the model.
 */
final class HtmlFixture {

    private final StringBuilder html = new StringBuilder();

    private HtmlFixture() {
    }

    /**
     * Parses a Markdown text and writes the result as HTML.
     *
     * @param markdown the Markdown text
     * @return the HTML, one block tag per line, without a line ending after the last one
     */
    static String html(String markdown) {
        HtmlFixture fixture = new HtmlFixture();
        fixture.blocks(MarkdownParser.parse(markdown).blocks(), false);
        String html = fixture.html.toString();
        return html.endsWith("\n") ? html.substring(0, html.length() - 1) : html;
    }

    /**
     * Writes blocks.
     *
     * @param inTightItem whether the blocks are the content of an item of a tight list
     */
    private void blocks(List<Block> blocks, boolean inTightItem) {
        for (Block block : blocks) {
            switch (block) {
                case Heading heading -> {
                    cr();
                    html.append("<h").append(heading.level()).append('>').append(inlines(heading.content()))
                            .append("</h").append(heading.level()).append('>');
                    cr();
                }
                case Paragraph paragraph -> {
                    if (inTightItem) {
                        html.append(inlines(paragraph.content()));
                    } else {
                        cr();
                        html.append("<p>").append(inlines(paragraph.content())).append("</p>");
                        cr();
                    }
                }
                case BulletList list -> list("<ul>", "</ul>", list.tight(), list.items());
                case OrderedList list -> list(list.start() == 1 ? "<ol>" : "<ol start=\"" + list.start() + "\">",
                        "</ol>", list.tight(), list.items());
                case BlockQuote quote -> {
                    cr();
                    html.append("<blockquote>");
                    cr();
                    blocks(quote.blocks(), false);
                    cr();
                    html.append("</blockquote>");
                    cr();
                }
                case ThematicBreak thematicBreak -> {
                    cr();
                    html.append("<hr />");
                    cr();
                }
                case CodeBlock code -> {
                    cr();
                    html.append("<pre><code").append(languageClass(code.info())).append('>')
                            .append(escape(code.literal())).append("</code></pre>");
                    cr();
                }
                case Table table -> table(table);
                case SourceBlock source -> source(source);
            }
        }
    }

    /**
     * Writes a table as cmark-gfm does: a {@code thead} with the header row, a {@code tbody} with the body rows if
     * there are any, a tag per line, and an {@code align} attribute on the cells of a column with a colon.
     */
    private void table(Table table) {
        cr();
        html.append("<table>\n<thead>\n");
        row(table.header(), table.alignments(), "th");
        html.append("</thead>\n");
        if (!table.rows().isEmpty()) {
            html.append("<tbody>\n");
            for (List<Table.Cell> row : table.rows()) {
                row(row, table.alignments(), "td");
            }
            html.append("</tbody>\n");
        }
        html.append("</table>");
        cr();
    }

    private void row(List<Table.Cell> cells, List<Table.Alignment> alignments, String tag) {
        html.append("<tr>\n");
        for (int i = 0; i < cells.size(); i++) {
            html.append('<').append(tag);
            switch (alignments.get(i)) {
                case NONE -> {
                }
                case LEFT -> html.append(" align=\"left\"");
                case CENTER -> html.append(" align=\"center\"");
                case RIGHT -> html.append(" align=\"right\"");
            }
            html.append('>').append(inlines(cells.get(i).content())).append("</").append(tag).append(">\n");
        }
        html.append("</tr>\n");
    }

    /**
     * Writes a source block as CommonMark writes the block it stands for: an HTML block raw, an indented code block
     * as code without the four spaces of indentation.
     */
    private void source(SourceBlock source) {
        cr();
        switch (source.kind()) {
            case HTML -> html.append(String.join("\n", source.lines()));
            case INDENTED_CODE -> {
                html.append("<pre><code>");
                for (String line : source.lines()) {
                    int indentation = 0;
                    while (indentation < 4 && indentation < line.length() && line.charAt(indentation) == ' ') {
                        indentation++;
                    }
                    html.append(escape(line.substring(indentation))).append('\n');
                }
                html.append("</code></pre>");
            }
        }
        cr();
    }

    private void list(String openingTag, String closingTag, boolean tight, List<ListItem> items) {
        cr();
        html.append(openingTag);
        cr();
        for (ListItem item : items) {
            html.append("<li>");
            blocks(item.blocks(), tight);
            html.append("</li>");
            cr();
        }
        cr();
        html.append(closingTag);
        cr();
    }

    /**
     * Starts a new line unless the output is empty or already ends in one.
     */
    private void cr() {
        if (!html.isEmpty() && html.charAt(html.length() - 1) != '\n') {
            html.append('\n');
        }
    }

    private static String inlines(List<Inline> inlines) {
        StringBuilder html = new StringBuilder();
        for (Inline inline : inlines) {
            switch (inline) {
                case PlainText plainText -> html.append(escape(plainText.text()));
                case CodeSpan codeSpan -> html.append("<code>").append(escape(codeSpan.code())).append("</code>");
                case Emphasis emphasis -> html.append("<em>").append(inlines(emphasis.content())).append("</em>");
                case Strong strong -> html.append("<strong>").append(inlines(strong.content())).append("</strong>");
                case Link link -> {
                    html.append("<a href=\"").append(escape(encodeUri(link.destination()))).append('"');
                    if (!link.title().isEmpty()) {
                        html.append(" title=\"").append(escape(link.title())).append('"');
                    }
                    html.append('>').append(inlines(link.content())).append("</a>");
                }
                case Image image -> {
                    html.append("<img src=\"").append(escape(encodeUri(image.destination()))).append("\" alt=\"")
                            .append(escape(image.alt())).append('"');
                    if (!image.title().isEmpty()) {
                        html.append(" title=\"").append(escape(image.title())).append('"');
                    }
                    html.append(" />");
                }
                case SoftBreak softBreak -> html.append('\n');
                case HardBreak hardBreak -> html.append("<br />\n");
            }
        }
        return html.toString();
    }

    /**
     * The class attribute CommonMark writes for a code block's info string: {@code language-} and its first word.
     */
    private static String languageClass(String info) {
        if (info.isEmpty()) {
            return "";
        }
        int end = 0;
        while (end < info.length() && info.charAt(end) != ' ' && info.charAt(end) != '\t') {
            end++;
        }
        return " class=\"language-" + escape(info.substring(0, end)) + "\"";
    }

    /**
     * Percent-encodes an address as commonmark.js writes it into {@code href} (mdurl's {@code encode}): ASCII letters,
     * digits, {@code ;/?:@&=+$,-_.!~*'()#} and existing {@code %XX} sequences stay, every other character becomes
     * the {@code %XX} sequences of its UTF-8 bytes. The parser keeps the address as written; only the fixture
     * encodes, so CommonMark's examples can be pasted verbatim.
     */
    private static String encodeUri(String address) {
        StringBuilder encoded = new StringBuilder();
        byte[] bytes = address.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            boolean keep = (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9')
                    || (b < 0x80 && ";/?:@&=+$,-_.!~*'()#".indexOf(b) >= 0);
            if (b == '%' && i + 2 < bytes.length && isHexDigit(bytes[i + 1]) && isHexDigit(bytes[i + 2])) {
                keep = true;
            }
            if (keep) {
                encoded.append((char) b);
            } else {
                encoded.append('%').append(String.format("%02X", b));
            }
        }
        return encoded.toString();
    }

    private static boolean isHexDigit(byte b) {
        return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
