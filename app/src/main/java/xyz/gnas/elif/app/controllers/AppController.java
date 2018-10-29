package xyz.gnas.elif.app.controllers;

import java.io.IOException;

import org.greenrobot.eventbus.EventBus;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.LoadRootsEvent;

public class AppController {
	@FXML
	private HBox hboExplorer;

	private void showError(Exception e, String message, boolean exit) {
		Utility.showError(getClass(), e, message, exit);
	}

	@FXML
	private void initialize() {
		try {
			// load both sides
			loadExplorer();
			loadExplorer();
			EventBus.getDefault().post(new LoadRootsEvent());
		} catch (Exception e) {
			showError(e, "Could not initialise app", true);
		}
	}

	private void loadExplorer() throws IOException {
		FXMLLoader loader = new FXMLLoader(ResourceManager.getExplorerFXML());
		Node explorer = loader.load();
		HBox.setHgrow(explorer, Priority.ALWAYS);
		hboExplorer.getChildren().add(explorer);
	}
}