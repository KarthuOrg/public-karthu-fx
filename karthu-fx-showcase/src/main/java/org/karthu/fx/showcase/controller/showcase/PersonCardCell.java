package org.karthu.fx.showcase.controller.showcase;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.karthu.fx.form.SearchInput;

/**
 * The person card of the <i>Search Input</i> page: a row of a suggestion popup, built from the suggestion input's public
 * API alone, as an application builds its own rows. On the left an avatar with the person's initials, on the right four
 * lines, each a highlight node of the field ({@link SearchInput#highlight(String)}), so that the marks of every line
 * follow the search: the name bold ({@code search-strong}, its marks underlined by the theme), the role and the
 * department, the address and the e-mail.
 *
 * <p>The card does nothing for clicks or the selection, which the field and the theme give every row. It gets the
 * popup's width from the field and wraps a line that does not fit there; the avatar keeps its size.
 */
final class PersonCardCell extends ListCell<Person> {

    /** The avatar's diameter, which the theme's rule {@code showcase-person-avatar} rounds to a circle. */
    static final double AVATAR_SIZE = 32;

    /** The space between the avatar and the lines. */
    private static final double GAP = 12;

    /** The field whose search the lines follow. */
    private final SearchInput field;

    /**
     * Creates a card cell for the field's popup.
     *
     * @param field the suggestion input whose popup shows the card, whose search the marks follow
     */
    PersonCardCell(SearchInput field) {
        this.field = field;
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(Person person, boolean empty) {
        super.updateItem(person, empty);
        setText(null);
        if (empty || person == null) {
            setGraphic(null);
            return;
        }
        setGraphic(card(person));
    }

    /** The card: the avatar, then the lines, which take the rest of the row's width and wrap there. */
    private HBox card(Person person) {
        TextFlow name = field.highlight(person.name());
        name.getStyleClass().add("search-strong");
        VBox lines = new VBox(name, field.highlight(person.roleLine()), field.highlight(person.address()),
                field.highlight(person.email()));
        lines.setMinWidth(0);
        HBox.setHgrow(lines, Priority.ALWAYS);

        Label avatar = avatar(person);
        // The avatar beside the name and the role line.
        HBox.setMargin(avatar, new Insets(2, 0, 0, 0));
        HBox card = new HBox(GAP, avatar, lines);
        card.setAlignment(Pos.TOP_LEFT);
        return card;
    }

    /**
     * The avatar: the person's initials in a circle, drawn by the theme's showcase rule
     * {@code showcase-person-avatar} from its tokens (the accent's tint, the text colour). A rule rather than an
     * inline style: the popup's first CSS pass runs before it has an owner, where an inline token is not resolved.
     *
     * @param person the person
     * @return a label of the avatar's size, its initials centred
     */
    static Label avatar(Person person) {
        Label avatar = new Label(person.initials());
        avatar.getStyleClass().add("showcase-person-avatar");
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setPrefSize(AVATAR_SIZE, AVATAR_SIZE);
        avatar.setMaxSize(AVATAR_SIZE, AVATAR_SIZE);
        return avatar;
    }
}
