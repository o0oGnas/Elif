package xyz.gnas.elif.core.models;

public class Progress {
    private double percentageDone;

    private boolean isComplete;

    private boolean isStop;

    public double getPercentageDone() {
        return percentageDone;
    }

    public void setPercentageDone(double percentageDone) {
        this.percentageDone = percentageDone;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public boolean isStop() {
        return isStop;
    }

    public void setStop(boolean stop) {
        isStop = stop;
    }
}
