module xyz.gnas.elif.app {
    requires xyz.gnas.elif.core;

    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.media;

    requires de.jensd.fx.glyphs.materialicons;
    requires de.jensd.fx.glyphs.fontawesome;
    requires de.jensd.fx.glyphs.octicons;
    requires java.desktop;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    requires org.apache.commons.io;

    requires org.slf4j;
    requires ch.qos.logback.core;
    requires eventbus;
}