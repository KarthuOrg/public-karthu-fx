package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeLexer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The languages a {@code MarkdownView} knows from the start: the built-in lexers under their names and short names,
 * the prefill of every view's map of languages.
 */
public final class BuiltInLanguages {

    private BuiltInLanguages() {
    }

    /**
     * Returns a new map of the built-in languages, by their lower-case names. Each call gives a map of its own, so
     * that views do not share theirs. A lexer holds no state, so its names share one instance.
     *
     * @return the built-in lexers by their names, in a map the caller owns
     */
    public static Map<String, CodeLexer> lexers() {
        Map<String, CodeLexer> languages = new LinkedHashMap<>();

        // Programming languages.
        add(languages, new JavaLexer(), "java");

        // Data languages: JSON, YAML and SQL.
        add(languages, new JsonLexer(), "json");
        add(languages, new YamlLexer(), "yaml", "yml");
        add(languages, new SqlLexer(), "sql");

        // Markup and shell: XML and HTML, Bash, Markdown.
        add(languages, new XmlLexer(), "xml", "html", "xhtml");
        add(languages, new BashLexer(), "bash", "sh", "shell");
        add(languages, new MarkdownLexer(), "markdown", "md");

        return languages;
    }

    /**
     * Adds a lexer under each of its names.
     */
    private static void add(Map<String, CodeLexer> languages, CodeLexer lexer, String... names) {
        for (String name : names) {
            languages.put(name, lexer);
        }
    }
}
