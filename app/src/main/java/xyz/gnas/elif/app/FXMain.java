package xyz.gnas.elif.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;

public class FXMain extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
                Utility.writeErrorLog(getClass(), "Uncaught exception", e);
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
            Utility.showError(getClass(), e, "Could not start the application", true);
        }
    }
}