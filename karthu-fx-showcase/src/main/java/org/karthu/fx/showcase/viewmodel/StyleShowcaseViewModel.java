package org.karthu.fx.showcase.viewmodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;

import org.karthu.fx.form.SearchInput;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

import static org.karthu.fx.showcase.viewmodel.ShowcaseSection.EXAMPLES;
import static org.karthu.fx.showcase.viewmodel.ShowcaseSection.JAVAFX;
import static org.karthu.fx.showcase.viewmodel.ShowcaseSection.KARTHU_FX;

/**
 * ViewModel for the style showcase application: the pages, the navigation tree built over them and the open page.
 * <p>
 * The pages are one ordered list; their order is the display order inside a group and among a section's directly
 * attached pages. The tree is built from the filtered view of that list ({@link #getFilteredItems()}), rebuilt whenever
 * it changes, under one root that stays the same node. A search field connected through
 * {@link #connectSearch(SearchInput)} narrows that list as the user types.
 */
public class StyleShowcaseViewModel {

    /** The group of the form collection (package org.karthu.fx.form). */
    private static final String FORM_INPUTS = "Form Inputs";

    /** The group of a page directly under its section: none. */
    private static final String NO_GROUP = null;

    private final ObservableList<ShowcaseItem> showcaseItems;
    private final FilteredList<ShowcaseItem> filteredItems;
    private final TreeItem<NavigationEntry> navigationRoot = new TreeItem<>();
    private final ObjectProperty<ShowcaseItem> selectedItem = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper hasPages = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyBooleanWrapper rebuilding = new ReadOnlyBooleanWrapper(false);

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
        filteredItems = new FilteredList<>(showcaseItems);
        navigationRoot.setExpanded(true);
        rebuildNavigation();
        filteredItems.addListener((ListChangeListener<ShowcaseItem>) change -> rebuildNavigation());
        // Initial selection is driven by the controller, which selects the first page's row so that the change event
        // fires and the first page gets loaded.
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
     * The pages the navigation tree shows: all of them until a predicate narrows the list. The tree is rebuilt
     * whenever this list changes.
     *
     * @return the filtered view of {@link #getShowcaseItems()}
     */
    public FilteredList<ShowcaseItem> getFilteredItems() {
        return filteredItems;
    }

    /**
     * The navigation tree's root, hidden in the view: its children are the sections that hold a page among the
     * filtered pages, each with its groups first, in the order of their first page, then its directly attached pages.
     * Every node is expanded. The root stays the same node when the tree is rebuilt.
     *
     * @return the root
     */
    public TreeItem<NavigationEntry> getNavigationRoot() {
        return navigationRoot;
    }

    /**
     * The tree node of a page.
     *
     * @param page the page
     * @return its node, or {@code null} when the tree does not show it
     */
    public TreeItem<NavigationEntry> navigationNodeOf(ShowcaseItem page) {
        return findPage(navigationRoot, page);
    }

    /**
     * The first hit of the search: the first page the tree shows, top to bottom.
     *
     * @return the page, or {@code null} while the tree shows none
     */
    public ShowcaseItem firstHit() {
        TreeItem<NavigationEntry> node = findPage(navigationRoot, null);
        if (node == null) {
            return null;
        }
        return (ShowcaseItem) node.getValue();
    }

    /**
     * Connects a search field to the pages: from now on the field narrows {@link #getFilteredItems()} as the user
     * types, through the field's own list filter, so matching follows karthu-fx's search rule (words, case and accents
     * ignored). A page's text for the search is its name, its group's name and its section's name, separated by
     * whitespace, so that no word spans two of them and a group's or section's name finds all of its pages.
     *
     * @param search the search field
     */
    public void connectSearch(SearchInput search) {
        search.filter(filteredItems, item -> searchText(item));
    }

    /**
     * The text a page is matched against: its name, its group's name if it has a group, and its section's name.
     *
     * @param item the page
     * @return the text, its parts separated by a space
     */
    private static String searchText(ShowcaseItem item) {
        StringBuilder text = new StringBuilder(item.displayName());
        if (item.group() != null) {
            text.append(' ').append(item.group());
        }
        text.append(' ').append(item.section().displayName());
        return text.toString();
    }

    /**
     * Whether the filtered pages hold at least one page, so that the tree shows a row: false while a search finds
     * nothing.
     *
     * @return the property, true at start
     */
    public ReadOnlyBooleanProperty hasPagesProperty() {
        return hasPages.getReadOnlyProperty();
    }

    /**
     * True while the tree is being rebuilt, back to false when the new nodes are in place. A view reads a change of the
     * tree's selection while it is true as no choice of the user's, and selects the open page's row again when it turns
     * false.
     *
     * @return the property
     */
    public ReadOnlyBooleanProperty rebuildingProperty() {
        return rebuilding.getReadOnlyProperty();
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

    /** Builds the sections, groups and page leaves from the filtered pages and puts them under the root. */
    private void rebuildNavigation() {
        List<TreeItem<NavigationEntry>> sections = new ArrayList<>();
        for (ShowcaseSection section : ShowcaseSection.values()) {
            Map<String, TreeItem<NavigationEntry>> groups = new LinkedHashMap<>();
            List<TreeItem<NavigationEntry>> attached = new ArrayList<>();
            for (ShowcaseItem item : filteredItems) {
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
        rebuilding.set(true);
        try {
            navigationRoot.getChildren().setAll(sections);
            hasPages.set(!filteredItems.isEmpty());
        } finally {
            rebuilding.set(false);
        }
    }

    private static TreeItem<NavigationEntry> expanded(NavigationEntry heading) {
        TreeItem<NavigationEntry> node = new TreeItem<>(heading);
        node.setExpanded(true);
        return node;
    }

    /**
     * Walks the tree depth first, top to bottom, for the node of a page.
     *
     * @param node the node to start at
     * @param page the page, or {@code null} for the first page of all
     * @return the node, or {@code null} when the tree below {@code node} shows no such page
     */
    private static TreeItem<NavigationEntry> findPage(TreeItem<NavigationEntry> node, ShowcaseItem page) {
        if (node.getValue() instanceof ShowcaseItem found && (page == null || found == page)) {
            return node;
        }
        for (TreeItem<NavigationEntry> child : node.getChildren()) {
            TreeItem<NavigationEntry> result = findPage(child, page);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
