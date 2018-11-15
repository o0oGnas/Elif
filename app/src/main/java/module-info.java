module xyz.gnas.elif.app {
    requires xyz.gnas.elif.core;

    requires transitive javafx.fxml;
    requires transitive javafx.controls;
    requires javafx.media;

    requires de.jensd.fx.glyphs.materialicons;
    requires de.jensd.fx.glyphs.fontawesome;
    requires java.desktop;

    requires org.slf4j;
    requires ch.qos.logback.core;
    requires eventbus;
}