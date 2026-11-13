package org.karthu.fx.markdown.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The parser's rules, pinned down without JavaFX: Markdown text in, the document model out.
 *
 * <p>Most expectations are the HTML that CommonMark's reference renderer gives for the same text, written from the
 * model by {@link HtmlFixture}, one block per line. That makes them checkable against the CommonMark specification.
 */
class MarkdownParserTest {

    @Test
    @DisplayName("an ATX heading becomes a heading block with its level and text")
    void atxHeading() {
        MarkdownDocument document = MarkdownParser.parse("# Title");

        assertEquals(List.of(new Heading(1, List.of(new PlainText("Title")))), document.blocks());
    }

    @Test
    @DisplayName("ATX headings have levels 1 to 6 and need a space after the hashes")
    void headingLevels() {
        assertHtml("<h1>one</h1>", "# one");
        assertHtml("<h2>two</h2>", "## two");
        assertHtml("<h3>three</h3>", "### three");
        assertHtml("<h4>four</h4>", "#### four");
        assertHtml("<h5>five</h5>", "##### five");
        assertHtml("<h6>six</h6>", "###### six");
        assertHtml("<p>####### seven</p>", "####### seven");
        assertHtml("<p>#5 bolt</p>", "#5 bolt");
        assertHtml("<p>#hashtag</p>", "#hashtag");
    }

    @Test
    @DisplayName("a heading's closing hashes and surrounding spaces are stripped, hashes inside the text stay")
    void headingClosingSequence() {
        assertHtml("<h1>foo</h1>", "# foo ##################################");
        assertHtml("<h5>foo</h5>", "##### foo ##");
        assertHtml("<h3>foo</h3>", "### foo ###     ");
        assertHtml("<h3>foo ### b</h3>", "### foo ### b");
        assertHtml("<h1>foo#</h1>", "# foo#");
        assertHtml("<h1>foo</h1>", "#                  foo                     ");
        assertHtml("<h2>C# and F#</h2>", "## C# and F#");
    }

    @Test
    @DisplayName("a heading may be empty and indented by up to three spaces")
    void headingEmptyAndIndented() {
        assertHtml("<h1></h1>", "#");
        assertHtml("<h2></h2>", "## ");
        assertHtml("<h3></h3>", "### ###");
        assertHtml("<h3>foo</h3>", " ### foo");
        assertHtml("<h1>foo</h1>", "   # foo");
        assertHtml("<h2>tab</h2>", "##\ttab");
    }

    @Test
    @DisplayName("consecutive lines form one paragraph, their line breaks soft breaks")
    void paragraphLinesFlowTogether() {
        MarkdownDocument document = MarkdownParser.parse("aaa\nbbb");

        assertEquals(List.of(new Paragraph(List.of(new PlainText("aaa"), new SoftBreak(), new PlainText("bbb")))),
                document.blocks());
    }

    @Test
    @DisplayName("blank lines separate paragraphs, a heading interrupts one, surrounding spaces are stripped")
    void paragraphBoundaries() {
        assertHtml("<p>aaa</p>\n<p>bbb</p>", "aaa\n\nbbb");
        assertHtml("<p>aaa</p>\n<p>bbb</p>", "aaa\n   \t\n\n\nbbb");
        assertHtml("<p>aaa</p>\n<h1>head</h1>\n<p>bbb</p>", "aaa\n# head\nbbb");
        assertHtml("<p>aaa\nbbb</p>", "  aaa\n bbb");
        assertHtml("<p>aaa\nbbb\nccc</p>", "aaa\n             bbb\n                                       ccc");
        assertHtml("<p>aaa\nbbb</p>", "aaa \nbbb   ");
        assertHtml("<p>aaa</p>", "\n\n  \naaa\n\n\n");
    }

    @Test
    @DisplayName("null, the empty text and blank lines give an empty document")
    void emptyInput() {
        assertEquals(List.of(), MarkdownParser.parse(null).blocks());
        assertEquals(List.of(), MarkdownParser.parse("").blocks());
        assertEquals(List.of(), MarkdownParser.parse("  \n\t\n\n").blocks());
    }

    @Test
    @DisplayName("Windows and old Mac line endings are normalised")
    void lineEndingsNormalised() {
        assertHtml("<p>aaa\nbbb\nccc</p>", "aaa\r\nbbb\rccc");
        assertHtml("<h1>a</h1>\n<p>b</p>", "# a\r\n\r\nb\r\n");
        assertHtml("<p>a</p>\n<p>b</p>", "a\r\rb");
    }

    @Test
    @DisplayName("two trailing spaces or a trailing backslash make a hard line break, not at the end of a block")
    void hardLineBreaks() {
        MarkdownDocument document = MarkdownParser.parse("foo  \nbar");
        assertEquals(List.of(new Paragraph(List.of(new PlainText("foo"), new HardBreak(), new PlainText("bar")))),
                document.blocks());

        assertHtml("<p>foo<br />\nbaz</p>", "foo\\\nbaz");
        assertHtml("<p>foo<br />\nbaz</p>", "foo       \nbaz");
        assertHtml("<p>foo<br />\nbar</p>", "foo  \n     bar");
        assertHtml("<p>foo\nbar</p>", "foo \nbar");
        assertHtml("<p>foo\\</p>", "foo\\");
        assertHtml("<p>foo</p>", "foo  ");
        assertHtml("<h3>foo\\</h3>", "### foo\\");
        assertHtml("<h3>foo</h3>", "### foo  ");
    }

    @Test
    @DisplayName("a backslash escapes ASCII punctuation and stays literal before anything else")
    void backslashEscapes() {
        assertHtml("<p>!&quot;#$%&amp;'()*+,-./:;&lt;=&gt;?@[\\]^_`{|}~</p>",
                "\\!\\\"\\#\\$\\%\\&\\'\\(\\)\\*\\+\\,\\-\\.\\/\\:\\;\\<\\=\\>\\?\\@\\[\\\\\\]\\^\\_\\`\\{\\|\\}\\~");
        assertHtml("<p>\\A\\a\\ \\3\\é</p>", "\\A\\a\\ \\3\\é");
        assertHtml("<h3>foo ###</h3>", "### foo \\###");
        assertHtml("<p>\\</p>", "\\");
    }

    @Test
    @DisplayName("a code span is closed by a backtick run of the same length")
    void codeSpans() {
        MarkdownDocument document = MarkdownParser.parse("a `b` c");
        assertEquals(List.of(new Paragraph(List.of(new PlainText("a "), new CodeSpan("b"), new PlainText(" c")))),
                document.blocks());

        assertHtml("<p><code>foo ` bar</code></p>", "`` foo ` bar ``");
        assertHtml("<p><code>``</code></p>", "` `` `");
        assertHtml("<p><code>foo`bar</code></p>", "``foo`bar``");
        assertHtml("<p>```foo``</p>", "```foo``");
        assertHtml("<p>`foo</p>", "`foo");
        assertHtml("<p>`foo<code>bar</code></p>", "`foo``bar``");
        assertHtml("<p>`not code`</p>", "\\`not code`");
    }

    @Test
    @DisplayName("a code span's line endings become spaces, one space on both sides is stripped")
    void codeSpanSpaces() {
        assertHtml("<p><code> `` </code></p>", "`  ``  `");
        assertHtml("<p><code> a</code></p>", "` a`");
        assertHtml("<p><code> </code>\n<code>  </code></p>", "` `\n`  `");
        assertHtml("<p><code>foo bar   baz</code></p>", "``\nfoo\nbar  \nbaz\n``");
        assertHtml("<p><code>foo   bar  baz</code></p>", "`foo   bar \nbaz`");
    }

    @Test
    @DisplayName("a code span's content is literal: no escapes, no emphasis")
    void codeSpanContentIsLiteral() {
        assertHtml("<p><code>foo\\</code>bar`</p>", "`foo\\`bar`");
        assertHtml("<p><code>**not bold** \\*</code></p>", "`**not bold** \\*`");
    }

    @Test
    @DisplayName("single delimiters make emphasis, double ones strong emphasis, triple ones both")
    void emphasisForms() {
        MarkdownDocument document = MarkdownParser.parse("a *b* c");
        assertEquals(List.of(new Paragraph(List.of(
                        new PlainText("a "), new Emphasis(List.of(new PlainText("b"))), new PlainText(" c")))),
                document.blocks());

        assertHtml("<p><em>foo bar</em></p>", "*foo bar*");
        assertHtml("<p><em>foo bar</em></p>", "_foo bar_");
        assertHtml("<p><strong>foo bar</strong></p>", "**foo bar**");
        assertHtml("<p><strong>foo bar</strong></p>", "__foo bar__");
        assertHtml("<p><em><strong>foo</strong></em></p>", "***foo***");
        assertHtml("<p><em><strong>foo</strong></em></p>", "___foo___");
        assertHtml("<p>foo<em><strong>bar</strong></em>baz</p>", "foo***bar***baz");
    }

    @Test
    @DisplayName("flanking rules keep spaced asterisks and intraword underscores literal")
    void emphasisFlankingRules() {
        assertHtml("<p>2 * 3 * 4</p>", "2 * 3 * 4");
        assertHtml("<p>a * foo bar*</p>", "a * foo bar*");
        assertHtml("<p>a*&quot;foo&quot;*</p>", "a*\"foo\"*");
        assertHtml("<p>*foo bar *</p>", "*foo bar *");
        assertHtml("<p>** foo bar**</p>", "** foo bar**");
        assertHtml("<p>foo<em>bar</em></p>", "foo*bar*");
        assertHtml("<p>5<em>6</em>78</p>", "5*6*78");
        assertHtml("<p>snake_case_name and MAX_ROW_COUNT stay</p>", "snake_case_name and MAX_ROW_COUNT stay");
        assertHtml("<p>foo_bar_</p>", "foo_bar_");
        assertHtml("<p>foo__bar__</p>", "foo__bar__");
        assertHtml("<p>_ foo bar_</p>", "_ foo bar_");
        assertHtml("<p>_foo_bar</p>", "_foo_bar");
        assertHtml("<p><em>foo_bar_baz</em></p>", "_foo_bar_baz_");
        assertHtml("<p>foo-<em>(bar)</em></p>", "foo-_(bar)_");
        assertHtml("<p><em>(bar)</em>.</p>", "_(bar)_.");
        assertHtml("<p>_foo*</p>", "_foo*");
        assertHtml("<p>*(*foo)</p>", "*(*foo)");
        assertHtml("<p><em>(<em>foo</em>)</em></p>", "*(*foo*)*");
        assertHtml("<p><em>foo</em>bar</p>", "*foo*bar");
        assertHtml("<p>** is not an empty emphasis</p>", "** is not an empty emphasis");
    }

    @Test
    @DisplayName("emphasis nests in strong emphasis and the other way round")
    void emphasisNesting() {
        assertHtml("<p><em>foo <strong>bar</strong> baz</em></p>", "*foo **bar** baz*");
        assertHtml("<p><em>foo<strong>bar</strong>baz</em></p>", "*foo**bar**baz*");
        assertHtml("<p><strong>foo <em>bar</em> baz</strong></p>", "**foo *bar* baz**");
        assertHtml("<p><strong>foo<em>bar</em>baz</strong></p>", "**foo*bar*baz**");
        assertHtml("<p><em><strong>foo</strong> bar</em></p>", "***foo** bar*");
        assertHtml("<p><em>foo <strong>bar</strong></em></p>", "*foo **bar***");
        assertHtml("<p><strong><em>foo</em> bar</strong></p>", "***foo* bar**");
        assertHtml("<p><strong>foo <em>bar</em></strong></p>", "**foo *bar***");
        assertHtml("<p><strong>foo, <strong>bar</strong>, baz</strong></p>", "__foo, __bar__, baz__");
        assertHtml("<p><em>(<strong>foo</strong>)</em></p>", "*(**foo**)*");
        assertHtml("<p><strong>foo &quot;<em>bar</em>&quot; foo</strong></p>", "**foo \"*bar*\" foo**");
        assertHtml("<p>foo<strong><strong><strong>bar</strong></strong></strong>***baz</p>",
                "foo******bar*********baz");
        assertHtml("<p><em>foo <strong>bar *baz bim</strong> bam</em></p>", "*foo __bar *baz bim__ bam*");
    }

    @Test
    @DisplayName("unmatched delimiters stay literal, the rule of three decides close calls")
    void emphasisUnmatchedAndRuleOfThree() {
        assertHtml("<p><em>foo**bar</em></p>", "*foo**bar*");
        assertHtml("<p>*<em>foo</em></p>", "**foo*");
        assertHtml("<p><em>foo</em>*</p>", "*foo**");
        assertHtml("<p>*<strong>foo</strong></p>", "***foo**");
        assertHtml("<p>***<em>foo</em></p>", "****foo*");
        assertHtml("<p><strong>foo</strong>*</p>", "**foo***");
        assertHtml("<p><em>foo</em>***</p>", "*foo****");
        assertHtml("<p><em>foo _bar</em> baz_</p>", "*foo _bar* baz_");
        assertHtml("<p>**foo <strong>bar baz</strong></p>", "**foo **bar baz**");
        assertHtml("<p>*foo <em>bar baz</em></p>", "*foo *bar baz*");
        assertHtml("<p>**foo</p>", "**foo");
        assertHtml("<p>__bold never closed</p>", "__bold never closed");
    }

    @Test
    @DisplayName("emphasis spans soft and hard line breaks")
    void emphasisAcrossLines() {
        assertHtml("<p><em>foo\nbar</em></p>", "*foo\nbar*");
        assertHtml("<p><strong>a long bold\nphrase</strong> ends</p>", "**a long bold\nphrase** ends");
        assertHtml("<p><em>foo<br />\nbar</em></p>", "*foo  \nbar*");
        assertHtml("<p><strong><em>both\nlines</em></strong></p>", "**_both\nlines_**");
    }

    @Test
    @DisplayName("code spans and escapes take precedence over emphasis")
    void emphasisPrecedence() {
        assertHtml("<p><em>a <code>*</code></em></p>", "*a `*`*");
        assertHtml("<p><em>a <code>_</code></em></p>", "_a `_`_");
        assertHtml("<p>*foo<code>*</code></p>", "*foo`*`");
        assertHtml("<p>*not emphasized*</p>", "\\*not emphasized*");
        assertHtml("<p>\\<em>emphasis</em></p>", "\\\\*emphasis*");
        assertHtml("<p>foo <em>*</em></p>", "foo *\\**");
        assertHtml("<p>foo <strong>*</strong></p>", "foo **\\***");
        assertHtml("<p><strong>bold <code>code</code></strong></p>", "**bold `code`**");
    }

    @Test
    @DisplayName("a setext underline is no heading: a === line stays paragraph text")
    void setextHeadingsAreNotRecognised() {
        assertHtml("<p>Title\n===</p>", "Title\n===");
    }

    @Test
    @DisplayName("consecutive bullet items form one bullet list, each item holding its content as blocks")
    void bulletList() {
        MarkdownDocument document = MarkdownParser.parse("- foo\n- bar");

        assertEquals(List.of(new BulletList(true, List.of(
                        new ListItem(List.of(new Paragraph(List.of(new PlainText("foo"))))),
                        new ListItem(List.of(new Paragraph(List.of(new PlainText("bar")))))))),
                document.blocks());
    }

    @Test
    @DisplayName("lines indented to an item's content column continue the item, which starts after the marker's spaces")
    void itemContinuationLines() {
        assertHtml("<ul>\n<li>foo\nbar</li>\n</ul>", "- foo\n  bar");
        assertHtml("<ul>\n<li>foo\nbar</li>\n</ul>", "- foo\n    bar");
        assertHtml("<ul>\n<li>foo\nbar</li>\n</ul>", "-   foo\n    bar");
        assertHtml("<ul>\n<li>one\ntwo</li>\n</ul>", " -    one\n      two");
        assertHtml("<ul>\n<li>\n<h1>Foo</h1>\n</li>\n<li>Bar</li>\n</ul>", "- # Foo\n- Bar");
        assertHtml("<ul>\n<li>foo</li>\n<li>bar</li>\n</ul>", "* foo\n* bar");
        assertHtml("<ul>\n<li>foo</li>\n<li>bar</li>\n</ul>", "+ foo\n+ bar");
    }

    @Test
    @DisplayName("a different bullet character starts a new list")
    void bulletCharacterChangeStartsNewList() {
        assertHtml("<ul>\n<li>foo</li>\n<li>bar</li>\n</ul>\n<ul>\n<li>baz</li>\n</ul>", "- foo\n- bar\n+ baz");
    }

    @Test
    @DisplayName("a list nests in an item to any depth, an item indented less than the content column is a sibling")
    void nestedBulletLists() {
        assertHtml("""
                <ul>
                <li>foo
                <ul>
                <li>bar
                <ul>
                <li>baz
                <ul>
                <li>boo</li>
                </ul>
                </li>
                </ul>
                </li>
                </ul>
                </li>
                </ul>""", "- foo\n  - bar\n    - baz\n      - boo");
        assertHtml("<ul>\n<li>foo</li>\n<li>bar</li>\n<li>baz</li>\n<li>boo</li>\n</ul>",
                "- foo\n - bar\n  - baz\n   - boo");
        assertHtml("<ul>\n<li>\n<ul>\n<li>foo</li>\n</ul>\n</li>\n</ul>", "- - foo");
        assertHtml("<ul>\n<li>a\n<ul>\n<li>b</li>\n</ul>\n</li>\n</ul>", "- a\n    - b");
    }

    @Test
    @DisplayName("an ordered list keeps its start number and delimiter")
    void orderedList() {
        MarkdownDocument document = MarkdownParser.parse("3) three\n4) four");
        assertEquals(List.of(new OrderedList(3, ')', true, List.of(
                        new ListItem(List.of(new Paragraph(List.of(new PlainText("three"))))),
                        new ListItem(List.of(new Paragraph(List.of(new PlainText("four")))))))),
                document.blocks());

        assertHtml("<ol>\n<li>one</li>\n<li>two</li>\n</ol>", "1. one\n2. two");
        assertHtml("<ol start=\"123456789\">\n<li>ok</li>\n</ol>", "123456789. ok");
        assertHtml("<ol start=\"0\">\n<li>ok</li>\n</ol>", "0. ok");
        assertHtml("<ol start=\"3\">\n<li>ok</li>\n</ol>", "003. ok");
        assertHtml("<ol>\n<li>a</li>\n<li>b</li>\n<li>c</li>\n</ol>", "1. a\n1. b\n1. c");
    }

    @Test
    @DisplayName("an ordered marker has one to nine digits, then a dot or parenthesis and a space")
    void orderedMarkerForms() {
        assertHtml("<p>1234567890. not ok</p>", "1234567890. not ok");
        assertHtml("<p>-1. not ok</p>", "-1. not ok");
        assertHtml("<p>-one</p>\n<p>2.two</p>", "-one\n\n2.two");
        assertHtml("<p>1: no</p>", "1: no");
        assertHtml("<ol>\n<li>foo</li>\n<li>bar</li>\n</ol>\n<ol start=\"3\">\n<li>baz</li>\n</ol>",
                "1. foo\n2. bar\n3) baz");
    }

    @Test
    @DisplayName("only an ordered list starting at 1 interrupts a paragraph, a bullet list always does")
    void listsInterruptingParagraphs() {
        assertHtml("<p>Foo</p>\n<ul>\n<li>bar</li>\n<li>baz</li>\n</ul>", "Foo\n- bar\n- baz");
        assertHtml("<p>The number of windows in my house is\n14.  The number of doors is 6.</p>",
                "The number of windows in my house is\n14.  The number of doors is 6.");
        assertHtml("<p>The number of windows in my house is</p>\n<ol>\n<li>The number of doors is 6.</li>\n</ol>",
                "The number of windows in my house is\n1.  The number of doors is 6.");
    }

    @Test
    @DisplayName("lists of either kind nest in either kind")
    void mixedNesting() {
        assertHtml("<ul>\n<li>a\n<ol>\n<li>one</li>\n<li>two</li>\n</ol>\n</li>\n<li>b</li>\n</ul>",
                "- a\n  1. one\n  2. two\n- b");
        assertHtml("<ol>\n<li>a\n<ul>\n<li>x</li>\n</ul>\n</li>\n<li>b</li>\n</ol>", "1. a\n   - x\n2. b");
        assertHtml("<ol start=\"10\">\n<li>foo\n<ul>\n<li>bar</li>\n</ul>\n</li>\n</ol>", "10) foo\n    - bar");
        assertHtml("<ol start=\"10\">\n<li>foo</li>\n</ol>\n<ul>\n<li>bar</li>\n</ul>", "10) foo\n   - bar");
        assertHtml("<ol>\n<li>\n<ul>\n<li>\n<ol start=\"2\">\n<li>foo</li>\n</ol>\n</li>\n</ul>\n</li>\n</ol>",
                "1. - 2. foo");
        assertHtml("""
                <ul>
                <li>one
                <ol>
                <li>two
                <ul>
                <li>three</li>
                </ul>
                </li>
                </ol>
                </li>
                </ul>""", "- one\n  1. two\n     - three");
    }

    @Test
    @DisplayName("after a blank line an item continues with lines indented to its content column, else it ends")
    void itemsAcrossBlankLines() {
        assertHtml("<ul>\n<li>one</li>\n</ul>\n<p>two</p>", "- one\n\n two");
        assertHtml("<ul>\n<li>\n<p>one</p>\n<p>two</p>\n</li>\n</ul>", "- one\n\n  two");
        assertHtml("<ul>\n<li>\n<p>one</p>\n<p>two</p>\n</li>\n</ul>", " -    one\n\n      two");
        assertHtml("<ul>\n<li>\n<p>foo</p>\n<p>bar</p>\n</li>\n</ul>", "- foo\n\n\n  bar");
        assertHtml("<ul>\n<li>foo</li>\n</ul>\n<p>bar</p>", "-    foo\n\n  bar");
        assertHtml("<ul>\n<li>\n<p>foo</p>\n<p>bar</p>\n</li>\n</ul>", "-  foo\n\n   bar");
    }

    @Test
    @DisplayName("an item may start with one blank line or be empty")
    void emptyAndBlankStartItems() {
        assertHtml("<ul>\n<li>foo</li>\n</ul>", "-\n  foo");
        assertHtml("<ul>\n<li>foo</li>\n</ul>", "-   \n  foo");
        assertHtml("<ul>\n<li></li>\n</ul>\n<p>foo</p>", "-\n\n  foo");
        assertHtml("<ul>\n<li></li>\n</ul>\n<p>foo</p>", "-\nfoo");
        assertHtml("<ul>\n<li>foo</li>\n<li></li>\n<li>bar</li>\n</ul>", "- foo\n-\n- bar");
        assertHtml("<ul>\n<li>foo</li>\n<li></li>\n<li>bar</li>\n</ul>", "- foo\n-   \n- bar");
        assertHtml("<ol>\n<li>foo</li>\n<li></li>\n<li>bar</li>\n</ol>", "1. foo\n2.\n3. bar");
        assertHtml("<ul>\n<li></li>\n</ul>", "*");
        assertHtml("<p>foo\n*</p>\n<p>foo\n1.</p>", "foo\n*\n\nfoo\n1.");
    }

    @Test
    @DisplayName("a list is loose if a blank line separates two items or two blocks of an item, else tight")
    void tightAndLooseLists() {
        MarkdownDocument document = MarkdownParser.parse("- a\n\n- b");
        assertEquals(List.of(new BulletList(false, List.of(
                        new ListItem(List.of(new Paragraph(List.of(new PlainText("a"))))),
                        new ListItem(List.of(new Paragraph(List.of(new PlainText("b")))))))),
                document.blocks());

        assertHtml("<ul>\n<li>\n<p>foo</p>\n</li>\n<li>\n<p>bar</p>\n</li>\n<li>\n<p>baz</p>\n</li>\n</ul>",
                "- foo\n\n- bar\n\n\n- baz");
        assertHtml("<ul>\n<li>\n<p>a</p>\n</li>\n<li>\n<p>b</p>\n</li>\n<li>\n<p>c</p>\n</li>\n</ul>",
                "- a\n- b\n\n- c");
        assertHtml("<ul>\n<li>\n<p>a</p>\n</li>\n<li></li>\n<li>\n<p>c</p>\n</li>\n</ul>", "* a\n*\n\n* c");
        assertHtml("<ul>\n<li>\n<p>a</p>\n</li>\n<li>\n<p>b</p>\n<p>c</p>\n</li>\n<li>\n<p>d</p>\n</li>\n</ul>",
                "- a\n- b\n\n  c\n- d");
        assertHtml("<ol>\n<li>\n<p>a</p>\n</li>\n<li>\n<p>b</p>\n</li>\n<li>\n<p>c</p>\n</li>\n</ol>",
                "1. a\n\n  2. b\n\n   3. c");
        assertHtml("<ul>\n<li>a</li>\n<li>b</li>\n<li>c</li>\n<li>d</li>\n<li>e</li>\n<li>f</li>\n<li>g</li>\n</ul>",
                "- a\n - b\n  - c\n   - d\n  - e\n - f\n- g");
    }

    @Test
    @DisplayName("blank lines inside a nested list make only the nested list loose")
    void loosenessOfNestedLists() {
        assertHtml("<ul>\n<li>a\n<ul>\n<li>\n<p>b</p>\n<p>c</p>\n</li>\n</ul>\n</li>\n<li>d</li>\n</ul>",
                "- a\n  - b\n\n    c\n- d");
        assertHtml("<ul>\n<li>\n<p>foo</p>\n<ul>\n<li>bar</li>\n</ul>\n<p>baz</p>\n</li>\n</ul>",
                "* foo\n  * bar\n\n  baz");
        assertHtml("""
                <ul>
                <li>
                <p>a</p>
                <ul>
                <li>b</li>
                <li>c</li>
                </ul>
                </li>
                <li>
                <p>d</p>
                <ul>
                <li>e</li>
                <li>f</li>
                </ul>
                </li>
                </ul>""", "- a\n  - b\n  - c\n\n- d\n  - e\n  - f");
        assertHtml("""
                <ul>
                <li>foo
                <ul>
                <li>bar
                <ul>
                <li>
                <p>baz</p>
                <p>bim</p>
                </li>
                </ul>
                </li>
                </ul>
                </li>
                </ul>""", "- foo\n  - bar\n    - baz\n\n\n      bim");
    }

    @Test
    @DisplayName("a less indented line right after an item's paragraph continues that paragraph lazily")
    void lazyContinuationLines() {
        assertHtml("<ol>\n<li>A paragraph\nwith two lines.</li>\n</ol>", "  1.  A paragraph\nwith two lines.");
        assertHtml("<ol>\n<li>A paragraph\nwith two lines.</li>\n</ol>", "  1.  A paragraph\n    with two lines.");
        assertHtml("<ul>\n<li>a\n<ul>\n<li>b\nlazy</li>\n</ul>\n</li>\n</ul>", "- a\n  - b\nlazy");
        assertHtml("<ul>\n<li>a</li>\n<li>b</li>\n<li>c</li>\n<li>d\n- e</li>\n</ul>",
                "- a\n - b\n  - c\n   - d\n    - e");
        assertHtml("<ol>\n<li>a\n<ul>\n<li>b\n- c</li>\n</ul>\n</li>\n</ol>", "1.   a\n     - b\n    - c");
    }

    @Test
    @DisplayName("no lazy line after a blank line or a block other than a paragraph, nor one that starts a block")
    void linesThatAreNotLazy() {
        assertHtml("<ul>\n<li>a</li>\n</ul>\n<p>b</p>", "- a\n\nb");
        assertHtml("<ul>\n<li>\n<h1>h</h1>\n</li>\n</ul>\n<p>lazy</p>", "- # h\nlazy");
        assertHtml("<ul>\n<li>a</li>\n</ul>\n<h1>h</h1>", "- a\n# h");
        assertHtml("<ul>\n<li>a</li>\n</ul>\n<ol start=\"2026\">\n<li>b</li>\n</ol>", "- a\n2026. b");
        assertHtml("<ul>\n<li>a</li>\n<li></li>\n</ul>", "- a\n-");
    }

    @Test
    @DisplayName("a line of three or more -, * or _, spaces allowed, is a thematic break")
    void thematicBreaks() {
        MarkdownDocument document = MarkdownParser.parse("---");
        assertEquals(List.of(new ThematicBreak()), document.blocks());

        assertHtml("<hr />\n<hr />\n<hr />", "***\n---\n___");
        assertHtml("<hr />\n<hr />\n<hr />", " ***\n  ***\n   ***");
        assertHtml("<hr />", "_____________________________________");
        assertHtml("<hr />", " - - -");
        assertHtml("<hr />", " **  * ** * ** * **");
        assertHtml("<hr />", "-     -      -      -");
        assertHtml("<hr />", "- - - -    ");
    }

    @Test
    @DisplayName("too few characters, mixed ones, other text on the line or four spaces of indentation make no break")
    void linesThatAreNoThematicBreak() {
        assertHtml("<p>+++</p>", "+++");
        assertHtml("<p>===</p>", "===");
        assertHtml("<p>--\n**\n__</p>", "--\n**\n__");
        assertHtml("<p>Foo\n***</p>", "Foo\n    ***");
        assertHtml("<p>_ _ _ _ a</p>\n<p>a------</p>\n<p>---a---</p>", "_ _ _ _ a\n\na------\n\n---a---");
        assertHtml("<p><em>-</em></p>", " *-*");
    }

    @Test
    @DisplayName("a thematic break interrupts a paragraph, and a --- line is a break, never a setext underline")
    void thematicBreakInterruptsParagraph() {
        assertHtml("<p>Foo</p>\n<hr />\n<p>bar</p>", "Foo\n***\nbar");
        assertHtml("<p>Foo</p>\n<hr />\n<p>bar</p>", "Foo\n---\nbar");
    }

    @Test
    @DisplayName("a thematic break takes precedence over a list item and ends a list")
    void thematicBreakBeforeListItem() {
        assertHtml("<ul>\n<li>foo</li>\n</ul>\n<hr />\n<ul>\n<li>bar</li>\n</ul>", "- foo\n***\n- bar");
        assertHtml("<ul>\n<li>Foo</li>\n</ul>\n<hr />\n<ul>\n<li>Bar</li>\n</ul>", "* Foo\n* * *\n* Bar");
        assertHtml("<ul>\n<li>foo</li>\n</ul>\n<hr />", "- foo\n- - -");
        assertHtml("<ul>\n<li>Foo</li>\n<li>\n<hr />\n</li>\n</ul>", "- Foo\n- * * *");
        assertHtml("<ul>\n<li>foo</li>\n</ul>\n<hr />", "- foo\n---");
    }

    @Test
    @DisplayName("lines starting with > form a block quote, its content parsed as blocks without the markers")
    void blockQuote() {
        MarkdownDocument document = MarkdownParser.parse("> foo\n> bar");
        assertEquals(List.of(new BlockQuote(List.of(
                        new Paragraph(List.of(new PlainText("foo"), new SoftBreak(), new PlainText("bar")))))),
                document.blocks());

        assertHtml("<blockquote>\n<h1>Foo</h1>\n<p>bar\nbaz</p>\n</blockquote>", "> # Foo\n> bar\n> baz");
        assertHtml("<blockquote>\n<h1>Foo</h1>\n<p>bar\nbaz</p>\n</blockquote>", "># Foo\n>bar\n> baz");
        assertHtml("<blockquote>\n<h1>Foo</h1>\n<p>bar\nbaz</p>\n</blockquote>", "   > # Foo\n   > bar\n > baz");
        assertHtml("<blockquote>\n<p>foo</p>\n<p>bar</p>\n</blockquote>", "> foo\n>\n> bar");
        assertHtml("<p>Foo\n&gt; bar</p>", "Foo\n    > bar");
    }

    @Test
    @DisplayName("a quote may be empty, a blank line ends it, and it interrupts a paragraph")
    void blockQuoteBoundaries() {
        assertHtml("<blockquote>\n</blockquote>", ">");
        assertHtml("<blockquote>\n</blockquote>", ">\n>  \n> ");
        assertHtml("<blockquote>\n<p>foo</p>\n</blockquote>", ">\n> foo\n>  ");
        assertHtml("<blockquote>\n<p>foo</p>\n</blockquote>\n<blockquote>\n<p>bar</p>\n</blockquote>",
                "> foo\n\n> bar");
        assertHtml("<p>foo</p>\n<blockquote>\n<p>bar</p>\n</blockquote>", "foo\n> bar");
        assertHtml("<blockquote>\n<p>aaa</p>\n</blockquote>\n<hr />\n<blockquote>\n<p>bbb</p>\n</blockquote>",
                "> aaa\n***\n> bbb");
        assertHtml("<blockquote>\n<p>bar</p>\n</blockquote>\n<p>baz</p>", "> bar\n\nbaz");
        assertHtml("<blockquote>\n<p>bar</p>\n</blockquote>\n<p>baz</p>", "> bar\n>\nbaz");
    }

    @Test
    @DisplayName("a line without > right after a quote's paragraph continues that paragraph lazily")
    void lazyLinesInQuotes() {
        assertHtml("<blockquote>\n<h1>Foo</h1>\n<p>bar\nbaz</p>\n</blockquote>", "> # Foo\n> bar\nbaz");
        assertHtml("<blockquote>\n<p>bar\nbaz\nfoo</p>\n</blockquote>", "> bar\nbaz\n> foo");
        assertHtml("<blockquote>\n<p>bar\nbaz</p>\n</blockquote>", "> bar\nbaz");
        assertHtml("<blockquote>\n<p>foo\n- bar</p>\n</blockquote>", "> foo\n    - bar");
        assertHtml("<blockquote>\n<blockquote>\n<blockquote>\n<p>foo\nbar</p>\n"
                + "</blockquote>\n</blockquote>\n</blockquote>", "> > > foo\nbar");
        assertHtml("<blockquote>\n<blockquote>\n<blockquote>\n<p>foo\nbar\nbaz</p>\n"
                + "</blockquote>\n</blockquote>\n</blockquote>", ">>> foo\n> bar\n>>baz");
    }

    @Test
    @DisplayName("no lazy line in a quote after a block other than a paragraph, nor one that starts a block")
    void linesThatAreNotLazyInQuotes() {
        assertHtml("<blockquote>\n<p>foo</p>\n</blockquote>\n<hr />", "> foo\n---");
        assertHtml("<blockquote>\n<ul>\n<li>foo</li>\n</ul>\n</blockquote>\n<ul>\n<li>bar</li>\n</ul>",
                "> - foo\n- bar");
        assertHtml("<blockquote>\n<h1>h</h1>\n</blockquote>\n<p>lazy</p>", "> # h\nlazy");
        assertHtml("<blockquote>\n<p>a</p>\n</blockquote>\n<h2>h</h2>", "> a\n## h");
        assertHtml("<blockquote>\n<p>a</p>\n</blockquote>\n<ol start=\"2\">\n<li>b</li>\n</ol>", "> a\n2. b");
    }

    @Test
    @DisplayName("quotes hold lists and lists hold quotes, in any nesting")
    void quotesAndListsNest() {
        assertHtml("<blockquote>\n<p>Note:</p>\n<ul>\n<li><strong>in</strong></li>\n<li><em>out</em></li>\n</ul>\n"
                + "</blockquote>", "> Note:\n>\n> - **in**\n> - *out*");
        assertHtml("<blockquote>\n<blockquote>\n<ol>\n<li>\n<p>one</p>\n<p>two</p>\n</li>\n</ol>\n</blockquote>\n"
                + "</blockquote>", "   > > 1.  one\n>>\n>>     two");
        assertHtml("<blockquote>\n<blockquote>\n<ul>\n<li>one</li>\n</ul>\n<p>two</p>\n</blockquote>\n</blockquote>",
                ">>- one\n>>\n  >  > two");
        assertHtml("<ul>\n<li>a\n<blockquote>\n<p>b\nc</p>\n</blockquote>\n</li>\n<li>d</li>\n</ul>",
                "- a\n  > b\n  > c\n- d");
        assertHtml("<ul>\n<li>\n<blockquote>\n<p>quoted</p>\n</blockquote>\n</li>\n</ul>", "- > quoted");
    }

    @Test
    @DisplayName("lazy lines pass through quotes and list items alike")
    void lazyLinesAcrossQuotesAndLists() {
        assertHtml("<blockquote>\n<ol>\n<li>\n<blockquote>\n<p>Blockquote\ncontinued here.</p>\n</blockquote>\n</li>\n"
                + "</ol>\n</blockquote>", "> 1. > Blockquote\ncontinued here.");
        assertHtml("<blockquote>\n<ol>\n<li>\n<blockquote>\n<p>Blockquote\ncontinued here.</p>\n</blockquote>\n</li>\n"
                + "</ol>\n</blockquote>", "> 1. > Blockquote\n> continued here.");
        assertHtml("<blockquote>\n<ul>\n<li>a\nb</li>\n</ul>\n</blockquote>", "> - a\nb");
        assertHtml("<ul>\n<li>\n<blockquote>\n<p>a\nb</p>\n</blockquote>\n</li>\n</ul>", "- > a\nb");
    }

    @Test
    @DisplayName("a blank line inside a quote keeps the list around it tight, one after the quote makes it loose")
    void blankLineInQuoteKeepsListTight() {
        assertHtml("<ul>\n<li>a\n<blockquote>\n<p>b</p>\n</blockquote>\n</li>\n<li>c</li>\n</ul>",
                "* a\n  > b\n  >\n* c");
        assertHtml("<ul>\n<li>\n<blockquote>\n<p>a</p>\n</blockquote>\n<p>b</p>\n</li>\n</ul>", "- > a\n\n  b");
    }

    @Test
    @DisplayName("a fenced code block holds its lines as written and its info string, without the fences")
    void fencedCodeBlock() {
        MarkdownDocument document = MarkdownParser.parse("```java\nint x = 1;\n  **not bold**\n```");

        assertEquals(List.of(new CodeBlock("java", "int x = 1;\n  **not bold**\n")), document.blocks());
    }

    @Test
    @DisplayName("a fence is three or more backticks or tildes, closed by the same character at least as often")
    void fenceCharactersAndLengths() {
        assertHtml("<pre><code>&lt;\n &gt;\n</code></pre>", "```\n<\n >\n```");
        assertHtml("<pre><code>&lt;\n &gt;\n</code></pre>", "~~~\n<\n >\n~~~");
        assertHtml("<p><code>foo</code></p>", "``\nfoo\n``");
        assertHtml("<pre><code>aaa\n~~~\n</code></pre>", "```\naaa\n~~~\n```");
        assertHtml("<pre><code>aaa\n```\n</code></pre>", "~~~\naaa\n```\n~~~");
        assertHtml("<pre><code>aaa\n```\n</code></pre>", "````\naaa\n```\n``````");
        assertHtml("<pre><code>aaa\n~~~\n</code></pre>", "~~~~\naaa\n~~~\n~~~~");
        assertHtml("<pre><code>aaa\n~~~ ~~\n</code></pre>", "~~~~~~\naaa\n~~~ ~~");
        assertHtml("<pre><code>``` aaa\n</code></pre>", "```\n``` aaa\n```");
    }

    @Test
    @DisplayName("the info string is read without its spaces, and a backtick fence's info string has no backtick")
    void fenceInfoString() {
        assertHtml("<pre><code class=\"language-ruby\">def foo(x)\n  return 3\nend\n</code></pre>",
                "```ruby\ndef foo(x)\n  return 3\nend\n```");
        assertHtml("<pre><code class=\"language-ruby\">def foo(x)\n  return 3\nend\n</code></pre>",
                "~~~~    ruby startline=3 $%@#$\ndef foo(x)\n  return 3\nend\n~~~~~~~");
        assertHtml("<pre><code class=\"language-;\"></code></pre>", "````;\n````");
        assertHtml("<p><code>aa</code>\nfoo</p>", "``` aa ```\nfoo");
        assertHtml("<pre><code class=\"language-aa\">foo\n</code></pre>", "~~~ aa ``` ~~~\nfoo\n~~~");
        assertEquals(List.of(new CodeBlock("java title", "x\n")), MarkdownParser.parse("```  java title  \nx\n```").blocks());
    }

    @Test
    @DisplayName("an unclosed fence runs to the end of the document, and a code block may be empty or blank")
    void unclosedAndEmptyFences() {
        assertHtml("<pre><code></code></pre>", "```");
        assertHtml("<pre><code>\n```\naaa\n</code></pre>", "`````\n\n```\naaa");
        assertHtml("<pre><code>\n  \n</code></pre>", "```\n\n  \n```");
        assertHtml("<pre><code></code></pre>", "```\n```");
        assertHtml("<pre><code># not a heading\n\n- not a list\n</code></pre>", "~~~\n# not a heading\n\n- not a list\n");
        assertHtml("<pre><code>aaa\n</code></pre>", "```\r\naaa\r\n");
    }

    @Test
    @DisplayName("a fence may be indented up to three spaces, which the code lines lose as far as they have them")
    void fenceIndentation() {
        assertHtml("<pre><code>aaa\naaa\n</code></pre>", " ```\n aaa\naaa\n```");
        assertHtml("<pre><code>aaa\naaa\naaa\n</code></pre>", "  ```\naaa\n  aaa\naaa\n  ```");
        assertHtml("<pre><code>aaa\n aaa\naaa\n</code></pre>", "   ```\n   aaa\n    aaa\n  aaa\n   ```");
        assertHtml("<pre><code>aaa\n</code></pre>", "```\naaa\n  ```");
        assertHtml("<pre><code>aaa\n</code></pre>", "   ```\naaa\n  ```");
        assertHtml("<pre><code>aaa\n    ```\n</code></pre>", "```\naaa\n    ```");
        assertHtml("<p>aaa\n```\nbbb</p>", "aaa\n    ```\nbbb");
    }

    @Test
    @DisplayName("a fenced code block interrupts a paragraph and needs no blank line around it")
    void fenceInterruptsParagraph() {
        assertHtml("<p>foo</p>\n<pre><code>bar\n</code></pre>\n<p>baz</p>", "foo\n```\nbar\n```\nbaz");
        assertHtml("<h1>head</h1>\n<pre><code>x\n</code></pre>\n<ul>\n<li>item</li>\n</ul>", "# head\n~~~\nx\n~~~\n- item");
    }

    @Test
    @DisplayName("code blocks sit in list items and quotes, without the item's indentation or the quote's markers")
    void fencesInContainers() {
        assertHtml("<blockquote>\n<pre><code>aaa\n</code></pre>\n</blockquote>\n<p>bbb</p>", "> ```\n> aaa\n\nbbb");
        assertHtml("<ol>\n<li>\n<p>Example:</p>\n<pre><code class=\"language-java\">int x;\n  indented;\n</code></pre>\n"
                + "</li>\n<li>\n<p>next</p>\n</li>\n</ol>", "1. Example:\n\n   ```java\n   int x;\n     indented;\n   ```\n2. next");
        assertHtml("<blockquote>\n<ul>\n<li>\n<pre><code>x\n</code></pre>\n</li>\n</ul>\n</blockquote>",
                "> - ```\n>   x\n>   ```");
        assertHtml("<blockquote>\n<pre><code>&gt; not a quote\n- not a list\n</code></pre>\n</blockquote>",
                "> ```\n> > not a quote\n> - not a list\n> ```");
    }

    @Test
    @DisplayName("blank lines inside an item's code block keep the list tight")
    void blankLinesInCodeKeepListTight() {
        assertHtml("<ul>\n<li>a</li>\n<li>\n<pre><code>b\n\n\n</code></pre>\n</li>\n<li>c</li>\n</ul>",
                "- a\n- ```\n  b\n\n\n  ```\n- c");
    }

    @Test
    @DisplayName("the end of a list item or quote closes its code block, and a fence line is never a lazy line")
    void containersCloseFences() {
        assertHtml("<ul>\n<li>\n<pre><code>code\n</code></pre>\n</li>\n</ul>\n<p>not code</p>\n<pre><code></code></pre>",
                "- ```\n  code\nnot code\n```");
        assertHtml("<blockquote>\n<pre><code></code></pre>\n</blockquote>\n<p>foo</p>\n<pre><code></code></pre>",
                "> ```\nfoo\n```");
        assertHtml("<blockquote>\n<p>a</p>\n</blockquote>\n<pre><code>b\n</code></pre>", "> a\n```\nb\n```");
        assertHtml("<ul>\n<li>a</li>\n</ul>\n<pre><code>b\n</code></pre>", "- a\n~~~\nb\n~~~");
    }

    @Test
    @DisplayName("a line with | followed by a delimiter row starts a table of header cells, alignments and body rows")
    void tableBlock() {
        MarkdownDocument document = MarkdownParser.parse("| a | b |\n|---|:-:|\n| `x` | \\| y |\n\nafter");

        assertEquals(List.of(
                new Table(List.of(cell(new PlainText("a")), cell(new PlainText("b"))),
                        List.of(Table.Alignment.NONE, Table.Alignment.CENTER),
                        List.of(List.of(cell(new CodeSpan("x")), cell(new PlainText("| y"))))),
                new Paragraph(List.of(new PlainText("after")))), document.blocks());
    }

    @Test
    @DisplayName("GFM's table examples: cells, alignment colons, escaped pipes, short and long rows, a header alone")
    void gfmTableExamples() {
        // GFM spec 0.29, examples 198 to 205 except 202, as cmark-gfm writes them. In 202 a line without | after the
        // table is one more row in GFM; the view ends the table there on purpose, see tableBoundaries.
        assertHtml("<table>\n<thead>\n<tr>\n<th>foo</th>\n<th>bar</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n<td>baz</td>\n"
                + "<td>bim</td>\n</tr>\n</tbody>\n</table>", "| foo | bar |\n| --- | --- |\n| baz | bim |");
        assertHtml("<table>\n<thead>\n<tr>\n<th align=\"center\">abc</th>\n<th align=\"right\">defghi</th>\n</tr>\n"
                + "</thead>\n<tbody>\n<tr>\n<td align=\"center\">bar</td>\n<td align=\"right\">baz</td>\n</tr>\n"
                + "</tbody>\n</table>", "| abc | defghi |\n:-: | -----------:\nbar | baz");
        assertHtml("<table>\n<thead>\n<tr>\n<th>f|oo</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n"
                        + "<td>b <code>|</code> az</td>\n</tr>\n<tr>\n<td>b <strong>|</strong> im</td>\n</tr>\n"
                        + "</tbody>\n</table>",
                "| f\\|oo  |\n| ------ |\n| b `\\|` az |\n| b **\\|** im |");
        assertHtml("<table>\n<thead>\n<tr>\n<th>abc</th>\n<th>def</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n<td>bar</td>\n"
                        + "<td>baz</td>\n</tr>\n</tbody>\n</table>\n<blockquote>\n<p>bar</p>\n</blockquote>",
                "| abc | def |\n| --- | --- |\n| bar | baz |\n> bar");
        assertHtml("<p>| abc | def |\n| --- |\n| bar |</p>", "| abc | def |\n| --- |\n| bar |");
        assertHtml("<table>\n<thead>\n<tr>\n<th>abc</th>\n<th>def</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n<td>bar</td>\n"
                        + "<td></td>\n</tr>\n<tr>\n<td>bar</td>\n<td>baz</td>\n</tr>\n</tbody>\n</table>",
                "| abc | def |\n| --- | --- |\n| bar |\n| bar | baz | boo |");
        assertHtml("<table>\n<thead>\n<tr>\n<th>abc</th>\n<th>def</th>\n</tr>\n</thead>\n</table>",
                "| abc | def |\n| --- | --- |");
    }

    @Test
    @DisplayName("each column takes its alignment from its delimiter cell: none, left, centre or right")
    void tableAlignments() {
        Table table = (Table) MarkdownParser.parse("a | b | c | d\n--- | :-- | :-: | --:").blocks().getFirst();

        assertEquals(List.of(Table.Alignment.NONE, Table.Alignment.LEFT, Table.Alignment.CENTER,
                Table.Alignment.RIGHT), table.alignments());
        assertEquals(List.of(), table.rows());
    }

    @Test
    @DisplayName("a cell is inline Markdown without the spaces around it, an empty cell has no inlines")
    void tableCellsAreInlineMarkdown() {
        Table table = (Table) MarkdownParser.parse("| **Mode** | *it* [l](u) | |\n|---|---|---|\n|  `a | b` |  x  |")
                .blocks().getFirst();

        assertEquals(List.of(cell(new Strong(List.of(new PlainText("Mode")))),
                cell(new Emphasis(List.of(new PlainText("it"))), new PlainText(" "),
                        new Link("u", "", List.of(new PlainText("l")))),
                cell()), table.header());
        // As in GFM, a | inside a code span splits the cell unless it is escaped.
        assertEquals(List.of(List.of(cell(new PlainText("`a")), cell(new PlainText("b`")), cell(new PlainText("x")))),
                table.rows());
    }

    @Test
    @DisplayName("a table ends at a blank line, a line without | or a line that starts a block, and interrupts a paragraph")
    void tableBoundaries() {
        assertHtml("<table>\n<thead>\n<tr>\n<th>a</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n<td>1</td>\n</tr>\n</tbody>\n"
                + "</table>\n<p>no pipe</p>", "| a |\n| - |\n| 1 |\nno pipe");
        assertHtml("<p>Intro:</p>\n<table>\n<thead>\n<tr>\n<th>a</th>\n<th>b</th>\n</tr>\n</thead>\n</table>",
                "Intro:\n| a | b |\n| - | - |");
        assertHtml("<table>\n<thead>\n<tr>\n<th>a</th>\n</tr>\n</thead>\n</table>\n<h1>h | x</h1>",
                "| a |\n| - |\n# h | x");
        assertHtml("<table>\n<thead>\n<tr>\n<th>a</th>\n</tr>\n</thead>\n</table>\n<blockquote>\n<p>q | x</p>\n"
                + "</blockquote>", "| a |\n| - |\n> q | x");
        assertHtml("<table>\n<thead>\n<tr>\n<th>a</th>\n</tr>\n</thead>\n</table>\n<ul>\n<li>i | x</li>\n</ul>",
                "| a |\n| - |\n- i | x");
        assertHtml("<table>\n<thead>\n<tr>\n<th align=\"left\">a</th>\n</tr>\n</thead>\n</table>", "  | a |\n   |:--|");
    }

    @Test
    @DisplayName("a | without a delimiter row of as many cells stays paragraph text, an escaped | splits no cell")
    void linesThatAreNoTable() {
        assertHtml("<p>a | b\nmore text</p>", "a | b\nmore text");
        assertHtml("<p>| a | b |\n|---|</p>", "| a | b |\n|---|");
        assertHtml("<p>| a |\n|-x-|</p>", "| a |\n|-x-|");
        assertHtml("<p>| a |\n--</p>", "| a |\n--");
        assertHtml("<p>| a |</p>\n<hr />", "| a |\n---");
        assertHtml("<p>| a |</p>\n<ul>\n<li>| -</li>\n</ul>", "| a |\n- | -");
        assertHtml("<p>| a | b |\n|----|----|</p>", "| a | b |\n    |----|----|");
        assertHtml("<p>| a | b |\n|---|---|</p>", "| a \\| b |\n|---|---|");
        assertHtml("<table>\n<thead>\n<tr>\n<th>a | b</th>\n</tr>\n</thead>\n</table>", "| a \\| b |\n|---|");
        assertHtml("<table>\n<thead>\n<tr>\n<th>`a</th>\n<th>b`</th>\n</tr>\n</thead>\n</table>",
                "| `a | b` |\n|---|---|");
    }

    @Test
    @DisplayName("a table sits in a list item or a quote, its lines without the item's indentation or the quote's markers")
    void tablesInContainers() {
        assertHtml("<ul>\n<li>\n<p><strong>Pattern</strong> formats:</p>\n<table>\n<thead>\n<tr>\n<th>P</th>\n"
                        + "<th>On</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n<td><code>dd</code></td>\n"
                        + "<td><code>31</code></td>\n</tr>\n</tbody>\n</table>\n</li>\n<li>\n<p>next</p>\n</li>\n</ul>",
                "- **Pattern** formats:\n\n  | P | On |\n  |---|---|\n  | `dd` | `31` |\n\n- next");
        assertHtml("<ul>\n<li>\n<table>\n<thead>\n<tr>\n<th>a</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n<td>1</td>\n</tr>\n"
                + "</tbody>\n</table>\n</li>\n<li>b</li>\n</ul>", "- | a |\n  |---|\n  | 1 |\n- b");
        assertHtml("<blockquote>\n<table>\n<thead>\n<tr>\n<th>a</th>\n</tr>\n</thead>\n<tbody>\n<tr>\n<td>1</td>\n"
                + "</tr>\n</tbody>\n</table>\n</blockquote>\n<p>after</p>", "> | a |\n> |---|\n> | 1 |\nafter");
    }

    @Test
    @DisplayName("a container's lazy lines are paragraph text, never a table")
    void lazyLinesAreNoTable() {
        assertHtml("<blockquote>\n<p>| a |\n|---|</p>\n</blockquote>", "> | a |\n|---|");
        assertHtml("<ul>\n<li>a\n| b |\n|---|</li>\n</ul>", "- a\n| b |\n|---|");
        assertHtml("<blockquote>\n<p>a</p>\n<table>\n<thead>\n<tr>\n<th>b</th>\n</tr>\n</thead>\n</table>\n"
                + "</blockquote>\n<p>| c |</p>", "> a\n> | b |\n> |---|\n| c |");
    }

    @Test
    @DisplayName("a line starting with a tag, a closing tag or <! starts an HTML block, kept as its lines up to a blank line")
    void htmlBlock() {
        MarkdownDocument document = MarkdownParser.parse("<div>\n*hello*\n</div>\n\nafter");
        assertEquals(List.of(new SourceBlock(SourceBlock.Kind.HTML, List.of("<div>", "*hello*", "</div>")),
                new Paragraph(List.of(new PlainText("after")))), document.blocks());

        assertHtml(" <div>\n  *hello*\n         <foo><a>", " <div>\n  *hello*\n         <foo><a>");
        assertHtml("</div>\n*foo*", "</div>\n*foo*");
        assertHtml("<DIV CLASS=\"foo\">\n<p><em>Markdown</em></p>\n</DIV>", "<DIV CLASS=\"foo\">\n\n*Markdown*\n\n</DIV>");
        assertHtml("<div id=\"foo\"\n  class=\"bar\">\n</div>", "<div id=\"foo\"\n  class=\"bar\">\n</div>");
        assertHtml("<div>\n*foo*\n<p><em>bar</em></p>", "<div>\n*foo*\n\n*bar*");
        assertHtml("<div *???-&&&-<---\n*foo*", "<div *???-&&&-<---\n*foo*");
        assertHtml("<div></div>\n``` c\nint x = 33;\n```", "<div></div>\n``` c\nint x = 33;\n```");
        assertHtml("<Warning>\n*bar*\n</Warning>", "<Warning>\n*bar*\n</Warning>");
        assertHtml("<br/>\n<p>text</p>", "<br/>\n\ntext");
        assertHtml("<!-- a comment -->\n<!DOCTYPE html>", "<!-- a comment -->\n<!DOCTYPE html>");
        assertHtml("<table><tr><td>\n<pre>\n**Hello**,\n<p><em>world</em>.</p>\n</pre>\n</td></tr></table>",
                "<table><tr><td>\n<pre>\n**Hello**,\n\n_world_.\n\n</pre>\n</td></tr></table>");
    }

    @Test
    @DisplayName("an HTML block interrupts no paragraph, and autolinks and other < lines start none")
    void linesThatStartNoHtmlBlock() {
        assertHtml("<p>Foo\n&lt;div&gt;\nbar</p>", "Foo\n<div>\nbar");
        assertHtml("<blockquote>\n<p>a\n&lt;div&gt;</p>\n</blockquote>", "> a\n<div>");
        assertHtml("<p>&lt;3 hearts</p>\n<p>&lt; div&gt;</p>\n<p>&lt;/ div&gt;</p>", "<3 hearts\n\n< div>\n\n</ div>");
        assertEquals(Paragraph.class, MarkdownParser.parse("<https://example.com> is the site").blocks().getFirst()
                .getClass());
        assertEquals(Paragraph.class, MarkdownParser.parse("<foo@example.com> writes").blocks().getFirst().getClass());
    }

    @Test
    @DisplayName("an HTML block sits in a list item or a quote, and ends with it")
    void htmlBlocksInContainers() {
        assertHtml("<blockquote>\n<div>\nfoo\n</blockquote>\n<p>bar</p>", "> <div>\n> foo\n\nbar");
        assertHtml("<ul>\n<li>\n<div>\n</li>\n<li>foo</li>\n</ul>", "- <div>\n- foo");
        assertHtml("<blockquote>\n<div>\n</blockquote>\n<p>lazy?</p>", "> <div>\nlazy?");
    }

    @Test
    @DisplayName("lines indented four or more spaces where no paragraph takes them are indented code, kept as written")
    void indentedCode() {
        MarkdownDocument document = MarkdownParser.parse("    a simple\n      indented code block\n\n\nafter");
        assertEquals(List.of(new SourceBlock(SourceBlock.Kind.INDENTED_CODE, List.of("    a simple",
                        "      indented code block")), new Paragraph(List.of(new PlainText("after")))),
                document.blocks());

        assertHtml("<pre><code>&lt;a/&gt;\n*hi*\n\n- one\n</code></pre>", "    <a/>\n    *hi*\n\n    - one");
        assertHtml("<pre><code>chunk1\n\nchunk2\n\n\n\nchunk3\n</code></pre>",
                "    chunk1\n\n    chunk2\n  \n \n \n    chunk3");
        assertHtml("<pre><code>chunk1\n  \n  chunk2\n</code></pre>", "    chunk1\n      \n      chunk2");
        assertHtml("<pre><code>foo\n</code></pre>\n<p>bar</p>", "    foo\nbar");
        assertHtml("<pre><code>    foo\nbar\n</code></pre>", "        foo\n    bar");
        assertHtml("<pre><code>foo\n</code></pre>", "\n    \n    foo\n    ");
        assertHtml("<pre><code>foo  \n</code></pre>", "    foo  ");
        assertHtml("<h1>Heading</h1>\n<pre><code>foo\n</code></pre>\n<h2>Heading</h2>\n<pre><code>foo\n</code></pre>\n<hr />",
                "# Heading\n    foo\n## Heading\n    foo\n----");
        assertHtml("<pre><code>| a |\n|---|\n</code></pre>", "    | a |\n    |---|");
        assertHtml("  <!-- foo -->\n<pre><code>&lt;!-- foo --&gt;\n</code></pre>", "  <!-- foo -->\n\n    <!-- foo -->");
    }

    @Test
    @DisplayName("indented code interrupts no paragraph and is never a lazy line's end")
    void indentedCodeAndParagraphs() {
        assertHtml("<p>Foo\nbar</p>", "Foo\n    bar");
        assertHtml("<blockquote>\n<p>a\nb</p>\n</blockquote>", "> a\n    b");
        assertHtml("<ul>\n<li>\n<p>foo</p>\n<pre><code>bar\n</code></pre>\n</li>\n</ul>\n<p>not lazy</p>",
                "- foo\n\n      bar\nnot lazy");
    }

    @Test
    @DisplayName("indented code sits in list items and quotes, indented four spaces beyond their content")
    void indentedCodeInContainers() {
        assertHtml("<ol>\n<li>\n<p>A paragraph\nwith two lines.</p>\n<pre><code>indented code\n</code></pre>\n"
                        + "<blockquote>\n<p>A block quote.</p>\n</blockquote>\n</li>\n</ol>",
                "1.  A paragraph\n    with two lines.\n\n        indented code\n\n    > A block quote.");
        assertHtml("<ol>\n<li>\n<pre><code>indented code\n</code></pre>\n<p>paragraph</p>\n<pre><code>more code\n"
                + "</code></pre>\n</li>\n</ol>", "1.     indented code\n\n   paragraph\n\n       more code");
        assertHtml("<blockquote>\n<pre><code>code\n</code></pre>\n</blockquote>\n<blockquote>\n<p>not code</p>\n"
                + "</blockquote>", ">     code\n\n>    not code");
        assertHtml("<ul>\n<li>\n<pre><code>b\n\nc\n</code></pre>\n</li>\n<li>d</li>\n</ul>", "-     b\n\n      c\n- d");
    }

    @Test
    @DisplayName("a long lazy paragraph deep in nested lists parses quickly")
    void longLazyParagraphInNestedLists() {
        String markdown = "- a\n  - b\n    - c\n      - d\n" + "lazy line\n".repeat(3000);

        MarkdownDocument document = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> MarkdownParser.parse(markdown));

        assertEquals(1, document.blocks().size());
    }

    @Test
    @DisplayName("lists nest 100 levels deep, deeper markers stay text instead of overflowing the stack")
    void absurdlyDeepNesting() {
        String markdown = "- ".repeat(20_000) + "deep";

        MarkdownDocument document = assertDoesNotThrow(() -> MarkdownParser.parse(markdown));

        List<Block> blocks = document.blocks();
        int lists = 0;
        while (blocks.size() == 1 && blocks.getFirst() instanceof BulletList list) {
            lists++;
            blocks = list.items().getFirst().blocks();
        }
        assertEquals(100, lists);
        Paragraph innermost = (Paragraph) blocks.getFirst();
        assertEquals(new PlainText("- ".repeat(19_900) + "deep"), innermost.content().getFirst());
    }

    @Test
    @DisplayName("quotes nest 100 levels deep, deeper markers stay text instead of overflowing the stack")
    void absurdlyDeepQuotes() {
        MarkdownDocument document = assertDoesNotThrow(() -> MarkdownParser.parse(">".repeat(20_000) + " deep"));

        List<Block> blocks = document.blocks();
        int quotes = 0;
        while (blocks.size() == 1 && blocks.getFirst() instanceof BlockQuote quote) {
            quotes++;
            blocks = quote.blocks();
        }
        assertEquals(100, quotes);
        assertEquals(new Paragraph(List.of(new PlainText(">".repeat(19_900) + " deep"))), blocks.getFirst());
    }

    @Test
    @DisplayName("the nesting limit counts quotes and lists together")
    void nestingLimitCoversQuotesAndLists() {
        MarkdownDocument document = assertDoesNotThrow(() -> MarkdownParser.parse("> - ".repeat(10_000) + "deep"));

        List<Block> blocks = document.blocks();
        int containers = 0;
        while (blocks.size() == 1) {
            if (blocks.getFirst() instanceof BlockQuote quote) {
                blocks = quote.blocks();
            } else if (blocks.getFirst() instanceof BulletList list) {
                blocks = list.items().getFirst().blocks();
            } else {
                break;
            }
            containers++;
        }
        assertEquals(100, containers);
        assertEquals(new Paragraph(List.of(new PlainText("> - ".repeat(9_950) + "deep"))), blocks.getFirst());
    }

    @Test
    @DisplayName("emphasis nests 100 levels deep, deeper delimiters stay text instead of overflowing the stack")
    void absurdlyDeepEmphasis() {
        String markdown = "*a ".repeat(4000) + "z" + "* x".repeat(4000) + "*";

        MarkdownDocument document = assertDoesNotThrow(() -> MarkdownParser.parse(markdown));

        List<Inline> content = ((Paragraph) document.blocks().getFirst()).content();
        assertEquals(3, content.size());
        assertEquals(new PlainText("*a ".repeat(3900)), content.get(0));
        assertEquals(new PlainText(" x*".repeat(3901)), content.get(2));
        int levels = 0;
        Inline inline = content.get(1);
        while (inline instanceof Emphasis emphasis) {
            levels++;
            inline = emphasis.content().size() == 3 ? emphasis.content().get(1) : null;
        }
        assertEquals(100, levels);
    }

    @Test
    @DisplayName("no inline nests deeper than 100 levels, strong emphasis and links counted")
    void inlineNestingLimit() {
        String strong = "**a ".repeat(4000) + "z" + "** x".repeat(4000) + "**";
        String inLink = "*a [" + "*a ".repeat(4000) + "z" + "* x".repeat(4000) + "*](/u) x*";

        for (String markdown : List.of(strong, inLink)) {
            MarkdownDocument document = assertDoesNotThrow(() -> MarkdownParser.parse(markdown));

            assertEquals(100, depth(((Paragraph) document.blocks().getFirst()).content()));
        }
    }

    @Test
    @DisplayName("long lazy paragraphs deep in nested quotes parse quickly")
    void longLazyParagraphsInNestedQuotes() {
        // Quote lines and lazy lines in turn: each lazy line makes the outer quote probe its content, and inner
        // levels must not probe again for a line they are handed as lazy, or the cost grows with every level.
        String oneLazyParagraph = "> > > > > a\n" + "lazy line\n".repeat(3000);
        String alternating = "> > > > > a\nlazy line\n".repeat(300);

        MarkdownDocument lazy = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> MarkdownParser.parse(oneLazyParagraph));
        MarkdownDocument mixed = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> MarkdownParser.parse(alternating));

        assertEquals(1, lazy.blocks().size());
        assertEquals(1, mixed.blocks().size());
    }

    @Test
    @DisplayName("lazy lines deep in list items inside a long quote parse quickly")
    void lazyLinesInItemsInsideQuotes() {
        String markdown = "> - - - a\n" + ">       b\nlazy line\n".repeat(300);

        MarkdownDocument document = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> MarkdownParser.parse(markdown));

        assertEquals(1, document.blocks().size());
    }

    @Test
    @DisplayName("an inline link holds its address as written, an optional title and its text, emphasis and code included")
    void inlineLink() {
        MarkdownDocument document = MarkdownParser.parse("See [the *docs*](https://example.com/a_b \"Title\").");

        assertEquals(List.of(new Paragraph(List.of(
                        new PlainText("See "),
                        new Link("https://example.com/a_b", "Title",
                                List.of(new PlainText("the "), new Emphasis(List.of(new PlainText("docs"))))),
                        new PlainText(".")))),
                document.blocks());
        assertHtml("<p><a href=\"/uri\" title=\"title\">link</a></p>", "[link](/uri \"title\")");
        assertHtml("<p><a href=\"/uri\">link</a></p>", "[link](/uri)");
        assertHtml("<p><a href=\"/uri\">link <code>code</code> <strong>bold</strong></a></p>",
                "[link `code` **bold**](/uri)");
    }

    @Test
    @DisplayName("a link destination may be empty, in angle brackets, with balanced or escaped parentheses, never spaced")
    void linkDestinations() {
        assertHtml("<p><a href=\"./target.md\"></a></p>", "[](./target.md)");
        assertHtml("<p><a href=\"\">link</a></p>", "[link]()");
        assertHtml("<p><a href=\"\">link</a></p>", "[link](<>)");
        assertHtml("<p><a href=\"\"></a></p>", "[]()");
        assertHtml("<p>[link](/my uri)</p>", "[link](/my uri)");
        assertHtml("<p><a href=\"/my%20uri\">link</a></p>", "[link](</my uri>)");
        assertHtml("<p>[link](foo\nbar)</p>", "[link](foo\nbar)");
        assertHtml("<p><a href=\"b)c\">a</a></p>", "[a](<b)c>)");
        assertHtml("<p>[link](&lt;foo&gt;)</p>", "[link](<foo\\>)");
        assertHtml("<p><a href=\"(foo)\">link</a></p>", "[link](\\(foo\\))");
        assertHtml("<p><a href=\"foo(and(bar))\">link</a></p>", "[link](foo(and(bar)))");
        assertHtml("<p>[link](foo(and(bar))</p>", "[link](foo(and(bar))");
        assertHtml("<p><a href=\"foo(and(bar)\">link</a></p>", "[link](foo\\(and\\(bar\\))");
        assertHtml("<p><a href=\"foo(and(bar)\">link</a></p>", "[link](<foo(and(bar)>)");
        assertHtml("<p><a href=\"foo):\">link</a></p>", "[link](foo\\)\\:)");
        assertHtml("<p><a href=\"#fragment\">link</a></p>\n<p><a href=\"https://example.com#fragment\">link</a></p>",
                "[link](#fragment)\n\n[link](https://example.com#fragment)");
        assertHtml("<p><a href=\"foo%5Cbar\">link</a></p>", "[link](foo\\bar)");
        assertHtml("<p><a href=\"%22title%22\">link</a></p>", "[link](\"title\")");
        assertHtml("<p><a href=\"/url%C2%A0%22title%22\">link</a></p>", "[link](/url\u00A0\"title\")");
        String nested32 = "(".repeat(32) + ")".repeat(32);
        String nested33 = "(".repeat(33) + ")".repeat(33);
        assertHtml("<p><a href=\"" + nested32 + "\">a</a></p>", "[a](" + nested32 + ")");
        assertHtml("<p>[a](" + nested33 + ")</p>", "[a](" + nested33 + ")");
    }

    @Test
    @DisplayName("a link title is in double or single quotes or parentheses, after whitespace, line endings allowed around it")
    void linkTitles() {
        assertHtml("<p><a href=\"/url\" title=\"title\">link</a>\n<a href=\"/url\" title=\"title\">link</a>\n"
                + "<a href=\"/url\" title=\"title\">link</a></p>",
                "[link](/url \"title\")\n[link](/url 'title')\n[link](/url (title))");
        assertHtml("<p>[link](/url &quot;title &quot;and&quot; title&quot;)</p>", "[link](/url \"title \"and\" title\")");
        assertHtml("<p><a href=\"/url\" title=\"title &quot;and&quot; title\">link</a></p>",
                "[link](/url 'title \"and\" title')");
        assertHtml("<p><a href=\"/uri\" title=\"title\">link</a></p>", "[link](   /uri\n  \"title\"  )");
        assertHtml("<p><a href=\"/url\" title=\"say &quot;hi&quot;\">link</a></p>", "[link](/url \"say \\\"hi\\\"\")");
        assertHtml("<p>[link](/url (a (b)))</p>", "[link](/url (a (b)))");
        assertHtml("<p>[link](/url &quot;unclosed)</p>", "[link](/url \"unclosed)");
    }

    @Test
    @DisplayName("link text holds balanced or escaped brackets, never a link: the innermost link wins")
    void linkTextBrackets() {
        assertHtml("<p>[link] (/uri)</p>", "[link] (/uri)");
        assertHtml("<p><a href=\"/uri\">link [foo [bar]]</a></p>", "[link [foo [bar]]](/uri)");
        assertHtml("<p>[link] bar](/uri)</p>", "[link] bar](/uri)");
        assertHtml("<p>[link <a href=\"/uri\">bar</a></p>", "[link [bar](/uri)");
        assertHtml("<p><a href=\"/uri\">link [bar</a></p>", "[link \\[bar](/uri)");
        assertHtml("<p><a href=\"/uri\">link <em>foo <strong>bar</strong> <code>#</code></em></a></p>",
                "[link *foo **bar** `#`*](/uri)");
        assertHtml("<p>[foo <a href=\"/uri\">bar</a>](/uri)</p>", "[foo [bar](/uri)](/uri)");
        assertHtml("<p>[foo <em>[bar <a href=\"/uri\">baz</a>](/uri)</em>](/uri)</p>",
                "[foo *[bar [baz](/uri)](/uri)*](/uri)");
    }

    @Test
    @DisplayName("emphasis does not reach into or out of link text, code spans take precedence over brackets")
    void linkPrecedence() {
        assertHtml("<p>*<a href=\"/uri\">foo*</a></p>", "*[foo*](/uri)");
        assertHtml("<p><a href=\"baz*\">foo *bar</a></p>", "[foo *bar](baz*)");
        assertHtml("<p><em>foo [bar</em> baz]</p>", "*foo [bar* baz]");
        assertHtml("<p>[foo<code>](/uri)</code></p>", "[foo`](/uri)`");
        assertHtml("<p><strong>bold <a href=\"/a\">link</a> text</strong></p>", "**bold [link](/a) text**");
        assertHtml("<h2><a href=\"/a\">Heading link</a></h2>", "## [Heading link](/a)");
    }

    @Test
    @DisplayName("an image becomes an image inline with its destination, its title and its alt text")
    void imageInline() {
        MarkdownDocument document = MarkdownParser.parse("See ![the *moon*](moon\\_1.jpg \"Full \\\"moon\\\"\").");

        assertEquals(List.of(new Paragraph(List.of(
                        new PlainText("See "),
                        new Image("moon_1.jpg", "Full \"moon\"", "the moon"),
                        new PlainText(".")))),
                document.blocks());
    }

    @Test
    @DisplayName("images as CommonMark reads them: the alt text is the description's plain text")
    void images() {
        assertHtml("<p><img src=\"/url\" alt=\"foo\" title=\"title\" /></p>", "![foo](/url \"title\")");
        assertHtml("<p><img src=\"train.jpg\" alt=\"foo\" /></p>", "![foo](train.jpg)");
        assertHtml("<p>My <img src=\"/path/to/train.jpg\" alt=\"foo bar\" title=\"title\" /></p>",
                "My ![foo bar](/path/to/train.jpg  \"title\"   )");
        assertHtml("<p><img src=\"url\" alt=\"foo\" /></p>", "![foo](<url>)");
        assertHtml("<p><img src=\"/url\" alt=\"\" /></p>", "![](/url)");
        assertHtml("<p><img src=\"/url\" alt=\"foo bar\" title=\"title\" /></p>", "![foo *bar*](/url \"title\")");
        assertHtml("<p><img src=\"/url2\" alt=\"foo bar\" /></p>", "![foo ![bar](/url)](/url2)");
        assertHtml("<p><img src=\"/url2\" alt=\"foo bar\" /></p>", "![foo [bar](/url)](/url2)");
        assertHtml("<p><img src=\"uri3\" alt=\"[foo](uri2)\" /></p>", "![[[foo](uri1)](uri2)](uri3)");
        assertHtml("<p><img src=\"d\" alt=\"a b c\" /></p>", "![a `b` **c**](d)");
    }

    @Test
    @DisplayName("an image's alt text is one line: its line breaks become spaces")
    void imageAltTextIsOneLine() {
        assertHtml("<p><img src=\"c\" alt=\"a b\" /></p>", "![a\nb](c)");
        assertHtml("<p><img src=\"c\" alt=\"a b\" /></p>", "![a\\\nb](c)");
    }

    @Test
    @DisplayName("a link's text may hold an image, so a linked image is a link around an image")
    void linkedImage() {
        assertHtml("<p><a href=\"/uri\"><img src=\"moon.jpg\" alt=\"moon\" /></a></p>", "[![moon](moon.jpg)](/uri)");
        assertHtml("<p><a href=\"https://example.com\">logo <img src=\"logo.png\" alt=\"Logo\" /></a></p>",
                "[logo ![Logo](logo.png)](https://example.com)");
    }

    @Test
    @DisplayName("reference-style images and images without a target stay literal text")
    void referenceImagesStayLiteral() {
        assertHtml("<p>![foo] and ![not closed</p>", "![foo] and ![not closed");
        assertHtml("<p>![foo][bar] and ![foo][]</p>", "![foo][bar] and ![foo][]");
        assertHtml("<p>!<a href=\"/a\">escaped</a> and a bang! <a href=\"/b\">link</a></p>",
                "\\![escaped](/a) and a bang! [link](/b)");
    }

    @Test
    @DisplayName("an autolink is an absolute address in angle brackets, its text the address as written")
    void autolinks() {
        MarkdownDocument document = MarkdownParser.parse("Go to <https://example.com/a\\_b>.");
        assertEquals(List.of(new Paragraph(List.of(
                        new PlainText("Go to "),
                        new Link("https://example.com/a\\_b", "", List.of(new PlainText("https://example.com/a\\_b"))),
                        new PlainText(".")))),
                document.blocks());

        assertHtml("<p><a href=\"http://foo.bar.baz\">http://foo.bar.baz</a></p>", "<http://foo.bar.baz>");
        assertHtml("<p><a href=\"https://foo.bar.baz/test?q=hello&amp;id=22&amp;boolean\">"
                + "https://foo.bar.baz/test?q=hello&amp;id=22&amp;boolean</a></p>",
                "<https://foo.bar.baz/test?q=hello&id=22&boolean>");
        assertHtml("<p><a href=\"irc://foo.bar:2233/baz\">irc://foo.bar:2233/baz</a></p>", "<irc://foo.bar:2233/baz>");
        assertHtml("<p><a href=\"MAILTO:FOO@BAR.BAZ\">MAILTO:FOO@BAR.BAZ</a></p>", "<MAILTO:FOO@BAR.BAZ>");
        assertHtml("<p><a href=\"a+b+c:d\">a+b+c:d</a></p>", "<a+b+c:d>");
        assertHtml("<p><a href=\"made-up-scheme://foo,bar\">made-up-scheme://foo,bar</a></p>",
                "<made-up-scheme://foo,bar>");
        assertHtml("<p><a href=\"localhost:5001/foo\">localhost:5001/foo</a></p>", "<localhost:5001/foo>");
        assertHtml("<p><a href=\"https://example.com/%5C%5B%5C\">https://example.com/\\[\\</a></p>",
                "<https://example.com/\\[\\>");
        assertHtml("<p><a href=\"https://foo.bar.%60baz\">https://foo.bar.`baz</a>`</p>", "<https://foo.bar.`baz>`");
        assertHtml("<p><code>&lt;https://foo.bar.</code>baz&gt;`</p>", "`<https://foo.bar.`baz>`");
        assertHtml("<p>[foo<a href=\"https://example.com/?search=%5D(uri)\">https://example.com/?search=](uri)</a></p>",
                "[foo<https://example.com/?search=](uri)>");
    }

    @Test
    @DisplayName("angle brackets around no absolute address, an e-mail address included, stay text")
    void textThatIsNoAutolink() {
        assertHtml("<p>&lt;&gt;</p>", "<>");
        assertHtml("<p>&lt;m:abc&gt;</p>", "<m:abc>");
        assertHtml("<p>&lt;foo.bar.baz&gt;</p>", "<foo.bar.baz>");
        assertHtml("<p>&lt;foo@bar.example.com&gt;</p>", "<foo@bar.example.com>");
        assertHtml("<p>&lt;https://foo.bar/baz bim&gt;</p>", "<https://foo.bar/baz bim>");
        assertHtml("<p>&lt;https://a\nb&gt;</p>", "<https://a\nb>");
    }

    @Test
    @DisplayName("an autolink inside link text is text of the link: a link never holds a link")
    void autolinkInsideLinkText() {
        assertHtml("<p><a href=\"/c\">see https://a.b <em>now</em></a></p>", "[see <https://a.b> *now*](/c)");
        assertEquals(List.of(new Paragraph(List.of(
                        new Link("/c", "", List.of(new PlainText("see https://a.b too")))))),
                MarkdownParser.parse("[see <https://a.b> too](/c)").blocks());
    }

    @Test
    @DisplayName("a bare http or https address in the text is a link, its text the address")
    void bareUrls() {
        MarkdownDocument document = MarkdownParser.parse("Source: https://archive.ics.uci.edu/dataset/2/adult");
        assertEquals(List.of(new Paragraph(List.of(
                        new PlainText("Source: "),
                        new Link("https://archive.ics.uci.edu/dataset/2/adult", "",
                                List.of(new PlainText("https://archive.ics.uci.edu/dataset/2/adult")))))),
                document.blocks());

        assertHtml("<p>a <a href=\"http://a.b/c\">http://a.b/c</a> and <a href=\"https://d.e\">https://d.e</a> f</p>",
                "a http://a.b/c and https://d.e f");
        assertHtml("<p><a href=\"https://a.b/c?q=1&amp;r=a.b,c:d;e!f\">https://a.b/c?q=1&amp;r=a.b,c:d;e!f</a></p>",
                "https://a.b/c?q=1&r=a.b,c:d;e!f");
        assertHtml("<h2>See <a href=\"https://a.b\">https://a.b</a></h2>", "## See https://a.b");
        assertHtml("<p><a href=\"https://localhost:8080/x\">https://localhost:8080/x</a></p>",
                "https://localhost:8080/x");
    }

    @Test
    @DisplayName("a bare URL ends before whitespace or <, its trailing . , : ; ! ? and unmatched ) left out")
    void bareUrlEnds() {
        assertHtml("<p>See <a href=\"https://a.b/c\">https://a.b/c</a>.</p>", "See https://a.b/c.");
        assertHtml("<p><a href=\"https://a.b/c\">https://a.b/c</a>.,:;!?</p>", "https://a.b/c.,:;!?");
        assertHtml("<p><a href=\"https://a.b/c\">https://a.b/c</a>?! Yes.</p>", "https://a.b/c?! Yes.");
        assertHtml("<p>(see <a href=\"https://a.b/c\">https://a.b/c</a>)</p>", "(see https://a.b/c)");
        assertHtml("<p>(<a href=\"https://a.b/c\">https://a.b/c</a>).</p>", "(https://a.b/c).");
        assertHtml("<p><a href=\"https://en.wikipedia.org/wiki/Foo_(bar)\">https://en.wikipedia.org/wiki/Foo_(bar)</a>"
                + "</p>", "https://en.wikipedia.org/wiki/Foo_(bar)");
        assertHtml("<p><a href=\"https://g.com/search?q=Markup+(business)\">https://g.com/search?q=Markup+(business)</a>"
                + "))</p>", "https://g.com/search?q=Markup+(business)))");
        assertHtml("<p><a href=\"https://a.b/(c\">https://a.b/(c</a></p>", "https://a.b/(c");
        assertHtml("<p><a href=\"https://a.b/c\">https://a.b/c</a>\nnext</p>", "https://a.b/c\nnext");
        assertHtml("<p><a href=\"https://a.b/c\">https://a.b/c</a><br />\nnext</p>", "https://a.b/c  \nnext");
        assertHtml("<p><a href=\"https://a.b/c\">https://a.b/c</a>&lt;br&gt;</p>", "https://a.b/c<br>");
    }

    @Test
    @DisplayName("a bare URL starts the text, follows whitespace, * _ ~ or (, and has more than its scheme")
    void bareUrlStarts() {
        assertHtml("<p><em><a href=\"https://a.b\">https://a.b</a></em> and <strong><a href=\"https://c.d\">"
                + "https://c.d</a></strong></p>", "*https://a.b* and __https://c.d__");
        assertHtml("<ul>\n<li><strong>Source:</strong> <a href=\"https://a.b/2/adult\">https://a.b/2/adult</a></li>\n"
                + "</ul>", "- **Source:** https://a.b/2/adult");
        assertHtml("<p>~<a href=\"https://a.b\">https://a.b</a></p>", "~https://a.b");
        assertHtml("<p>xhttps://a.b and &quot;https://a.b&quot; and :https://a.b</p>",
                "xhttps://a.b and \"https://a.b\" and :https://a.b");
        assertHtml("<p>https:// and http://. and https://)</p>", "https:// and http://. and https://)");
        assertHtml("<p>&lt; <a href=\"https://foo.bar\">https://foo.bar</a> &gt;</p>", "< https://foo.bar >");
    }

    @Test
    @DisplayName("a bare URL is no link in code, in link text or after a construct, but is one in literal text")
    void bareUrlsAndOtherInlines() {
        assertHtml("<p><code>https://a.b</code></p>", "`https://a.b`");
        assertHtml("<p><a href=\"https://c.d\">https://a.b</a></p>", "[https://a.b](https://c.d)");
        assertHtml("<p><a href=\"https://a.b\">https://a.b</a></p>", "<https://a.b>");
        assertHtml("<p>[https://a.b] <code>x</code>https://a.b</p>", "[https://a.b] `x`https://a.b");
        assertHtml("<p><img src=\"https://a.b/i.png\" alt=\"x\" /></p>", "![x](https://a.b/i.png)");
        assertHtml("<p>[foo]: <a href=\"https://a.b\">https://a.b</a> &quot;t&quot;</p>", "[foo]: https://a.b \"t\"");
    }

    @Test
    @DisplayName("a bare URL is taken from the source as written, before emphasis and escapes")
    void bareUrlAsWritten() {
        assertEquals(List.of(new Paragraph(List.of(
                        new PlainText("See "),
                        new Link("https://x.y/__init__.py", "", List.of(new PlainText("https://x.y/__init__.py"))),
                        new PlainText(".")))),
                MarkdownParser.parse("See https://x.y/__init__.py.").blocks());
        assertEquals(List.of(new Paragraph(List.of(
                        new Link("https://a.b/c\\_d", "", List.of(new PlainText("https://a.b/c\\_d")))))),
                MarkdownParser.parse("https://a.b/c\\_d").blocks());
        assertHtml("<p><a href=\"https://a.b/*c*/d\">https://a.b/*c*/d</a> and <em>e</em></p>",
                "https://a.b/*c*/d and *e*");
        // Trailing * and _ are left out, so the emphasis around an address is no part of it.
        assertHtml("<p><strong><a href=\"https://a.b/__init__.py\">https://a.b/__init__.py</a></strong>.</p>",
                "**https://a.b/__init__.py**.");
    }

    @Test
    @DisplayName("no bare URL starts while a bracket is open, as in GFM, so it never runs across a link's ](")
    void noBareUrlInBrackets() {
        assertHtml("<p><a href=\"/u\">see https://x.y/[1]</a></p>", "[see https://x.y/[1]](/u)");
        assertHtml("<p>[see https://a.b] and <a href=\"https://c.d\">https://c.d</a></p>",
                "[see https://a.b] and https://c.d");
    }

    @Test
    @DisplayName("long runs of brackets, parentheses and links parse quickly")
    void pathologicalLinksParseQuickly() {
        int count = 30000;
        String manyOpenersThenLinks = "[".repeat(count) + "[a](b)".repeat(count);
        String unclosedTargets = "[](".repeat(count);
        String parenthesesAfterUrl = "https://a.b/c" + ")".repeat(count * 4);

        List<MarkdownDocument> documents = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> List.of(
                MarkdownParser.parse(manyOpenersThenLinks),
                MarkdownParser.parse(unclosedTargets),
                MarkdownParser.parse(parenthesesAfterUrl)));

        Paragraph links = (Paragraph) documents.get(0).blocks().getFirst();
        assertEquals(new PlainText("[".repeat(count)), links.content().getFirst());
        assertEquals(count + 1, links.content().size());
        assertEquals(new Link("https://a.b/c", "", List.of(new PlainText("https://a.b/c"))),
                ((Paragraph) documents.get(2).blocks().getFirst()).content().getFirst());
    }

    @Test
    @DisplayName("reference-style links, link definitions, footnotes and inline HTML stay literal text")
    void otherLinkFormsStayLiteral() {
        assertHtml("<p>[foo][bar] and [foo][] and [foo]</p>", "[foo][bar] and [foo][] and [foo]");
        assertHtml("<p>[foo]: /url &quot;title&quot;</p>\n<p>[foo]</p>", "[foo]: /url \"title\"\n\n[foo]");
        assertHtml("<p>a footnote[^1]</p>", "a footnote[^1]");
        assertHtml("<p>a &lt;span&gt;<em>b</em>&lt;/span&gt; and &lt;a href=&quot;/x&quot;&gt;c&lt;/a&gt;</p>",
                "a <span>*b*</span> and <a href=\"/x\">c</a>");
    }

    @Test
    @DisplayName("malformed Markdown stays text")
    void malformedInputStaysText() {
        assertHtml("<h1>**unclosed <code>code</code> and `more</h1>", "# **unclosed `code` and `more");
        assertHtml("<p>**`</p>", "**`");
        assertHtml("<p>*foo _bar</p>", "*foo _bar");
        assertHtml("<p>``</p>", "``");
    }

    @Test
    @DisplayName("the parser never throws, whatever the text")
    void parserNeverThrows() {
        String alphabet = "*_`~\\# \n\r\tab1.-+>|[]()<!:";
        Random random = new Random(20261110L);
        for (int sample = 0; sample < 5000; sample++) {
            StringBuilder markdown = new StringBuilder();
            int length = random.nextInt(48);
            for (int i = 0; i < length; i++) {
                markdown.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String input = markdown.toString();
            assertDoesNotThrow(() -> MarkdownParser.parse(input), () -> "Markdown: " + input);
        }
    }

    @Test
    @DisplayName("the parser never throws on random mixes of link syntax")
    void parserNeverThrowsOnLinkSyntax() {
        String[] tokens = {"[", "]", "![", "](", "(", ")", "<", ">", "https://", "a.b", "a:b", " ", "\n", "*", "_",
            "`", "\\", "\"", "'", "- ", "> "};
        Random random = new Random(20261111L);
        for (int sample = 0; sample < 5000; sample++) {
            StringBuilder markdown = new StringBuilder();
            int length = random.nextInt(24);
            for (int i = 0; i < length; i++) {
                markdown.append(tokens[random.nextInt(tokens.length)]);
            }
            String input = markdown.toString();
            assertDoesNotThrow(() -> MarkdownParser.parse(input), () -> "Markdown: " + input);
        }
    }

    private static void assertHtml(String expectedHtml, String markdown) {
        assertEquals(expectedHtml, HtmlFixture.html(markdown), () -> "Markdown: " + markdown);
    }

    private static Table.Cell cell(Inline... content) {
        return new Table.Cell(List.of(content));
    }

    /**
     * How deep inlines nest: each emphasis, strong emphasis and link is one level.
     */
    private static int depth(List<Inline> inlines) {
        int deepest = 0;
        for (Inline inline : inlines) {
            int depth = switch (inline) {
                case Emphasis emphasis -> 1 + depth(emphasis.content());
                case Strong strong -> 1 + depth(strong.content());
                case Link link -> 1 + depth(link.content());
                default -> 0;
            };
            deepest = Math.max(deepest, depth);
        }
        return deepest;
    }
}
