package org.karthu.fx.markdown.parser;

/**
 * The classes of characters the parser reads Markdown by, as CommonMark defines them.
 */
final class Characters {

    private Characters() {
    }

    /**
     * A space or a tab.
     *
     * @param c the character
     * @return true if the character is one
     */
    static boolean isSpaceOrTab(char c) {
        return c == ' ' || c == '\t';
    }

    /**
     * An ASCII digit.
     *
     * @param c the character
     * @return true if the character is one
     */
    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * An ASCII letter.
     *
     * @param c the character
     * @return true if the character is one
     */
    static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * ASCII punctuation, the characters a backslash escapes: {@code !} to {@code /}, {@code :} to {@code @},
     * {@code [} to {@code `} and <code>&#123;</code> to {@code ~}.
     *
     * @param c the character
     * @return true if the character is ASCII punctuation
     */
    static boolean isAsciiPunctuation(char c) {
        return (c >= '!' && c <= '/') || (c >= ':' && c <= '@') || (c >= '[' && c <= '`') || (c >= '{' && c <= '~');
    }

    /**
     * Unicode whitespace: the space separators, tab, line feed, form feed, carriage return.
     *
     * @param c the character
     * @return true if the character is Unicode whitespace
     */
    static boolean isWhitespace(char c) {
        return c == '\t' || c == '\n' || c == '\f' || c == '\r' || Character.getType(c) == Character.SPACE_SEPARATOR;
    }

    /**
     * Unicode punctuation: the general categories P (punctuation) and S (symbols).
     *
     * @param c the character
     * @return true if the character is Unicode punctuation
     */
    static boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_SYMBOL;
    }
}
