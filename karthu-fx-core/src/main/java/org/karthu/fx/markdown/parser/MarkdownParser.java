package org.karthu.fx.markdown.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Markdown text into a {@link MarkdownDocument}: karthu-fx's own parser for the subset the Markdown view
 * renders, close to CommonMark.
 *
 * <p>The parser never throws on any text: whatever it cannot read as a construct stays text.
 */
public final class MarkdownParser {

    private MarkdownParser() {
    }

    /**
     * Parses a Markdown text.
     *
     * @param markdown the Markdown text; {@code null} gives an empty document
     * @return the parsed document
     */
    public static MarkdownDocument parse(String markdown) {
        if (markdown == null) {
            return new MarkdownDocument(List.of());
        }
        return new MarkdownDocument(BlockParser.parseBlocks(lines(markdown)));
    }

    /**
     * Splits the text into lines, with {@code \r\n} and {@code \r} normalised to {@code \n}. A line ending ends a
     * line: the end of a text that ends in one starts no further, empty line (CommonMark), which an unclosed code
     * block would otherwise take as a blank line of its code.
     */
    private static List<String> lines(String markdown) {
        String normalised = markdown.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        int start = 0;
        int end = normalised.indexOf('\n');
        while (end >= 0) {
            lines.add(normalised.substring(start, end));
            start = end + 1;
            end = normalised.indexOf('\n', start);
        }
        if (start < normalised.length() || lines.isEmpty()) {
            lines.add(normalised.substring(start));
        }
        return lines;
    }
}
