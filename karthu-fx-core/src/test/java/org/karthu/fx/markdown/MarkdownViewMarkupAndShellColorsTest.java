package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.karthu.fx.markdown.MarkdownTestSupport.assertCodeRuns;
import static org.karthu.fx.markdown.MarkdownTestSupport.codeRunsByText;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;

/**
 * Code blocks of markup and shell, XML and HTML, Bash, PowerShell and Markdown, coloured by their language and seen
 * from outside the view: Markdown with a fenced block in, the block's runs out, read by their text and their style
 * classes (through {@link MarkdownTestSupport#assertCodeRuns}). One table of cases per language.
 */
class MarkdownViewMarkupAndShellColorsTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    /**
     * The cases of the XML and HTML lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> xmlCases() {
        return Stream.of(
                // Tags.
                Arguments.of("a start tag and an end tag around text", "<a>text</a>",
                        List.of("tag:<a>", "plain:text", "tag:</a>")),
                Arguments.of("attributes with values in double and single quotes", "<a href=\"x\" id='y'>",
                        List.of("tag:<a", "plain: ", "attribute:href", "punctuation:=", "string:\"x\"", "plain: ",
                                "attribute:id", "punctuation:=", "string:'y'", "tag:>")),
                Arguments.of("spaces around the equals sign", "<a b = \"1\">",
                        List.of("tag:<a", "plain: ", "attribute:b", "plain: ", "punctuation:=", "plain: ",
                                "string:\"1\"", "tag:>")),
                Arguments.of("empty element tags", "<br/> <img src=\"a.png\" />",
                        List.of("tag:<br/>", "plain: ", "tag:<img", "plain: ", "attribute:src", "punctuation:=",
                                "string:\"a.png\"", "plain: ", "tag:/>")),
                Arguments.of("names with a namespace prefix, dashes and dots", "<xs:element data-id=\"1\" a.b='2'/>",
                        List.of("tag:<xs:element", "plain: ", "attribute:data-id", "punctuation:=", "string:\"1\"",
                                "plain: ", "attribute:a.b", "punctuation:=", "string:'2'", "tag:/>")),
                Arguments.of("an end tag with a space before its bracket", "</a >",
                        List.of("tag:</a", "plain: ", "tag:>")),
                Arguments.of("a tag over lines", "<a\n  b=\"1\"\n>",
                        List.of("tag:<a", "plain:\n  ", "attribute:b", "punctuation:=", "string:\"1\"", "plain:\n",
                                "tag:>")),
                Arguments.of("HTML: an unquoted value and an attribute without a value", "<input type=text disabled>",
                        List.of("tag:<input", "plain: ", "attribute:type", "punctuation:=", "string:text", "plain: ",
                                "attribute:disabled", "tag:>")),
                Arguments.of("a processing instruction", "<?xml version=\"1.0\"?>\n<a/>",
                        List.of("tag:<?xml version=\"1.0\"?>", "plain:\n", "tag:<a/>")),
                Arguments.of("a doctype", "<!DOCTYPE html>\n<html>",
                        List.of("tag:<!DOCTYPE html>", "plain:\n", "tag:<html>")),
                Arguments.of("a doctype with an identifier and an internal subset",
                        "<!DOCTYPE n PUBLIC \"-//A//>\" [<!ELEMENT n (#PCDATA)>]> x",
                        List.of("tag:<!DOCTYPE n PUBLIC \"-//A//>\" [<!ELEMENT n (#PCDATA)>]>", "plain: x")),
                // Comments.
                Arguments.of("a comment, a tag in it staying in it", "a <!-- <b>x</b> --> c",
                        List.of("plain:a ", "comment:<!-- <b>x</b> -->", "plain: c")),
                Arguments.of("a comment over lines", "<!-- one\n two -->\n<a/>",
                        List.of("comment:<!-- one\n two -->", "plain:\n", "tag:<a/>")),
                Arguments.of("an unterminated comment runs to the end of the block", "<a/> <!-- open\n<b>x</b>",
                        List.of("tag:<a/>", "plain: ", "comment:<!-- open\n<b>x</b>")),
                // Entities and CDATA.
                Arguments.of("entities: named, decimal and hex, a lone ampersand plain",
                        "a &amp; b &#169; &#x1F600; & c",
                        List.of("plain:a ", "escape:&amp;", "plain: b ", "escape:&#169;", "plain: ",
                                "escape:&#x1F600;", "plain: & c")),
                Arguments.of("entities in an attribute value", "<a title=\"a &quot;b&quot;\">",
                        List.of("tag:<a", "plain: ", "attribute:title", "punctuation:=", "string:\"a ",
                                "escape:&quot;", "string:b", "escape:&quot;", "string:\"", "tag:>")),
                Arguments.of("CDATA content as a string, markup in it staying in it", "<![CDATA[a < b && <c>]]>",
                        List.of("tag:<![CDATA[", "string:a < b && <c>", "tag:]]>")),
                Arguments.of("an unterminated CDATA section runs to the end of the block", "<![CDATA[open\n<a>",
                        List.of("tag:<![CDATA[", "string:open\n<a>")),
                // Text between tags.
                Arguments.of("text between tags stays plain", "<p>if x &lt; 1 then</p>",
                        List.of("tag:<p>", "plain:if x ", "escape:&lt;", "plain: 1 then", "tag:</p>")),
                Arguments.of("a bracket that opens no tag is plain", "a < b <= c",
                        List.of("plain:a < b <= c")),
                // Script and style.
                Arguments.of("the content of a script is not coloured, nor read as markup",
                        "<script>if (a<b) x(\"</p>\");</script>",
                        List.of("tag:<script>", "plain:if (a<b) x(\"</p>\");", "tag:</script>")),
                Arguments.of("the content of a style is not coloured", "<style>p > a { color: red; }</style>",
                        List.of("tag:<style>", "plain:p > a { color: red; }", "tag:</style>")),
                Arguments.of("a script with attributes", "<script type=\"module\">a<b</script>",
                        List.of("tag:<script", "plain: ", "attribute:type", "punctuation:=", "string:\"module\"",
                                "tag:>", "plain:a<b", "tag:</script>")),
                Arguments.of("an unterminated script runs to the end of the block", "<script>\nlet a = 1;",
                        List.of("tag:<script>", "plain:\nlet a = 1;")),
                Arguments.of("an empty script element has no content", "<script src=\"a.js\"/> <b>",
                        List.of("tag:<script", "plain: ", "attribute:src", "punctuation:=", "string:\"a.js\"",
                                "tag:/>", "plain: ", "tag:<b>")),
                Arguments.of("XML's Style with a capital keeps its elements", "<Style> <Setter/> </Style>",
                        List.of("tag:<Style>", "plain: ", "tag:<Setter/>", "plain: ", "tag:</Style>")),
                // Unterminated, broken and no markup.
                Arguments.of("an unterminated attribute value runs to the end of the block", "<a href=\"open>\ntext",
                        List.of("tag:<a", "plain: ", "attribute:href", "punctuation:=", "string:\"open>\ntext")),
                Arguments.of("a tag that a bracket interrupts ends there", "<a <b>",
                        List.of("tag:<a", "plain: ", "tag:<b>")),
                Arguments.of("markup that is not well formed is coloured too", "<a b=>c</d",
                        List.of("tag:<a", "plain: ", "attribute:b", "punctuation:=", "tag:>", "plain:c",
                                "tag:</d")),
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("xmlCases")
    @DisplayName("the XML and HTML lexer")
    void xmlRuns(String what, String code, List<String> expected) {
        assertCodeRuns("xml", code, expected);
    }

    /**
     * The cases of the Bash lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> bashCases() {
        return Stream.of(
                // Comments.
                Arguments.of("a comment line and a comment after a command", "# setup\necho hi # done",
                        List.of("comment:# setup", "plain:\necho hi ", "comment:# done")),
                Arguments.of("a shebang", "#!/bin/bash\nls",
                        List.of("comment:#!/bin/bash", "plain:\nls")),
                Arguments.of("a comment after a separator", "a;# c",
                        List.of("plain:a", "punctuation:;", "comment:# c")),
                Arguments.of("a # inside a word or a variable starts no comment", "echo a#b $# ${#x} x#1",
                        List.of("plain:echo a#b ", "variable:$#", "plain: ", "variable:${#x}", "plain: x#1")),
                Arguments.of("a keyword in a comment stays in it", "# if then fi",
                        List.of("comment:# if then fi")),
                // Strings.
                Arguments.of("a string in single quotes, nothing in it special", "echo 'it $x \\n'",
                        List.of("plain:echo ", "string:'it $x \\n'")),
                Arguments.of("variables and escaped quotes inside double quotes stay in the string",
                        "echo \"Hello $USER ${x} \\\"q\\\"\"",
                        List.of("plain:echo ", "string:\"Hello $USER ${x} \\\"q\\\"\"")),
                Arguments.of("a keyword in a string stays in it", "echo \"if then\" 'for done'",
                        List.of("plain:echo ", "string:\"if then\"", "plain: ", "string:'for done'")),
                Arguments.of("a string with escapes after a dollar sign", "echo $'a\\'b' x",
                        List.of("plain:echo ", "string:$'a\\'b'", "plain: x")),
                Arguments.of("an unterminated string runs to the end of the block", "echo \"open\nfi",
                        List.of("plain:echo ", "string:\"open\nfi")),
                Arguments.of("an unterminated string in single quotes too", "echo 'open\nfi",
                        List.of("plain:echo ", "string:'open\nfi")),
                Arguments.of("escaped characters outside quotes are plain", "echo \\$HOME \\\" \\# x",
                        List.of("plain:echo \\$HOME \\\" \\# x")),
                // Variables.
                Arguments.of("variables by name, in braces, positional and special",
                        "$NAME ${a:-b} $1 $? $# $@ $$ $0",
                        List.of("variable:$NAME", "plain: ", "variable:${a:-b}", "plain: ", "variable:$1",
                                "plain: ", "variable:$?", "plain: ", "variable:$#", "plain: ", "variable:$@",
                                "plain: ", "variable:$$", "plain: ", "variable:$0")),
                Arguments.of("a variable in a word", "cp $SRC/a.txt file_$i.bak",
                        List.of("plain:cp ", "variable:$SRC", "plain:/a.txt file_", "variable:$i", "plain:.bak")),
                Arguments.of("braces inside braces", "${a:-${b}} x",
                        List.of("variable:${a:-${b}}", "plain: x")),
                Arguments.of("an unterminated variable in braces runs to the end of the block", "${a\nb",
                        List.of("variable:${a\nb")),
                Arguments.of("a lone dollar sign is plain", "echo $ 5",
                        List.of("plain:echo $ ", "number:5")),
                // Keywords and functions.
                Arguments.of("if, then, elif, else and fi", "if a; then b; elif c; then d; else e; fi",
                        List.of("keyword:if", "plain: a", "punctuation:;", "plain: ", "keyword:then", "plain: b",
                                "punctuation:;", "plain: ", "keyword:elif", "plain: c", "punctuation:;", "plain: ",
                                "keyword:then", "plain: d", "punctuation:;", "plain: ", "keyword:else", "plain: e",
                                "punctuation:;", "plain: ", "keyword:fi")),
                Arguments.of("for, in, do and done", "for f in *.txt; do echo $f; done",
                        List.of("keyword:for", "plain: f ", "keyword:in", "plain: *.txt", "punctuation:;", "plain: ",
                                "keyword:do", "plain: echo ", "variable:$f", "punctuation:;", "plain: ",
                                "keyword:done")),
                Arguments.of("while and until", "while read l; do :; done\nuntil x; do y; done",
                        List.of("keyword:while", "plain: read l", "punctuation:;", "plain: ", "keyword:do",
                                "plain: :", "punctuation:;", "plain: ", "keyword:done", "plain:\n", "keyword:until",
                                "plain: x", "punctuation:;", "plain: ", "keyword:do", "plain: y", "punctuation:;",
                                "plain: ", "keyword:done")),
                Arguments.of("case, in and esac", "case $1 in\n  a|b) x;;\n  *) y;;\nesac",
                        List.of("keyword:case", "plain: ", "variable:$1", "plain: ", "keyword:in", "plain:\n  a",
                                "operator:|", "plain:b", "punctuation:)", "plain: x", "punctuation:;;",
                                "plain:\n  *", "punctuation:)", "plain: y", "punctuation:;;", "plain:\n",
                                "keyword:esac")),
                Arguments.of("select, time and coproc, the other reserved words",
                        "select x in a b; do echo $x; done\ntime make\ncoproc worker { cat; }\ntimeout 5 select2",
                        List.of("keyword:select", "plain: x ", "keyword:in", "plain: a b", "punctuation:;",
                                "plain: ", "keyword:do", "plain: echo ", "variable:$x", "punctuation:;", "plain: ",
                                "keyword:done", "plain:\n", "keyword:time", "plain: make\n", "keyword:coproc",
                                "plain: worker ", "punctuation:{", "plain: cat", "punctuation:;", "plain: ",
                                "punctuation:}", "plain:\ntimeout ", "number:5", "plain: select2")),
                Arguments.of("a function's definition with the keyword, and return",
                        "function build {\n  return 0\n}",
                        List.of("keyword:function", "plain: ", "function:build", "plain: ", "punctuation:{",
                                "plain:\n  ", "keyword:return", "plain: ", "number:0", "plain:\n", "punctuation:}")),
                Arguments.of("a function's definition with brackets", "build() {\n  make\n}\nmy-func ()",
                        List.of("function:build", "punctuation:()", "plain: ", "punctuation:{", "plain:\n  make\n",
                                "punctuation:}", "plain:\n", "function:my-func", "plain: ", "punctuation:()")),
                Arguments.of("a keyword inside a longer word is no keyword", "fi.txt done-list in_x ./do",
                        List.of("plain:fi.txt done-list in_x ./do")),
                // Numbers, operators and punctuation.
                Arguments.of("numbers are whole words", "sleep 0.5; head -n 10 a1 -1 1.2.3 2>&1",
                        List.of("plain:sleep ", "number:0.5", "punctuation:;", "plain: head -n ", "number:10",
                                "plain: a1 -1 1.2.3 ", "number:2", "operator:>&", "number:1")),
                Arguments.of("operators", "a && b || c | d > f 2>> g < h; x=1; ! y",
                        List.of("plain:a ", "operator:&&", "plain: b ", "operator:||", "plain: c ", "operator:|",
                                "plain: d ", "operator:>", "plain: f ", "number:2", "operator:>>", "plain: g ",
                                "operator:<", "plain: h", "punctuation:;", "plain: x", "operator:=", "number:1",
                                "punctuation:;", "plain: ", "operator:!", "plain: y")),
                Arguments.of("punctuation", "(cd a; [ -d b ] && [[ c ]]) { d; }",
                        List.of("punctuation:(", "plain:cd a", "punctuation:;", "plain: ", "punctuation:[",
                                "plain: -d b ", "punctuation:]", "plain: ", "operator:&&", "plain: ",
                                "punctuation:[[", "plain: c ", "punctuation:]])", "plain: ", "punctuation:{",
                                "plain: d", "punctuation:;", "plain: ", "punctuation:}")),
                Arguments.of("command substitution and arithmetic", "d=$(date) n=$((1 + 2)) t=`pwd`",
                        List.of("plain:d", "operator:=", "punctuation:$(", "plain:date", "punctuation:)",
                                "plain: n", "operator:=", "punctuation:$((", "number:1", "plain: + ", "number:2",
                                "punctuation:))", "plain: t", "operator:=", "punctuation:`", "plain:pwd",
                                "punctuation:`")),
                // Here-documents.
                Arguments.of("a here-document, a quote in it opening no string", "cat <<EOF\nIt's $HOME\nEOF\nls",
                        List.of("plain:cat ", "operator:<<", "string:EOF", "plain:\n", "string:It's $HOME\nEOF",
                                "plain:\nls")),
                Arguments.of("a here-document with a quoted word and a command after it",
                        "cat <<'EOF' > out\nIt's\nEOF",
                        List.of("plain:cat ", "operator:<<", "string:'EOF'", "plain: ", "operator:>",
                                "plain: out\n", "string:It's\nEOF")),
                Arguments.of("a here-document whose last line may be indented by tabs", "cat <<-END\n\tx\n\tEND\nls",
                        List.of("plain:cat ", "operator:<<-", "string:END", "plain:\n", "string:\tx\n\tEND",
                                "plain:\nls")),
                Arguments.of("an unterminated here-document runs to the end of the block", "cat <<EOF\nopen\nls",
                        List.of("plain:cat ", "operator:<<", "string:EOF", "plain:\n", "string:open\nls")),
                Arguments.of("a here-string and a shift are no here-documents", "cat <<< \"$x\"; echo $((1 << 2))",
                        List.of("plain:cat ", "operator:<<<", "plain: ", "string:\"$x\"", "punctuation:;",
                                "plain: echo ", "punctuation:$((", "number:1", "plain: ", "operator:<<", "plain: ",
                                "number:2", "punctuation:))")),
                // Code that does not run, and no code.
                Arguments.of("code that does not run is coloured too", "if then \"",
                        List.of("keyword:if", "plain: ", "keyword:then", "plain: ", "string:\"")),
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bashCases")
    @DisplayName("the Bash lexer")
    void bashRuns(String what, String code, List<String> expected) {
        assertCodeRuns("bash", code, expected);
    }

    /**
     * The cases of the PowerShell lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> powerShellCases() {
        return Stream.of(
                // Comments.
                Arguments.of("a comment line and a comment after a command", "# setup\nGet-Date # now",
                        List.of("comment:# setup", "plain:\n", "function:Get-Date", "plain: ", "comment:# now")),
                Arguments.of("a block comment over lines", "<# help\n  more #>\nexit",
                        List.of("comment:<# help\n  more #>", "plain:\n", "keyword:exit")),
                Arguments.of("a hash inside a word starts no comment", "echo a#b",
                        List.of("plain:echo a#b")),
                // Strings.
                Arguments.of("strings in single and double quotes, with a doubled quote and a backtick",
                        "'it''s' \"a `\" b\"",
                        List.of("string:'it''s'", "plain: ", "string:\"a `\" b\"")),
                Arguments.of("a variable and a subexpression with quotes stay in a double-quoted string",
                        "\"Hi $name, $(Get-Date -f \"yyyy\")\"",
                        List.of("string:\"Hi $name, $(Get-Date -f \"yyyy\")\"")),
                Arguments.of("here-strings in double and single quotes, quotes inside them",
                        "$t = @\"\nLine \"one\"\n\"@\n$u = @'\nx\n'@",
                        List.of("variable:$t", "plain: ", "operator:=", "plain: ", "string:@\"\nLine \"one\"\n\"@",
                                "plain:\n", "variable:$u", "plain: ", "operator:=", "plain: ", "string:@'\nx\n'@")),
                // Variables and literals.
                Arguments.of("variables with a scope or a drive, in braces, automatic and splatted",
                        "$env:PATH $script:n ${my var} $_ $? @args",
                        List.of("variable:$env:PATH", "plain: ", "variable:$script:n", "plain: ",
                                "variable:${my var}", "plain: ", "variable:$_", "plain: ", "variable:$?", "plain: ",
                                "variable:@args")),
                Arguments.of("$true, $false and $null in any case are literals", "$true, $False, $NULL",
                        List.of("literal:$true", "punctuation:,", "plain: ", "literal:$False", "punctuation:,",
                                "plain: ", "literal:$NULL")),
                // Keywords.
                Arguments.of("keywords in any case", "If ($a) { return }",
                        List.of("keyword:If", "plain: ", "punctuation:(", "variable:$a", "punctuation:)", "plain: ",
                                "punctuation:{", "plain: ", "keyword:return", "plain: ", "punctuation:}")),
                Arguments.of("in is a keyword in a foreach", "foreach ($f in $files)",
                        List.of("keyword:foreach", "plain: ", "punctuation:(", "variable:$f", "plain: ", "keyword:in",
                                "plain: ", "variable:$files", "punctuation:)")),
                // Operators and parameters.
                Arguments.of("operators written as a dash and a word, in any case and with a case prefix",
                        "$a -eq 1 -and $b -NotMatch 'x' -ceq $c",
                        List.of("variable:$a", "plain: ", "operator:-eq", "plain: ", "number:1", "plain: ",
                                "operator:-and", "plain: ", "variable:$b", "plain: ", "operator:-NotMatch", "plain: ",
                                "string:'x'", "plain: ", "operator:-ceq", "plain: ", "variable:$c")),
                Arguments.of("a command's parameters", "Get-ChildItem -Path C:\\temp -Recurse -ErrorAction Stop",
                        List.of("function:Get-ChildItem", "plain: ", "attribute:-Path", "plain: C:\\temp ",
                                "attribute:-Recurse", "plain: ", "attribute:-ErrorAction", "plain: Stop")),
                Arguments.of("the operators of characters, a dash alone among them",
                        "$n = $n - 1; $s -= 2; $i++",
                        List.of("variable:$n", "plain: ", "operator:=", "plain: ", "variable:$n", "plain: ",
                                "operator:-", "plain: ", "number:1", "punctuation:;", "plain: ", "variable:$s",
                                "plain: ", "operator:-=", "plain: ", "number:2", "punctuation:;", "plain: ",
                                "variable:$i", "operator:++")),
                // Types, attributes and brackets.
                Arguments.of("types in brackets, an array type and a generic one, and a static method",
                        "[string]$s = [int[]]$a + [System.Collections.Generic.List[string]]::new()",
                        List.of("punctuation:[", "type:string", "punctuation:]", "variable:$s", "plain: ",
                                "operator:=", "plain: ", "punctuation:[", "type:int[]", "punctuation:]",
                                "variable:$a", "plain: ", "operator:+", "plain: ", "punctuation:[",
                                "type:System.Collections.Generic.List[string]", "punctuation:]::", "function:new",
                                "punctuation:()")),
                Arguments.of("attributes, and a bracket that indexes",
                        "[CmdletBinding()]\nparam([Parameter(Mandatory)] $x)\n$list[0]",
                        List.of("punctuation:[", "annotation:CmdletBinding", "punctuation:()]", "plain:\n",
                                "keyword:param", "punctuation:([", "annotation:Parameter", "punctuation:(",
                                "plain:Mandatory", "punctuation:)]", "plain: ", "variable:$x", "punctuation:)",
                                "plain:\n", "variable:$list", "punctuation:[", "number:0", "punctuation:]")),
                Arguments.of("arrays and hash tables", "$h = @{ a = 1 }; $l = @(1, 2)",
                        List.of("variable:$h", "plain: ", "operator:=", "plain: ", "punctuation:@{", "plain: a ",
                                "operator:=", "plain: ", "number:1", "plain: ", "punctuation:};", "plain: ",
                                "variable:$l", "plain: ", "operator:=", "plain: ", "punctuation:@(", "number:1",
                                "punctuation:,", "plain: ", "number:2", "punctuation:)")),
                // Commands and functions.
                Arguments.of("a function's definition, commands where a command starts and a method",
                        "function Get-Stuff { }\nGet-Stuff | ForEach-Object { $_.Trim() }\ncd my-folder",
                        List.of("keyword:function", "plain: ", "function:Get-Stuff", "plain: ", "punctuation:{",
                                "plain: ", "punctuation:}", "plain:\n", "function:Get-Stuff", "plain: ", "operator:|",
                                "plain: ", "function:ForEach-Object", "plain: ", "punctuation:{", "plain: ",
                                "variable:$_", "plain:.", "function:Trim", "punctuation:()", "plain: ",
                                "punctuation:}", "plain:\ncd my-folder")),
                // Numbers.
                Arguments.of("numbers: hexadecimal, with an exponent, a multiplier, a range; a version is none",
                        "1 0x1F 2.5e3 10KB 1..5 v1.2",
                        List.of("number:1", "plain: ", "number:0x1F", "plain: ", "number:2.5e3", "plain: ",
                                "number:10KB", "plain: ", "number:1", "operator:..", "number:5", "plain: v1.2")),
                // Escapes, code that does not run, and no code.
                Arguments.of("a backtick takes the character after it, a line's end too", "Write-Host a`$b `\n  c",
                        List.of("function:Write-Host", "plain: a`$b `\n  c")),
                Arguments.of("an unterminated string runs to the end of the block", "Write-Host \"open\nmore",
                        List.of("function:Write-Host", "plain: ", "string:\"open\nmore")),
                Arguments.of("an unterminated block comment runs to the end of the block", "x <# open\nmore",
                        List.of("plain:x ", "comment:<# open\nmore")),
                Arguments.of("code that does not run is coloured too", "if ( \"",
                        List.of("keyword:if", "plain: ", "punctuation:(", "plain: ", "string:\"")),
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("powerShellCases")
    @DisplayName("the PowerShell lexer")
    void powerShellRuns(String what, String code, List<String> expected) {
        assertCodeRuns("powershell", code, expected);
    }

    /**
     * The cases of the Markdown lexer: what a case shows, the code, and the runs of its block.
     */
    static Stream<Arguments> markdownCases() {
        return Stream.of(
                // Headings.
                Arguments.of("ATX headings, the whole line", "# Title\n## Sub ##\nText",
                        List.of("heading:# Title", "plain:\n", "heading:## Sub ##", "plain:\nText")),
                Arguments.of("a heading's emphasis, code and links stay in it", "### A *b* `c` [d](e)",
                        List.of("heading:### A *b* `c` [d](e)")),
                Arguments.of("an empty heading", "#\n",
                        List.of("heading:#", "plain:\n")),
                Arguments.of("no heading without a space after the hashes, nor with seven", "#tag\n####### seven",
                        List.of("plain:#tag\n####### seven")),
                // Emphasis.
                Arguments.of("emphasis with stars and underscores", "a *em* and _em_ b",
                        List.of("plain:a ", "emphasis:*em*", "plain: and ", "emphasis:_em_", "plain: b")),
                Arguments.of("strong emphasis, and both", "**strong** __strong__ ***both***",
                        List.of("emphasis:**strong**", "plain: ", "emphasis:__strong__", "plain: ",
                                "emphasis:***both***")),
                Arguments.of("emphasis inside strong emphasis is one piece", "**a *b* c** d",
                        List.of("emphasis:**a *b* c**", "plain: d")),
                Arguments.of("stars with spaces and underscores inside words are no emphasis",
                        "2 * 3 * 4 and snake_case_name",
                        List.of("plain:2 * 3 * 4 and snake_case_name")),
                Arguments.of("an unclosed emphasis is plain", "a *open and **more",
                        List.of("plain:a *open and **more")),
                Arguments.of("escaped stars and backticks are plain", "\\*not em\\* and \\`not code\\`",
                        List.of("plain:\\*not em\\* and \\`not code\\`")),
                // Code.
                Arguments.of("inline code, also with a backtick inside", "a `code` b ``x ` y`` c",
                        List.of("plain:a ", "code:`code`", "plain: b ", "code:``x ` y``", "plain: c")),
                Arguments.of("emphasis inside inline code stays in it", "`*x*` and `[a](b)`",
                        List.of("code:`*x*`", "plain: and ", "code:`[a](b)`")),
                Arguments.of("an unclosed backtick is plain", "a ` b",
                        List.of("plain:a ` b")),
                Arguments.of("a fenced block, whole, its content not coloured as its language",
                        "```java\nint x; // *no*\n# no heading\n```\nafter",
                        List.of("code:```java\nint x; // *no*\n# no heading\n```", "plain:\nafter")),
                Arguments.of("a fence of tildes", "~~~\n- no list\n~~~",
                        List.of("code:~~~\n- no list\n~~~")),
                Arguments.of("a shorter fence does not close a longer one", "````\n```\n````\nx",
                        List.of("code:````\n```\n````", "plain:\nx")),
                Arguments.of("an unterminated fence runs to the end of the block", "```\n# x\n- y",
                        List.of("code:```\n# x\n- y")),
                Arguments.of("a fence inside a list item", "- a\n\n  ```\n  # x\n  ```",
                        List.of("list:-", "plain: a\n\n  ", "code:```\n  # x\n  ```")),
                // Links.
                Arguments.of("links and images", "see [the docs](https://x.org/a_b) and ![logo](a.png \"Logo\")",
                        List.of("plain:see ", "link:[the docs](https://x.org/a_b)", "plain: and ",
                                "link:![logo](a.png \"Logo\")")),
                Arguments.of("brackets in a link's text and its address", "[a [b] c](x(1)) d",
                        List.of("link:[a [b] c](x(1))", "plain: d")),
                Arguments.of("reference links", "[text][ref] and [text][]",
                        List.of("link:[text][ref]", "plain: and ", "link:[text][]")),
                Arguments.of("brackets that make no link are plain, emphasis in them coloured",
                        "[just *brackets*] and [open(x)",
                        List.of("plain:[just ", "emphasis:*brackets*", "plain:] and [open(x)")),
                Arguments.of("addresses in angle brackets, an HTML tag plain",
                        "<https://example.org> and <mail@example.org> but <b>",
                        List.of("link:<https://example.org>", "plain: and ", "link:<mail@example.org>",
                                "plain: but <b>")),
                Arguments.of("a link reference definition, the whole line", "[ref]: https://x.org \"Title\"\nx",
                        List.of("link:[ref]: https://x.org \"Title\"", "plain:\nx")),
                // Lists.
                Arguments.of("list markers", "- a\n* b\n+ c\n1. d\n10) e\n  - nested",
                        List.of("list:-", "plain: a\n", "list:*", "plain: b\n", "list:+", "plain: c\n", "list:1.",
                                "plain: d\n", "list:10)", "plain: e\n  ", "list:-", "plain: nested")),
                Arguments.of("a list item's text is coloured", "- *x* `y`",
                        List.of("list:-", "plain: ", "emphasis:*x*", "plain: ", "code:`y`")),
                Arguments.of("a task list's box is plain", "- [ ] open\n- [x] done",
                        List.of("list:-", "plain: [ ] open\n", "list:-", "plain: [x] done")),
                Arguments.of("a marker without a space after it is no list marker", "-x\n1.5 m",
                        List.of("plain:-x\n1.5 m")),
                // Quotes and thematic breaks.
                Arguments.of("quote lines, the whole line", "> quoted *x*\n> > nested\ntext",
                        List.of("quote:> quoted *x*", "plain:\n", "quote:> > nested", "plain:\ntext")),
                Arguments.of("thematic breaks, not list items", "---\n* * *\n___\n- - -",
                        List.of("punctuation:---", "plain:\n", "punctuation:* * *", "plain:\n", "punctuation:___",
                                "plain:\n", "punctuation:- - -")),
                // HTML, broken Markdown and no Markdown.
                Arguments.of("HTML stays plain, Markdown around it coloured", "<div>*x*</div>",
                        List.of("plain:<div>", "emphasis:*x*", "plain:</div>")),
                Arguments.of("Markdown that is not well formed is coloured too", "**a [b](c `d\n# e",
                        List.of("plain:**a [b](c `d\n", "heading:# e")),
                Arguments.of("an empty block is one empty run", "",
                        List.of("plain:")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("markdownCases")
    @DisplayName("the Markdown lexer")
    void markdownRuns(String what, String code, List<String> expected) {
        assertCodeRuns("markdown", code, expected);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "xml   | <a/>  | tag:<a/>",
            "html  | <a/>  | tag:<a/>",
            "xhtml | <a/>  | tag:<a/>",
            "bash  | fi    | keyword:fi",
            "sh    | fi    | keyword:fi",
            "shell | fi    | keyword:fi",
            "powershell | if | keyword:if",
            "pwsh  | if    | keyword:if",
            "ps1   | if    | keyword:if",
            "markdown | # T | heading:# T",
            "md    | # T   | heading:# T"})
    @DisplayName("each language is known by its name and its short names")
    void namesAndShortNames(String language, String code, String expected) {
        assertCodeRuns(language, code, List.of(expected));
    }

    @Test
    @DisplayName("by default the pieces of markup, shell and Markdown take their colours or those they fall back to")
    void defaultColors() {
        MarkdownView view = layOut(new MarkdownView("```html\n<a href=\"x\">\n```\n\n```bash\necho $HOME\n```\n\n"
                + "````md\n# Head\n- [l](u) `c` *e* x\n> q\n````"));
        List<Node> blocks = new ArrayList<>(view.lookupAll(".markdown-code-block"));
        Map<String, Text> html = codeRunsByText(blocks.get(0));
        Map<String, Text> bash = codeRunsByText(blocks.get(1));
        Map<String, Text> markdown = codeRunsByText(blocks.get(2));

        assertEquals(Color.web("#ff8fa3"), html.get("<a").getFill(), "tag");
        assertEquals(Color.web("#ff9e64"), html.get("href").getFill(), "attribute, as number");
        assertEquals(Color.web("#e0a84c"), html.get("\"x\"").getFill(), "string");
        assertEquals(Color.web("#ff8fa3"), bash.get("$HOME").getFill(), "variable");
        assertEquals(Color.web("#5d9bff"), markdown.get("# Head").getFill(), "heading, as function");
        assertEquals(Color.web("#c88bff"), markdown.get("-").getFill(), "list, as keyword");
        assertEquals(Color.web("#7b8cff"), markdown.get("[l](u)").getFill(), "link, as property");
        assertEquals(Color.web("#e0a84c"), markdown.get("`c`").getFill(), "code, as string");
        assertEquals(Color.web("#8a90a8"), markdown.get("> q").getFill(), "quote, as comment");
        assertEquals(markdown.get(" x\n").getFill(), markdown.get("*e*").getFill(), "emphasis, as code text");
    }
}
