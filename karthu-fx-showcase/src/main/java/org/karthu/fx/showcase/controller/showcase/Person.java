package org.karthu.fx.showcase.controller.showcase;

import java.util.List;
import java.util.Locale;

/**
 * An invented person of the <i>Search Input</i> page: the entry of its person cards, the showcase's own type, as an
 * application has its own (karthu-fx has no person type). The card shows the name, the role and the department on one
 * line, the address and the e-mail, and the search covers exactly these texts.
 *
 * @param name       the name, first name first, the text that goes into the field when the person is chosen
 * @param role       the role, invented, such as Developer
 * @param department the department, invented, such as Engineering
 * @param address    the address on one line, street, postcode and town
 * @param email      the e-mail address
 */
record Person(String name, String role, String department, String address, String email) {

    /** The middle dot between the role and the department, as an escape: the build does not depend on the encoding. */
    private static final String MIDDLE_DOT = " \u00B7 ";

    /**
     * The initials the card's avatar shows: the first letter of the name's first word and of its last, in upper case;
     * one letter for a name of one word.
     *
     * @return the initials, such as {@code AM} for Anna Muster
     */
    String initials() {
        String[] words = name.strip().split("\\s+");
        String first = firstLetter(words[0]);
        if (words.length == 1) {
            return first;
        }
        return first + firstLetter(words[words.length - 1]);
    }

    /**
     * The card's second line: the role and the department with a middle dot between them.
     *
     * @return the role line, such as <code>Developer &middot; Engineering</code>
     */
    String roleLine() {
        return role + MIDDLE_DOT + department;
    }

    /**
     * The texts the card shows, top to bottom: the suggestion input's search texts, so that every match shows its mark.
     *
     * @return the name, the role line, the address and the e-mail
     */
    List<String> texts() {
        return List.of(name, roleLine(), address, email);
    }

    private static String firstLetter(String word) {
        if (word.isEmpty()) {
            return "";
        }
        return new String(Character.toChars(word.codePointAt(0))).toUpperCase(Locale.ROOT);
    }
}
