package xyz.gnas.elif.core.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Contains information about an operation
 */
public class Operation {
    private String name;

    private StringProperty suboperationName = new SimpleStringProperty();

    private BooleanProperty paused = new SimpleBooleanProperty();

    private BooleanProperty stopped = new SimpleBooleanProperty();

    private BooleanProperty complete = new SimpleBooleanProperty();

    private DoubleProperty percentageDone = new SimpleDoubleProperty();

    public String getName() {
        return name;
    }

    public String getSuboperationName() {
        return suboperationName.get();
    }

    public StringProperty suboperationNameProperty() {
        return suboperationName;
    }

    public void setSuboperationName(String suboperationName) {
        this.suboperationName.set(suboperationName);
    }

    public boolean getPaused() {
        return paused.get();
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused.set(paused);
    }

    public boolean getStopped() {
        return stopped.get();
    }

    public BooleanProperty stoppedProperty() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        if (stopped) {
            this.paused.set(false);
        }

        this.stopped.set(stopped);
    }

    public boolean getComplete() {
        return complete.get();
    }

    public BooleanProperty completeProperty() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete.set(complete);
    }

    public double getPercentageDone() {
        return percentageDone.get();
    }

    public DoubleProperty percentageDoneProperty() {
        return percentageDone;
    }

    public void setPercentageDone(double percentageDone) {
        this.percentageDone.set(percentageDone);
    }

    public Operation(String name) {
        this.name = name;
    }
}
