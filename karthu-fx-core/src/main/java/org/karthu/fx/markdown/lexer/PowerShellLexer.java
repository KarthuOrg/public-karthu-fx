package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The lexer of PowerShell scripts: a scanner over the characters, without parsing, so a script that does not run is
 * coloured too. Keywords, operators and the literals are found in any case, as PowerShell finds them. It marks:
 * <ul>
 *   <li>comments: a {@code #} at the start of a word, to the line's end, and a block comment {@code <# ... #>}, also
 *       over lines (comment); unterminated, a block comment runs to the end of the code;</li>
 *   <li>strings in single and double quotes, a doubled quote inside them included, and here-strings, from
 *       {@code @'} or {@code @"} at a line's end through {@code '@} or {@code "@} at the start of a line (string); a
 *       backtick in a double-quoted string takes the character after it, a variable or a subexpression {@code $(...)}
 *       inside it stays in the string's piece, quotes in the subexpression included, and an unterminated string runs to
 *       the end of the code;</li>
 *   <li>variables: {@code $name}, with a scope or a drive ({@code $env:PATH}, {@code $script:count}),
 *       {@code ${any name}}, {@code $_} and the automatic {@code $$}, {@code $?} and {@code $^}, and a splatted
 *       {@code @name} (variable); {@code $true}, {@code $false} and {@code $null} (literal);</li>
 *   <li>the keywords {@code begin}, {@code break}, {@code catch}, {@code class}, {@code continue}, {@code data},
 *       {@code do}, {@code dynamicparam}, {@code else}, {@code elseif}, {@code end}, {@code enum}, {@code exit},
 *       {@code filter}, {@code finally}, {@code for}, {@code foreach}, {@code function}, {@code if}, {@code in},
 *       {@code param}, {@code process}, {@code return}, {@code switch}, {@code throw}, {@code trap}, {@code try},
 *       {@code until}, {@code using} and {@code while}, each as a whole word (keyword);</li>
 *   <li>the operators written as a dash and a word, such as {@code -eq}, {@code -like}, {@code -match},
 *       {@code -replace}, {@code -and}, {@code -not}, {@code -split}, {@code -join} and {@code -f}, those that compare
 *       also with their prefix {@code c} or {@code i} ({@code -ceq}, {@code -imatch}), and the operators
 *       {@code = + - * / % ! < > | &} and {@code ..} (operator);</li>
 *   <li>a command's parameters, a dash and a word that is no operator, such as {@code -Recurse} (attribute);</li>
 *   <li>a type in brackets, such as {@code [string]}, {@code [int[]]} or {@code [System.IO.File]}, generic arguments
 *       included (type), and an attribute, a name in brackets before {@code (}, such as {@code [CmdletBinding()]}
 *       (annotation);</li>
 *   <li>a command in the form verb-noun, such as {@code Get-ChildItem}, where a command starts: at the start of a
 *       line or after {@code | ; ( { = &}; the name of a function's definition after {@code function} or
 *       {@code filter}; a name before {@code (}, a method after {@code .} or {@code ::} too (function);</li>
 *   <li>numbers as whole words, decimal, hexadecimal ({@code 0x1F}) and with an exponent, with the multipliers
 *       {@code kb} to {@code pb} (number), and punctuation (brackets, braces, {@code ;}, {@code ,}, {@code ::}, the
 *       {@code $} of {@code $(} and the {@code @} of {@code @(} and <code>@&#123;</code>).</li>
 * </ul>
 * A backtick outside quotes takes the character after it as plain text, as at a line's end. Adjacent pieces of one
 * category are one piece. Everything else, a command's arguments and white space, is plain.
 */
final class PowerShellLexer implements CodeLexer {

    private static final Set<String> KEYWORDS = Set.of("begin", "break", "catch", "class", "continue", "data", "do",
            "dynamicparam", "else", "elseif", "end", "enum", "exit", "filter", "finally", "for", "foreach",
            "function", "if", "in", "param", "process", "return", "switch", "throw", "trap", "try", "until",
            "using", "while");

    /**
     * The operators written as a dash and a word that take no case prefix.
     */
    private static final Set<String> OPERATOR_WORDS = Set.of("and", "or", "xor", "not", "band", "bor", "bxor",
            "bnot", "shl", "shr", "is", "isnot", "as", "join", "f");

    /**
     * The operators written as a dash and a word that also take the prefix {@code c} (case-sensitive) or {@code i}
     * (case-insensitive).
     */
    private static final Set<String> CASE_OPERATOR_WORDS = Set.of("eq", "ne", "gt", "ge", "lt", "le", "like",
            "notlike", "match", "notmatch", "replace", "contains", "notcontains", "in", "notin", "split");

    private static final Set<String> LITERALS = Set.of("true", "false", "null");

    private static final Pattern NUMBER = Pattern.compile(
            "(?i)(?:0x[0-9a-f]+|[0-9]+(?:\\.[0-9]+)?(?:e[+-]?[0-9]+)?)(?:kb|mb|gb|tb|pb)?[ld]?");

    /**
     * A command in the form verb-noun.
     */
    private static final Pattern VERB_NOUN = Pattern.compile("[A-Za-z]+-[A-Za-z][A-Za-z0-9]*");

    private static final String OPERATORS = "=+*/%!<>|&";
    private static final String PUNCTUATION = "(){}[];,";

    /**
     * The characters after which a command starts, beside the start of a line.
     */
    private static final String COMMAND_STARTS = "|;({=&";

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        int length = code.length();
        int position = 0;
        while (position < length) {
            char character = code.charAt(position);
            if (code.startsWith("<#", position)) {
                position = blockComment(code, position, pieces);
            } else if (character == '#' && startsWord(code, position)) {
                int end = Scanning.lineEnd(code, position);
                pieces.add(position, end, CodeCategory.COMMENT);
                position = end;
            } else if (character == '@' && opensHereString(code, position)) {
                position = hereString(code, position, pieces);
            } else if (character == '\'') {
                position = singleQuoted(code, position, pieces);
            } else if (character == '"') {
                position = doubleQuoted(code, position, pieces);
            } else if (character == '$') {
                position = dollar(code, position, pieces);
            } else if (character == '@') {
                position = at(code, position, pieces);
            } else if (character == '`') {
                position = Math.min(length, position + 2);
            } else if (character == '[') {
                position = bracket(code, position, pieces);
            } else if (character == '-') {
                position = dash(code, position, pieces);
            } else if (code.startsWith("::", position)) {
                pieces.add(position, position + 2, CodeCategory.PUNCTUATION);
                position = member(code, position + 2, pieces);
            } else if (character == '.' && isMemberAccess(code, position)) {
                position = member(code, position + 1, pieces);
            } else if (OPERATORS.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.OPERATOR);
                position++;
            } else if (PUNCTUATION.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                position++;
            } else if (isWordCharacter(character)) {
                position = word(code, position, pieces);
            } else {
                position++;
            }
        }
        return pieces.toList();
    }

    /**
     * A word: a keyword, a number or a range of numbers, a command, a name before {@code (}, else plain. After the
     * keywords {@code function} and {@code filter}, the next word is the function's name.
     *
     * @return the position after the word, or after the function's name
     */
    private static int word(String code, int start, Pieces pieces) {
        int end = wordEnd(code, start);
        String word = code.substring(start, end);
        String lower = word.toLowerCase(Locale.ROOT);
        boolean whole = startsWord(code, start) && (end >= code.length() || !isWordCharacter(code.charAt(end)));
        int range = word.indexOf("..");
        if (whole && KEYWORDS.contains(lower)) {
            pieces.add(start, end, CodeCategory.KEYWORD);
            if (lower.equals("function") || lower.equals("filter")) {
                return functionName(code, end, pieces);
            }
        } else if (range > 0 && isNumber(word.substring(0, range)) && isBoundaryBefore(code, start)) {
            pieces.add(start, start + range, CodeCategory.NUMBER);
            pieces.add(start + range, start + range + 2, CodeCategory.OPERATOR);
            String upper = word.substring(range + 2);
            if (isNumber(upper)) {
                pieces.add(start + range + 2, end, CodeCategory.NUMBER);
            }
        } else if (isNumber(word) && isBoundaryBefore(code, start)) {
            pieces.add(start, end, CodeCategory.NUMBER);
        } else if (VERB_NOUN.matcher(word).matches() && startsCommand(code, start)) {
            pieces.add(start, end, CodeCategory.FUNCTION);
        } else if (isName(word) && Scanning.bracketFollows(code, end)) {
            pieces.add(start, end, CodeCategory.FUNCTION);
        }
        return end;
    }

    /**
     * The name after the keyword {@code function} or {@code filter}, as function; it may hold dashes, as a command's
     * name does.
     *
     * @return the position after the name, or after the spaces if no name follows
     */
    private static int functionName(String code, int start, Pieces pieces) {
        int position = start;
        while (position < code.length() && (code.charAt(position) == ' ' || code.charAt(position) == '\t')) {
            position++;
        }
        if (position >= code.length() || !isWordCharacter(code.charAt(position))) {
            return position;
        }
        int end = wordEnd(code, position);
        if (isName(code.substring(position, end))) {
            pieces.add(position, end, CodeCategory.FUNCTION);
        }
        return end;
    }

    /**
     * A member after {@code .} or {@code ::}: its name, as function when {@code (} follows, a method; else plain.
     *
     * @param start the first character after the {@code .} or {@code ::}
     * @return the position after the name
     */
    private static int member(String code, int start, Pieces pieces) {
        int end = start;
        while (end < code.length() && (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
            end++;
        }
        if (end > start && Scanning.bracketFollows(code, end)) {
            pieces.add(start, end, CodeCategory.FUNCTION);
        }
        return end;
    }

    /**
     * Whether a {@code .} reaches into what comes before it, a member of a variable, a call or a bracket, followed by
     * a name: {@code $file.Name}, {@code (Get-Date).AddDays(1)}.
     */
    private static boolean isMemberAccess(String code, int position) {
        if (position == 0 || position + 1 >= code.length()) {
            return false;
        }
        char before = code.charAt(position - 1);
        char after = code.charAt(position + 1);
        return (Character.isLetterOrDigit(before) || before == '_' || before == ')' || before == ']'
                || before == '}') && (Character.isLetter(after) || after == '_');
    }

    /**
     * What follows a dash at the start of a word: an operator such as {@code -eq} or a parameter such as
     * {@code -Recurse}; else the dash is an operator, with a dash or an equals sign after it ({@code --},
     * {@code -=}).
     *
     * @return the position after it
     */
    private static int dash(String code, int start, Pieces pieces) {
        int next = start + 1;
        if (next < code.length() && Character.isLetter(code.charAt(next)) && startsWord(code, start)) {
            int end = next;
            while (end < code.length() && (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
                end++;
            }
            String word = code.substring(next, end).toLowerCase(Locale.ROOT);
            pieces.add(start, end, isOperatorWord(word) ? CodeCategory.OPERATOR : CodeCategory.ATTRIBUTE);
            return end;
        }
        if (next < code.length() && (code.charAt(next) == '-' || code.charAt(next) == '=')) {
            pieces.add(start, next + 1, CodeCategory.OPERATOR);
            return next + 1;
        }
        pieces.add(start, next, CodeCategory.OPERATOR);
        return next;
    }

    /**
     * Whether the word after a dash names an operator, with or without its case prefix.
     */
    private static boolean isOperatorWord(String word) {
        if (OPERATOR_WORDS.contains(word) || CASE_OPERATOR_WORDS.contains(word)) {
            return true;
        }
        return word.length() > 1 && (word.charAt(0) == 'c' || word.charAt(0) == 'i')
                && CASE_OPERATOR_WORDS.contains(word.substring(1));
    }

    /**
     * What a {@code [} opens: a type, such as {@code [string]} or {@code [List[int]]}, as type between its brackets;
     * an attribute, a name before {@code (}, as annotation; else the bracket is punctuation, as one that indexes
     * ({@code $list[0]}).
     *
     * @return the position after the closing bracket of a type, after the name of an attribute, else after the
     *         bracket
     */
    private static int bracket(String code, int start, Pieces pieces) {
        pieces.add(start, start + 1, CodeCategory.PUNCTUATION);
        if (indexes(code, start)) {
            return start + 1;
        }
        int nameStart = start + 1;
        int position = nameStart;
        if (position >= code.length() || !(Character.isLetter(code.charAt(position)) || code.charAt(position) == '_')) {
            return start + 1;
        }
        while (position < code.length() && (Character.isLetterOrDigit(code.charAt(position))
                || code.charAt(position) == '_' || code.charAt(position) == '.')) {
            position++;
        }
        int nameEnd = position;
        if (position < code.length() && code.charAt(position) == '(') {
            pieces.add(nameStart, nameEnd, CodeCategory.ANNOTATION);
            return nameEnd;
        }
        // Array brackets and generic arguments belong to the type: [int[]], [List[string]].
        while (position < code.length() && code.charAt(position) == '[') {
            int close = matchingBracket(code, position);
            if (close < 0) {
                break;
            }
            position = close + 1;
        }
        if (position < code.length() && code.charAt(position) == ']') {
            pieces.add(nameStart, position, CodeCategory.TYPE);
            pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
            return position + 1;
        }
        return start + 1;
    }

    /**
     * Whether a bracket indexes what comes right before it: a name, a variable, a call or another bracket.
     */
    private static boolean indexes(String code, int position) {
        if (position == 0) {
            return false;
        }
        char before = code.charAt(position - 1);
        return Character.isLetterOrDigit(before) || before == '_' || before == ')' || before == ']' || before == '}';
    }

    /**
     * The bracket that closes the one at a position, brackets inside counted, on the same line.
     *
     * @return its position, or -1 if the line holds none
     */
    private static int matchingBracket(String code, int open) {
        int depth = 0;
        int end = Scanning.lineEnd(code, open);
        for (int position = open; position < end; position++) {
            char character = code.charAt(position);
            if (character == '[') {
                depth++;
            } else if (character == ']') {
                depth--;
                if (depth == 0) {
                    return position;
                }
            }
        }
        return -1;
    }

    /**
     * What follows a {@code $}: a variable or a literal, a variable in braces, the {@code $} of a subexpression as
     * punctuation, else a plain {@code $}.
     *
     * @return the position after it
     */
    private static int dollar(String code, int start, Pieces pieces) {
        int next = start + 1;
        if (next >= code.length()) {
            return next;
        }
        char character = code.charAt(next);
        if (character == '(') {
            pieces.add(start, next, CodeCategory.PUNCTUATION);
            return next;
        }
        if (character == '{') {
            int close = code.indexOf('}', next);
            int end = close < 0 ? code.length() : close + 1;
            pieces.add(start, end, CodeCategory.VARIABLE);
            return end;
        }
        if (Character.isLetter(character) || character == '_') {
            int end = nameEnd(code, next);
            // A scope or a drive: $env:PATH, $script:count; a colon before anything else ends the name.
            if (end + 1 < code.length() && code.charAt(end) == ':'
                    && (Character.isLetter(code.charAt(end + 1)) || code.charAt(end + 1) == '_')) {
                end = nameEnd(code, end + 1);
            }
            String name = code.substring(next, end).toLowerCase(Locale.ROOT);
            pieces.add(start, end, LITERALS.contains(name) ? CodeCategory.LITERAL : CodeCategory.VARIABLE);
            return end;
        }
        if (character == '$' || character == '?' || character == '^') {
            pieces.add(start, next + 1, CodeCategory.VARIABLE);
            return next + 1;
        }
        return next;
    }

    /**
     * What follows an {@code @} that opens no here-string: the {@code @} of an array or a hash table, {@code @(} or
     * <code>@&#123;</code>, as punctuation; a splatted variable, {@code @name}, as variable; else a plain {@code @}.
     *
     * @return the position after it
     */
    private static int at(String code, int start, Pieces pieces) {
        int next = start + 1;
        if (next >= code.length()) {
            return next;
        }
        char character = code.charAt(next);
        if (character == '(' || character == '{') {
            pieces.add(start, next, CodeCategory.PUNCTUATION);
            return next;
        }
        if ((Character.isLetter(character) || character == '_') && startsWord(code, start)) {
            int end = nameEnd(code, next);
            pieces.add(start, end, CodeCategory.VARIABLE);
            return end;
        }
        return next;
    }

    /**
     * The end of a variable's name: letters, digits and underscores.
     */
    private static int nameEnd(String code, int start) {
        int end = start;
        while (end < code.length() && (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
            end++;
        }
        return end;
    }

    /**
     * A string in single quotes, as one piece: no escapes, a doubled quote inside closes and opens again, and the two
     * pieces join; unterminated, it runs to the end of the code.
     *
     * @return the position after the string
     */
    private static int singleQuoted(String code, int start, Pieces pieces) {
        int close = code.indexOf('\'', start + 1);
        int end = close < 0 ? code.length() : close + 1;
        pieces.add(start, end, CodeCategory.STRING);
        return end;
    }

    /**
     * A string in double quotes, as one piece, up to its closing quote: a backtick takes the character after it, and
     * a subexpression {@code $(...)} inside runs to its closing bracket, brackets and quotes inside it included;
     * unterminated, the string runs to the end of the code.
     *
     * @return the position after the string
     */
    private static int doubleQuoted(String code, int start, Pieces pieces) {
        int length = code.length();
        int position = start + 1;
        int depth = 0;
        while (position < length) {
            char character = code.charAt(position);
            if (character == '`') {
                position += 2;
            } else if (code.startsWith("$(", position)) {
                depth++;
                position += 2;
            } else if (depth > 0 && character == '(') {
                depth++;
                position++;
            } else if (depth > 0 && character == ')') {
                depth--;
                position++;
            } else if (depth == 0 && character == '"') {
                position++;
                pieces.add(start, position, CodeCategory.STRING);
                return position;
            } else {
                position++;
            }
        }
        pieces.add(start, length, CodeCategory.STRING);
        return length;
    }

    /**
     * Whether an {@code @} opens a here-string: a quote after it, then nothing but spaces and tabs to the line's end.
     */
    private static boolean opensHereString(String code, int position) {
        if (position + 1 >= code.length()) {
            return false;
        }
        char quote = code.charAt(position + 1);
        if (quote != '\'' && quote != '"') {
            return false;
        }
        int lineEnd = Scanning.lineEnd(code, position);
        return lineEnd < code.length() && code.substring(position + 2, lineEnd).isBlank();
    }

    /**
     * A here-string, from its {@code @'} or {@code @"} through the line that starts with {@code '@} or {@code "@},
     * as one piece; unterminated, it runs to the end of the code.
     *
     * @return the position after its closing {@code '@} or {@code "@}
     */
    private static int hereString(String code, int start, Pieces pieces) {
        String closing = code.charAt(start + 1) + "@";
        int line = Scanning.lineEnd(code, start) + 1;
        while (line < code.length()) {
            if (code.startsWith(closing, line)) {
                int end = line + 2;
                pieces.add(start, end, CodeCategory.STRING);
                return end;
            }
            line = Scanning.lineEnd(code, line) + 1;
        }
        pieces.add(start, code.length(), CodeCategory.STRING);
        return code.length();
    }

    /**
     * A block comment, {@code <# ... #>}, as one piece; unterminated, it runs to the end of the code.
     *
     * @return the position after it
     */
    private static int blockComment(String code, int start, Pieces pieces) {
        int close = code.indexOf("#>", start + 2);
        int end = close < 0 ? code.length() : close + 2;
        pieces.add(start, end, CodeCategory.COMMENT);
        return end;
    }

    /**
     * Whether a word starts at a position: the start of the code, white space, an operator or punctuation before it.
     */
    private static boolean startsWord(String code, int position) {
        if (position == 0) {
            return true;
        }
        char before = code.charAt(position - 1);
        return Character.isWhitespace(before) || OPERATORS.indexOf(before) >= 0 || PUNCTUATION.indexOf(before) >= 0;
    }

    /**
     * Whether a number may start at a position: where a word starts, or after a dash, so that the {@code 1} of
     * {@code -1} and of {@code $a-1} is a number.
     */
    private static boolean isBoundaryBefore(String code, int position) {
        return startsWord(code, position) || code.charAt(position - 1) == '-';
    }

    /**
     * Whether a command starts at a position: at the start of a line, spaces and tabs before it only, or after one
     * of {@code | ; ( { = &}.
     */
    private static boolean startsCommand(String code, int position) {
        int before = position - 1;
        while (before >= 0 && (code.charAt(before) == ' ' || code.charAt(before) == '\t')) {
            before--;
        }
        return before < 0 || code.charAt(before) == '\n' || COMMAND_STARTS.indexOf(code.charAt(before)) >= 0;
    }

    /**
     * Whether a word is a number: decimal, hexadecimal or with an exponent, with a multiplier and a type suffix.
     */
    private static boolean isNumber(String word) {
        return !word.isEmpty() && Scanning.isDigit(word.charAt(0)) && NUMBER.matcher(word).matches();
    }

    /**
     * Whether a word can name a function: it starts with a letter or {@code _}.
     */
    private static boolean isName(String word) {
        return !word.isEmpty() && (Character.isLetter(word.charAt(0)) || word.charAt(0) == '_');
    }

    /**
     * Whether a character belongs to a plain word: anything but white space, an operator, punctuation, a quote, a
     * {@code $} or a backtick. A dash, a dot, a colon, a backslash and a {@code #} inside a word are part of it, as
     * in {@code Get-ChildItem}, {@code C:\temp\a.txt} or {@code a#b}.
     */
    private static boolean isWordCharacter(char character) {
        return !Character.isWhitespace(character) && OPERATORS.indexOf(character) < 0
                && PUNCTUATION.indexOf(character) < 0 && "'\"$`".indexOf(character) < 0;
    }

    /**
     * The end of a plain word from a position on.
     */
    private static int wordEnd(String code, int start) {
        int position = start;
        while (position < code.length() && isWordCharacter(code.charAt(position))) {
            position++;
        }
        return position;
    }
}
