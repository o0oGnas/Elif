package xyz.gnas.elif.app.controllers;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
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
import xyz.gnas.elif.app.events.dialog.SimpleRenameEvent;
import xyz.gnas.elif.app.events.explorer.ChangeItemSelectionEvent;
import xyz.gnas.elif.app.events.explorer.FocusExplorerEvent;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.events.operation.AddNewFileEvent;
import xyz.gnas.elif.app.events.operation.AddNewFolderEvent;
import xyz.gnas.elif.app.events.operation.CopyToClipboardEvent;
import xyz.gnas.elif.app.events.operation.CopyToOtherEvent;
import xyz.gnas.elif.app.events.operation.DeleteEvent;
import xyz.gnas.elif.app.events.operation.InitialiseOperationEvent;
import xyz.gnas.elif.app.events.operation.MoveEvent;
import xyz.gnas.elif.app.events.operation.PasteEvent;
import xyz.gnas.elif.app.events.window.ExitEvent;
import xyz.gnas.elif.app.models.Setting;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;
import xyz.gnas.elif.core.logic.ClipboardLogic;
import xyz.gnas.elif.core.logic.FileLogic;
import xyz.gnas.elif.core.models.Operation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Thread.sleep;
import static javafx.application.Platform.runLater;
import static xyz.gnas.elif.app.common.Utility.showAlert;
import static xyz.gnas.elif.app.common.Utility.showConfirmation;
import static xyz.gnas.elif.app.common.Utility.showCustomDialog;

public class AppController {
    @FXML
    private HBox hboExplorer;

    @FXML
    private HBox hboRenameEditCopyMove;

    @FXML
    private HBox hboNewFolderFile;

    @FXML
    private ScrollPane scpOperation;

    @FXML
    private VBox vboOperations;

    private enum CopyMode {
        COPY, MOVE, PASTE
    }

    private final int THREAD_SLEEP_TIME = 500;

    private Node singleRenameDialog = null;

    private List<Operation> operationList = new LinkedList<>();

    /**
     * The model of the currently selected tab
     */
    private ExplorerModel selectedModel;

    /**
     * List of selected items in the last active tab
     */
    private List<ExplorerItemModel> selectedItemList;

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeErrorLog(String message, Throwable e) {
        Utility.writeErrorLog(getClass(), message, e);
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
    public void onFocusExplorerEvent(FocusExplorerEvent event) {
        try {
            selectedModel = event.getModel();
            hboNewFolderFile.setDisable(false);
        } catch (Exception e) {
            showError(e, "Error when handling focus change event", false);
        }
    }

    @Subscribe
    public void onChangeItemSelectionEvent(ChangeItemSelectionEvent event) {
        try {
            selectedItemList = event.getItemList();

            if (selectedItemList != null && !selectedItemList.isEmpty()) {
                hboRenameEditCopyMove.setDisable(false);
            }
        } catch (Exception e) {
            showError(e, "Error when handling focus change event", false);
        }
    }

    @Subscribe
    public void onCopyToOtherEvent(CopyToOtherEvent event) {
        try {
            copy(event.getSourceModel(), event.getSourceList(), getTargetPath(event.getSourceModel()), CopyMode.COPY);
        } catch (Exception e) {
            showError(e, "Error when copying to other tab", false);
        }
    }

    private String getTargetPath(ExplorerModel sourceModel) {
        Setting setting = Setting.getInstance();
        ExplorerModel leftModel = setting.getLeftModel();
        ExplorerModel targetModel = sourceModel == leftModel ? setting.getRightModel() : leftModel;
        return targetModel.getFolder().getAbsolutePath();
    }

    private void copy(ExplorerModel model, List<ExplorerItemModel> sourceList, String targetPath,
                      CopyMode mode) throws IOException {
        if (mode == CopyMode.MOVE && model.getFolder().getAbsolutePath().equalsIgnoreCase(targetPath)) {
            showAlert("Invalid operation", "Cannot move files to their current folder!");
        } else {
            CopyParameterContainer container = new CopyParameterContainer();
            container.model = model;
            container.sourceList = sourceList;
            container.mode = mode;
            container.targetPath = targetPath + "\\";
            copyFromSourceList(container);
        }
    }

    private void copyFromSourceList(CopyParameterContainer container) throws IOException {
        Map<ExplorerItemModel, File> sourceTargetMap = new TreeMap<>();

        for (ExplorerItemModel source : container.sourceList) {
            File targetFile = new File(container.targetPath + source.getFile().getName());

            if (targetFile.exists()) {
                container.hasDuplicate = true;
            }

            container.totalSize += source.getSize();
            sourceTargetMap.put(source, targetFile);
        }

        container.sourceTargetMap = sourceTargetMap;
        confirmDuplicateAndCopy(container);
    }

    private void confirmDuplicateAndCopy(CopyParameterContainer container) throws IOException {
        String replace = "Replace existing files";
        String duplicate = "Copy as duplicates";
        String skip = "Skip existing files";
        String cancel = "Cancel";
        String confirmResult = duplicate;
        boolean checkDifferentPath =
                !container.model.getFolder().getAbsolutePath().equalsIgnoreCase(container.targetPath);

        // Ask for choice if mode is copy and paths are different, for other modes always ask
        if ((checkDifferentPath || container.mode != CopyMode.COPY) && container.hasDuplicate) {
            confirmResult = Utility.showOptions("There are files in the target folder with the same name", replace,
                    skip, duplicate, cancel);
        }

        if (confirmResult != null && !confirmResult.equalsIgnoreCase(cancel)) {
            container.confirmResult = confirmResult;
            performCopy(container, skip, duplicate);
        }
    }

    private void performCopy(CopyParameterContainer container, String skip, String duplicate) throws IOException {
        if (container.confirmResult.equalsIgnoreCase(skip)) {
            removeExistingFiles(container);
        }

        String operationName = container.mode == CopyMode.MOVE ? "Move" : "Copy";
        container.operation = createNewOperation(operationName + " files to " + container.targetPath);
        runCopyMasterThread(container, duplicate);
    }

    private void removeExistingFiles(CopyParameterContainer container) {
        Map<ExplorerItemModel, File> temp = new HashMap<>(container.sourceTargetMap);
        container.sourceTargetMap.clear();

        for (ExplorerItemModel source : temp.keySet()) {
            File target = temp.get(source);

            if (target.exists()) {
                container.totalSize -= source.getSize();
            } else {
                container.sourceTargetMap.put(source, target);
            }
        }
    }

    private Operation createNewOperation(String name) throws IOException {
        Operation operation = new Operation(name);
        operationList.add(operation);
        FXMLLoader loader = new FXMLLoader(ResourceManager.getOperationFXML());
        Node n = loader.load();
        vboOperations.getChildren().add(0, n);
        addOperationCompleteListener(operation, n);
        postEvent(new InitialiseOperationEvent(operation));
        return operation;
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

                    operationList.remove(operation);
                    vboOperations.getChildren().remove(n);
                }
            } catch (Exception e) {
                showError(e, "Error removing operation from list", false);
            }
        });
    }

    private void runCopyMasterThread(CopyParameterContainer container, String duplicate) {
        runNewThread(new Task<>() {
            @Override
            protected Integer call() {
                try {
                    processSourceTargetMap(container, duplicate);

                    runLater(() -> {
                                try {
                                    container.operation.setComplete(true);
                                } catch (Exception e) {
                                    showError(e, "Error when completing copy master thread", false);
                                }
                            }
                    );
                } catch (Exception e) {
                    showError(e, "Error when copying files", false);
                }

                return 1;
            }
        });
    }

    private void runNewThread(Task<Integer> task) {
        new Thread(task).start();
    }

    private void processSourceTargetMap(CopyParameterContainer container, String duplicate) throws InterruptedException {
        for (ExplorerItemModel source : container.sourceTargetMap.keySet()) {
            while (container.operation.isPaused()) {
                sleep(THREAD_SLEEP_TIME);
            }

            if (container.operation.isStopped()) {
                break;
            } else {
                createCopySuboperation(container, source, duplicate);
            }
        }
    }

    private void createCopySuboperation(CopyParameterContainer container, ExplorerItemModel source, String duplicate) throws InterruptedException {
        runLater(() -> {
            try {
                String name = container.mode == CopyMode.MOVE ? "Moving" : "Copying";
                container.operation.setSuboperationName(name + " " + source.getFile().getAbsolutePath());
            } catch (Exception e) {
                showError(e, "Error when changing sub operation name", false);
            }
        });

        checkAndUpdateSourceTargetMap(container, source, duplicate);
        copyFile(container, source);
    }

    private void checkAndUpdateSourceTargetMap(CopyParameterContainer container, ExplorerItemModel source,
                                               String duplicate) {
        if (container.sourceTargetMap.get(source).exists()) {
            if (container.confirmResult.equalsIgnoreCase(duplicate)) {
                // update target name to prevent overwriting existing file
                updateSourceTargetMap(container, source);
            }
        }
    }

    private void updateSourceTargetMap(CopyParameterContainer container, ExplorerItemModel source) {
        int index = 2;

        // add suffix to target name until there's no more duplicate
        do {
            File target =
                    new File(container.targetPath + source.getName() + " (" + index + ")." + source.getExtension());
            container.sourceTargetMap.put(source, target);
            ++index;
        } while (container.sourceTargetMap.get(source).exists());
    }

    private void copyFile(CopyParameterContainer container, ExplorerItemModel source) throws InterruptedException {
        DoubleProperty progress = new SimpleDoubleProperty();
        BooleanProperty error = new SimpleBooleanProperty();
        File target = container.sourceTargetMap.get(source);

        runNewThread(new Task<>() {
            @Override
            protected Integer call() {
                try {
                    moveOrCopy(container, source, progress);
                } catch (Exception e) {
                    handleCopyError(container, error, progress, source, target, e);
                }

                return 1;
            }
        });

        monitorFileProgress(container, progress, error, source, target);
    }

    private void moveOrCopy(CopyParameterContainer container, ExplorerItemModel source, DoubleProperty progress)
            throws IOException, InterruptedException {
        File sourceFile = source.getFile();

        if (container.mode == CopyMode.MOVE) {
            FileLogic.move(sourceFile, container.sourceTargetMap.get(source), container.operation, progress);
        } else {
            FileLogic.copy(sourceFile, container.sourceTargetMap.get(source), container.operation, progress);
        }
    }

    private void handleCopyError(CopyParameterContainer container, BooleanProperty error, DoubleProperty progress,
                                 ExplorerItemModel source,
                                 File target, Exception e) {
        error.set(true);
        String sourcePath = source.getFile().getAbsolutePath();
        writeErrorLog("Error when copying file", e);

        runLater(() -> {
            try {
                // ask for confirmation to continue when there is an error
                if (showConfirmation("Error when copying " + sourcePath + " to " + target.getAbsolutePath() + " - " + e.getMessage() +
                        ". Do you want to " + "continue?")) {
                    // considered the file with the error finished
                    progress.set(1);
                } else {
                    container.operation.setStopped(true);
                }
            } catch (Exception ex) {
                showError(e, "Error when handling copy error", false);
            }
        });
    }

    private void monitorFileProgress(CopyParameterContainer container, DoubleProperty progress, BooleanProperty error
            , ExplorerItemModel source, File target) throws InterruptedException {
        double currentPercentageDone = container.operation.getPercentageDone();

        // calculate the amount of contribution this file has to the total size of the operation
        double contribution = source.getSize() / container.totalSize;

        while (progress.get() < 1 && !container.operation.isStopped()) {
            setPercentageDone(container, currentPercentageDone + contribution * progress.get());
            sleep(THREAD_SLEEP_TIME);
        }

        finishCopyFileProgress(container, error, target, currentPercentageDone, contribution);
    }

    private void setPercentageDone(CopyParameterContainer container, double percent) {
        runLater(() -> {
            try {
                container.operation.setPercentageDone(percent);
            } catch (Exception e) {
                showError(e, "Error updating operation progress", false);
            }
        });
    }

    private void finishCopyFileProgress(CopyParameterContainer container, BooleanProperty error, File target,
                                        double currentPercentageDone, double contribution) {
        setPercentageDone(container, currentPercentageDone + contribution);

        runLater(() -> {
            try {
                // reload source folder if operation is move and there was no error
                if (container.mode == CopyMode.MOVE && !container.operation.isStopped() && !error.get()) {
                    postEvent(new ReloadEvent(container.model.getFolder().getAbsolutePath()));
                }

                // reload target folder each time a file process is finished
                postEvent(new ReloadEvent(target.getParent()));
            } catch (Exception e) {
                showError(e, "Error updating finished progress", false);
            }
        });
    }

    @Subscribe
    public void onCopyToClipboardEvent(CopyToClipboardEvent event) {
        try {
            List<File> fileList = new LinkedList<>();

            for (ExplorerItemModel item : event.getSourceList()) {
                fileList.add(item.getFile());
            }

            ClipboardLogic.copyToClipboard(fileList);
        } catch (Exception e) {
            showError(e, "Error when copying to clipboard", false);
        }
    }

    @Subscribe
    public void onPasteEvent(PasteEvent event) {
        try {
            List<File> fileList = ClipboardLogic.getFiles();

            if (fileList != null && !fileList.isEmpty()) {
                List<ExplorerItemModel> sourceList = new LinkedList<>();

                for (File source : fileList) {
                    // only paste if source file still exists
                    if (source.exists()) {
                        ExplorerItemModel item = new ExplorerItemModel(source);
                        sourceList.add(item);
                    }
                }

                if (!sourceList.isEmpty()) {
                    writeInfoLog("Pasting files from clipboard");
                    ExplorerModel model = event.getSourceModel();
                    copy(event.getSourceModel(), sourceList, model.getFolder().getAbsolutePath(), CopyMode.PASTE);
                }
            }
        } catch (Exception e) {
            showError(e, "Error when pasting from clipboard", false);
        }
    }

    @Subscribe
    public void onMoveEvent(MoveEvent event) {
        try {
            copy(event.getSourceModel(), event.getSourceList(), getTargetPath(event.getSourceModel()), CopyMode.MOVE);
        } catch (Exception e) {
            showError(e, "Error when moving files", false);
        }
    }

    @Subscribe
    public void onDeleteEvent(DeleteEvent event) {
        try {
            final List<ExplorerItemModel> sourceList = event.getSourceList();

            if (showConfirmation("Are you sure you want to delete selected files/folders (" + sourceList.size() + ")" + "?")) {
                for (ExplorerItemModel item : sourceList) {
                    writeInfoLog("Deleting file " + item.getFile().getAbsolutePath());

                    try {
                        FileLogic.delete(item.getFile());
                    } catch (Exception e) {
                        writeErrorLog("Error when deleting file", e);

                        // ask for confirmation to continue when there is an error
                        if (!showConfirmation("Error when deleting " + item.getFile().getAbsolutePath() + " - " + e.getMessage() + ". Do " +
                                "you want to continue?")) {
                            break;
                        }
                    }
                }

                postEvent(new ReloadEvent(event.getSourceModel().getFolder().getAbsolutePath()));
            }
        } catch (Exception e) {
            showError(e, "Error when deleting files", false);
        }
    }

    @Subscribe
    public void onRenameEvent(SimpleRenameEvent event) {
        try {
            showCustomDialog("Simple rename", singleRenameDialog, ResourceManager.getRenameSingleIcon());
        } catch (Exception e) {
            showError(e, "Error handling simple rename event", false);
        }
    }

    @Subscribe
    public void onAddNewFolderEvent(AddNewFolderEvent event) {
        try {
            File folder = FileLogic.addNewFolder(selectedModel.getFolder().getAbsolutePath());
            reloadAndRenameNewFileFolder(folder);
        } catch (Exception e) {
            showError(e, "Error when adding new folder", false);
        }
    }

    private void reloadAndRenameNewFileFolder(File fileOrFolder) {
        postEvent(new ReloadEvent(selectedModel.getFolder().getAbsolutePath()));
        postEvent(new SimpleRenameEvent(fileOrFolder));
    }

    @Subscribe
    public void onAddNewFileEvent(AddNewFileEvent event) {
        try {
            File file = FileLogic.addNewFile(selectedModel.getFolder().getAbsolutePath());
            reloadAndRenameNewFileFolder(file);
        } catch (Exception e) {
            showError(e, "Error when adding new file", false);
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
                try {
                    scpOperation.setVisible(!vboOperations.getChildren().isEmpty());
                } catch (Exception e) {
                    showError(e, "Error handling operation list change event", false);
                }
            });

            initialiseExplorers();
            FXMLLoader loader = new FXMLLoader(ResourceManager.getSimpleRenameFXML());
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

    @FXML
    private void simpleRename(ActionEvent event) {
        try {
            checkNullAndPostEvent(new SimpleRenameEvent(selectedItemList.get(0).getFile()));
        } catch (Exception e) {
            showError(e, "Could not perform simple rename", false);
        }
    }

    private void checkNullAndPostEvent(Object event) {
        if (selectedItemList != null) {
            postEvent(event);
        }
    }

    @FXML
    private void advancedRename(ActionEvent event) {
        try {
            checkNullAndPostEvent(new SimpleRenameEvent(selectedItemList.get(0).getFile()));
        } catch (Exception e) {
            showError(e, "Could not perform advanced rename", false);
        }
    }

    @FXML
    private void copy(ActionEvent event) {
        try {
            checkNullAndPostEvent(new CopyToOtherEvent(selectedModel, selectedItemList));
        } catch (Exception e) {
            showError(e, "Could not copy files", false);
        }
    }

    @FXML
    private void move(ActionEvent event) {
        try {
            checkNullAndPostEvent(new MoveEvent(selectedModel, selectedItemList));
        } catch (Exception e) {
            showError(e, "Could not move files", false);
        }
    }

    @FXML
    private void addNewFolder(ActionEvent event) {
        try {
            checkNullAndPostEvent(new AddNewFolderEvent(selectedModel));
        } catch (Exception e) {
            showError(e, "Could not delete files", false);
        }
    }

    @FXML
    private void addNewFile(ActionEvent event) {
        try {
            checkNullAndPostEvent(new AddNewFileEvent(selectedModel));
        } catch (Exception e) {
            showError(e, "Could not delete files", false);
        }
    }

    /**
     * Convenient class that contains parameters used by copy and move operation
     */
    private class CopyParameterContainer {
        private ExplorerModel model;

        private List<ExplorerItemModel> sourceList;

        private CopyMode mode;

        private Map<ExplorerItemModel, File> sourceTargetMap;

        private Operation operation;

        private String targetPath;
        private String confirmResult;

        private boolean hasDuplicate;

        private double totalSize;
    }
}