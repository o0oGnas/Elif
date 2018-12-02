package xyz.gnas.elif.app.common.utility.window_event;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import xyz.gnas.elif.app.common.utility.runner.RunnerUtility;
import xyz.gnas.elif.app.common.utility.runner.VoidRunner;

public class WindowEventUtility {
    private static void executeRunner(Class callerClass, String errorMessage, VoidRunner runner) {
        RunnerUtility.executeVoidrunner(callerClass, errorMessage, runner);
    }

    /**
     * Bind window_event event handler.
     *
     * @param callerClass the caller class
     * @param node        a control in the calling class to get its window_event
     * @param handler     the handler
     */
    public static void bindWindowEventHandler(Class callerClass, Node node, WindowEventHandler handler) {
        bindSceneListener(callerClass, node, handler);
        node.sceneProperty().addListener(l -> executeRunner(callerClass, "Error when handling scene change event",
                () -> bindSceneListener(callerClass, node, handler)));
    }

    private static void bindSceneListener(Class callerClass, Node node, WindowEventHandler handler) {
        Scene scene = node.getScene();

        if (scene != null) {
            bindWindowListener(callerClass, scene, handler);
            scene.windowProperty().addListener(
                    l -> executeRunner(callerClass, "Error when handling window_event change event",
                            () -> bindWindowListener(callerClass, scene, handler)));
        }
    }

    private static void bindWindowListener(Class callerClass, Scene scene, WindowEventHandler handler) {
        Window window = scene.getWindow();

        if (window != null) {
            window.setOnShown(l -> executeRunner(callerClass, "Error when handling window_event shown event",
                    handler::handleShownEvent));

            window.focusedProperty().addListener(
                    l -> executeRunner(callerClass, "Error when handling window_event focused event", () -> {
                        if (window.isFocused()) {
                            handler.handleFocusedEvent();
                        }
                    }));

            window.setOnCloseRequest((WindowEvent windowEvent) ->
                    executeRunner(callerClass, "Error when handling window_event close event",
                            () -> handler.handleCloseEvent(windowEvent)));
        }
    }
}
