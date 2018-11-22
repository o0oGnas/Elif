package xyz.gnas.elif.app.controllers;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.ExitEvent;
import xyz.gnas.elif.app.events.dialog.SingleRenameEvent;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.operation.AddOperationEvent;
import xyz.gnas.elif.app.events.operation.InitialiseOperationEvent;
import xyz.gnas.elif.app.models.Setting;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;
import xyz.gnas.elif.core.models.Operation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static xyz.gnas.elif.app.common.Utility.showConfirmation;
import static xyz.gnas.elif.app.common.Utility.showCustomDialog;

public class AppController {
    @FXML
    private HBox hboExplorer;

    @FXML
    private ScrollPane scpOperation;

    @FXML
    private VBox vboOperations;

    private Node singleRenameDialog = null;

    private List<Operation> operationList = new LinkedList<>();

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        Utility.writeInfoLog(getClass(), log);
    }

    private void postEvent(Object object) {
        EventBus.getDefault().post(object);
    }

    @Subscribe
    public void onExitEvent(ExitEvent event) {
        try {
            // show confirmation is there are running processes
            if (!operationList.isEmpty()) {
                if (showConfirmation("There are running operations, are you sure you want to exit?")) {
                    for (Operation operation : operationList) {
                        operation.setStopped(true);
                    }
                } else {
                    event.getWindowEvent().consume();
                }
            }
        } catch (Exception e) {
            showError(e, "Error when closing the application", true);
        }
    }

    @Subscribe
    public void onAddOperationEvent(AddOperationEvent event) {
        try {
            Operation operation = event.getOperation();
            operationList.add(operation);
            FXMLLoader loader = new FXMLLoader(ResourceManager.getOperationFXML());
            Node n = loader.load();
            vboOperations.getChildren().add(0, n);
            addOperationCompleteListener(operation, n);
            postEvent(new InitialiseOperationEvent(operation));
        } catch (Exception e) {
            showError(e, "Error handling add operation event", false);
        }
    }

    private void addOperationCompleteListener(Operation operation, Node n) {
        operation.completeProperty().addListener(l -> {
            try {
                if (operation.isComplete()) {
                    // play notification sound is operation is not stopped
                    if (!operation.isStopped()) {
                        Media media = ResourceManager.getNotificationSound();
                        MediaPlayer mediaPlayer = new MediaPlayer(media);
                        mediaPlayer.play();
                    }

                    removeOperation(operation, n);
                }
            } catch (Exception e) {
                showError(e, "Error removing operation from list", false);
            }
        });
    }

    private synchronized void removeOperation(Operation operation, Node n) {
        operationList.remove(operation);
        vboOperations.getChildren().remove(n);
    }

    @Subscribe
    public void onSingleRenameEvent(SingleRenameEvent event) {
        try {
            showCustomDialog("Single rename", singleRenameDialog, ResourceManager.getRenameSingleIcon());
        } catch (Exception e) {
            showError(e, "Error handling single rename event", false);
        }
    }

    @FXML
    private void initialize() {
        try {
            EventBus.getDefault().register(this);
            scpOperation.setManaged(false);
            scpOperation.managedProperty().bind(scpOperation.visibleProperty());

            // only show scroll pane if there are running operations
            vboOperations.getChildren().addListener((ListChangeListener<Node>) l -> {
                scpOperation.setVisible(!vboOperations.getChildren().isEmpty());
            });

            initialiseExplorers();
            FXMLLoader loader = new FXMLLoader(ResourceManager.getSingleRenameFXML());
            singleRenameDialog = loader.load();
        } catch (Exception e) {
            showError(e, "Could not initialise app", true);
        }
    }

    private void initialiseExplorers() throws IOException {
        Setting setting = Setting.getInstance();

        // load both sides
        writeInfoLog("Loading left side");
        loadExplorer(setting.getLeftModel(), true);
        writeInfoLog("Loading right side");
        loadExplorer(setting.getRightModel(), false);
    }

    private void loadExplorer(ExplorerModel model, boolean isLeft) throws IOException {
        FXMLLoader loader = new FXMLLoader(ResourceManager.getExplorerFXML());
        Node explorer = loader.load();
        HBox.setHgrow(explorer, Priority.ALWAYS);
        hboExplorer.getChildren().add(explorer);
        postEvent(new InitialiseExplorerEvent(model, isLeft));
    }
}