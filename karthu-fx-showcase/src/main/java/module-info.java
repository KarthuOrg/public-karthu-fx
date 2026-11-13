module org.karthu.fx.showcase {
    requires org.karthu.fx;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.bootstrapicons;
    requires org.kordamp.ikonli.core;
    requires org.slf4j;

    opens org.karthu.fx.showcase to javafx.graphics, javafx.fxml;
    opens org.karthu.fx.showcase.controller to javafx.fxml;
    opens org.karthu.fx.showcase.controller.showcase to javafx.fxml;
    opens org.karthu.fx.showcase.viewmodel to javafx.fxml;
}
