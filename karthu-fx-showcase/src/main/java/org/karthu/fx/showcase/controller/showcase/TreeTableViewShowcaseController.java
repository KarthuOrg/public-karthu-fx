package org.karthu.fx.showcase.controller.showcase;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

/**
 * Controller for tree-table-view-showcase.fxml.
 * Populates a file-browser-style tree table with sample data.
 */
public class TreeTableViewShowcaseController {

    @FXML
    private TreeTableView<FileEntry> treeTable;

    @FXML
    private TreeTableColumn<FileEntry, String> nameCol;

    @FXML
    private TreeTableColumn<FileEntry, String> sizeCol;

    @FXML
    private TreeTableColumn<FileEntry, String> typeCol;

    @FXML
    private void initialize() {
        nameCol.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());
        sizeCol.setCellValueFactory(cellData -> cellData.getValue().getValue().sizeProperty());
        typeCol.setCellValueFactory(cellData -> cellData.getValue().getValue().typeProperty());

        TreeItem<FileEntry> root = new TreeItem<>(new FileEntry("Root", "", ""));

        // src folder
        TreeItem<FileEntry> src = new TreeItem<>(new FileEntry("src", "", "Folder"));
        TreeItem<FileEntry> main = new TreeItem<>(new FileEntry("main", "", "Folder"));
        main.getChildren().addAll(
                new TreeItem<>(new FileEntry("App.java", "4.2 KB", "Java Source")),
                new TreeItem<>(new FileEntry("Config.java", "1.8 KB", "Java Source")),
                new TreeItem<>(new FileEntry("Utils.java", "2.5 KB", "Java Source"))
        );
        TreeItem<FileEntry> test = new TreeItem<>(new FileEntry("test", "", "Folder"));
        test.getChildren().addAll(
                new TreeItem<>(new FileEntry("AppTest.java", "3.1 KB", "Java Source")),
                new TreeItem<>(new FileEntry("ConfigTest.java", "1.2 KB", "Java Source"))
        );
        src.getChildren().addAll(main, test);
        src.setExpanded(true);
        main.setExpanded(true);

        // resources folder
        TreeItem<FileEntry> resources = new TreeItem<>(new FileEntry("resources", "", "Folder"));
        resources.getChildren().addAll(
                new TreeItem<>(new FileEntry("application.properties", "512 B", "Properties")),
                new TreeItem<>(new FileEntry("styles.css", "3.7 KB", "Stylesheet")),
                new TreeItem<>(new FileEntry("logo.png", "24.1 KB", "Image"))
        );
        resources.setExpanded(true);

        // root files
        TreeItem<FileEntry> pom = new TreeItem<>(new FileEntry("pom.xml", "2.9 KB", "XML"));
        TreeItem<FileEntry> readme = new TreeItem<>(new FileEntry("README.md", "1.4 KB", "Markdown"));
        TreeItem<FileEntry> gitignore = new TreeItem<>(new FileEntry(".gitignore", "248 B", "Git Config"));

        root.getChildren().addAll(src, resources, pom, readme, gitignore);
        treeTable.setRoot(root);
    }

    public static class FileEntry {
        private final SimpleStringProperty name;
        private final SimpleStringProperty size;
        private final SimpleStringProperty type;

        public FileEntry(String name, String size, String type) {
            this.name = new SimpleStringProperty(name);
            this.size = new SimpleStringProperty(size);
            this.type = new SimpleStringProperty(type);
        }

        public String getName() { return name.get(); }
        public StringProperty nameProperty() { return name; }

        public String getSize() { return size.get(); }
        public StringProperty sizeProperty() { return size; }

        public String getType() { return type.get(); }
        public StringProperty typeProperty() { return type; }
    }
}
