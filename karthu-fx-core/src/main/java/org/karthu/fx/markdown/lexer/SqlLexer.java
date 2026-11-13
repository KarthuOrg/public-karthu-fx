package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lexer of SQL, standard SQL and PostgreSQL's: a scanner over the characters, without parsing, so SQL that does not
 * parse is coloured too. It marks:
 * <ul>
 *   <li>comments: {@code --} to the line's end and block comments {@code /* *}{@code /} (comment); an unterminated
 *       comment runs to the end of the code;</li>
 *   <li>strings in single quotes, {@code ''} a quote inside (string); an unterminated string runs to the end of the
 *       code; identifiers in double quotes stay plain, whatever they hold;</li>
 *   <li>keywords of standard SQL and PostgreSQL in any case (keyword), {@code NULL}, {@code TRUE} and {@code FALSE}
 *       in any case (literal), data types such as {@code INTEGER}, {@code VARCHAR}, {@code NUMERIC} and
 *       {@code TIMESTAMP} in any case (type), also before {@code (};</li>
 *   <li>parameters {@code :name}, {@code ?} and {@code $1} (variable);</li>
 *   <li>a name followed directly by {@code (} as a function ({@code count(*)}), unless it is a keyword or a type, or
 *       the name of a table or view: a name right after {@code TABLE}, {@code INTO}, {@code REFERENCES} or
 *       {@code VIEW}, or after {@code IF NOT EXISTS} or {@code IF EXISTS} behind one of them, qualified
 *       ({@code public.users}) or not, with white space and comments between them, stays plain, as does a name with a
 *       space before its bracket ({@code users (id)}), so that a table reads as in a database tool;</li>
 *   <li>numbers as whole words, with a fraction and an exponent;</li>
 *   <li>operators ({@code ::} among them) and punctuation (brackets, {@code ;}, {@code ,}, {@code .}).</li>
 * </ul>
 * Adjacent pieces of one category are one piece. Everything else, other names and white space, is plain.
 */
final class SqlLexer implements CodeLexer {

    /**
     * The keywords of standard SQL and PostgreSQL, in lower case. Left out: words that are mostly functions
     * ({@code count}, {@code cast}, {@code coalesce}), so that they show as functions before {@code (}, and
     * non-reserved words that are common column names ({@code name}, {@code type}, {@code level}, {@code comment},
     * {@code action}, {@code data}, {@code value}).
     */
    private static final Set<String> KEYWORDS = Set.of("abort", "add", "after", "all", "alter", "always", "analyze",
            "and", "any", "array", "as", "asc", "asymmetric", "at", "authorization", "before", "begin", "between",
            "both", "by", "cascade", "cascaded", "case", "check", "checkpoint", "cluster", "collate", "column",
            "commit", "committed", "concurrently", "conflict", "constraint", "continue", "copy", "create", "cross",
            "current", "current_date", "current_time", "current_timestamp", "current_user", "cursor", "database",
            "deallocate", "declare", "default", "deferrable", "deferred", "definer", "delete", "desc", "disable",
            "discard", "distinct", "do", "domain", "drop", "each", "else", "elsif", "enable", "end", "escape",
            "except", "exception", "exclude", "execute", "exists", "exit", "explain", "extension", "fetch", "filter",
            "first", "following", "for", "foreach", "foreign", "from", "full", "function", "generated", "global",
            "grant", "group", "having", "identity", "if", "ilike", "immediate", "immutable", "in", "include", "index",
            "inherits", "initially", "inner", "insert", "instead", "intersect", "into", "invoker", "is", "isnull",
            "isolation", "join", "key", "language", "last", "lateral", "leading", "left", "like", "limit", "listen",
            "local", "localtime", "localtimestamp", "lock", "locked", "loop", "materialized", "matched", "merge",
            "natural", "next", "no", "not", "nothing", "notice", "notify", "notnull", "nowait", "nulls", "of",
            "offset", "on", "only", "option", "or", "order", "ordinality", "outer", "over", "overlaps", "partition",
            "perform", "policy", "preceding", "prepare", "primary", "privileges", "procedure", "raise", "range",
            "recursive", "references", "refresh", "reindex", "release", "rename", "repeatable", "replace", "reset",
            "restrict", "return", "returning", "returns", "revoke", "right", "rollback", "row", "rows", "savepoint",
            "schema", "security", "select", "sequence", "serializable", "session", "session_user", "set", "setof",
            "share", "show", "similar", "skip", "some", "stable", "stored", "strict", "symmetric", "table",
            "tablesample", "temp", "temporary", "then", "ties", "to", "trailing", "transaction", "trigger",
            "truncate", "unbounded", "uncommitted", "union", "unique", "update", "user", "using", "vacuum", "values",
            "variadic", "verbose", "view", "volatile", "when", "where", "while", "window", "with", "within",
            "without", "zone");

    private static final Set<String> LITERALS = Set.of("null", "true", "false");

    /**
     * The keywords a table's or view's name follows, in lower case: that name is no function, also before {@code (}.
     */
    private static final Set<String> TABLE_WORDS = Set.of("table", "into", "references", "view");

    /**
     * Data types of standard SQL and PostgreSQL, and a few common ones of other databases, in lower case.
     * {@code precision} and {@code varying} complete {@code double precision} and {@code character varying}.
     */
    private static final Set<String> TYPES = Set.of("bigint", "bigserial", "binary", "bit", "blob", "bool", "boolean",
            "bytea", "char", "character", "cidr", "clob", "date", "datetime", "dec", "decimal", "double", "float",
            "float4", "float8", "inet", "int", "int2", "int4", "int8", "integer", "interval", "json", "jsonb",
            "macaddr", "money", "nchar", "numeric", "nvarchar", "precision", "real", "serial", "serial4", "serial8",
            "smallint", "smallserial", "text", "time", "timestamp", "timestamptz", "timetz", "tinyint", "tsquery",
            "tsvector", "uuid", "varbinary", "varbit", "varchar", "varying", "xml");

    /**
     * A number from its first character on: digits with a fraction and an exponent.
     */
    private static final Pattern NUMBER = Pattern.compile("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

    private static final String OPERATORS = "=<>!+-*/%|&^~#@:";
    private static final String PUNCTUATION = "()[];,.";

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        int length = code.length();
        int position = 0;
        // Whether the next name is a table's or a view's: right after one of TABLE_WORDS, past white space, comments,
        // IF NOT EXISTS or IF EXISTS, and the qualifying names and dots before it. Every other token ends it.
        boolean tableNameNext = false;
        // The table word or the word of IF NOT EXISTS or IF EXISTS read last before the table's name, else null.
        String guardWord = null;
        while (position < length) {
            char character = code.charAt(position);
            boolean tableName = tableNameNext;
            tableNameNext = false;
            if (code.startsWith("--", position)) {
                int end = Scanning.lineEnd(code, position);
                pieces.add(position, end, CodeCategory.COMMENT);
                position = end;
                tableNameNext = tableName;
            } else if (code.startsWith("/*", position)) {
                int close = code.indexOf("*/", position + 2);
                int end = close < 0 ? length : close + 2;
                pieces.add(position, end, CodeCategory.COMMENT);
                position = end;
                tableNameNext = tableName;
            } else if (character == '\'') {
                int end = quotedEnd(code, position, '\'');
                pieces.add(position, end, CodeCategory.STRING);
                position = end;
            } else if (character == '"') {
                // A quoted identifier: plain, so that a keyword in it is no keyword.
                position = quotedEnd(code, position, '"');
                tableNameNext = tableName && dotAt(code, position);
                guardWord = null;
            } else if (character == '?') {
                pieces.add(position, position + 1, CodeCategory.VARIABLE);
                position++;
            } else if (character == '$' && position + 1 < length && Scanning.isDigit(code.charAt(position + 1))) {
                int end = position + 1;
                while (end < length && Scanning.isDigit(code.charAt(end))) {
                    end++;
                }
                pieces.add(position, end, CodeCategory.VARIABLE);
                position = end;
            } else if (code.startsWith("::", position)) {
                pieces.add(position, position + 2, CodeCategory.OPERATOR);
                position += 2;
            } else if (character == ':' && position + 1 < length && isNameStart(code.charAt(position + 1))) {
                int end = nameEnd(code, position + 1);
                pieces.add(position, end, CodeCategory.VARIABLE);
                position = end;
            } else if (isNameStart(character)) {
                int end = nameEnd(code, position);
                String name = code.substring(position, end).toLowerCase(Locale.ROOT);
                name(name, position, end, !tableName && end < length && code.charAt(end) == '(', pieces);
                // A table word makes the next name a table's. IF NOT EXISTS or IF EXISTS after it keeps that name to
                // come, and so does a qualifier: public.users.
                boolean guard = tableName && continuesGuard(guardWord, name);
                tableNameNext = TABLE_WORDS.contains(name) || guard || tableName && dotAt(code, end);
                guardWord = TABLE_WORDS.contains(name) || guard ? name : null;
                position = end;
            } else if (Scanning.isDigit(character) || character == '.' && position + 1 < length
                    && Scanning.isDigit(code.charAt(position + 1))) {
                position = number(code, position, pieces);
            } else if (OPERATORS.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.OPERATOR);
                position++;
            } else if (PUNCTUATION.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                tableNameNext = tableName && character == '.';
                position++;
            } else {
                tableNameNext = tableName && Character.isWhitespace(character);
                position++;
            }
        }
        return pieces.toList();
    }

    /**
     * A name, given in lower case with its range: a keyword, a literal or a type in any case, a function if it may be
     * one, else plain.
     */
    private static void name(String name, int start, int end, boolean function, Pieces pieces) {
        if (KEYWORDS.contains(name)) {
            pieces.add(start, end, CodeCategory.KEYWORD);
        } else if (LITERALS.contains(name)) {
            pieces.add(start, end, CodeCategory.LITERAL);
        } else if (TYPES.contains(name)) {
            pieces.add(start, end, CodeCategory.TYPE);
        } else if (function) {
            pieces.add(start, end, CodeCategory.FUNCTION);
        }
    }

    /**
     * Whether a word, in lower case, goes on with {@code IF NOT EXISTS} or {@code IF EXISTS} after a table word:
     * {@code if} right after the table word, {@code not} after that {@code if}, {@code exists} after that {@code if} or
     * {@code not}.
     *
     * @param previous the table word or the word of the guard read before the word, {@code null} if none
     */
    private static boolean continuesGuard(String previous, String word) {
        if (previous == null) {
            return false;
        }
        return switch (word) {
            case "if" -> TABLE_WORDS.contains(previous);
            case "not" -> previous.equals("if");
            case "exists" -> previous.equals("if") || previous.equals("not");
            default -> false;
        };
    }

    /**
     * Whether a dot stands at a position: a qualified name goes on there.
     */
    private static boolean dotAt(String code, int position) {
        return position < code.length() && code.charAt(position) == '.';
    }

    /**
     * A number, if it is a whole word; a number that runs into letters or digits ({@code 12ab}) is no number, the
     * whole word stays plain.
     *
     * @return the position after the number or the word
     */
    private static int number(String code, int start, Pieces pieces) {
        Matcher matcher = NUMBER.matcher(code).region(start, code.length());
        int end = matcher.lookingAt() ? matcher.end() : start;
        if (end > start && (end >= code.length() || !isNamePart(code.charAt(end)))) {
            pieces.add(start, end, CodeCategory.NUMBER);
            return end;
        }
        return nameEnd(code, start + 1);
    }

    /**
     * The end of a string or a quoted identifier, where the quote doubled is a quote inside: the position after its
     * closing quote, or the end of the code.
     */
    private static int quotedEnd(String code, int start, char quote) {
        int position = start + 1;
        while (position < code.length()) {
            if (code.charAt(position) == quote) {
                if (position + 1 >= code.length() || code.charAt(position + 1) != quote) {
                    return position + 1;
                }
                position += 2;
            } else {
                position++;
            }
        }
        return code.length();
    }

    /**
     * The end of the name characters from a position on.
     */
    private static int nameEnd(String code, int start) {
        int position = start;
        while (position < code.length() && isNamePart(code.charAt(position))) {
            position++;
        }
        return position;
    }

    private static boolean isNameStart(char character) {
        return Character.isLetter(character) || character == '_';
    }

    /**
     * Whether a character may stand in a name after its first: a letter, a digit, {@code _} or, as PostgreSQL allows,
     * {@code $}.
     */
    private static boolean isNamePart(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '$';
    }
}
