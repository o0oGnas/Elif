package xyz.gnas.elif.app.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.operation.AddOperationEvent;
import xyz.gnas.elif.app.events.operation.InitialiseOperationEvent;
import xyz.gnas.elif.app.models.Operation;
import xyz.gnas.elif.app.models.Setting;
import xyz.gnas.elif.core.models.explorer.ExplorerModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AppController {
    @FXML
    private VBox vboOperations;

    @FXML
    private HBox hboExplorer;

    private ExplorerModel leftModel;
    private ExplorerModel rightModel;

    private List<Operation> operationList = new LinkedList<>();

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        Utility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onAddOperationEvent(AddOperationEvent event) {
        try {
            Operation operation = event.getOperation();
            operationList.add(operation);
            FXMLLoader loader = new FXMLLoader(ResourceManager.getOperationFXML());
            Node n = loader.load();
            vboOperations.getChildren().add(n);
            EventBus.getDefault().post(new InitialiseOperationEvent(operation));

            operation.isCompleteProperty().addListener(l -> {
                try {
                    if (operation.isIsComplete()) {
                        // play notification sound
                        Media media = ResourceManager.getNotificationSound();
                        MediaPlayer mediaPlayer = new MediaPlayer(media);
                        mediaPlayer.play();
                        vboOperations.getChildren().remove(n);
                    }
                } catch (Exception e) {
                    showError(e, "Error removing operation from list", false);
                }
            });
        } catch (Exception e) {
            showError(e, "Error handling add operation event", false);
        }
    }

    @FXML
    private void initialize() {
        try {
            EventBus.getDefault().register(this);
            ArrayList<ExplorerModel> modelList = Setting.getInstance().getExplorerModelList();

            if (modelList.isEmpty()) {
                writeInfoLog("Initialising explorer models");
                leftModel = new ExplorerModel();
                rightModel = new ExplorerModel();
                leftModel.setOtherModel(rightModel);
                rightModel.setOtherModel(leftModel);
                modelList.add(leftModel);
                modelList.add(rightModel);
            } else {
                leftModel = modelList.get(0);
                rightModel = modelList.get(1);
            }

            // load both sides
            writeInfoLog("Loading left side");
            loadExplorer(leftModel);
            writeInfoLog("Loading right side");
            loadExplorer(rightModel);
        } catch (Exception e) {
            showError(e, "Could not initialise app", true);
        }
    }

    private void loadExplorer(ExplorerModel model) throws IOException {
        FXMLLoader loader = new FXMLLoader(ResourceManager.getExplorerFXML());
        Node explorer = loader.load();
        HBox.setHgrow(explorer, Priority.ALWAYS);
        hboExplorer.getChildren().add(explorer);
        EventBus.getDefault().post(new InitialiseExplorerEvent(model));
    }
}