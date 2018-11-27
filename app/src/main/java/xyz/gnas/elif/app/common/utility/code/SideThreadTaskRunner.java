package xyz.gnas.elif.app.common.utility.code;

import javafx.concurrent.Task;

import static xyz.gnas.elif.app.common.utility.DialogUtility.showError;

public class SideThreadTaskRunner extends Task {
    private Class callerClass;
    private String errorMessage;
    private Runner runner;

    public SideThreadTaskRunner(Class callerClass, String errorMessage, Runner runner) {
        this.callerClass = callerClass;
        this.errorMessage = errorMessage;
        this.runner = runner;
    }

    @Override
    protected Object call() {
        try {
            runner.run();
        } catch (Exception e) {
            showError(callerClass, errorMessage, e, false);
        }

        return null;
    }
}
