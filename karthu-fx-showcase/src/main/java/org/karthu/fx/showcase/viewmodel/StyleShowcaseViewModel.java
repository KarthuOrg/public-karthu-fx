package org.karthu.fx.showcase.viewmodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

import static org.karthu.fx.showcase.viewmodel.ShowcaseSection.EXAMPLES;
import static org.karthu.fx.showcase.viewmodel.ShowcaseSection.JAVAFX;
import static org.karthu.fx.showcase.viewmodel.ShowcaseSection.KARTHU_FX;

/**
 * ViewModel for the style showcase application: the pages, the navigation tree built over them, the text its search
 * matches, and the open page.
 * <p>
 * The pages are one ordered list; their order is the display order inside a group and among a section's directly
 * attached pages. The tree is built from that list once, every heading unfolded. The view shows it in a navigation
 * tree, which searches it and keeps the user's folding: the view model never filters or rebuilds it.
 */
public class StyleShowcaseViewModel {

    /** The group of the form collection (package org.karthu.fx.form). */
    private static final String FORM_INPUTS = "Form Inputs";

    /** The group of a page directly under its section: none. */
    private static final String NO_GROUP = null;

    private final ObservableList<ShowcaseItem> showcaseItems;
    private final TreeItem<NavigationEntry> navigationRoot = new TreeItem<>();
    private final ObjectProperty<ShowcaseItem> selectedItem = new SimpleObjectProperty<>();

    public StyleShowcaseViewModel() {
        showcaseItems = FXCollections.observableArrayList(
                page(JAVAFX, "Inputs", "Buttons", "button-showcase.fxml", BootstrapIcons.MENU_BUTTON_WIDE),
                page(JAVAFX, "Inputs", "Text Field", "text-field-showcase.fxml", BootstrapIcons.INPUT_CURSOR_TEXT),
                page(JAVAFX, "Inputs", "Text Area", "text-area-showcase.fxml", BootstrapIcons.TEXTAREA_RESIZE),
                page(JAVAFX, "Inputs", "Checkbox", "checkbox-showcase.fxml", BootstrapIcons.CHECK2_SQUARE),
                page(JAVAFX, "Inputs", "Radio Button", "radio-button-showcase.fxml", BootstrapIcons.RECORD_CIRCLE),
                page(JAVAFX, "Inputs", "Hyperlink", "hyperlink-showcase.fxml", BootstrapIcons.LINK_45DEG),
                page(JAVAFX, "Selection", "ComboBox", "combo-box-showcase.fxml", BootstrapIcons.MENU_BUTTON),
                page(JAVAFX, "Selection", "ChoiceBox", "choice-box-showcase.fxml", BootstrapIcons.CHEVRON_EXPAND),
                page(JAVAFX, "Selection", "DatePicker", "date-picker-showcase.fxml", BootstrapIcons.CALENDAR3),
                page(JAVAFX, "Selection", "Spinner", "spinner-showcase.fxml", BootstrapIcons.ARROW_DOWN_UP),
                page(JAVAFX, "Selection", "Slider", "slider-showcase.fxml", BootstrapIcons.SLIDERS),
                page(JAVAFX, "Selection", "ColorPicker", "color-picker-showcase.fxml", BootstrapIcons.PALETTE),
                page(JAVAFX, "Data Display", "TableView", "table-view-showcase.fxml", BootstrapIcons.TABLE),
                page(JAVAFX, "Data Display", "ListView", "list-view-showcase.fxml", BootstrapIcons.LIST_UL),
                page(JAVAFX, "Data Display", "TreeView", "tree-view-showcase.fxml", BootstrapIcons.LIST_NESTED),
                page(JAVAFX, "Data Display", "TreeTableView", "tree-table-view-showcase.fxml", BootstrapIcons.DIAGRAM_3),
                page(JAVAFX, "Layout", "Accordion", "accordion-showcase.fxml", BootstrapIcons.VIEW_STACKED),
                page(JAVAFX, "Layout", "TabPane", "tab-pane-showcase.fxml", BootstrapIcons.SEGMENTED_NAV),
                page(JAVAFX, "Layout", "SplitPane", "split-pane-showcase.fxml", BootstrapIcons.LAYOUT_SPLIT),
                page(JAVAFX, "Layout", "ScrollPane", "scroll-pane-showcase.fxml", BootstrapIcons.ARROWS_EXPAND),
                page(JAVAFX, "Navigation", "Pagination", "pagination-showcase.fxml", BootstrapIcons.THREE_DOTS),
                page(JAVAFX, "Navigation", "Menu Bar", "menu-bar-showcase.fxml", BootstrapIcons.LIST),
                page(JAVAFX, "Navigation", "Context Menu", "context-menu-showcase.fxml", BootstrapIcons.MENU_APP),
                page(JAVAFX, NO_GROUP, "Progress", "progress-showcase.fxml", BootstrapIcons.HOURGLASS_SPLIT),
                // The form collection: the overview first, then one page per input type, the path and cron inputs,
                // then the search input, in the look of the form inputs though no form input.
                page(KARTHU_FX, FORM_INPUTS, "Overview", "form-inputs-overview-showcase.fxml",
                        BootstrapIcons.UI_CHECKS_GRID),
                page(KARTHU_FX, FORM_INPUTS, "Text Input", "text-input-showcase.fxml", BootstrapIcons.CURSOR_TEXT),
                page(KARTHU_FX, FORM_INPUTS, "Password Input", "password-input-showcase.fxml", BootstrapIcons.KEY),
                page(KARTHU_FX, FORM_INPUTS, "Number Input", "number-input-showcase.fxml", BootstrapIcons.HASH),
                page(KARTHU_FX, FORM_INPUTS, "Combo Input", "combo-input-showcase.fxml", BootstrapIcons.MENU_DOWN),
                page(KARTHU_FX, FORM_INPUTS, "Editable Combo Input", "editable-combo-input-showcase.fxml",
                        BootstrapIcons.INPUT_CURSOR),
                page(KARTHU_FX, FORM_INPUTS, "Text Area Input", "text-area-input-showcase.fxml",
                        BootstrapIcons.TEXTAREA_T),
                page(KARTHU_FX, FORM_INPUTS, "Path Input", "path-input-showcase.fxml", BootstrapIcons.FOLDER2_OPEN),
                page(KARTHU_FX, FORM_INPUTS, "Cron Input", "cron-input-showcase.fxml", BootstrapIcons.CLOCK),
                page(KARTHU_FX, FORM_INPUTS, "Search Input", "search-input-showcase.fxml", BootstrapIcons.SEARCH),
                page(KARTHU_FX, "Components", "Toggle Switch", "toggle-switch-showcase.fxml", BootstrapIcons.TOGGLE_ON),
                page(KARTHU_FX, "Components", "Twin Column Selector", "twin-column-selector-showcase.fxml",
                        BootstrapIcons.ARROW_LEFT_RIGHT),
                page(KARTHU_FX, "Components", "Key-Value Editor", "key-value-editor-showcase.fxml",
                        BootstrapIcons.LIST_CHECK),
                page(KARTHU_FX, "Components", "Side Bar", "side-bar-showcase.fxml", BootstrapIcons.LAYOUT_SIDEBAR),
                page(KARTHU_FX, "Dialogs", "Dialog / Alert", "dialog-showcase.fxml", BootstrapIcons.CHAT_SQUARE_TEXT),
                page(KARTHU_FX, "Dialogs", "Settings Dialog", "settings-dialog-showcase.fxml", BootstrapIcons.GEAR),
                page(KARTHU_FX, NO_GROUP, "Markdown", "markdown-showcase.fxml", BootstrapIcons.MARKDOWN),
                page(KARTHU_FX, NO_GROUP, "Shortcuts", "shortcuts-showcase.fxml", BootstrapIcons.KEYBOARD),
                page(KARTHU_FX, NO_GROUP, "Icons", "icons-showcase.fxml", BootstrapIcons.EMOJI_SMILE),
                page(EXAMPLES, NO_GROUP, "Job Scheduler", "job-scheduler-showcase.fxml", BootstrapIcons.ALARM)
        );
        navigationRoot.setExpanded(true);
        buildNavigation();
        // The open page at start is the controller's to choose: it opens the first page and selects its row.
    }

    private static ShowcaseItem page(ShowcaseSection section, String group, String name, String fxmlFileName,
            Ikon icon) {
        return new ShowcaseItem(section, group, name, fxmlFileName, icon);
    }

    /**
     * Every page of the showcase, in display order.
     *
     * @return the pages
     */
    public ObservableList<ShowcaseItem> getShowcaseItems() {
        return showcaseItems;
    }

    /**
     * The navigation tree's root, hidden in the view: its children are the sections, each with its groups first, in
     * the order of their first page, then its directly attached pages. Every heading starts unfolded; from then on the
     * user folds and unfolds them.
     *
     * @return the root
     */
    public TreeItem<NavigationEntry> getNavigationRoot() {
        return navigationRoot;
    }

    /**
     * The text the navigation's search matches for an entry of the tree. A section or group heading is matched by its
     * name, and a matching heading keeps all its pages in the search's result. A page is matched by its name, its
     * group's name and its section's name, separated by whitespace so that no word spans two of them: several words
     * then narrow across them, "form path" finding the Path Input of the Form Inputs.
     *
     * @param entry the entry
     * @return the text, its parts separated by a space
     */
    public String searchText(NavigationEntry entry) {
        if (!(entry instanceof ShowcaseItem page)) {
            return entry.text();
        }
        StringBuilder text = new StringBuilder(page.displayName());
        if (page.group() != null) {
            text.append(' ').append(page.group());
        }
        text.append(' ').append(page.section().displayName());
        return text.toString();
    }

    public ObjectProperty<ShowcaseItem> selectedItemProperty() {
        return selectedItem;
    }

    public ShowcaseItem getSelectedItem() {
        return selectedItem.get();
    }

    public void selectItem(ShowcaseItem item) {
        selectedItem.set(item);
    }

    /** Builds the sections, groups and page leaves from the pages and puts them under the root. */
    private void buildNavigation() {
        List<TreeItem<NavigationEntry>> sections = new ArrayList<>();
        for (ShowcaseSection section : ShowcaseSection.values()) {
            Map<String, TreeItem<NavigationEntry>> groups = new LinkedHashMap<>();
            List<TreeItem<NavigationEntry>> attached = new ArrayList<>();
            for (ShowcaseItem item : showcaseItems) {
                if (item.section() != section) {
                    continue;
                }
                TreeItem<NavigationEntry> leaf = new TreeItem<>(item);
                if (item.group() == null) {
                    attached.add(leaf);
                } else {
                    TreeItem<NavigationEntry> group = groups.get(item.group());
                    if (group == null) {
                        group = expanded(new GroupHeading(section, item.group()));
                        groups.put(item.group(), group);
                    }
                    group.getChildren().add(leaf);
                }
            }
            if (groups.isEmpty() && attached.isEmpty()) {
                continue;
            }
            TreeItem<NavigationEntry> sectionNode = expanded(new SectionHeading(section));
            sectionNode.getChildren().addAll(groups.values());
            sectionNode.getChildren().addAll(attached);
            sections.add(sectionNode);
        }
        navigationRoot.getChildren().setAll(sections);
    }

    private static TreeItem<NavigationEntry> expanded(NavigationEntry heading) {
        TreeItem<NavigationEntry> node = new TreeItem<>(heading);
        node.setExpanded(true);
        return node;
    }
}
