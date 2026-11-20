package org.karthu.fx.markdown.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The source lines the parser gives blocks, list items, table rows and code lines, in the cases the view's tests do
 * not show: line endings, and blocks inside containers.
 */
class SourceLinesParserTest {

    @Test
    @DisplayName("\\n, \\r\\n and \\r each end one line, mixed in one text too")
    void lineEndings() {
        String[] endings = {"\n", "\r\n", "\r"};
        for (String ending : endings) {
            String text = String.join(ending, "# a", "", "b", "c", "", "---");
            assertEquals(List.of(1, 3, 6), blockLines(MarkdownParser.parse(text).blocks()), ending);
        }
        assertEquals(List.of(1, 3, 5, 7), blockLines(MarkdownParser.parse("a\r\n\rb\n\r\nc\r\rd").blocks()));
    }

    @Test
    @DisplayName("a \\n\\r is two line endings, a \\r\\n one")
    void lineFeedThenCarriageReturn() {
        assertEquals(List.of(1, 3), blockLines(MarkdownParser.parse("a\n\rb").blocks()));
        assertEquals(List.of(1, 3), blockLines(MarkdownParser.parse("a\r\n\r\nb").blocks()));
    }

    @Test
    @DisplayName("each list item has its marker's line, after blank lines and lazy lines too")
    void listItems() {
        BulletList list = (BulletList) MarkdownParser.parse("- a\nlazy\n- b\n\n  more\n\n- c").blocks().getFirst();

        assertEquals(1, list.line());
        assertEquals(List.of(1, 3, 7), itemLines(list.items()));
        assertEquals(List.of(3, 5), blockLines(list.items().get(1).blocks()));
    }

    @Test
    @DisplayName("an item whose marker stands alone starts its paragraph on the next line, a task's paragraph on its own")
    void itemContentOnTheNextLine() {
        OrderedList list = (OrderedList) MarkdownParser.parse("1.\n   text\n2. [x] done").blocks().getFirst();

        assertEquals(List.of(1, 3), itemLines(list.items()));
        assertEquals(List.of(2), blockLines(list.items().getFirst().blocks()));
        assertEquals(List.of(3), blockLines(list.items().get(1).blocks()));
    }

    @Test
    @DisplayName("blocks in quotes and nested items count the lines of the whole text")
    void nestedContainers() {
        BlockQuote quote = (BlockQuote) MarkdownParser.parse("intro\n\n> # h\n> - a\n>   - b\n>\n>   para\n> end")
                .blocks().get(1);

        assertEquals(3, quote.line());
        assertEquals(List.of(3, 4), blockLines(quote.blocks()));
        BulletList outer = (BulletList) quote.blocks().get(1);
        assertEquals(List.of(4), itemLines(outer.items()));
        BulletList inner = (BulletList) outer.items().getFirst().blocks().get(1);
        assertEquals(List.of(5), itemLines(inner.items()));
        assertEquals(List.of(4, 5, 7), blockLines(outer.items().getFirst().blocks()));
    }

    @Test
    @DisplayName("a code block's lines follow its fence, a table's rows its delimiter row, a source block's lines its first")
    void linesOfBlocks() {
        List<Block> blocks = MarkdownParser.parse("```\nx\ny\n```\n| a |\n|---|\n| 1 |\n| 2 |\n\n<div>\n</div>\n\n"
                + "    code\n    more").blocks();

        CodeBlock code = (CodeBlock) blocks.getFirst();
        assertEquals(1, code.line());
        assertEquals(2, code.codeLine(0));
        assertEquals(3, code.codeLine(1));
        Table table = (Table) blocks.get(1);
        assertEquals(5, table.line());
        assertEquals(7, table.rowLine(0));
        assertEquals(8, table.rowLine(1));
        SourceBlock html = (SourceBlock) blocks.get(2);
        assertEquals(10, html.line());
        assertEquals(11, html.sourceLine(1));
        SourceBlock indented = (SourceBlock) blocks.get(3);
        assertEquals(13, indented.sourceLine(0));
        assertEquals(14, indented.sourceLine(1));
    }

    @Test
    @DisplayName("a code block in a list item has the item's lines, not the item content's")
    void codeInItem() {
        BulletList list = (BulletList) MarkdownParser.parse("- a\n\n- b\n\n  ```\n  code\n  ```").blocks().getFirst();

        CodeBlock code = (CodeBlock) list.items().get(1).blocks().get(1);
        assertEquals(5, code.line());
        assertEquals(6, code.codeLine(0));
    }

    private static List<Integer> blockLines(List<Block> blocks) {
        List<Integer> lines = new ArrayList<>();
        for (Block block : blocks) {
            lines.add(block.line());
        }
        return lines;
    }

    private static List<Integer> itemLines(List<ListItem> items) {
        List<Integer> lines = new ArrayList<>();
        for (ListItem item : items) {
            lines.add(item.line());
        }
        return lines;
    }
}
