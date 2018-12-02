package xyz.gnas.elif.app.common.utility.runner;

import xyz.gnas.elif.app.common.utility.DialogUtility;

class MainThreadRunner implements Runnable {
    private Class callerClass;
    private String errorMessage;
    private VoidRunner runner;
    private ExceptionRunner exceptionHandler;

    public MainThreadRunner(Class callerClass, String errorMessage, VoidRunner runner) {
        this.callerClass = callerClass;
        this.errorMessage = errorMessage;
        this.runner = runner;
    }

    public MainThreadRunner(VoidRunner runner, ExceptionRunner exceptionHandler) {
        this.runner = runner;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void run() {
        try {
            runner.run();
        } catch (Exception e) {
            if (exceptionHandler == null) {
                DialogUtility.showError(callerClass, errorMessage, e, false);
            } else {
                exceptionHandler.run(e);
            }
        }
    }
}
