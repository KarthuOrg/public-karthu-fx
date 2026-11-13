package org.karthu.fx.markdown.lexer;

/**
 * The small questions several lexers ask of the code while they scan it, answered in one place.
 */
final class Scanning {

    private Scanning() {
    }

    /**
     * The end of the line a position is on: its line feed, or the end of the code.
     *
     * @param code     the code
     * @param position a position in the line
     * @return the position of the line's line feed, or the code's length
     */
    static int lineEnd(String code, int position) {
        int end = code.indexOf('\n', position);
        return end < 0 ? code.length() : end;
    }

    /**
     * Whether an opening bracket follows a position, after spaces and tabs.
     *
     * @param code     the code
     * @param position the position after a name
     * @return whether {@code (} comes next, past spaces and tabs only
     */
    static boolean bracketFollows(String code, int position) {
        int next = position;
        while (next < code.length() && (code.charAt(next) == ' ' || code.charAt(next) == '\t')) {
            next++;
        }
        return next < code.length() && code.charAt(next) == '(';
    }

    /**
     * Whether a character is an ASCII digit, the digits of the numbers of a programming language.
     *
     * @param character the character
     * @return whether it is one of {@code 0} to {@code 9}
     */
    static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }
}
