package xyz.gnas.elif.app.common.utility;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class WindowEventUtility {
    public interface WindowEventHandler {
        void handleShownEvent();

        void handleFocusedEvent();

        void handleCloseEvent(WindowEvent windowEvent);
    }

    private static void showError(Throwable e, String message, boolean exit) {
        DialogUtility.showError(WindowEventUtility.class, e, message, exit);
    }

    public static void bindWindowEventHandler(Node node, WindowEventHandler handler) {
        bindSceneListener(node, handler);

        node.sceneProperty().addListener(s -> {
            try {
                bindSceneListener(node, handler);
            } catch (Exception e) {
                showError(e, "Error handling scene change event", false);
            }
        });
    }

    private static void bindSceneListener(Node node, WindowEventHandler handler) {
        Scene scene = node.getScene();

        if (scene != null) {
            bindWindowListener(scene, handler);

            scene.windowProperty().addListener(w -> {
                try {
                    bindWindowListener(scene, handler);
                } catch (Exception e) {
                    showError(e, "Error handling window change event", false);
                }
            });
        }
    }

    private static void bindWindowListener(Scene scene, WindowEventHandler handler) {
        Window window = scene.getWindow();

        if (window != null) {
            window.setOnShown(l -> handler.handleShownEvent());

            window.focusedProperty().addListener(l -> {
                try {
                    if (window.isFocused()) {
                        handler.handleFocusedEvent();
                    }
                } catch (Exception e) {
                    showError(e, "Error handling window focused event", false);
                }
            });

            window.setOnCloseRequest((WindowEvent windowEvent) -> handler.handleCloseEvent(windowEvent));
        }
    }
}
