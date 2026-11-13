package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Controller for tree-view-showcase.fxml.
 * Builds a tree structure with folders and items.
 */
public class TreeViewShowcaseController {

    @FXML
    private TreeView<String> treeView;

    @FXML
    private void initialize() {
        TreeItem<String> root = new TreeItem<>("Root");

        // Folder 1 with 3 items
        TreeItem<String> folder1 = new TreeItem<>("Folder 1");
        folder1.getChildren().addAll(
                new TreeItem<>("Item 1.1"),
                new TreeItem<>("Item 1.2"),
                new TreeItem<>("Item 1.3")
        );

        // Folder 2 with 2 items
        TreeItem<String> folder2 = new TreeItem<>("Folder 2");
        folder2.getChildren().addAll(
                new TreeItem<>("Item 2.1"),
                new TreeItem<>("Item 2.2")
        );

        // Folder 3 with subfolder (2 items) and 1 item
        TreeItem<String> folder3 = new TreeItem<>("Folder 3");
        TreeItem<String> subfolder = new TreeItem<>("Subfolder");
        subfolder.getChildren().addAll(
                new TreeItem<>("Item 3.1.1"),
                new TreeItem<>("Item 3.1.2")
        );
        folder3.getChildren().addAll(subfolder, new TreeItem<>("Item 3.2"));

        root.getChildren().addAll(folder1, folder2, folder3);
        treeView.setRoot(root);

        // Expand Root, Folder 1, and Folder 3
        root.setExpanded(true);
        folder1.setExpanded(true);
        folder3.setExpanded(true);
    }
}
