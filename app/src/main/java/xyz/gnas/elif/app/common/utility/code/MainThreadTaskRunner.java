package xyz.gnas.elif.app.common.utility.code;

import static xyz.gnas.elif.app.common.utility.DialogUtility.showError;

class MainThreadTaskRunner implements Runnable {
    private Class callerClass;
    private String errorMessage;
    private Runner runner;
    private ExceptionHandler exceptionHandler;

    public MainThreadTaskRunner(Class callerClass, String errorMessage, Runner runner) {
        this.callerClass = callerClass;
        this.errorMessage = errorMessage;
        this.runner = runner;
    }

    public MainThreadTaskRunner(Runner runner, ExceptionHandler exceptionHandler) {
        this.runner = runner;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void run() {
        try {
            runner.run();
        } catch (Exception e) {
            if (exceptionHandler == null) {
                showError(callerClass, errorMessage, e, false);
            } else {
                exceptionHandler.run(e);
            }
        }
    }
}
