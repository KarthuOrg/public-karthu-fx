package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.assertCodeRuns;
import static org.karthu.fx.markdown.MarkdownTestSupport.codeRuns;
import static org.karthu.fx.markdown.MarkdownTestSupport.codeRunsByText;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.runAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.shortcut;
import static org.karthu.fx.markdown.MarkdownTestSupport.textFlows;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;

/**
 * Code blocks coloured by their language, seen from outside the view: Markdown with a fenced block in, the block's
 * runs out, read by their text, their style classes (through {@link MarkdownTestSupport#assertCodeRuns}) and their
 * fill after CSS.
 */
class MarkdownViewCodeColorsTest {

    /**
     * A Java block with a piece of every category Java marks, each run's text unique where a test looks it up.
     */
    private static final String COLORED = "Intro.\n\n```java\n/** Doc. */\n@Override\nvoid run() {\n    int x = 1;\n"
            + "    long y = MAX + 2;\n    Row r = null;\n    s = \"a\\tb\"; // done\n}\n```\n\nAfter.";

    /**
     * An application's rule that makes the runs of a style class bold, and underlined, which shows whatever faces the
     * code's font family has.
     */
    private static final String BOLD = ".%s { -fx-font-weight: bold; -fx-underline: true; }";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a java block is a run per piece and per plain stretch, each piece with its category's classes")
    void javaBlockIsColored() {
        assertJavaRuns("int x = 1;",
                "keyword:int", "plain: x ", "operator:=", "plain: ", "number:1", "punctuation:;");
    }

    /**
     * The cases of the Java lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> javaCases() {
        return Stream.of(
                // Comments.
                Arguments.of("a line comment up to the line's end", "x = 1; // one\ny",
                        List.of("plain:x ", "operator:=", "plain: ", "number:1", "punctuation:;", "plain: ",
                                "comment:// one", "plain:\ny")),
                Arguments.of("a block comment over lines", "a /* one\n two */ b",
                        List.of("plain:a ", "comment:/* one\n two */", "plain: b")),
                Arguments.of("a doc comment", "/** Doc. */\nint a;",
                        List.of("doc:/** Doc. */", "plain:\n", "keyword:int", "plain: a", "punctuation:;")),
                Arguments.of("an empty block comment is no doc comment", "/**/ int",
                        List.of("comment:/**/", "plain: ", "keyword:int")),
                Arguments.of("a keyword in a comment stays in it", "// if (x) return null;",
                        List.of("comment:// if (x) return null;")),
                Arguments.of("an unterminated comment runs to the end of the block", "a /* open\nint b;",
                        List.of("plain:a ", "comment:/* open\nint b;")),
                Arguments.of("an unterminated doc comment too", "/** open\nint b;",
                        List.of("doc:/** open\nint b;")),
                // Strings, text blocks and characters.
                Arguments.of("a string", "s = \"text\";",
                        List.of("plain:s ", "operator:=", "plain: ", "string:\"text\"", "punctuation:;")),
                Arguments.of("an escape in a string", "\"a\\tb\\\"c\"",
                        List.of("string:\"a", "escape:\\t", "string:b", "escape:\\\"", "string:c\"")),
                Arguments.of("escapes at a string's ends, a unicode and an octal escape", "\"\\u00e9\\0\\\\\"",
                        List.of("string:\"", "escape:\\u00e9\\0\\\\", "string:\"")),
                Arguments.of("a keyword in a string stays in it", "\"if null return\"",
                        List.of("string:\"if null return\"")),
                Arguments.of("an empty string", "\"\" + \"\"",
                        List.of("string:\"\"", "plain: ", "operator:+", "plain: ", "string:\"\"")),
                Arguments.of("an unterminated string runs to the end of the block", "s = \"open;\nint b;",
                        List.of("plain:s ", "operator:=", "plain: ", "string:\"open;\nint b;")),
                Arguments.of("a text block with an escape and quotes inside", "\"\"\"\n  a \"q\" \\n\n  \"\"\";",
                        List.of("string:\"\"\"\n  a \"q\" ", "escape:\\n", "string:\n  \"\"\"", "punctuation:;")),
                Arguments.of("an unterminated text block runs to the end of the block", "\"\"\"\n  a\nint b;",
                        List.of("string:\"\"\"\n  a\nint b;")),
                Arguments.of("characters with escapes", "c = 'x' + '\\'' + '\\n';",
                        List.of("plain:c ", "operator:=", "plain: ", "string:'x'", "plain: ", "operator:+",
                                "plain: ", "string:'", "escape:\\'", "string:'", "plain: ", "operator:+",
                                "plain: ", "string:'", "escape:\\n", "string:'", "punctuation:;")),
                // Numbers, whole words only.
                Arguments.of("decimal numbers with underscores, fractions, exponents and suffixes",
                        "1_000 2L 3.5 .5 1e10 6.02E+23d 1.5f 7D",
                        List.of("number:1_000", "plain: ", "number:2L", "plain: ", "number:3.5", "plain: ",
                                "number:.5", "plain: ", "number:1e10", "plain: ", "number:6.02E+23d", "plain: ",
                                "number:1.5f", "plain: ", "number:7D")),
                Arguments.of("hex, binary and octal numbers", "0xFF_FFL 0x1.8p3 0b1010_1010 017",
                        List.of("number:0xFF_FFL", "plain: ", "number:0x1.8p3", "plain: ", "number:0b1010_1010",
                                "plain: ", "number:017")),
                Arguments.of("digits inside a name are no number", "x1 = a2b;",
                        List.of("plain:x1 ", "operator:=", "plain: a2b", "punctuation:;")),
                Arguments.of("a number that runs into letters is no number", "12ab + 0x",
                        List.of("plain:12ab ", "operator:+", "plain: 0x")),
                // Keywords, literals and annotations.
                Arguments.of("keywords", "public static final void",
                        List.of("keyword:public", "plain: ", "keyword:static", "plain: ", "keyword:final", "plain: ",
                                "keyword:void")),
                Arguments.of("contextual keywords", "var r; record R sealed permits yield when non-sealed _",
                        List.of("keyword:var", "plain: r", "punctuation:;", "plain: ", "keyword:record", "plain: ",
                                "type:R", "plain: ", "keyword:sealed", "plain: ", "keyword:permits", "plain: ",
                                "keyword:yield", "plain: ", "keyword:when", "plain: ", "keyword:non-sealed",
                                "plain: ", "keyword:_")),
                Arguments.of("literals", "true false null",
                        List.of("literal:true", "plain: ", "literal:false", "plain: ", "literal:null")),
                Arguments.of("annotations, with arguments and qualified", "@Override @SuppressWarnings(\"x\") @java.lang.Deprecated",
                        List.of("annotation:@Override", "plain: ", "annotation:@SuppressWarnings", "punctuation:(",
                                "string:\"x\"", "punctuation:)", "plain: ", "annotation:@java.lang.Deprecated")),
                Arguments.of("an annotation type's declaration", "public @interface Marker",
                        List.of("keyword:public", "plain: ", "keyword:@interface", "plain: ", "type:Marker")),
                // Names.
                Arguments.of("names in capitals of two characters or more are constants, a capital T a type",
                        "MAX_ROWS OPEN ID UTF_8 T Row String",
                        List.of("constant:MAX_ROWS", "plain: ", "constant:OPEN", "plain: ", "constant:ID", "plain: ",
                                "constant:UTF_8", "plain: ", "type:T", "plain: ", "type:Row", "plain: ",
                                "type:String")),
                Arguments.of("a name before ( is a function, also after spaces, unless it is a keyword",
                        "getKitchenValue(\"x\"); print (a); if (b) this(c);",
                        List.of("function:getKitchenValue", "punctuation:(", "string:\"x\"", "punctuation:);",
                                "plain: ", "function:print", "plain: ", "punctuation:(", "plain:a",
                                "punctuation:);", "plain: ", "keyword:if", "plain: ", "punctuation:(", "plain:b",
                                "punctuation:)", "plain: ", "keyword:this", "punctuation:(", "plain:c",
                                "punctuation:);")),
                Arguments.of("a type before ( is a function too, a constructor's call", "new Row(1)",
                        List.of("keyword:new", "plain: ", "function:Row", "punctuation:(", "number:1",
                                "punctuation:)")),
                Arguments.of("generic types and a member reference",
                        "List<Map.Entry<String, Long>> l = list.stream().map(Row::name);",
                        List.of("type:List", "operator:<", "type:Map", "punctuation:.", "type:Entry", "operator:<",
                                "type:String", "punctuation:,", "plain: ", "type:Long", "operator:>>", "plain: l ",
                                "operator:=", "plain: list", "punctuation:.", "function:stream", "punctuation:().",
                                "function:map", "punctuation:(", "type:Row", "operator:::", "plain:name",
                                "punctuation:);")),
                // Operators and punctuation.
                Arguments.of("operators", "a += b >>> 2 != c && !d ? e -> f : g",
                        List.of("plain:a ", "operator:+=", "plain: b ", "operator:>>>", "plain: ", "number:2",
                                "plain: ", "operator:!=", "plain: c ", "operator:&&", "plain: ", "operator:!",
                                "plain:d ", "operator:?", "plain: e ", "operator:->", "plain: f ", "operator::",
                                "plain: g")),
                Arguments.of("punctuation and varargs", "f(String... a) { x[0]; }",
                        List.of("function:f", "punctuation:(", "type:String", "punctuation:...", "plain: a",
                                "punctuation:)", "plain: ", "punctuation:{", "plain: x", "punctuation:[",
                                "number:0", "punctuation:];", "plain: ", "punctuation:}")),
                // Code that does not compile, and no code.
                Arguments.of("code that does not compile is coloured too", "int = class 3x \"",
                        List.of("keyword:int", "plain: ", "operator:=", "plain: ", "keyword:class", "plain: 3x ",
                                "string:\"")),
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("javaCases")
    @DisplayName("the Java lexer")
    void javaRuns(String what, String code, List<String> expected) {
        assertJavaRuns(code, expected.toArray(new String[0]));
    }

    @Test
    @DisplayName("by default a base piece takes its base colour, a fine one the colour it falls back to, plain code the"
            + " code colour")
    void defaultColors() {
        MarkdownView view = layOut(new MarkdownView(COLORED));
        Map<String, Text> runs = codeRunsByText(view.lookup(".markdown-code-block"));

        assertEquals(Color.web("#c88bff"), runs.get("int").getFill(), "keyword");
        assertEquals(Color.web("#c88bff"), runs.get("@Override").getFill(), "annotation, as keyword");
        assertEquals(Color.web("#ff9e64"), runs.get("1").getFill(), "number");
        assertEquals(Color.web("#ff9e64"), runs.get("MAX").getFill(), "constant, as literal, as number");
        assertEquals(Color.web("#ff9e64"), runs.get("null").getFill(), "literal, as number");
        assertEquals(Color.web("#8a90a8"), runs.get("// done").getFill(), "comment");
        assertEquals(Color.web("#8a90a8"), runs.get("/** Doc. */").getFill(), "doc, as comment");
        assertEquals(Color.web("#e0a84c"), runs.get("\"a").getFill(), "string");
        assertEquals(Color.web("#e0a84c"), runs.get("\\t").getFill(), "escape, as string");
        assertEquals(Color.web("#4cd38a"), runs.get("Row").getFill(), "type");
        assertEquals(Color.web("#5d9bff"), runs.get("run").getFill(), "function");
        assertEquals(themeColor(view, "-fx-code-text-color"), runs.get("=").getFill(), "operator, as code text");
        assertEquals(themeColor(view, "-fx-code-text-color"), runs.get(";").getFill(), "punctuation, as code text");
        assertEquals(themeColor(view, "-fx-code-text-color"), runs.get(" x ").getFill(), "plain code");
    }

    @Test
    @DisplayName("the other base colours are the theme's variables with their default colours")
    void otherBaseColors() {
        MarkdownView view = layOut(new MarkdownView("Text."));

        assertEquals(Color.web("#ff8fa3"), themeColor(view, "-fx-code-variable-color"));
        assertEquals(Color.web("#7b8cff"), themeColor(view, "-fx-code-property-color"));
        assertEquals(Color.web("#ff8fa3"), themeColor(view, "-fx-code-tag-color"));
        assertEquals(Color.web("#ff9e64"), themeColor(view, "-fx-code-attribute-color"), "as number");
        assertEquals(Color.web("#5d9bff"), themeColor(view, "-fx-code-heading-color"), "as function");
        assertEquals(Color.web("#7b8cff"), themeColor(view, "-fx-code-link-color"), "as property");
        assertEquals(Color.web("#e0a84c"), themeColor(view, "-fx-code-code-color"), "as string");
        assertEquals(Color.web("#c88bff"), themeColor(view, "-fx-code-list-color"), "as keyword");
        assertEquals(Color.web("#8a90a8"), themeColor(view, "-fx-code-quote-color"), "as comment");
        assertEquals(themeColor(view, "-fx-code-text-color"), themeColor(view, "-fx-code-emphasis-color"),
                "as code text");
    }

    @Test
    @DisplayName("setStyle on the view changes a base colour at once, and a fine piece that falls back to it follows")
    void baseColorOnTheView() {
        MarkdownView view = layOut(new MarkdownView(COLORED));
        Map<String, Text> runs = codeRunsByText(view.lookup(".markdown-code-block"));

        view.setStyle("-fx-code-keyword-color: #ff0000; -fx-code-number-color: #00ff00;");
        relayOut(view);

        assertEquals(Color.web("#ff0000"), runs.get("int").getFill(), "keyword");
        assertEquals(Color.web("#ff0000"), runs.get("@Override").getFill(), "annotation follows keyword");
        assertEquals(Color.web("#00ff00"), runs.get("MAX").getFill(), "constant follows number through literal");
        assertEquals(Color.web("#e0a84c"), runs.get("\"a").getFill(), "string unchanged");
    }

    @Test
    @DisplayName("setting a fine variable changes only the pieces of that category")
    void fineColorAlone() {
        MarkdownView view = layOut(new MarkdownView(COLORED));
        Map<String, Text> runs = codeRunsByText(view.lookup(".markdown-code-block"));

        view.setStyle("-fx-code-annotation-color: #00ff00; -fx-code-literal-color: #0000ff;"
                + " -fx-code-operator-color: #ffffff;");
        relayOut(view);

        assertEquals(Color.web("#00ff00"), runs.get("@Override").getFill(), "annotation");
        assertEquals(Color.web("#c88bff"), runs.get("int").getFill(), "keyword unchanged");
        assertEquals(Color.web("#0000ff"), runs.get("null").getFill(), "literal");
        assertEquals(Color.web("#0000ff"), runs.get("MAX").getFill(), "constant follows literal");
        assertEquals(Color.web("#ff9e64"), runs.get("1").getFill(), "number unchanged");
        assertEquals(Color.web("#ffffff"), runs.get("=").getFill(), "operator");
        assertEquals(themeColor(view, "-fx-code-text-color"), runs.get(";").getFill(), "punctuation unchanged");
    }

    @Test
    @DisplayName("an application stylesheet rule on code-keyword makes keywords and annotations bold, one on"
            + " code-annotation only annotations")
    void boldByStyleClass() {
        MarkdownView keywords = layOut(new MarkdownView(COLORED));
        Map<String, Text> runs = codeRunsByText(keywords.lookup(".markdown-code-block"));
        keywords.getScene().getStylesheets().add(stylesheet(BOLD.formatted("code-keyword")));
        relayOut(keywords);

        assertBold(runs.get("int"), true, "keyword");
        assertBold(runs.get("@Override"), true, "annotation, through code-keyword");
        assertBold(runs.get("Row"), false, "type");
        assertBold(runs.get(" x "), false, "plain code");
        assertTrue(isMonospaced(runs.get("int")), "still monospaced");
        assertEquals(Color.web("#c88bff"), runs.get("int").getFill(), "and still in its colour");

        MarkdownView annotations = layOut(new MarkdownView(COLORED));
        runs = codeRunsByText(annotations.lookup(".markdown-code-block"));
        annotations.getScene().getStylesheets().add(stylesheet(BOLD.formatted("code-annotation")));
        relayOut(annotations);

        assertBold(runs.get("@Override"), true, "annotation");
        assertBold(runs.get("int"), false, "keyword");
    }

    @Test
    @DisplayName("a java block inside a quote keeps its colours")
    void coloredBlockInQuote() {
        MarkdownView view = layOut(new MarkdownView("> Quoted:\n>\n> ```java\n> @Override int x = 1;\n> ```"));
        Region block = (Region) view.lookup(".markdown-code-block");
        Map<String, Text> runs = codeRunsByText(block);

        assertTrue(block.getParent().getStyleClass().contains("markdown-quote"), "in the quote");
        assertEquals(Color.web("#c88bff"), runs.get("int").getFill(), "keyword");
        assertEquals(Color.web("#c88bff"), runs.get("@Override").getFill(), "annotation");
        assertEquals(Color.web("#ff9e64"), runs.get("1").getFill(), "number");
        assertEquals(themeColor(view, "-fx-code-text-color"), runs.get(" x ").getFill(), "plain code");
        assertEquals(themeColor(view, "-fx-code-text-color"), runs.get("=").getFill(), "operator");
    }

    @Test
    @DisplayName("a drag over the pieces of a coloured block selects the characters between press and pointer: the"
            + " layer over the runs shows them in the selection's colours, the shortcut key plus C copies them as written")
    void dragSelectsInColoredBlock() {
        MarkdownView view = layOut(new MarkdownView("```java\nint total = price * count;\nreturn total;\n```"));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        TextFlow flow = flow(block);
        assertTrue(codeRuns(block).size() > 1, "coloured");

        drag(runAt(flow, 4), textPoint(flow, 4), textPoint(flow, 17));
        relayOut(view);
        shortcut(view, KeyCode.C);
        Path highlight = (Path) block.lookup(".markdown-selection");
        Bounds selected = sceneBounds(highlight);
        assertEquals(sceneCharacterBounds(flow, 4).getMinX(), selected.getMinX(), 0.5, "from the fifth character");
        assertEquals(sceneCharacterBounds(flow, 16).getMaxX(), selected.getMaxX(), 0.5, "to the seventeenth");
        assertEquals(sceneCharacterBounds(flow, 4).getHeight(), selected.getHeight(), 1.0, "on one line");
        drag(runAt(flow, 32), textPoint(flow, 32), textPoint(flow, 2));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("total = price", "t total = price * count;\nretur"), copied);
        assertEquals(themeColor(view, "-fx-selection-color"), highlight.getFill());
        Parent layer = highlight.getParent();
        assertSame(flow, layer.getParent(), "a layer of the block's flow, over its runs");
        StringBuilder shown = new StringBuilder();
        for (Node child : layer.getChildrenUnmodifiable()) {
            if (child instanceof Text copy) {
                assertEquals(themeColor(view, "-fx-selection-text-color"), copy.getFill(), "the selected characters");
                assertTrue(copy.getStyleClass().isEmpty(), "no code- class reaches the copies");
                shown.append(copy.getText());
            }
        }
        assertEquals(textOf(flow), shown.toString(), "a copy of each run, in the runs' order");
        assertEquals(2 * sceneCharacterBounds(flow, 2).getHeight(), sceneBounds(highlight).getHeight(), 1.0,
                "the second selection over both lines");
    }

    @Test
    @DisplayName("in a coloured block a double click selects a word as code has it and a triple click a line, whatever"
            + " the pieces")
    void doubleAndTripleClickInColoredBlock() {
        MarkdownView view = layOut(new MarkdownView(
                "```java\nlong ovens = getKitchenValue(\"ovens\", tLong);\nif (o.index() >= max_rows) break;\n```"));
        List<String> copied = recordCopies(view);
        TextFlow flow = flow(view.lookup(".markdown-code-block"));

        clicks(flow, 16, 2);
        shortcut(view, KeyCode.C);
        clicks(flow, 32, 2);
        shortcut(view, KeyCode.C);
        clicks(flow, 29, 2);
        shortcut(view, KeyCode.C);
        clicks(flow, 52, 3);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("getKitchenValue", "ovens", "\"", "if (o.index() >= max_rows) break;"), copied);
    }

    @Test
    @DisplayName("a drag from a paragraph into a coloured block and on to the next paragraph copies the code as written,"
            + " set apart from the paragraphs")
    void dragAcrossParagraphsAndColoredBlock() {
        MarkdownView view = layOut(new MarkdownView("Text before.\n\n```java\nint total = price * count;\n"
                + "    return total;\n```\n\nText after."));
        List<String> copied = recordCopies(view);
        TextFlow before = textFlows(view, ".markdown-paragraph").get(0);
        TextFlow after = textFlows(view, ".markdown-paragraph").get(1);

        drag(runAt(before, 5), textPoint(before, 5), textPoint(after, 4));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("before.\n\nint total = price * count;\n    return total;\n\nText"), copied);
        assertFalse(((Path) view.lookup(".markdown-code-block .markdown-selection")).getElements().isEmpty(),
                "the whole code highlighted");
    }

    /**
     * Renders a {@code java} block of the code and asserts its runs, each given as its finest category's name (or
     * {@code plain}), a colon and its text, see {@link MarkdownTestSupport#assertCodeRuns}.
     */
    private static void assertJavaRuns(String code, String... expected) {
        assertCodeRuns("java", code, List.of(expected));
    }

    /**
     * The colour a theme token resolves to in the view's scene: a probe region beside the view takes the token as its
     * background, so the test does not repeat the theme's values.
     */
    private static Paint themeColor(MarkdownView view, String token) {
        Region probe = new Region();
        probe.setStyle("-fx-background-color: " + token + ";");
        StackPane root = (StackPane) view.getScene().getRoot();
        root.getChildren().add(probe);
        try {
            probe.applyCss();
            return probe.getBackground().getFills().get(0).getFill();
        } finally {
            root.getChildren().remove(probe);
        }
    }

    /**
     * A stylesheet of an application, from its text: a {@code data:} address, so no file is written.
     */
    private static String stylesheet(String css) {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Asserts that an application's rule from {@link #BOLD} reached a run or not: its underline, and its bold face
     * where the code's family has one. JavaFX finds no bold face of a variable font: "Cascadia Mono", the code's family
     * on a Windows machine without "JetBrains Mono NL", gives its regular face for bold (seen with JavaFX 25.0.2).
     */
    private static void assertBold(Text run, boolean expected, String what) {
        assertEquals(expected, run.isUnderline(), what + " underlined");
        Font font = run.getFont();
        if (isBold(Font.font(font.getFamily(), FontWeight.BOLD, font.getSize()))) {
            assertEquals(expected, isBold(font), what + " bold: " + font);
        }
    }

    private static boolean isBold(Font font) {
        return font.getStyle().toLowerCase(Locale.ROOT).contains("bold");
    }

    /**
     * Whether a narrow and a wide text of the same length are equally wide in a run's font.
     */
    private static boolean isMonospaced(Text run) {
        Text narrow = new Text("iiiiiiiiii");
        narrow.setFont(run.getFont());
        Text wide = new Text("MMMMMMMMMM");
        wide.setFont(run.getFont());
        return Math.abs(narrow.getLayoutBounds().getWidth() - wide.getLayoutBounds().getWidth()) < 0.5;
    }

    /**
     * Clicks the primary button as often as given over a character of a flow, as JavaFX delivers a double or triple
     * click to the run under the pointer: a press, a release and a click per click, each with the count so far.
     */
    private static void clicks(TextFlow flow, int index, int count) {
        Text run = runAt(flow, index);
        Point2D at = textPoint(flow, index);
        for (int clickCount = 1; clickCount <= count; clickCount++) {
            Event.fireEvent(run, mouse(MouseEvent.MOUSE_PRESSED, at, clickCount));
            Event.fireEvent(run, mouse(MouseEvent.MOUSE_RELEASED, at, clickCount));
            Event.fireEvent(run, mouse(MouseEvent.MOUSE_CLICKED, at, clickCount));
        }
    }

    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at, int clickCount) {
        return new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), MouseButton.PRIMARY, clickCount, false,
                false, false, false, type == MouseEvent.MOUSE_PRESSED, false, false, false, false, true, null);
    }

    /**
     * The text flow of a code block, {@code markdown-block-content}.
     */
    private static TextFlow flow(Node block) {
        return (TextFlow) block.lookup(".markdown-block-content");
    }

    /**
     * The text of a flow's runs.
     */
    private static String textOf(TextFlow flow) {
        StringBuilder text = new StringBuilder();
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                text.append(run.getText());
            }
        }
        return text.toString();
    }
}
