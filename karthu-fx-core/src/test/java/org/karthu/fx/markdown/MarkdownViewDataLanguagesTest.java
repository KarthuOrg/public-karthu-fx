package org.karthu.fx.markdown;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.assertCodeRuns;

/**
 * The built-in data languages JSON, YAML and SQL, seen from outside the view: Markdown with a fenced block of the
 * language in, the block's runs out, each read by its text and its style classes (through
 * {@link MarkdownTestSupport#assertCodeRuns}). One table of cases per language.
 */
class MarkdownViewDataLanguagesTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    /**
     * The cases of the JSON lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> jsonCases() {
        return Stream.of(
                // Properties and strings.
                Arguments.of("a string before : is a property", "{\"a\": 1}",
                        List.of("punctuation:{", "property:\"a\"", "punctuation::", "plain: ", "number:1",
                                "punctuation:}")),
                Arguments.of("a property before : after white space, a line feed too", "{\"a\" : 1,\n\"b\"\n: 2}",
                        List.of("punctuation:{", "property:\"a\"", "plain: ", "punctuation::", "plain: ",
                                "number:1", "punctuation:,", "plain:\n", "property:\"b\"", "plain:\n",
                                "punctuation::", "plain: ", "number:2", "punctuation:}")),
                Arguments.of("other strings are strings, their escapes pieces of their own",
                        "[\"x\\ty\", \"\\u00e9\\\"\\\\\"]",
                        List.of("punctuation:[", "string:\"x", "escape:\\t", "string:y\"", "punctuation:,",
                                "plain: ", "string:\"", "escape:\\u00e9\\\"\\\\", "string:\"", "punctuation:]")),
                Arguments.of("a property is one piece, its escapes too", "{\"a\\\"b\": \"c\"}",
                        List.of("punctuation:{", "property:\"a\\\"b\"", "punctuation::", "plain: ", "string:\"c\"",
                                "punctuation:}")),
                Arguments.of("an empty string and an empty property", "{\"\": \"\"}",
                        List.of("punctuation:{", "property:\"\"", "punctuation::", "plain: ", "string:\"\"",
                                "punctuation:}")),
                Arguments.of("literals and numbers in a string stay in it", "{\"k\": \"true, null: 1\"}",
                        List.of("punctuation:{", "property:\"k\"", "punctuation::", "plain: ",
                                "string:\"true, null: 1\"", "punctuation:}")),
                Arguments.of("an unterminated string runs to the end of the block", "{\"a\": \"open,\n\"b\": 1}",
                        List.of("punctuation:{", "property:\"a\"", "punctuation::", "plain: ", "string:\"open,\n\"",
                                "plain:b", "string:\": 1}")),
                Arguments.of("an escape at the end of an unterminated string", "\"a\\",
                        List.of("string:\"a", "escape:\\")),
                // Numbers and literals.
                Arguments.of("numbers: integers, negative ones, fractions and exponents",
                        "[0, -12, 3.5, 1e10, -2.5E-3]",
                        List.of("punctuation:[", "number:0", "punctuation:,", "plain: ", "number:-12",
                                "punctuation:,", "plain: ", "number:3.5", "punctuation:,", "plain: ", "number:1e10",
                                "punctuation:,", "plain: ", "number:-2.5E-3", "punctuation:]")),
                Arguments.of("true, false and null are literals", "[true, false, null]",
                        List.of("punctuation:[", "literal:true", "punctuation:,", "plain: ", "literal:false",
                                "punctuation:,", "plain: ", "literal:null", "punctuation:]")),
                Arguments.of("a number or literal that runs into other characters is plain",
                        "[12ab, 1.2.3, -x, nulls, True]",
                        List.of("punctuation:[", "plain:12ab", "punctuation:,", "plain: 1.2.3", "punctuation:,",
                                "plain: -x", "punctuation:,", "plain: nulls", "punctuation:,", "plain: True",
                                "punctuation:]")),
                // Nesting, broken JSON and no JSON.
                Arguments.of("nested objects and arrays", "{\"a\": [1, {\"b\": null}]}",
                        List.of("punctuation:{", "property:\"a\"", "punctuation::", "plain: ", "punctuation:[",
                                "number:1", "punctuation:,", "plain: ", "punctuation:{", "property:\"b\"",
                                "punctuation::", "plain: ", "literal:null", "punctuation:}]}")),
                Arguments.of("JSON that does not parse is coloured too", "{a: 1,, 'b' true}",
                        List.of("punctuation:{", "plain:a", "punctuation::", "plain: ", "number:1",
                                "punctuation:,,", "plain: 'b' ", "literal:true", "punctuation:}")),
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @Test
    @DisplayName("every view knows json, yaml with its short name yml, and sql from the start")
    void dataLanguagesAreBuiltIn() {
        MarkdownView view = new MarkdownView("Text.");

        assertTrue(view.getCodeLanguages().keySet().containsAll(List.of("json", "yaml", "yml", "sql")),
                "names in the map: " + view.getCodeLanguages().keySet());
    }

    @Test
    @DisplayName("yml is YAML, and the names are found in any case and with words after them")
    void shortNameAndCase() {
        assertCodeRuns("yml", "k: 1", List.of("property:k", "punctuation::", "plain: ", "number:1"));
        assertCodeRuns("JSON title", "[null]", List.of("punctuation:[", "literal:null", "punctuation:]"));
        assertCodeRuns("Sql{1}", "select 1", List.of("keyword:select", "plain: ", "number:1"));
        assertCodeRuns("YAML", "~", List.of("literal:~"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("jsonCases")
    @DisplayName("the JSON lexer")
    void jsonRuns(String what, String code, List<String> expected) {
        assertCodeRuns("json", code, expected);
    }

    /**
     * The cases of the YAML lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> yamlCases() {
        return Stream.of(
                // Keys.
                Arguments.of("a key before : is a property", "name: bakery",
                        List.of("property:name", "punctuation::", "plain: bakery")),
                Arguments.of("keys with spaces, nested keys and a key without a value", "a b:\n  c: x\n  d:",
                        List.of("property:a b", "punctuation::", "plain:\n  ", "property:c", "punctuation::",
                                "plain: x\n  ", "property:d", "punctuation::")),
                Arguments.of("a colon inside a value or a key's name is no key's end", "url: http://x:8080/a\na:b: c",
                        List.of("property:url", "punctuation::", "plain: http://x:8080/a\n", "property:a:b",
                                "punctuation::", "plain: c")),
                // Comments.
                Arguments.of("comments at a line's start or after a space, a # elsewhere is text",
                        "# top\nk: v # end\nu: a#b\n  # indented",
                        List.of("comment:# top", "plain:\n", "property:k", "punctuation::", "plain: v ",
                                "comment:# end", "plain:\n", "property:u", "punctuation::", "plain: a#b\n  ",
                                "comment:# indented")),
                Arguments.of("a key in a comment stays in it", "# key: 1",
                        List.of("comment:# key: 1")),
                // Document markers and list dashes.
                Arguments.of("document markers and list dashes are punctuation", "---\n- a\n- - b\n...",
                        List.of("punctuation:---", "plain:\n", "punctuation:-", "plain: a\n", "punctuation:-",
                                "plain: ", "punctuation:-", "plain: b\n", "punctuation:...")),
                Arguments.of("a list of mappings", "- name: a\n  size: big",
                        List.of("punctuation:-", "plain: ", "property:name", "punctuation::", "plain: a\n  ",
                                "property:size", "punctuation::", "plain: big")),
                Arguments.of("a dash or dots inside a value are no punctuation", "a: x - y\nb: -x\nc: --- d\n --- e",
                        List.of("property:a", "punctuation::", "plain: x - y\n", "property:b", "punctuation::",
                                "plain: -x\n", "property:c", "punctuation::", "plain: --- d\n --- e")),
                Arguments.of("a document marker with a comment after it", "--- # doc\nk: v",
                        List.of("punctuation:---", "plain: ", "comment:# doc", "plain:\n", "property:k",
                                "punctuation::", "plain: v")),
                // Quoted strings.
                Arguments.of("quoted strings, escapes in double quotes only", "a: \"x\\ty\"\nb: 'it''s \\t'",
                        List.of("property:a", "punctuation::", "plain: ", "string:\"x", "escape:\\t", "string:y\"",
                                "plain:\n", "property:b", "punctuation::", "plain: ", "string:'it''s \\t'")),
                Arguments.of("unicode escapes of four and eight digits, a hex escape", "s: \"\\u00e9\\U0001F600\\x41\"",
                        List.of("property:s", "punctuation::", "plain: ", "string:\"",
                                "escape:\\u00e9\\U0001F600\\x41", "string:\"")),
                Arguments.of("a quoted key is a property, also without a space before the value",
                        "\"a b\": 1\n'c': 2\n\"d\":3",
                        List.of("property:\"a b\"", "punctuation::", "plain: ", "number:1", "plain:\n",
                                "property:'c'", "punctuation::", "plain: ", "number:2", "plain:\n", "property:\"d\"",
                                "punctuation::", "number:3")),
                Arguments.of("a keyword, a key and a comment in a string stay in it", "a: \"true # no: 1\"  # c",
                        List.of("property:a", "punctuation::", "plain: ", "string:\"true # no: 1\"", "plain:  ",
                                "comment:# c")),
                Arguments.of("a quote inside a plain value is no string", "a: it's \"so\"",
                        List.of("property:a", "punctuation::", "plain: it's \"so\"")),
                Arguments.of("a quoted string over lines", "a: \"one\n  two\"\nb: 1",
                        List.of("property:a", "punctuation::", "plain: ", "string:\"one\n  two\"", "plain:\n",
                                "property:b", "punctuation::", "plain: ", "number:1")),
                Arguments.of("an unterminated string runs to the end of the block", "a: \"open\nb: 1",
                        List.of("property:a", "punctuation::", "plain: ", "string:\"open\nb: 1")),
                Arguments.of("an unterminated single-quoted string too", "- 'open\n- b",
                        List.of("punctuation:-", "plain: ", "string:'open\n- b")),
                // Numbers and literals, as whole values.
                Arguments.of("numbers", "- 1\n- -2.5e3\n- +.5\n- 0x1F\n- 0o17\n- .inf\n- -.Inf\n- .NaN",
                        List.of("punctuation:-", "plain: ", "number:1", "plain:\n", "punctuation:-", "plain: ",
                                "number:-2.5e3", "plain:\n", "punctuation:-", "plain: ", "number:+.5", "plain:\n",
                                "punctuation:-", "plain: ", "number:0x1F", "plain:\n", "punctuation:-", "plain: ",
                                "number:0o17", "plain:\n", "punctuation:-", "plain: ", "number:.inf", "plain:\n",
                                "punctuation:-", "plain: ", "number:-.Inf", "plain:\n", "punctuation:-", "plain: ",
                                "number:.NaN")),
                Arguments.of("true, false, null and ~ are literals, in each case YAML knows",
                        "a: true\nb: False\nc: NULL\nd: ~\ne: null # none",
                        List.of("property:a", "punctuation::", "plain: ", "literal:true", "plain:\n", "property:b",
                                "punctuation::", "plain: ", "literal:False", "plain:\n", "property:c",
                                "punctuation::", "plain: ", "literal:NULL", "plain:\n", "property:d",
                                "punctuation::", "plain: ", "literal:~", "plain:\n", "property:e", "punctuation::",
                                "plain: ", "literal:null", "plain: ", "comment:# none")),
                Arguments.of("a value that is more than a number or a literal is plain",
                        "v: 1.2.3\nw: true story\nx: 10 apples\ny: tRUE\nz: yes",
                        List.of("property:v", "punctuation::", "plain: 1.2.3\n", "property:w", "punctuation::",
                                "plain: true story\n", "property:x", "punctuation::", "plain: 10 apples\n",
                                "property:y", "punctuation::", "plain: tRUE\n", "property:z", "punctuation::",
                                "plain: yes")),
                Arguments.of("a number or a literal as a key is a property", "1: a\ntrue: b",
                        List.of("property:1", "punctuation::", "plain: a\n", "property:true", "punctuation::",
                                "plain: b")),
                // Anchors and aliases.
                Arguments.of("anchors and aliases are variables", "base: &base\n  a: 1\nother:\n  <<: *base\n- *x",
                        List.of("property:base", "punctuation::", "plain: ", "variable:&base", "plain:\n  ",
                                "property:a", "punctuation::", "plain: ", "number:1", "plain:\n", "property:other",
                                "punctuation::", "plain:\n  ", "property:<<", "punctuation::", "plain: ",
                                "variable:*base", "plain:\n", "punctuation:-", "plain: ", "variable:*x")),
                Arguments.of("an anchor before a value, an & or * inside a value or without a name is text",
                        "a: &n 5\nb: x&y *z\nc: & d\ne: [*, &]",
                        List.of("property:a", "punctuation::", "plain: ", "variable:&n", "plain: ", "number:5",
                                "plain:\n", "property:b", "punctuation::", "plain: x&y *z\n", "property:c",
                                "punctuation::", "plain: & d\n", "property:e", "punctuation::", "plain: ",
                                "punctuation:[", "plain:*", "punctuation:,", "plain: &", "punctuation:]")),
                // Flow collections.
                Arguments.of("flow collections, their brackets and commas punctuation",
                        "a: {b: 1, c: [x, \"y\", true]}",
                        List.of("property:a", "punctuation::", "plain: ", "punctuation:{", "property:b",
                                "punctuation::", "plain: ", "number:1", "punctuation:,", "plain: ", "property:c",
                                "punctuation::", "plain: ", "punctuation:[", "plain:x", "punctuation:,", "plain: ",
                                "string:\"y\"", "punctuation:,", "plain: ", "literal:true", "punctuation:]}")),
                Arguments.of("JSON is YAML", "{\"a\":[1,null]}",
                        List.of("punctuation:{", "property:\"a\"", "punctuation::[", "number:1", "punctuation:,",
                                "literal:null", "punctuation:]}")),
                Arguments.of("a comma or bracket outside a flow collection is text", "a: x, y]",
                        List.of("property:a", "punctuation::", "plain: x, y]")),
                // Block scalars.
                Arguments.of("a block scalar's lines stay plain, up to a line indented no deeper than its key",
                        "run: |\n  echo \"a: b\" # no comment\n\n  - 1\nnext: >-\n  x\ny: 2",
                        List.of("property:run", "punctuation::", "plain: ", "punctuation:|",
                                "plain:\n  echo \"a: b\" # no comment\n\n  - 1\n", "property:next", "punctuation::",
                                "plain: ", "punctuation:>-", "plain:\n  x\n", "property:y", "punctuation::",
                                "plain: ", "number:2")),
                Arguments.of("a block scalar in a list, with a comment on its first line", "- |2 # text\n   a\n- b",
                        List.of("punctuation:-", "plain: ", "punctuation:|2", "plain: ", "comment:# text",
                                "plain:\n   a\n", "punctuation:-", "plain: b")),
                Arguments.of("a | or > inside a value is text", "a: x | y\nb: > z",
                        List.of("property:a", "punctuation::", "plain: x | y\n", "property:b", "punctuation::",
                                "plain: > z")),
                // YAML that does not parse, and no YAML.
                Arguments.of("YAML that does not parse is coloured too", "a: b: c\n  - ]\n\"x",
                        List.of("property:a", "punctuation::", "plain: ", "property:b", "punctuation::",
                                "plain: c\n  ", "punctuation:-", "plain: ", "punctuation:]", "plain:\n",
                                "string:\"x")),
                // No YAML.
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("yamlCases")
    @DisplayName("the YAML lexer")
    void yamlRuns(String what, String code, List<String> expected) {
        assertCodeRuns("yaml", code, expected);
    }

    /**
     * The cases of the SQL lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> sqlCases() {
        return Stream.of(
                // Keywords, in any case.
                Arguments.of("keywords, a function, numbers, operators and punctuation",
                        "SELECT count(*) FROM t WHERE id = 1;",
                        List.of("keyword:SELECT", "plain: ", "function:count", "punctuation:(", "operator:*",
                                "punctuation:)", "plain: ", "keyword:FROM", "plain: t ", "keyword:WHERE",
                                "plain: id ", "operator:=", "plain: ", "number:1", "punctuation:;")),
                Arguments.of("keywords in lower and mixed case", "select Distinct name from t order By 1",
                        List.of("keyword:select", "plain: ", "keyword:Distinct", "plain: name ", "keyword:from",
                                "plain: t ", "keyword:order", "plain: ", "keyword:By", "plain: ", "number:1")),
                Arguments.of("keywords of PostgreSQL",
                        "insert into t values (1) on conflict do nothing returning *",
                        List.of("keyword:insert", "plain: ", "keyword:into", "plain: t ", "keyword:values",
                                "plain: ", "punctuation:(", "number:1", "punctuation:)", "plain: ", "keyword:on",
                                "plain: ", "keyword:conflict", "plain: ", "keyword:do", "plain: ", "keyword:nothing",
                                "plain: ", "keyword:returning", "plain: ", "operator:*")),
                // Comments.
                Arguments.of("a line comment and a block comment over lines", "a -- one\n/* two\n three */ b",
                        List.of("plain:a ", "comment:-- one", "plain:\n", "comment:/* two\n three */", "plain: b")),
                Arguments.of("a keyword in a comment stays in it", "-- select * from t\n/* where null */",
                        List.of("comment:-- select * from t", "plain:\n", "comment:/* where null */")),
                Arguments.of("an unterminated comment runs to the end of the block", "a /* open\nselect 1",
                        List.of("plain:a ", "comment:/* open\nselect 1")),
                // Strings and quoted identifiers.
                Arguments.of("strings in single quotes, a doubled quote inside", "x = 'it''s' || ''",
                        List.of("plain:x ", "operator:=", "plain: ", "string:'it''s'", "plain: ", "operator:||",
                                "plain: ", "string:''")),
                Arguments.of("a keyword, a comment and a parameter in a string stay in it", "'select -- :p ?'",
                        List.of("string:'select -- :p ?'")),
                Arguments.of("an unterminated string runs to the end of the block", "x = 'open\nselect 1",
                        List.of("plain:x ", "operator:=", "plain: ", "string:'open\nselect 1")),
                Arguments.of("quoted identifiers stay plain, keywords and quotes inside too",
                        "select \"from\", \"Order\".\"Id\", \"a\"\"b\"",
                        List.of("keyword:select", "plain: \"from\"", "punctuation:,", "plain: \"Order\"",
                                "punctuation:.", "plain:\"Id\"", "punctuation:,", "plain: \"a\"\"b\"")),
                Arguments.of("an unterminated quoted identifier runs to the end of the block", "\"open\nselect",
                        List.of("plain:\"open\nselect")),
                // Literals and data types.
                Arguments.of("NULL, TRUE and FALSE are literals in any case", "NULL true False is not null",
                        List.of("literal:NULL", "plain: ", "literal:true", "plain: ", "literal:False", "plain: ",
                                "keyword:is", "plain: ", "keyword:not", "plain: ", "literal:null")),
                Arguments.of("data types, also with a length or a precision",
                        "id INTEGER, name varchar(100), price NUMERIC (10, 2), at TIMESTAMP WITH TIME ZONE",
                        List.of("plain:id ", "type:INTEGER", "punctuation:,", "plain: name ", "type:varchar",
                                "punctuation:(", "number:100", "punctuation:),", "plain: price ", "type:NUMERIC",
                                "plain: ", "punctuation:(", "number:10", "punctuation:,", "plain: ", "number:2",
                                "punctuation:),", "plain: ", "keyword:at", "plain: ", "type:TIMESTAMP", "plain: ",
                                "keyword:WITH", "plain: ", "type:TIME", "plain: ", "keyword:ZONE")),
                Arguments.of("more data types of PostgreSQL", "bigint text boolean jsonb uuid double precision",
                        List.of("type:bigint", "plain: ", "type:text", "plain: ", "type:boolean", "plain: ",
                                "type:jsonb", "plain: ", "type:uuid", "plain: ", "type:double", "plain: ",
                                "type:precision")),
                // Parameters.
                Arguments.of("parameters :name, ? and $1, a cast :: an operator",
                        "a = :name and b = ? and c = $12 and d::text",
                        List.of("plain:a ", "operator:=", "plain: ", "variable::name", "plain: ", "keyword:and",
                                "plain: b ", "operator:=", "plain: ", "variable:?", "plain: ", "keyword:and",
                                "plain: c ", "operator:=", "plain: ", "variable:$12", "plain: ", "keyword:and",
                                "plain: d", "operator:::", "type:text")),
                Arguments.of("a parameter in a function's brackets, a $ inside a name is the name's",
                        "lower(:p_name) = a$b",
                        List.of("function:lower", "punctuation:(", "variable::p_name", "punctuation:)", "plain: ",
                                "operator:=", "plain: a$b")),
                // Functions: in SQL only a name directly before (.
                Arguments.of("a name directly before ( is a function, also qualified, unless it is a keyword",
                        "coalesce(a, 0) + pg_catalog.now() in (1) and exists(x)",
                        List.of("function:coalesce", "punctuation:(", "plain:a", "punctuation:,", "plain: ",
                                "number:0", "punctuation:)", "plain: ", "operator:+", "plain: pg_catalog",
                                "punctuation:.", "function:now", "punctuation:()", "plain: ", "keyword:in", "plain: ",
                                "punctuation:(", "number:1", "punctuation:)", "plain: ", "keyword:and", "plain: ",
                                "keyword:exists", "punctuation:(", "plain:x", "punctuation:)")),
                Arguments.of("a name with spaces before ( is no function", "f (x) and count  (*)",
                        List.of("plain:f ", "punctuation:(", "plain:x", "punctuation:)", "plain: ", "keyword:and",
                                "plain: count  ", "punctuation:(", "operator:*", "punctuation:)")),
                Arguments.of("a name right after TABLE, INTO, REFERENCES or VIEW is no function, in any case",
                        "create TABLE orders(id int References customers(id));\n"
                                + "insert into users(id) values (1);\ncreate view v(a) as select max(a)",
                        List.of("keyword:create", "plain: ", "keyword:TABLE", "plain: orders", "punctuation:(",
                                "plain:id ", "type:int", "plain: ", "keyword:References", "plain: customers",
                                "punctuation:(", "plain:id", "punctuation:));", "plain:\n", "keyword:insert",
                                "plain: ", "keyword:into", "plain: users", "punctuation:(", "plain:id",
                                "punctuation:)", "plain: ", "keyword:values", "plain: ", "punctuation:(", "number:1",
                                "punctuation:);", "plain:\n", "keyword:create", "plain: ", "keyword:view", "plain: v",
                                "punctuation:(", "plain:a", "punctuation:)", "plain: ", "keyword:as", "plain: ",
                                "keyword:select", "plain: ", "function:max", "punctuation:(", "plain:a",
                                "punctuation:)")),
                Arguments.of("a qualified table name and one on the next line count as right after the word",
                        "insert into public.users(id);\ncreate table\n  t(a)",
                        List.of("keyword:insert", "plain: ", "keyword:into", "plain: public", "punctuation:.",
                                "plain:users", "punctuation:(", "plain:id", "punctuation:);", "plain:\n",
                                "keyword:create", "plain: ", "keyword:table", "plain:\n  t", "punctuation:(",
                                "plain:a", "punctuation:)")),
                Arguments.of("after the table's name, a name before ( is a function again", "into t f(x)",
                        List.of("keyword:into", "plain: t ", "function:f", "punctuation:(", "plain:x",
                                "punctuation:)")),
                Arguments.of("IF NOT EXISTS after TABLE or VIEW keeps the name after it plain, in any case, qualified"
                                + " too",
                        "CREATE TABLE IF NOT EXISTS orders(id int);\ncreate view if not exists public.v(a)",
                        List.of("keyword:CREATE", "plain: ", "keyword:TABLE", "plain: ", "keyword:IF", "plain: ",
                                "keyword:NOT", "plain: ", "keyword:EXISTS", "plain: orders", "punctuation:(",
                                "plain:id ", "type:int", "punctuation:);", "plain:\n", "keyword:create", "plain: ",
                                "keyword:view", "plain: ", "keyword:if", "plain: ", "keyword:not", "plain: ",
                                "keyword:exists", "plain: public", "punctuation:.", "plain:v", "punctuation:(",
                                "plain:a", "punctuation:)")),
                Arguments.of("IF EXISTS after a table word keeps the name plain too, after FUNCTION the name stays a"
                                + " function",
                        "drop table if exists t(x);\ndrop function if exists f(int)",
                        List.of("keyword:drop", "plain: ", "keyword:table", "plain: ", "keyword:if", "plain: ",
                                "keyword:exists", "plain: t", "punctuation:(", "plain:x", "punctuation:);",
                                "plain:\n", "keyword:drop", "plain: ", "keyword:function", "plain: ", "keyword:if",
                                "plain: ", "keyword:exists", "plain: ", "function:f", "punctuation:(", "type:int",
                                "punctuation:)")),
                Arguments.of("after a name behind IF NOT EXISTS, a name before ( is a function again",
                        "create table if not exists t(a int default f(1))",
                        List.of("keyword:create", "plain: ", "keyword:table", "plain: ", "keyword:if", "plain: ",
                                "keyword:not", "plain: ", "keyword:exists", "plain: t", "punctuation:(", "plain:a ",
                                "type:int", "plain: ", "keyword:default", "plain: ", "function:f", "punctuation:(",
                                "number:1", "punctuation:))")),
                Arguments.of("NOT EXISTS without IF, or IF EXISTS elsewhere, keeps nothing plain",
                        "create table not exists t(x) where if exists g(y)",
                        List.of("keyword:create", "plain: ", "keyword:table", "plain: ", "keyword:not", "plain: ",
                                "keyword:exists", "plain: ", "function:t", "punctuation:(", "plain:x",
                                "punctuation:)", "plain: ", "keyword:where", "plain: ", "keyword:if", "plain: ",
                                "keyword:exists", "plain: ", "function:g", "punctuation:(", "plain:y",
                                "punctuation:)")),
                // Numbers, operators and punctuation.
                Arguments.of("numbers as whole words", "1 2.5 .5 1e3 6.02E+23 12ab x1",
                        List.of("number:1", "plain: ", "number:2.5", "plain: ", "number:.5", "plain: ",
                                "number:1e3", "plain: ", "number:6.02E+23", "plain: 12ab x1")),
                Arguments.of("operators", "a <> b and c >= d or e || f % 2",
                        List.of("plain:a ", "operator:<>", "plain: b ", "keyword:and", "plain: c ", "operator:>=",
                                "plain: d ", "keyword:or", "plain: e ", "operator:||", "plain: f ", "operator:%",
                                "plain: ", "number:2")),
                Arguments.of("punctuation", "t.a, (b); x[1]",
                        List.of("plain:t", "punctuation:.", "plain:a", "punctuation:,", "plain: ", "punctuation:(",
                                "plain:b", "punctuation:);", "plain: x", "punctuation:[", "number:1",
                                "punctuation:]")),
                // SQL that does not parse, and no SQL.
                Arguments.of("SQL that does not parse is coloured too", "SELECT FROM WHERE 3x '",
                        List.of("keyword:SELECT", "plain: ", "keyword:FROM", "plain: ", "keyword:WHERE",
                                "plain: 3x ", "string:'")),
                Arguments.of("a whole statement", "CREATE TABLE t (\n  id BIGINT PRIMARY KEY,\n"
                                + "  name VARCHAR(80) NOT NULL DEFAULT 'x' -- the name\n);",
                        List.of("keyword:CREATE", "plain: ", "keyword:TABLE", "plain: t ",
                                "punctuation:(", "plain:\n  id ", "type:BIGINT", "plain: ", "keyword:PRIMARY",
                                "plain: ", "keyword:KEY", "punctuation:,", "plain:\n  name ", "type:VARCHAR",
                                "punctuation:(", "number:80", "punctuation:)", "plain: ", "keyword:NOT", "plain: ",
                                "literal:NULL", "plain: ", "keyword:DEFAULT", "plain: ", "string:'x'", "plain: ",
                                "comment:-- the name", "plain:\n", "punctuation:);")),
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sqlCases")
    @DisplayName("the SQL lexer")
    void sqlRuns(String what, String code, List<String> expected) {
        assertCodeRuns("sql", code, expected);
    }
}
