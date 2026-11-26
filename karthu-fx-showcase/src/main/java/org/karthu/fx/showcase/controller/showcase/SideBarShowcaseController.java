package org.karthu.fx.showcase.controller.showcase;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.karthu.fx.sidebar.NavigationTree;
import org.karthu.fx.sidebar.OpenMode;
import org.karthu.fx.sidebar.SideBar;
import org.karthu.fx.sidebar.SideBarTab;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Showcase page of the side bar: an example window with a side bar of three tabs beside the application's content.
 * <i>Project</i> is a navigation tree of a project's files that opens a file on a double click or Enter, <i>Guide</i>
 * a navigation tree of a guide's chapters and pages that opens a page when the user selects it, and <i>Notes</i> a list
 * of the application's own with a header button and a menu item of its tab. A switch moves the side bar to the other
 * side of the content, and read-outs name what was opened and the state an application would keep.
 */
public class SideBarShowcaseController {

    @FXML private HBox sideSwitch;
    @FXML private BorderPane exampleWindow;
    @FXML private Label openedLabel;
    @FXML private Label stateLabel;

    private final SideBar sideBar = new SideBar();

    @FXML
    private void initialize() {
        fillSideBar();
        exampleWindow.setLeft(sideBar);
        fillSideSwitch();
        // What an application keeps of the side bar and sets again on its next start: the side bar stores nothing.
        stateLabel.textProperty().bind(Bindings.createStringBinding(() -> stateText(),
                sideBar.selectedTabProperty(), sideBar.collapsedProperty(), sideBar.contentWidthProperty()));
    }

    /** The three tabs, <i>Project</i> open. */
    private void fillSideBar() {
        SideBarTab project = new SideBarTab("project", "Project", BootstrapIcons.FOLDER2, projectTree());
        SideBarTab guide = new SideBarTab("guide", "Guide", BootstrapIcons.BOOK, guideTree());
        ListView<String> notesList = notesList();
        SideBarTab notes = new SideBarTab("notes", "Notes", BootstrapIcons.STICKY, notesList);
        // The tab's own action, a button of its header, as the list offers none, and its own item in its menu.
        notes.getHeaderButtons().add(newNoteButton(notesList));
        MenuItem clearNotes = new MenuItem("Clear Notes");
        clearNotes.setOnAction(event -> notesList.getItems().clear());
        notes.getMenuItems().add(clearNotes);
        sideBar.getTabs().addAll(project, guide, notes);
        sideBar.setSelectedTab(project);
    }

    /**
     * The switch of the side: the side bar sits on that side of the window, the application places it in that region
     * of its border pane.
     */
    private void fillSideSwitch() {
        ToggleGroup sides = new ToggleGroup();
        RadioButton left = new RadioButton("Left");
        left.setToggleGroup(sides);
        left.setSelected(true);
        RadioButton right = new RadioButton("Right");
        right.setToggleGroup(sides);
        right.selectedProperty().addListener((observable, wasRight, isRight) -> {
            exampleWindow.setLeft(null);
            exampleWindow.setRight(null);
            sideBar.setSide(isRight ? Side.RIGHT : Side.LEFT);
            if (isRight) {
                exampleWindow.setRight(sideBar);
            } else {
                exampleWindow.setLeft(sideBar);
            }
        });
        sideSwitch.getChildren().addAll(new Label("Side:"), left, right);
    }

    /** The read-out of the state an application keeps: the selected tab's id, collapsed and the content width. */
    private String stateText() {
        SideBarTab selected = sideBar.getSelectedTab();
        return "To keep: selected tab " + (selected == null ? "none" : selected.getId())
                + ", collapsed " + sideBar.isCollapsed()
                + ", content width " + Math.round(sideBar.getContentWidth()) + " px";
    }

    /**
     * The tab <i>Project</i>: a project's folders and files, opened by a double click or Enter, the open mode by
     * default.
     */
    private NavigationTree<SampleEntry> projectTree() {
        TreeItem<SampleEntry> root = new TreeItem<>(SampleEntry.folder("karthu-demo"));
        TreeItem<SampleEntry> docs = folder("docs", false, file("components.md"), file("getting-started.md"));
        TreeItem<SampleEntry> main = folder("main", true,
                folder("java", true, file("App.java"), file("MainWindow.java")),
                folder("resources", false, file("app.css")));
        TreeItem<SampleEntry> test = folder("test", false, folder("java", true, file("AppTest.java")));
        root.getChildren().addAll(docs, folder("src", true, main, test), file("pom.xml"), file("README.md"));
        NavigationTree<SampleEntry> tree = new NavigationTree<>(root);
        tree.setTextFunction(entry -> entry.name());
        // The search finds a file by its kind too, a text the tree does not show: markdown finds the .md files.
        tree.setSearchTextFunction(entry -> entry.name() + "\n" + projectKind(entry));
        tree.setIconFunction(entry -> projectIcon(entry));
        tree.setOnOpen(entry -> opened(entry, "Project"));
        return tree;
    }

    /**
     * The tab <i>Guide</i>: a guide's chapters and pages, opened as soon as the user selects them. A chapter has no
     * icon and opens nothing.
     */
    private NavigationTree<SampleEntry> guideTree() {
        TreeItem<SampleEntry> root = new TreeItem<>(SampleEntry.folder("Guide"));
        root.getChildren().addAll(
                folder("Getting Started", true, file("Installation"), file("First Window"), file("Themes")),
                folder("Components", true, file("Form Inputs"), file("Markdown View"), file("Side Bar")),
                folder("Reference", false, file("Style Classes"), file("Colour Tokens")));
        NavigationTree<SampleEntry> tree = new NavigationTree<>(root);
        tree.setOpenMode(OpenMode.SELECTION);
        tree.setTextFunction(entry -> entry.name());
        tree.setIconFunction(entry -> entry.folder() ? null : BootstrapIcons.FILE_TEXT);
        tree.setOnOpen(entry -> opened(entry, "Guide"));
        return tree;
    }

    /** The tab <i>Notes</i>: a list of the application's own, which takes no part in the side bar's contract. */
    private ListView<String> notesList() {
        ListView<String> notes = new ListView<>(FXCollections.observableArrayList(
                "Call the printer service", "Review the release notes", "Order new keyboards"));
        notes.setPlaceholder(new Label("No notes"));
        return notes;
    }

    /**
     * <i>New Note</i>, the notes tab's header button: an icon button with a tooltip, as the header's own, which adds a
     * note, selects it and puts the focus into the list.
     */
    private static Button newNoteButton(ListView<String> notes) {
        Button button = new Button();
        button.getStyleClass().add("icon-button");
        button.setGraphic(new FontIcon(BootstrapIcons.PLUS));
        button.setTooltip(new Tooltip("New Note"));
        button.setAccessibleText("New Note");
        button.setOnAction(event -> {
            String note = "Note " + (notes.getItems().size() + 1);
            notes.getItems().add(note);
            notes.getSelectionModel().select(note);
            notes.scrollTo(note);
            notes.requestFocus();
        });
        return button;
    }

    /**
     * What a tree opened: a file or a page goes into the read-out with the tab's title. A folder, which a tree also
     * hands over, opens nothing, as in an application that has no use for it.
     */
    private void opened(SampleEntry entry, String tabTitle) {
        if (!entry.folder()) {
            openedLabel.setText("Opened: " + entry.name() + " (" + tabTitle + ")");
        }
    }

    /** A project file's kind, which the search finds and the tree does not show. */
    private static String projectKind(SampleEntry entry) {
        if (entry.folder()) {
            return "Folder";
        }
        String name = entry.name();
        if (name.endsWith(".md")) {
            return "Markdown document";
        }
        if (name.endsWith(".java")) {
            return "Java source";
        }
        if (name.endsWith(".css")) {
            return "Stylesheet";
        }
        return "Build file";
    }

    /** A project file's icon: a folder, or a file by its kind. */
    private static Ikon projectIcon(SampleEntry entry) {
        if (entry.folder()) {
            return BootstrapIcons.FOLDER;
        }
        if (entry.name().endsWith(".md")) {
            return BootstrapIcons.FILE_EARMARK_TEXT;
        }
        return BootstrapIcons.FILE_EARMARK_CODE;
    }

    /** A folder of a sample tree, unfolded or folded, with its entries. */
    @SafeVarargs
    private static TreeItem<SampleEntry> folder(String name, boolean unfolded, TreeItem<SampleEntry>... entries) {
        TreeItem<SampleEntry> item = new TreeItem<>(SampleEntry.folder(name));
        item.getChildren().addAll(entries);
        item.setExpanded(unfolded);
        return item;
    }

    /** An entry of a sample tree without entries of its own: a file or a page. */
    private static TreeItem<SampleEntry> file(String name) {
        return new TreeItem<>(new SampleEntry(name, false));
    }

    /**
     * An entry of the page's sample trees, the value of its tree item: its name, and whether it is a folder, which
     * holds entries and opens nothing.
     *
     * @param name   the name the tree shows
     * @param folder whether it is a folder
     */
    record SampleEntry(String name, boolean folder) {

        static SampleEntry folder(String name) {
            return new SampleEntry(name, true);
        }
    }
}
