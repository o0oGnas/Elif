package main.java.xyz.gnas.elif.controllers;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import main.java.xyz.gnas.elif.common.CommonUtility;
import main.java.xyz.gnas.elif.common.ResourceManager;

public class AppController {
	@FXML
	private HBox hboExplorer;

	private ExplorerController leftController;

	private ExplorerController rightController;

	@FXML
	private void initialize() {
		try {
			leftController = loadExplorer();
			rightController = loadExplorer();
			File[] rootList = File.listRoots();
			leftController.initialiseAll(rootList);
			rightController.initialiseAll(rootList);
		} catch (Exception e) {
			CommonUtility.showError(e, "Could not initialise app", true);
		}
	}

	private ExplorerController loadExplorer() throws IOException {
		FXMLLoader loader = new FXMLLoader(ResourceManager.getExplorerFXML());
		Node explorer = loader.load();
		HBox.setHgrow(explorer, Priority.ALWAYS);
		hboExplorer.getChildren().add(explorer);
		return loader.getController();
	}
}