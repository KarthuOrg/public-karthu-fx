package org.karthu.fx.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The category of a piece of code in a coloured code block of the {@link MarkdownView}: keyword, string, comment and
 * so on. A {@link CodeLexer} cuts a block's code into {@link CodePiece pieces}, each of one category, and the view
 * colours each piece by its category. Text between pieces is plain code, in the block's code colour
 * {@code -fx-code-block-text-color} (the code colour {@code -fx-code-text-color} in Nebula Dark, black in Nebula
 * Light).
 *
 * <p><b>Base and fine categories.</b> Nine categories are <em>base</em> categories with a colour of their own, so that
 * nine colours colour every language completely. The fourteen others are <em>fine</em> categories: each takes the
 * colour of the category it falls back to ({@link #fallback()}) until it is set itself, and three of them (operator,
 * punctuation, emphasis) fall back to the code text.
 *
 * <p><b>CSS.</b> Every category is a CSS variable of karthu-fx's theme, named like {@code -fx-code-text-color}
 * ({@link #colorVariable()}): {@code -fx-code-keyword-color}, {@code -fx-code-annotation-color} and so on, defined at
 * the theme's root. A base variable holds its colour, a fine one refers to the variable it falls back to
 * ({@code -fx-code-annotation-color: -fx-code-keyword-color}), so a base colour set on one view with {@code setStyle}
 * also changes the fine categories of that view that still fall back to it. Each piece's text node carries the
 * style class of its category ({@link #styleClass()}, {@code code-keyword}) and the classes of every category its
 * fallback chain passes: an annotation carries {@code code-annotation} and {@code code-keyword}, a constant
 * {@code code-constant}, {@code code-literal} and {@code code-number}. So an own rule for {@code code-keyword} (an
 * underline, say) also reaches annotations, and one for {@code code-annotation} only them. The defaults are colours
 * only, no bold, no italic, each readable on the code background {@code -fx-code-background-color}. Each colour
 * scheme of the theme, Nebula Dark and Nebula Light, has base colours of its own. In Nebula Dark every fine variable
 * refers to its fallback. Nebula Light follows the default colours of Eclipse's light editors, and some of its fine
 * variables hold a colour of their own or refer to another category's variable. Bold and italic from an own rule show only when the code font has those faces: JavaFX finds none
 * of a variable font such as "Cascadia Mono".
 *
 * <p>Categories are never removed; new fine categories with a fallback may be added.
 */
public enum CodeCategory {

    /**
     * Comments. Style class {@code code-comment}, variable {@code -fx-code-comment-color}, default {@code #8a90a8} in
     * Nebula Dark, {@code #3F7F5F} in Nebula Light.
     */
    COMMENT(true, null),

    /**
     * Keywords of a language. Style class {@code code-keyword}, variable {@code -fx-code-keyword-color}, default
     * {@code #c88bff} in Nebula Dark, {@code #7F0055} in Nebula Light.
     */
    KEYWORD(true, null),

    /**
     * Text in quotes. Style class {@code code-string}, variable {@code -fx-code-string-color}, default
     * {@code #e0a84c} in Nebula Dark, {@code #2A00FF} in Nebula Light.
     */
    STRING(true, null),

    /**
     * Numbers. Style class {@code code-number}, variable {@code -fx-code-number-color}, default {@code #ff9e64} in
     * Nebula Dark, {@code #000000} in Nebula Light.
     */
    NUMBER(true, null),

    /**
     * Type names. Style class {@code code-type}, variable {@code -fx-code-type-color}, default {@code #4cd38a} in
     * Nebula Dark, {@code #000000} in Nebula Light.
     */
    TYPE(true, null),

    /**
     * A name followed by {@code (}, PowerShell commands such as {@code Get-ChildItem}. Style class
     * {@code code-function}, variable {@code -fx-code-function-color}, default {@code #5d9bff} in Nebula Dark,
     * {@code #000000} in Nebula Light.
     */
    FUNCTION(true, null),

    /**
     * Variables: Bash and PowerShell variables, SQL parameters, YAML anchors. Style class {@code code-variable},
     * variable {@code -fx-code-variable-color}, default {@code #ff8fa3} in Nebula Dark, {@code #000000} in Nebula
     * Light.
     */
    VARIABLE(true, null),

    /**
     * Keys in JSON and YAML. Style class {@code code-property}, variable {@code -fx-code-property-color}, default
     * {@code #7b8cff} in Nebula Dark, {@code #0000C0} in Nebula Light.
     */
    PROPERTY(true, null),

    /**
     * XML and HTML tags. Style class {@code code-tag}, variable {@code -fx-code-tag-color}, default {@code #ff8fa3} in
     * Nebula Dark, {@code #3F7F7F} in Nebula Light.
     */
    TAG(true, null),

    /**
     * Documentation comments such as Javadoc. Falls back to {@link #COMMENT}. Style classes {@code code-doc} and
     * {@code code-comment}, variable {@code -fx-code-doc-color}, default {@code -fx-code-comment-color} in Nebula Dark,
     * {@code #3F5FBF} in Nebula Light.
     */
    DOC(false, COMMENT),

    /**
     * Java annotations, PowerShell attributes. Falls back to {@link #KEYWORD}. Style classes {@code code-annotation}
     * and {@code code-keyword}, variable {@code -fx-code-annotation-color}, default {@code -fx-code-keyword-color} in
     * Nebula Dark, {@code #646464} in Nebula Light.
     */
    ANNOTATION(false, KEYWORD),

    /**
     * {@code true}, {@code false}, {@code null}, SQL {@code NULL}, PowerShell {@code $true}, {@code $false} and
     * {@code $null}. Falls back to {@link #NUMBER}. Style classes {@code code-literal} and {@code code-number},
     * variable {@code -fx-code-literal-color}, default {@code -fx-code-number-color} in Nebula Dark,
     * {@code -fx-code-keyword-color} in Nebula Light.
     */
    LITERAL(false, NUMBER),

    /**
     * Java names in capitals only, at least two characters. Falls back to {@link #LITERAL}. Style classes
     * {@code code-constant}, {@code code-literal} and {@code code-number}, variable {@code -fx-code-constant-color},
     * default {@code -fx-code-literal-color} in Nebula Dark, {@code -fx-code-property-color} in Nebula Light.
     */
    CONSTANT(false, LITERAL),

    /**
     * Escapes in strings, XML entities. Falls back to {@link #STRING}. Style classes {@code code-escape} and
     * {@code code-string}, variable {@code -fx-code-escape-color}, default {@code -fx-code-string-color}.
     */
    ESCAPE(false, STRING),

    /**
     * XML and HTML attribute names, PowerShell parameters. Falls back to {@link #NUMBER}. Style classes
     * {@code code-attribute} and {@code code-number}, variable {@code -fx-code-attribute-color}, default
     * {@code -fx-code-number-color} in Nebula Dark, {@code #7F007F} in Nebula Light.
     */
    ATTRIBUTE(false, NUMBER),

    /**
     * Operators. Falls back to the code text. Style class {@code code-operator}, variable
     * {@code -fx-code-operator-color}, default {@code -fx-code-text-color} in Nebula Dark,
     * {@code -fx-code-block-text-color} in Nebula Light.
     */
    OPERATOR(false, null),

    /**
     * Brackets and separators. Falls back to the code text. Style class {@code code-punctuation}, variable
     * {@code -fx-code-punctuation-color}, default {@code -fx-code-text-color} in Nebula Dark, {@code #008080} in
     * Nebula Light.
     */
    PUNCTUATION(false, null),

    /**
     * Markdown headings. Falls back to {@link #FUNCTION}. Style classes {@code code-heading} and
     * {@code code-function}, variable {@code -fx-code-heading-color}, default {@code -fx-code-function-color}.
     */
    HEADING(false, FUNCTION),

    /**
     * Markdown links and addresses. Falls back to {@link #PROPERTY}. Style classes {@code code-link} and
     * {@code code-property}, variable {@code -fx-code-link-color}, default {@code -fx-code-property-color}.
     */
    LINK(false, PROPERTY),

    /**
     * Markdown inline code and fences. Falls back to {@link #STRING}. Style classes {@code code-code} and
     * {@code code-string}, variable {@code -fx-code-code-color}, default {@code -fx-code-string-color}.
     */
    CODE(false, STRING),

    /**
     * Markdown list markers. Falls back to {@link #KEYWORD}. Style classes {@code code-list} and {@code code-keyword},
     * variable {@code -fx-code-list-color}, default {@code -fx-code-keyword-color}.
     */
    LIST(false, KEYWORD),

    /**
     * Markdown quote lines. Falls back to {@link #COMMENT}. Style classes {@code code-quote} and
     * {@code code-comment}, variable {@code -fx-code-quote-color}, default {@code -fx-code-comment-color}.
     */
    QUOTE(false, COMMENT),

    /**
     * Markdown emphasis and strong emphasis. Falls back to the code text. Style class {@code code-emphasis}, variable
     * {@code -fx-code-emphasis-color}, default {@code -fx-code-text-color} in Nebula Dark,
     * {@code -fx-code-block-text-color} in Nebula Light.
     */
    EMPHASIS(false, null);

    private final boolean base;
    private final CodeCategory fallback;
    private final String styleClass;
    private final String colorVariable;

    CodeCategory(boolean base, CodeCategory fallback) {
        this.base = base;
        this.fallback = fallback;
        String name = name().toLowerCase(Locale.ROOT);
        this.styleClass = "code-" + name;
        this.colorVariable = "-fx-code-" + name + "-color";
    }

    /**
     * Returns whether this is a base category, with a colour of its own.
     *
     * @return true for the nine base categories, false for the fine ones
     */
    public boolean isBase() {
        return base;
    }

    /**
     * Returns the category whose colour this one takes until it is set itself.
     *
     * @return the category it falls back to; empty for a base category and for a fine one that falls back to the
     *         code text ({@code -fx-code-text-color}): operator, punctuation and emphasis
     */
    public Optional<CodeCategory> fallback() {
        return Optional.ofNullable(fallback);
    }

    /**
     * Returns the style class of this category, carried by the text node of each of its pieces.
     *
     * @return the category's name in lower case with the prefix {@code code-}, such as {@code code-keyword}
     */
    public String styleClass() {
        return styleClass;
    }

    /**
     * Returns the theme's CSS variable that holds the colour of this category.
     *
     * @return the variable, such as {@code -fx-code-keyword-color}
     */
    public String colorVariable() {
        return colorVariable;
    }

    /**
     * The style classes a piece of this category carries: its own and those of every category its fallback chain
     * passes, the finest first.
     *
     * @return the style classes, the finest first
     */
    List<String> styleClasses() {
        List<String> classes = new ArrayList<>();
        CodeCategory category = this;
        while (category != null) {
            classes.add(category.styleClass);
            category = category.fallback;
        }
        return classes;
    }
}
