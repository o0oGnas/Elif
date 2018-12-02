package xyz.gnas.elif.app.common.utility.runner;

import javafx.concurrent.Task;
import xyz.gnas.elif.app.common.utility.DialogUtility;

class SideThreadRunner extends Task {
    private Class callerClass;
    private String errorMessage;
    private VoidRunner runner;

    public SideThreadRunner(Class callerClass, String errorMessage, VoidRunner runner) {
        this.callerClass = callerClass;
        this.errorMessage = errorMessage;
        this.runner = runner;
    }

    @Override
    protected Object call() {
        try {
            runner.run();
        } catch (Exception e) {
            DialogUtility.showError(callerClass, errorMessage, e, false);
        }

        return null;
    }
}
