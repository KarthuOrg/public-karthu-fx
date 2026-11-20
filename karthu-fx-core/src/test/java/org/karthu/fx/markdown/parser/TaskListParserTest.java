package org.karthu.fx.markdown.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GFM task list items: which list items the parser reads as tasks, open or done, and what stays text.
 *
 * <p>As in {@link MarkdownParserTest}, most expectations are HTML written by {@link HtmlFixture}, which writes a task
 * item's check box as the GFM specification's examples show it, so those examples paste verbatim.
 */
class TaskListParserTest {

    private static final String OPEN = "<input disabled=\"\" type=\"checkbox\"> ";
    private static final String DONE = "<input checked=\"\" disabled=\"\" type=\"checkbox\"> ";

    @Test
    @DisplayName("GFM's task list examples: open and done items, also nested")
    void gfmTaskListExamples() {
        // GFM spec 0.29, examples 279 and 280.
        assertHtml("<ul>\n<li><input disabled=\"\" type=\"checkbox\"> foo</li>\n"
                + "<li><input checked=\"\" disabled=\"\" type=\"checkbox\"> bar</li>\n</ul>", "- [ ] foo\n- [x] bar");
        assertHtml("<ul>\n<li><input checked=\"\" disabled=\"\" type=\"checkbox\"> foo\n<ul>\n"
                        + "<li><input disabled=\"\" type=\"checkbox\"> bar</li>\n"
                        + "<li><input checked=\"\" disabled=\"\" type=\"checkbox\"> baz</li>\n</ul>\n</li>\n"
                        + "<li><input disabled=\"\" type=\"checkbox\"> bim</li>\n</ul>",
                "- [x] foo\n  - [ ] bar\n  - [x] baz\n- [ ] bim");
    }

    @Test
    @DisplayName("the document model carries each item's state, and the marker is out of the paragraph's text")
    void itemsCarryTheirState() {
        MarkdownDocument document = MarkdownParser.parse("- [ ] open\n- [x] done\n- [X] done too\n- plain");

        assertEquals(List.of(new BulletList(true, List.of(
                        new ListItem(List.of(paragraph("open", 1)), ListItem.Task.OPEN, 1),
                        new ListItem(List.of(paragraph("done", 2)), ListItem.Task.DONE, 2),
                        new ListItem(List.of(paragraph("done too", 3)), ListItem.Task.DONE, 3),
                        new ListItem(List.of(paragraph("plain", 4)), ListItem.Task.NONE, 4)), 1)),
                document.blocks());
        assertEquals(ListItem.Task.NONE, new ListItem(List.of(), ListItem.Task.NONE, 1).task());
    }

    @Test
    @DisplayName("every bullet and both delimiters of ordered lists take tasks")
    void everyListKind() {
        assertHtml("<ul>\n<li>" + DONE + "star</li>\n</ul>\n<ul>\n<li>" + OPEN + "plus</li>\n</ul>",
                "* [x] star\n+ [ ] plus");
        assertHtml("<ol>\n<li>" + OPEN + "one</li>\n<li>" + DONE + "two</li>\n</ol>\n<ol start=\"3\">\n<li>" + DONE
                + "three</li>\n</ol>", "1. [ ] one\n2. [x] two\n3) [X] three");
    }

    @Test
    @DisplayName("a tab or more spaces may follow the marker, and the text starts after them")
    void whitespaceAfterTheMarker() {
        assertHtml("<ul>\n<li>" + DONE + "tab</li>\n</ul>", "- [x]\ttab");
        assertHtml("<ul>\n<li>" + OPEN + "spaced</li>\n</ul>", "- [ ]    spaced");
        assertHtml("<ul>\n<li>" + OPEN + "four</li>\n</ul>", "-    [ ] four");
    }

    @Test
    @DisplayName("tasks in a loose list, inside a quote, deep in nested lists of either kind")
    void tasksInContainers() {
        assertHtml("<ul>\n<li>" + OPEN + "\n<p>one</p>\n</li>\n<li>" + DONE + "\n<p>two</p>\n</li>\n</ul>",
                "- [ ] one\n\n- [x] two");
        assertHtml("<blockquote>\n<ul>\n<li>" + DONE + "quoted</li>\n</ul>\n</blockquote>", "> - [x] quoted");
        assertHtml("<ol>\n<li>plain\n<ul>\n<li>" + OPEN + "sub\n<ol>\n<li>" + DONE + "deep</li>\n</ol>\n</li>\n</ul>\n"
                + "</li>\n</ol>", "1. plain\n   - [ ] sub\n     1. [x] deep");
    }

    @Test
    @DisplayName("the task's text may hold Markdown that would start a block on a line of its own, and lazy lines")
    void theTextIsParagraphText() {
        assertHtml("<ul>\n<li>" + OPEN + "# no heading</li>\n</ul>", "- [ ] # no heading");
        assertHtml("<ul>\n<li>" + DONE + "&gt; no quote</li>\n</ul>", "- [x] > no quote");
        assertHtml("<ul>\n<li>" + OPEN + "<strong>bold</strong> and <a href=\"https://example.com\">a link</a></li>\n"
                + "</ul>", "- [ ] **bold** and [a link](https://example.com)");
        assertHtml("<ul>\n<li>" + OPEN + "foo\nbar</li>\n</ul>", "- [ ] foo\nbar");
        assertHtml("<ul>\n<li>" + OPEN + "foo\nbar</li>\n</ul>", "- [ ] foo\n  bar");
    }

    @Test
    @DisplayName("an empty task has no paragraph, alone, followed by blocks or by its text on the next line")
    void emptyTasks() {
        assertHtml("<ul>\n<li>" + OPEN + "</li>\n</ul>", "- [ ]");
        assertHtml("<ul>\n<li>" + DONE + "</li>\n<li>" + OPEN + "</li>\n</ul>", "- [x]   \n- [ ]\t");
        assertEquals(List.of(new BulletList(true, List.of(new ListItem(List.of(), ListItem.Task.OPEN, 1)), 1)),
                MarkdownParser.parse("- [ ]").blocks());
        assertHtml("<ul>\n<li>" + OPEN + "\n<pre><code>code\n</code></pre>\n</li>\n</ul>", "- [ ]\n  ```\n  code\n  ```");
        assertHtml("<ul>\n<li>" + DONE + "\n<ul>\n<li>sub</li>\n</ul>\n</li>\n</ul>", "- [x]\n  - sub");
        assertHtml("<ul>\n<li>" + OPEN + "next line</li>\n</ul>", "- [ ]\n  next line");
        assertHtml("<ul>\n<li>" + OPEN + "lazy</li>\n</ul>", "- [ ]\nlazy");
    }

    @Test
    @DisplayName("a blank line after an empty task's marker line makes the list loose, as after any paragraph")
    void blankLineAfterAnEmptyTask() {
        assertHtml("<ul>\n<li>" + OPEN + "\n<p>after a blank line</p>\n</li>\n</ul>", "- [ ]\n\n  after a blank line");
    }

    @Test
    @DisplayName("other characters between the brackets stay text: no states beyond GitHub's")
    void otherStatesStayText() {
        assertHtml("<ul>\n<li>[/] half</li>\n<li>[-] dropped</li>\n<li>[?] asked</li>\n<li>[q] q</li>\n</ul>",
                "- [/] half\n- [-] dropped\n- [?] asked\n- [q] q");
        assertHtml("<ul>\n<li>[  ] two spaces</li>\n<li>[ x ] spaced</li>\n<li>[] empty</li>\n</ul>",
                "- [  ] two spaces\n- [ x ] spaced\n- [] empty");
        assertHtml("<ul>\n<li>[\t] tab</li>\n</ul>", "- [\t] tab");
    }

    @Test
    @DisplayName("without whitespace after the bracket the marker is a link or text")
    void noWhitespaceAfterTheMarker() {
        assertHtml("<ul>\n<li><a href=\"https://example.com\">x</a></li>\n</ul>", "- [x](https://example.com)");
        assertHtml("<ul>\n<li>[x]done</li>\n</ul>", "- [x]done");
        assertHtml("<ul>\n<li>[ ]<em>soon</em></li>\n</ul>", "- [ ]*soon*");
        assertHtml("<ul>\n<li>[x] escaped</li>\n</ul>", "- \\[x] escaped");
    }

    @Test
    @DisplayName("the marker counts only at the start of an item's first paragraph, on the list marker's line")
    void markerElsewhereStaysText() {
        assertHtml("<ul>\n<li>foo [x] bar</li>\n</ul>", "- foo [x] bar");
        assertHtml("<ul>\n<li>foo\n[x] bar</li>\n</ul>", "- foo\n  [x] bar");
        assertHtml("<ul>\n<li>[x] next line</li>\n</ul>", "-\n  [x] next line");
        assertHtml("<ul>\n<li>\n<p>first</p>\n<p>[x] second</p>\n</li>\n</ul>", "- first\n\n  [x] second");
        assertHtml("<ul>\n<li>\n<pre><code>[x] five spaces\n</code></pre>\n</li>\n</ul>", "-     [x] five spaces");
    }

    @Test
    @DisplayName("outside a list item the marker is text: a paragraph, a quote, a table cell")
    void outsideListsStaysText() {
        assertHtml("<p>[x] no list</p>", "[x] no list");
        assertHtml("<blockquote>\n<p>[ ] quote</p>\n</blockquote>", "> [ ] quote");
        assertHtml("<ul>\n<li>\n<table>\n<thead>\n<tr>\n<th>[ ] a</th>\n<th>b</th>\n</tr>\n</thead>\n</table>\n</li>\n"
                + "</ul>", "- [ ] a | b\n  --- | ---");
    }

    private static Paragraph paragraph(String text, int line) {
        return new Paragraph(List.of(new PlainText(text)), line);
    }

    private static void assertHtml(String expectedHtml, String markdown) {
        assertEquals(expectedHtml, HtmlFixture.html(markdown), () -> "Markdown: " + markdown);
    }
}
