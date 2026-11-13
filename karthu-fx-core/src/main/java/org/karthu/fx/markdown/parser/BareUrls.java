package org.karthu.fx.markdown.parser;

/**
 * Finds bare {@code http://} and {@code https://} addresses in an inline text, as GFM's extended autolinks do.
 *
 * <p>The inline parser asks at each {@code h} of its scan whether an address starts there, before it reads the
 * emphasis and escapes after it. So an address is taken from the source as written: {@code https://x.y/__init__.py}
 * stays whole, and {@code https://a.b/c\_d} keeps its backslash. The constructs the scan reads first, a code span and
 * an autolink, are never searched; the parser asks no question while a bracket is open, as GFM does, so an address
 * neither ends up in link text nor runs across a link's {@code ](}.
 *
 * <p>An address starts at the start of the text or after whitespace, {@code *}, {@code _}, {@code ~} or {@code (}
 * (GFM's rule, so {@code "https://a.b"} or {@code xhttps://a.b} stay text). It ends before whitespace or a {@code <};
 * then trailing {@code .}, {@code ,}, {@code :}, {@code ;}, {@code !}, {@code ?}, {@code *}, {@code _} and closing
 * parentheses without a partner in the address are left out, as long as there are any: the {@code *} and {@code _}
 * so that the emphasis around an address ({@code **https://a.b**}) is no part of it, as in GFM. What is left must
 * be more than the scheme.
 */
final class BareUrls {

    private static final String TRAILING_PUNCTUATION = ".,:;!?*_";

    private BareUrls() {
    }

    /**
     * The end of the bare URL starting at an index.
     *
     * @param text  the inline text
     * @param start the index where the address would start
     * @param limit the index the address ends at the latest
     * @return the index after the address, or -1 if no address starts at {@code start}
     */
    static int end(String text, int start, int limit) {
        int schemeEnd = schemeEnd(text, start);
        if (schemeEnd < 0 || schemeEnd > limit || (start > 0 && !canPrecede(text.charAt(start - 1)))) {
            return -1;
        }
        int end = addressEnd(text, schemeEnd, limit);
        return end == schemeEnd ? -1 : end;
    }

    /**
     * The index after {@code http://} or {@code https://} at {@code index}, or -1 if neither starts there.
     */
    private static int schemeEnd(String text, int index) {
        if (text.startsWith("http://", index)) {
            return index + "http://".length();
        }
        if (text.startsWith("https://", index)) {
            return index + "https://".length();
        }
        return -1;
    }

    private static boolean canPrecede(char c) {
        return Characters.isWhitespace(c) || c == '*' || c == '_' || c == '~' || c == '(';
    }

    /**
     * The end of an address: before whitespace, {@code <} or the limit, without its trailing punctuation and
     * unmatched closing parentheses.
     *
     * @return the index after the address; {@code schemeEnd} if nothing is left after the scheme
     */
    private static int addressEnd(String text, int schemeEnd, int limit) {
        int end = schemeEnd;
        int opening = 0;
        int closing = 0;
        while (end < limit && !Characters.isWhitespace(text.charAt(end)) && text.charAt(end) != '<') {
            if (text.charAt(end) == '(') {
                opening++;
            } else if (text.charAt(end) == ')') {
                closing++;
            }
            end++;
        }
        // The counts are kept along while trimming, so a long run of parentheses takes linear time.
        while (end > schemeEnd) {
            char last = text.charAt(end - 1);
            if (TRAILING_PUNCTUATION.indexOf(last) >= 0) {
                end--;
            } else if (last == ')' && closing > opening) {
                closing--;
                end--;
            } else {
                break;
            }
        }
        return end;
    }
}
