package xyz.gnas.elif.app.common.utility.code;

import static javafx.application.Platform.runLater;
import static xyz.gnas.elif.app.common.utility.DialogUtility.showError;

/**
 * Provides wrapper to execute code and catch exception, which is usually shown by calling DialogUtility.showError()
 */
public class CodeRunnerUtility {
    /**
     * Execute runner
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     */
    public static void executeRunner(Class callerClass, String errorMessage, Runner runner) {
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
    public static void executeRunnerOrExit(Class callerClass, String errorMessage, Runner runner) {
        try {
            runner.run();
        } catch (Exception e) {
            showError(callerClass, errorMessage, e, true);
        }
    }

    public static int executeRunnerWithIntReturn(Class callerClass, String errorMessage, int errorReturnValue,
                                                 RunnerWithIntReturn runner) {
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
    public static Object executeRunnerWithObjectReturn(Class callerClass, String errorMessage,
                                                       RunnerWithObjectReturn runner) {
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
    public static void executeRunnerAndHandleException(Runner runner, ExceptionHandler handler) {
        try {
            runner.run();
        } catch (Exception e) {
            handler.run(e);
        }
    }

    /**
     * Run code in a new thread
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     * @return the thread
     */
    public static Thread runInSideThread(Class callerClass, String errorMessage, Runner runner) {
        Thread t = new Thread(new SideThreadTaskRunner(callerClass, errorMessage, runner));
        t.start();
        return t;
    }

    /**
     * Run code in the main thread
     *
     * @param callerClass  the caller class
     * @param errorMessage the error message
     * @param runner       the runner
     */
    public static void runInMainThread(Class callerClass, String errorMessage, Runner runner) {
        runLater(new MainThreadTaskRunner(callerClass, errorMessage, runner));
    }

    /**
     * Run in main thread and handle exception.
     *
     * @param runner  the runner
     * @param handler the handler
     */
    public static void runInMainThreadAndHandleException(Runner runner, ExceptionHandler handler) {
        runLater(new MainThreadTaskRunner(runner, handler));
    }
}
