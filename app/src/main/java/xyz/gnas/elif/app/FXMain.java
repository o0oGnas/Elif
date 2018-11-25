package xyz.gnas.elif.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.greenrobot.eventbus.EventBus;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.window.ExitEvent;
import xyz.gnas.elif.app.events.window.WindowFocusedEvent;

import static xyz.gnas.elif.app.common.Utility.writeErrorLog;

public class FXMain extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> writeErrorLog(getClass(), "Uncaught "
                    + "exception", e));

            stage.setOnCloseRequest((WindowEvent arg0) -> {
                // raise exit event
                EventBus.getDefault().post(new ExitEvent(arg0));
            });

            stage.focusedProperty().addListener(l -> {
                try {
                    EventBus.getDefault().post(new WindowFocusedEvent());
                } catch (Exception e) {
                    Utility.showError(getClass(), e, "Could not handle window focused event", false);
                }
            });

            FXMLLoader loader = new FXMLLoader(ResourceManager.getAppFXML());
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().addAll(ResourceManager.getCSSList());
            stage.setScene(scene);
            stage.setTitle("Elif");
            stage.getIcons().add(ResourceManager.getAppIcon());
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            writeErrorLog(getClass(), "Could not start the application", e);
        }
    }
}