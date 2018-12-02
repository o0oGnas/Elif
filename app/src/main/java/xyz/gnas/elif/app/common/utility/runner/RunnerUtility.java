package xyz.gnas.elif.app.common.utility.runner;

import static javafx.application.Platform.runLater;
import static xyz.gnas.elif.app.common.utility.DialogUtility.showError;

/**
 * Provides wrapper to execute runner and catch exception, which is usually shown by calling DialogUtility.showError()
 */
public class RunnerUtility {
    /**
     * Execute runner
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     */
    public static void executeVoidrunner(Class callerClass, String errorMessage, VoidRunner runner) {
        try {
            runner.run();
        } catch (Exception e) {
            showError(callerClass, errorMessage, e, false);
        }
    }

    /**
     * Execute runner and exit if an exception is thrown
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     */
    public static void executeVoidRunnerOrExit(Class callerClass, String errorMessage, VoidRunner runner) {
        try {
            runner.run();
        } catch (Exception e) {
            showError(callerClass, errorMessage, e, true);
        }
    }

    public static int executeIntRunner(Class callerClass, String errorMessage, int errorReturnValue,
                                       IntRunner runner) {
        try {
            return runner.run();
        } catch (Exception e) {
            showError(callerClass, errorMessage, e, false);
            return errorReturnValue;
        }
    }

    public static boolean executeBooleanRunner(Class callerClass, String errorMessage,
                                               boolean errorReturnValue, BooleanRunner runner) {
        try {
            return runner.run();
        } catch (Exception e) {
            showError(callerClass, errorMessage, e, false);
            return errorReturnValue;
        }
    }

    /**
     * Execute runner with return object.
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     * @return the object
     */
    public static Object executeObjectRunner(Class callerClass, String errorMessage,
                                             ObjectRunner runner) {
        try {
            return runner.run();
        } catch (Exception e) {
            showError(callerClass, errorMessage, e, false);
            return null;
        }
    }

    /**
     * Execute runner with custom exception handling
     *
     * @param runner  the runner
     * @param handler the exception handler
     */
    public static void executeVoidAndExceptionRunner(VoidRunner runner, ExceptionRunner handler) {
        try {
            runner.run();
        } catch (Exception e) {
            handler.run(e);
        }
    }

    /**
     * Run runner in a new thread
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     * @return the thread
     */
    public static Thread executeSideThreadRunner(Class callerClass, String errorMessage, VoidRunner runner) {
        Thread t = new Thread(new SideThreadRunner(callerClass, errorMessage, runner));
        t.start();
        return t;
    }

    /**
     * Run runner in the main thread
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     */
    public static void executeMainThreadRunner(Class callerClass, String errorMessage, VoidRunner runner) {
        runLater(new MainThreadRunner(callerClass, errorMessage, runner));
    }

    /**
     * Run in main thread and handle exception.
     *
     * @param runner  the runner
     * @param handler the handler
     */
    public static void executeMainThreadAndExceptionRunner(VoidRunner runner, ExceptionRunner handler) {
        runLater(new MainThreadRunner(runner, handler));
    }
}
