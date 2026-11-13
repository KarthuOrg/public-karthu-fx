package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;

/**
 * The languages an application gives a view, and the limit of lines above which a block stays in one colour, seen
 * from outside the view: the map and the limit in, the runs of the block out, read by their text and their
 * {@code code-} classes.
 */
class MarkdownViewCodeLanguagesTest {

    /**
     * A {@code rules} block between two paragraphs, and a Java block after them.
     */
    private static final String AROUND_A_RULES_BLOCK = "Before.\n\n```rules\nif x\n```\n\nAfter.\n\n```java\nint y\n```";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a language the application puts into the map colours its blocks")
    void putAddsALanguage() {
        MarkdownView view = new MarkdownView();

        view.getCodeLanguages().put("rules", firstTwoAsKeyword());
        view.setMarkdown("```rules\nif x\n```");

        assertEquals(List.of("code-keyword:if", "plain: x"), runs(layOut(view)));
    }

    @Test
    @DisplayName("put replaces a built-in language, here Java, with the application's lexer")
    void putReplacesALanguage() {
        MarkdownView view = new MarkdownView();

        view.getCodeLanguages().put("java", firstTwoAsKeyword());
        view.setMarkdown("```java\nif x\n```");

        assertEquals(List.of("code-keyword:if", "plain: x"), runs(layOut(view)));
    }

    @Test
    @DisplayName("remove switches a language off: a java block is one run")
    void removeSwitchesALanguageOff() {
        MarkdownView view = new MarkdownView();

        view.getCodeLanguages().remove("java");
        view.setMarkdown("```java\nint x = 1;\n```");

        assertEquals(List.of("plain:int x = 1;"), runs(layOut(view)));
    }

    @Test
    @DisplayName("put and remove rebuild the shown document, as a new text does")
    void changeRebuildsTheShownDocument() {
        MarkdownView view = layOut(new MarkdownView("```rules\nif x\n```\n\n```java\nint x\n```"));
        assertEquals(List.of("plain:if x"), runs(view), "an unknown language");

        view.getCodeLanguages().put("rules", firstTwoAsKeyword());
        relayOut(view);
        assertEquals(List.of("code-keyword:if", "plain: x"), runs(view), "after put");

        view.getCodeLanguages().remove("rules");
        relayOut(view);
        assertEquals(List.of("plain:if x"), runs(view), "after remove");

        view.getCodeLanguages().put("java", firstTwoAsKeyword());
        relayOut(view);
        assertEquals(List.of("code-keyword:in", "plain:t x"), runs(view, 1), "after replacing Java");
    }

    @Test
    @DisplayName("every view starts with Java, and two views have maps of their own")
    void viewsHaveIndependentMaps() {
        MarkdownView first = new MarkdownView();
        MarkdownView second = new MarkdownView();
        assertTrue(first.getCodeLanguages().containsKey("java"), "Java in the first view's map");
        assertTrue(second.getCodeLanguages().containsKey("java"), "Java in the second view's map");

        first.getCodeLanguages().remove("java");
        first.getCodeLanguages().put("rules", firstTwoAsKeyword());
        String markdown = "```java\nint x\n```\n\n```rules\nif x\n```";
        first.setMarkdown(markdown);
        second.setMarkdown(markdown);
        layOut(first);
        layOut(second);

        assertEquals(List.of("plain:int x"), runs(first, 0), "Java removed from the first view only");
        assertEquals(List.of("code-keyword:if", "plain: x"), runs(first, 1), "rules put into the first view only");
        assertEquals(List.of("code-keyword:int", "plain: x"), runs(second, 0), "the second view still colours Java");
        assertEquals(List.of("plain:if x"), runs(second, 1), "the second view knows no rules");
        assertNotSame(first.getCodeLanguages(), second.getCodeLanguages());
    }

    /**
     * The info strings of the lookup: the first word, ending at the first space or brace, in lower case, names the
     * language.
     */
    static Stream<Arguments> lookups() {
        return Stream.of(
                Arguments.of("rules", true),
                Arguments.of("Rules", true),
                Arguments.of("RULES", true),
                Arguments.of("rules title", true),
                Arguments.of("rules{1}", true),
                Arguments.of("rules {1}", true),
                Arguments.of("", false),
                Arguments.of("text", false),
                Arguments.of("rulesets", false),
                Arguments.of("rule", false));
    }

    @ParameterizedTest(name = "```{0} coloured: {1}")
    @MethodSource("lookups")
    @DisplayName("the language of a block is the first word of its info string, in lower case")
    void lookup(String info, boolean colored) {
        MarkdownView view = new MarkdownView();
        view.getCodeLanguages().put("rules", firstTwoAsKeyword());

        view.setMarkdown("```" + info + "\nif x\n```");

        assertEquals(colored ? List.of("code-keyword:if", "plain: x") : List.of("plain:if x"), runs(layOut(view)));
    }

    @Test
    @DisplayName("the lookup is in lower case: Java in any case is coloured, a key with capitals is never found")
    void lookupInLowerCase() {
        MarkdownView view = new MarkdownView();
        view.getCodeLanguages().put("Rules", firstTwoAsKeyword());

        view.setMarkdown("```Java\nint x\n```\n\n```java title\nint x\n```\n\n```java{1}\nint x\n```\n\n"
                + "```Rules\nif x\n```");
        layOut(view);

        for (int index = 0; index < 3; index++) {
            assertEquals(List.of("code-keyword:int", "plain: x"), runs(view, index), "block " + index);
        }
        assertEquals(List.of("plain:if x"), runs(view, 3), "a key with capitals");
    }

    @Test
    @DisplayName("a lexer that throws leaves its block as one run, and the rest of the document renders")
    void throwingLexer() {
        MarkdownView view = new MarkdownView();
        RecordingLogger log = new RecordingLogger();
        view.setLogger(log);
        IllegalStateException bug = new IllegalStateException("a bug in the lexer");
        view.getCodeLanguages().put("rules", code -> {
            throw bug;
        });

        view.setMarkdown(AROUND_A_RULES_BLOCK);
        layOut(view);

        assertEquals(List.of("plain:if x"), runs(view, 0), "the faulty lexer's block");
        assertEquals(List.of("code-keyword:int", "plain: y"), runs(view, 1), "the Java block after it");
        assertEquals(List.of("Before.", "After."), paragraphTexts(view));
        assertEquals(1, log.records.size(), () -> "one record: " + log.records);
        assertEquals(Level.WARN, log.records.get(0).level());
        assertTrue(log.records.get(0).message().contains("'rules'"), () -> "names the language: " + log.records);
        assertSame(bug, log.records.get(0).thrown(), "with the lexer's exception");
    }

    /**
     * Lexers that break the contract on the code {@code if x}: what each returns, and the lexer.
     */
    static Stream<Arguments> badPieces() {
        return Stream.of(
                Arguments.of("an empty piece", returning(piece(0, 2), piece(3, 3))),
                Arguments.of("a piece that ends before it starts", returning(piece(3, 1))),
                Arguments.of("a piece that starts before the text", returning(piece(-1, 2))),
                Arguments.of("a piece that ends after the text", returning(piece(3, 5))),
                Arguments.of("a piece wholly after the text", returning(piece(5, 6))),
                Arguments.of("pieces out of order", returning(piece(3, 4), piece(0, 2))),
                Arguments.of("overlapping pieces", returning(piece(0, 3), piece(2, 4))),
                Arguments.of("a null piece", returning(piece(0, 2), null)),
                Arguments.of("no list but null", (CodeLexer) code -> null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("badPieces")
    @DisplayName("a lexer that returns bad pieces leaves its block as one run, and the rest of the document renders")
    void badPiecesLeaveOneRun(String what, CodeLexer lexer) {
        MarkdownView view = new MarkdownView();
        RecordingLogger log = new RecordingLogger();
        view.setLogger(log);
        view.getCodeLanguages().put("rules", lexer);

        view.setMarkdown(AROUND_A_RULES_BLOCK);
        layOut(view);

        assertEquals(List.of("plain:if x"), runs(view, 0), "the faulty lexer's block");
        assertEquals(List.of("code-keyword:int", "plain: y"), runs(view, 1), "the Java block after it");
        assertEquals(List.of("Before.", "After."), paragraphTexts(view));
        assertEquals(1, log.records.size(), () -> "one record: " + log.records);
        assertEquals(Level.WARN, log.records.get(0).level());
        assertTrue(log.records.get(0).message().contains("'rules'"), () -> "names the language: " + log.records);
    }

    @Test
    @DisplayName("pieces that touch, and pieces up to the text's ends, are no fault")
    void touchingPiecesAreFine() {
        MarkdownView view = new MarkdownView();
        RecordingLogger log = new RecordingLogger();
        view.setLogger(log);
        view.getCodeLanguages().put("rules", returning(piece(0, 2), piece(2, 3), piece(3, 4)));

        view.setMarkdown("```rules\nif x\n```");

        assertEquals(List.of("code-keyword:if", "code-keyword: ", "code-keyword:x"), runs(layOut(view)));
        assertEquals(List.of(), log.records, "no warning");
    }

    @Test
    @DisplayName("the line limit is 500 by default, an integer property of the view")
    void lineLimitDefault() {
        MarkdownView view = new MarkdownView();

        // 500 lines show in about 0.1 s, 5000 in about 15 s.
        assertEquals(500, view.getCodeHighlightLineLimit());
        assertEquals(500, view.codeHighlightLineLimitProperty().get());
        assertSame(view, view.codeHighlightLineLimitProperty().getBean());
        assertEquals("codeHighlightLineLimit", view.codeHighlightLineLimitProperty().getName());
    }

    @Test
    @DisplayName("a block of exactly the limit's lines is coloured, one line more is one run and its lexer is not called")
    void blockAtAndOverTheLimit() {
        MarkdownView view = new MarkdownView();
        CountingLexer lexer = new CountingLexer();
        view.getCodeLanguages().put("rules", lexer);
        view.setCodeHighlightLineLimit(3);

        view.setMarkdown("```rules\nif\nb\nc\n```");
        assertEquals(List.of("code-keyword:if", "plain:\nb\nc"), runs(layOut(view)), "three lines");
        assertEquals(1, lexer.calls, "the lexer called for three lines");

        view.setMarkdown("```rules\nif\nb\nc\nd\n```");
        assertEquals(List.of("plain:if\nb\nc\nd"), runs(layOut(view)), "four lines");
        assertEquals(1, lexer.calls, "the lexer not called for four lines");
    }

    @Test
    @DisplayName("lines are line feeds plus one, counted on the code as the block shows it")
    void linesAsTheBlockShowsThem() {
        MarkdownView view = new MarkdownView();
        view.getCodeLanguages().put("rules", firstTwoAsKeyword());
        view.setCodeHighlightLineLimit(2);

        // The fence's code is "if\n\n": the block shows "if\n", two lines, the last one empty.
        view.setMarkdown("```rules\nif\n\n```");
        assertEquals(List.of("code-keyword:if", "plain:\n"), runs(layOut(view)), "an empty last line counts");

        view.setMarkdown("```rules\nif\n\n\n```");
        assertEquals(List.of("plain:if\n\n"), runs(layOut(view)), "three lines, two of them empty");

        view.setCodeHighlightLineLimit(1);
        view.setMarkdown("```rules\nif x\n```");
        assertEquals(List.of("code-keyword:if", "plain: x"), runs(layOut(view)), "one line under a limit of one");
    }

    @ParameterizedTest(name = "limit {0}")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("a limit of 0 or below colours no block, and no lexer is called")
    void noLimitColorsNothing(int limit) {
        MarkdownView view = new MarkdownView();
        CountingLexer lexer = new CountingLexer();
        view.getCodeLanguages().put("rules", lexer);
        view.setCodeHighlightLineLimit(limit);

        view.setMarkdown("```rules\nif x\n```\n\n```rules\n```\n\n```java\nint x\n```");
        layOut(view);

        assertEquals(List.of("plain:if x"), runs(view, 0), "a block of one line");
        assertEquals(List.of("plain:"), runs(view, 1), "an empty block");
        assertEquals(List.of("plain:int x"), runs(view, 2), "a java block");
        assertEquals(0, lexer.calls, "no call");
    }

    @Test
    @DisplayName("a new limit rebuilds the shown document")
    void newLimitRebuilds() {
        MarkdownView view = layOut(new MarkdownView("```java\nint a\nint b\n```"));
        assertEquals(List.of("code-keyword:int", "plain: a\n", "code-keyword:int", "plain: b"), runs(view),
                "under the default limit");

        view.setCodeHighlightLineLimit(1);
        relayOut(view);
        assertEquals(List.of("plain:int a\nint b"), runs(view), "over a limit of one");

        view.setCodeHighlightLineLimit(2);
        relayOut(view);
        assertEquals(List.of("code-keyword:int", "plain: a\n", "code-keyword:int", "plain: b"), runs(view),
                "at a limit of two");
    }

    /**
     * A lexer that returns the given pieces, whatever the code, nulls included.
     */
    private static CodeLexer returning(CodePiece... pieces) {
        return code -> Arrays.asList(pieces);
    }

    /**
     * A keyword piece of a range, which the record takes unchecked.
     */
    private static CodePiece piece(int start, int end) {
        return new CodePiece(start, end, CodeCategory.KEYWORD);
    }

    /**
     * A lexer that marks the code's first two characters as a keyword.
     */
    private static CodeLexer firstTwoAsKeyword() {
        return code -> List.of(new CodePiece(0, 2, CodeCategory.KEYWORD));
    }

    /**
     * The texts of the view's paragraphs, in document order.
     */
    private static List<String> paragraphTexts(MarkdownView view) {
        List<String> texts = new ArrayList<>();
        for (TextFlow paragraph : paragraphs(view)) {
            StringBuilder text = new StringBuilder();
            for (Node child : paragraph.getChildren()) {
                if (child instanceof Text run) {
                    text.append(run.getText());
                }
            }
            texts.add(text.toString());
        }
        return texts;
    }

    /**
     * The runs of the view's first code block, each as its {@code code-} classes (or {@code plain}), a colon and its
     * text.
     */
    private static List<String> runs(MarkdownView view) {
        return runs(view, 0);
    }

    /**
     * The runs of one of the view's code blocks, as {@link #runs(MarkdownView)} gives them.
     *
     * @param index the block's index among the code blocks, in document order
     */
    private static List<String> runs(MarkdownView view, int index) {
        Node block = new ArrayList<>(view.lookupAll(".markdown-code-block")).get(index);
        TextFlow flow = (TextFlow) block.lookup(".markdown-block-content");
        List<String> runs = new ArrayList<>();
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                List<String> classes = new ArrayList<>();
                for (String styleClass : run.getStyleClass()) {
                    if (styleClass.startsWith("code-")) {
                        classes.add(styleClass);
                    }
                }
                runs.add((classes.isEmpty() ? "plain" : String.join(" ", classes)) + ":" + run.getText());
            }
        }
        return runs;
    }

    /**
     * A lexer that marks the code's first two characters as a keyword and counts its calls.
     */
    private static final class CountingLexer implements CodeLexer {

        /**
         * How often the view called the lexer.
         */
        private int calls;

        @Override
        public List<CodePiece> lex(String code) {
            calls++;
            return List.of(piece(0, 2));
        }
    }
}
