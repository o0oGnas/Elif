package main.java.xyz.gnas.elif;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.java.xyz.gnas.elif.common.CommonUtility;
import main.java.xyz.gnas.elif.common.ResourceManager;

public class Main extends Application {
	@Override
	public void start(Stage stage) {
		try {
			FXMLLoader loader = new FXMLLoader(ResourceManager.getAppFXML());
			Scene scene = new Scene((Parent) loader.load());
			scene.getStylesheets().addAll(ResourceManager.getCSSList());
			stage.setScene(scene);
			stage.setTitle("Piz");
			stage.getIcons().add(ResourceManager.getAppIcon());
			stage.setMaximized(true);
			stage.show();
		} catch (Exception e) {
			CommonUtility.showError(getClass(), e, "Could not start the application", true);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}