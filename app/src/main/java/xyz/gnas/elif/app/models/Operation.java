package xyz.gnas.elif.app.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import xyz.gnas.elif.core.models.Progress;

import java.util.LinkedList;
import java.util.List;

/**
 * Contains information about an operation
 */
public class Operation {
    private String name;

    private StringProperty suboperationName = new SimpleStringProperty();

    private BooleanProperty isComplete = new SimpleBooleanProperty();

    private DoubleProperty percentageDone = new SimpleDoubleProperty(0);

    private List<Progress> progressList = new LinkedList<>();

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

    public boolean isIsComplete() {
        return isComplete.get();
    }

    public BooleanProperty isCompleteProperty() {
        return isComplete;
    }

    public void setIsComplete(boolean isComplete) {
        this.isComplete.set(isComplete);
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

    public List<Progress> getProgressList() {
        return progressList;
    }

    public Operation(String name) {
        this.name = name;
    }
}
