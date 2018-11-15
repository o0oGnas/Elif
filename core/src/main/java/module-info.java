module xyz.gnas.elif.core {
    requires javafx.controls;

    requires transitive com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    requires transitive org.apache.commons.io;

    exports xyz.gnas.elif.core.logic;
    exports xyz.gnas.elif.core.models;
    exports xyz.gnas.elif.core.models.explorer;
}