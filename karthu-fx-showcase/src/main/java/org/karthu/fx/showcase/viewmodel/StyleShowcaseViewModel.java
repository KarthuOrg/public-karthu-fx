package org.karthu.fx.showcase.viewmodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

/**
 * ViewModel for the style showcase application.
 * Contains the sidebar items and selection state.
 */
public class StyleShowcaseViewModel {

    /** The sidebar group of the form collection (package org.karthu.fx.form). */
    private static final String FORM_INPUTS = "Form Inputs";

    private final ObservableList<ShowcaseItem> showcaseItems;
    private final ObjectProperty<ShowcaseItem> selectedItem = new SimpleObjectProperty<>();

    public StyleShowcaseViewModel() {
        showcaseItems = FXCollections.observableArrayList(
                new ShowcaseItem("Buttons", "button-showcase.fxml", "Inputs", BootstrapIcons.MENU_BUTTON_WIDE),
                new ShowcaseItem("Text Field", "text-field-showcase.fxml", "Inputs", BootstrapIcons.INPUT_CURSOR_TEXT),
                new ShowcaseItem("Text Area", "text-area-showcase.fxml", "Inputs", BootstrapIcons.TEXTAREA_RESIZE),
                new ShowcaseItem("Checkbox", "checkbox-showcase.fxml", "Inputs", BootstrapIcons.CHECK2_SQUARE),
                new ShowcaseItem("Radio Button", "radio-button-showcase.fxml", "Inputs", BootstrapIcons.RECORD_CIRCLE),
                new ShowcaseItem("Toggle Switch", "toggle-switch-showcase.fxml", "Inputs", BootstrapIcons.TOGGLE_ON),
                new ShowcaseItem("Hyperlink", "hyperlink-showcase.fxml", "Inputs", BootstrapIcons.LINK_45DEG),
                // The form collection: the overview first, then one page per input type, the path and cron inputs, then the
                // search input.
                new ShowcaseItem("Overview", "form-inputs-overview-showcase.fxml", FORM_INPUTS, BootstrapIcons.UI_CHECKS_GRID),
                new ShowcaseItem("Text Input", "text-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.CURSOR_TEXT),
                new ShowcaseItem("Password Input", "password-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.KEY),
                new ShowcaseItem("Number Input", "number-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.HASH),
                new ShowcaseItem("Combo Input", "combo-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.MENU_DOWN),
                new ShowcaseItem("Editable Combo Input", "editable-combo-input-showcase.fxml", FORM_INPUTS,
                        BootstrapIcons.INPUT_CURSOR),
                new ShowcaseItem("Text Area Input", "text-area-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.TEXTAREA_T),
                new ShowcaseItem("Path Input", "path-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.FOLDER2_OPEN),
                new ShowcaseItem("Cron Input", "cron-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.CLOCK),
                // The search input and its suggestions: in the look of the form inputs, though no form input.
                new ShowcaseItem("Search Input", "search-input-showcase.fxml", FORM_INPUTS, BootstrapIcons.SEARCH),
                new ShowcaseItem("ComboBox", "combo-box-showcase.fxml", "Selection", BootstrapIcons.MENU_BUTTON),
                new ShowcaseItem("ChoiceBox", "choice-box-showcase.fxml", "Selection", BootstrapIcons.CHEVRON_EXPAND),
                new ShowcaseItem("DatePicker", "date-picker-showcase.fxml", "Selection", BootstrapIcons.CALENDAR3),
                new ShowcaseItem("Spinner", "spinner-showcase.fxml", "Selection", BootstrapIcons.ARROW_DOWN_UP),
                new ShowcaseItem("Slider", "slider-showcase.fxml", "Selection", BootstrapIcons.SLIDERS),
                new ShowcaseItem("ColorPicker", "color-picker-showcase.fxml", "Selection", BootstrapIcons.PALETTE),
                new ShowcaseItem("Twin Column Selector", "twin-column-selector-showcase.fxml", "Selection", BootstrapIcons.ARROW_LEFT_RIGHT),
                new ShowcaseItem("Key-Value Editor", "key-value-editor-showcase.fxml", "Inputs", BootstrapIcons.LIST_CHECK),
                new ShowcaseItem("TableView", "table-view-showcase.fxml", "Data Display", BootstrapIcons.TABLE),
                new ShowcaseItem("ListView", "list-view-showcase.fxml", "Data Display", BootstrapIcons.LIST_UL),
                new ShowcaseItem("TreeView", "tree-view-showcase.fxml", "Data Display", BootstrapIcons.LIST_NESTED),
                new ShowcaseItem("TreeTableView", "tree-table-view-showcase.fxml", "Data Display", BootstrapIcons.DIAGRAM_3),
                new ShowcaseItem("Markdown", "markdown-showcase.fxml", "Data Display", BootstrapIcons.MARKDOWN),
                new ShowcaseItem("Accordion", "accordion-showcase.fxml", "Layout", BootstrapIcons.VIEW_STACKED),
                new ShowcaseItem("TabPane", "tab-pane-showcase.fxml", "Layout", BootstrapIcons.SEGMENTED_NAV),
                new ShowcaseItem("SplitPane", "split-pane-showcase.fxml", "Layout", BootstrapIcons.LAYOUT_SPLIT),
                new ShowcaseItem("ScrollPane", "scroll-pane-showcase.fxml", "Layout", BootstrapIcons.ARROWS_EXPAND),
                new ShowcaseItem("Progress", "progress-showcase.fxml", "Feedback", BootstrapIcons.HOURGLASS_SPLIT),
                new ShowcaseItem("Dialog / Alert", "dialog-showcase.fxml", "Feedback", BootstrapIcons.CHAT_SQUARE_TEXT),
                new ShowcaseItem("Pagination", "pagination-showcase.fxml", "Navigation", BootstrapIcons.THREE_DOTS),
                new ShowcaseItem("Menu Bar", "menu-bar-showcase.fxml", "Navigation", BootstrapIcons.LIST),
                new ShowcaseItem("Context Menu", "context-menu-showcase.fxml", "Navigation", BootstrapIcons.MENU_APP),
                new ShowcaseItem("Shortcuts", "shortcuts-showcase.fxml", "Navigation", BootstrapIcons.KEYBOARD),
                new ShowcaseItem("Icons", "icons-showcase.fxml", "Data Display", BootstrapIcons.EMOJI_SMILE),
                new ShowcaseItem("Settings Dialog", "settings-dialog-showcase.fxml", "Feedback", BootstrapIcons.GEAR),
                new ShowcaseItem("Job Scheduler", "job-scheduler-showcase.fxml", "Examples", BootstrapIcons.ALARM)
        );
        // Initial selection is driven by the controller (selectFirst) so the
        // change event actually fires and the first page gets loaded.
    }

    public ObservableList<ShowcaseItem> getShowcaseItems() {
        return showcaseItems;
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
}
