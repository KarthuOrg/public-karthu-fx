package org.karthu.fx.theme;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.skin.UncachedScrollPaneSkin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The theme's scroll pane skin: no viewport of a themed scene caches its content as a bitmap, into which JavaFX draws
 * text in gray, so the text inside scroll panes keeps the theme's LCD smoothing. Whether the text then comes out LCD
 * on screen is a matter of the rendering, which a test of the scene graph cannot see.
 */
class ScrollPaneSkinTest {

    private static final String SKIN_RULE = "-fx-skin: \"org.karthu.fx.theme.skin.UncachedScrollPaneSkin\";";
    private static final String NATIVE_IMAGE_METADATA =
            "/META-INF/native-image/org.karthu/karthu-fx-core/reachability-metadata.json";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        // The first control created off the toolkit's thread has the toolkit set its default stylesheet on its own
        // thread: wait for it, so the theme's CSS is not applied before it.
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("every scroll pane of a themed scene has the theme's skin and a viewport without bitmap cache: a plain"
            + " one, an application's subclass, one inside another and the one inside a text area")
    void everyScrollPaneUncached() {
        ScrollPane inner = new ScrollPane(new Label("Inner"));
        ScrollPane outer = new ScrollPane(new VBox(new Label("Outer"), inner));
        ScrollPane subclass = new ScrollPane(new Label("Subclass")) {
        };
        TextArea textArea = new TextArea("Text area");
        VBox root = themed(outer, subclass, textArea);

        List<ScrollPane> scrollPanes = scrollPanes(root);
        assertEquals(4, scrollPanes.size(), "the outer, the inner, the subclass and the text area's scroll panes");
        assertTrue(scrollPanes.contains(textArea.lookup(".scroll-pane")));
        for (ScrollPane scrollPane : scrollPanes) {
            assertSame(UncachedScrollPaneSkin.class, scrollPane.getSkin().getClass(), "the theme's skin");
            assertUncached(scrollPane);
        }
    }

    @Test
    @DisplayName("a scroll pane added to a themed scene later gets the skin at its first CSS pass")
    void addedLater() {
        VBox root = themed(new Label("First"));
        ScrollPane later = new ScrollPane(new Label("Later"));

        root.getChildren().add(later);
        root.applyCss();
        root.layout();

        assertSame(UncachedScrollPaneSkin.class, later.getSkin().getClass());
        assertUncached(later);
    }

    @Test
    @DisplayName("list, table, tree and tree table views cache nothing: their rows are drawn in a virtual flow, not in"
            + " a scroll pane, and keep JavaFX's skins")
    void virtualFlowsCacheNothing() {
        ListView<String> list = new ListView<>(FXCollections.observableArrayList("One", "Two"));
        TableView<String> table = new TableView<>(FXCollections.observableArrayList("One", "Two"));
        TableColumn<String, String> column = new TableColumn<>("Column");
        column.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue()));
        table.getColumns().add(column);
        TreeItem<String> treeRoot = new TreeItem<>("Root");
        treeRoot.getChildren().add(new TreeItem<>("Child"));
        treeRoot.setExpanded(true);
        TreeView<String> tree = new TreeView<>(treeRoot);
        TreeTableView<String> treeTable = new TreeTableView<>(treeRoot);
        TreeTableColumn<String, String> treeColumn = new TreeTableColumn<>("Column");
        treeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getValue()));
        treeTable.getColumns().add(treeColumn);
        VBox root = themed(list, table, tree, treeTable);

        assertFalse(root.lookupAll(".virtual-flow").isEmpty(), "the views draw their rows in virtual flows");
        assertFalse(root.lookupAll(".table-row-cell").isEmpty(), "with rows");
        assertTrue(scrollPanes(root).isEmpty(), "no scroll pane inside the views");
        List<Node> cached = cachedNodes(root);
        assertTrue(cached.isEmpty(), "nodes cached as bitmaps: " + cached);
    }

    @Test
    @DisplayName("a scroll pane that names a skin of its own in a rule of its own keeps it: here JavaFX's, which"
            + " caches the viewport")
    void ownSkinRuleWins() {
        ScrollPane own = new ScrollPane(new Label("Own skin"));
        own.setStyle("-fx-skin: \"javafx.scene.control.skin.ScrollPaneSkin\";");
        themed(own);

        assertSame(ScrollPaneSkin.class, own.getSkin().getClass());
        assertTrue(own.lookup(".viewport").isCache(), "JavaFX's skin caches the viewport");
    }

    @Test
    @DisplayName("the skin base.css names is the theme's, which JavaFX's controls create through the module's export"
            + " to them alone, and a native image through its registration for reflection")
    void skinWiring() throws IOException, ClassNotFoundException, NoSuchMethodException, URISyntaxException {
        String css = resource("/org/karthu/fx/theme/css/base.css");
        Matcher rule = Pattern.compile("\\.scroll-pane \\{[^}]*-fx-skin: \"([^\"]+)\";").matcher(css);
        assertTrue(rule.find(), "base.css names the skin on .scroll-pane: " + SKIN_RULE);
        String skinClassName = rule.group(1);
        Class<?> skinClass = Class.forName(skinClassName);
        assertSame(UncachedScrollPaneSkin.class, skinClass);
        assertNotNull(skinClass.getConstructor(ScrollPane.class), "JavaFX looks for a public constructor");

        // The module's compiled descriptor, read from where the skin was loaded: the suite may run on the class path,
        // where no module is named and every package is open to all.
        Path classes = Path.of(skinClass.getProtectionDomain().getCodeSource().getLocation().toURI());
        ModuleDescriptor descriptor;
        try (InputStream in = Files.newInputStream(classes.resolve("module-info.class"))) {
            descriptor = ModuleDescriptor.read(in);
        }
        ModuleDescriptor.Exports export = null;
        for (ModuleDescriptor.Exports candidate : descriptor.exports()) {
            if (candidate.source().equals(skinClass.getPackageName())) {
                export = candidate;
            }
        }
        assertNotNull(export, "the skin's package is exported, so JavaFX's controls can create the skin");
        assertEquals(Set.of("javafx.controls"), export.targets(), "to javafx.controls alone: not an API");

        // The metadata is a small JSON file of fixed shape; the test reads it as text, so it needs no JSON library.
        String metadata = resource(NATIVE_IMAGE_METADATA);
        Matcher registered = Pattern.compile("\\{\\s*\"type\"\\s*:\\s*\"" + Pattern.quote(skinClassName) + "\"\\s*,"
                + "\\s*\"methods\"\\s*:\\s*\\[\\s*\\{\\s*\"name\"\\s*:\\s*\"<init>\"\\s*,"
                + "\\s*\"parameterTypes\"\\s*:\\s*\\[\\s*\"([^\"]+)\"\\s*\\]\\s*\\}\\s*\\]").matcher(metadata);
        assertTrue(registered.find(), "the native image registers the skin's constructor for reflection");
        assertEquals(ScrollPane.class.getName(), registered.group(1));
    }

    @Test
    @DisplayName("with the theme applied before the scene's first CSS pass, the theme's skin is the scroll pane's first"
            + " and only skin: JavaFX's skin, once replaced, would keep listening to the content")
    void firstAndOnlySkin() {
        ScrollPane scrollPane = new ScrollPane(new Label("Content"));
        List<Object> skins = new ArrayList<>();
        scrollPane.skinProperty().addListener((observable, oldSkin, newSkin) -> skins.add(newSkin.getClass()));

        VBox root = themed(scrollPane);
        root.applyCss();
        root.layout();

        assertEquals(List.of(UncachedScrollPaneSkin.class), skins);
    }

    /**
     * Puts the nodes into a scene with the theme that is never shown and applies the CSS and a layout pass, which
     * creates the controls' skins.
     */
    private static VBox themed(Node... nodes) {
        VBox root = new VBox(nodes);
        Scene scene = new Scene(root, 400, 600);
        ThemeManager.apply(scene);
        root.applyCss();
        root.layout();
        return root;
    }

    private static void assertUncached(ScrollPane scrollPane) {
        Node viewport = null;
        for (Node child : scrollPane.getChildrenUnmodifiable()) {
            if (child.getStyleClass().contains("viewport")) {
                viewport = child;
            }
        }
        assertNotNull(viewport, "the skin's viewport");
        assertFalse(viewport.isCache(), "a viewport without bitmap cache");
    }

    private static List<ScrollPane> scrollPanes(Parent root) {
        List<ScrollPane> scrollPanes = new ArrayList<>();
        for (Node node : root.lookupAll(".scroll-pane")) {
            scrollPanes.add((ScrollPane) node);
        }
        return scrollPanes;
    }

    private static List<Node> cachedNodes(Node node) {
        List<Node> cached = new ArrayList<>();
        if (node.isCache()) {
            cached.add(node);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                cached.addAll(cachedNodes(child));
            }
        }
        return cached;
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = ScrollPaneSkinTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "no resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
