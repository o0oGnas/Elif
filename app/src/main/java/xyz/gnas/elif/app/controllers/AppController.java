package xyz.gnas.elif.app.controllers;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.WindowEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility.ExceptionHandler;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility.MainThreadTaskRunner;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility.Runner;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility.SideThreadTaskRunner;
import xyz.gnas.elif.app.common.utility.DialogUtility;
import xyz.gnas.elif.app.common.utility.WindowEventUtility;
import xyz.gnas.elif.app.events.dialog.EditAsTextEvent;
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
import static xyz.gnas.elif.app.common.utility.DialogUtility.showAlert;
import static xyz.gnas.elif.app.common.utility.DialogUtility.showConfirmation;
import static xyz.gnas.elif.app.common.utility.DialogUtility.showCustomDialog;

public class AppController {
    @FXML
    private HBox hbxExplorer;

    @FXML
    private HBox hbxRenameEditCopyMove;

    @FXML
    private HBox hbxNewFolderFile;

    @FXML
    private ScrollPane scpOperation;

    @FXML
    private VBox vbxOperations;

    @FXML
    private Button btnSimpleRename;

    @FXML
    private Button btnEditAsText;

    private enum CopyMode {
        COPY, MOVE, PASTE
    }

    private final int THREAD_SLEEP_TIME = 500;

    private Node textEditorDialog;

    private Node simpleRenameDialog;

    private ObservableList<Operation> operationList = FXCollections.observableArrayList();

    /**
     * The model of the currently selected tab
     */
    private ExplorerModel selectedModel;

    /**
     * List of selected items in the last active tab
     */
    private List<ExplorerItemModel> selectedItemList;

    private void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(getClass(), errorMessage, runner);
    }

    private void executeRunnerOrExit(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunnerOrExit(getClass(), errorMessage, runner);
    }

    private void executeRunnerAndHandleException(Runner runner, ExceptionHandler handler) {
        CodeRunnerUtility.executeRunnerAndHandleException(runner, handler);
    }

    private void writeErrorLog(String message, Throwable e) {
        DialogUtility.writeErrorLog(getClass(), message, e);
    }

    private void writeInfoLog(String log) {
        DialogUtility.writeInfoLog(getClass(), log);
    }

    private void postEvent(Object object) {
        EventBus.getDefault().post(object);
    }

    @Subscribe
    public void onFocusExplorerEvent(FocusExplorerEvent event) {
        executeRunner("Error handling focus explorer event", () -> {
            selectedModel = event.getModel();
            hbxNewFolderFile.setDisable(false);
        });
    }

    @Subscribe
    public void onChangeItemSelectionEvent(ChangeItemSelectionEvent event) {
        executeRunner("Error handling item selection change evnet", () -> {
            selectedItemList = event.getItemList();

            if (selectedItemList != null && !selectedItemList.isEmpty()) {
                hbxRenameEditCopyMove.setDisable(false);

                if (selectedItemList.size() == 1) {
                    btnSimpleRename.setDisable(false);

                    // only allow edit file as text
                    btnEditAsText.setDisable(selectedItemList.get(0).getFile().isDirectory());
                } else {
                    btnSimpleRename.setDisable(true);
                    btnEditAsText.setDisable(true);
                }
            }
        });
    }

    @Subscribe
    public void onCopyToOtherEvent(CopyToOtherEvent event) {
        executeRunner("Error handling copy to other tab event", () -> copy(event.getSourceModel(),
                event.getSourceList(), getTargetPath(event.getSourceModel()), CopyMode.COPY));
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
            confirmResult = DialogUtility.showOptions("There are files in the target folder with the same name",
                    replace,
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
        container.operation = createNewOperation(operationName + " files to \"" + container.targetPath + "\"");

        runInSideThread("Error running copy task", () -> {
            processSourceTargetMap(container, duplicate);

            runInMainThread("Error updating complete status for operation",
                    () -> container.operation.setComplete(true));
        });
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
        vbxOperations.getChildren().add(0, n);
        addOperationCompleteListener(operation, n);
        postEvent(new InitialiseOperationEvent(operation));
        return operation;
    }

    private void addOperationCompleteListener(Operation operation, Node n) {
        operation.completeProperty().addListener(
                l -> executeRunner("Error handling operation complete property change", () -> {
                    if (operation.isComplete()) {
                        // play notification sound is operation is not stopped
                        if (!operation.isStopped()) {
                            Media media = ResourceManager.getNotificationSound();
                            MediaPlayer mediaPlayer = new MediaPlayer(media);
                            mediaPlayer.play();
                        }

                        operationList.remove(operation);
                        vbxOperations.getChildren().remove(n);
                    }
                }));
    }

    private void runInSideThread(String errorMessage, Runner runner) {
        new Thread(new SideThreadTaskRunner(getClass(), errorMessage, runner)).start();
    }

    private void runInMainThread(String errorMessage, Runner runner) {
        runLater(new MainThreadTaskRunner(getClass(), errorMessage, runner));
    }

    private void processSourceTargetMap(CopyParameterContainer container, String duplicate) throws InterruptedException {
        for (ExplorerItemModel source : container.sourceTargetMap.keySet()) {
            while (container.operation.isPaused()) {
                sleep(THREAD_SLEEP_TIME);
            }

            if (container.operation.isStopped()) {
                break;
            } else {
                runInMainThread("Error creating new operation", () -> {
                    String name = container.mode == CopyMode.MOVE ? "Moving" : "Copying";
                    container.operation.setSuboperationName(name + " \"" + source.getFile().getAbsolutePath() + "\"");
                });

                checkAndUpdateSourceTargetMap(container, source, duplicate);
                copyFile(container, source);
            }
        }
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

        runInSideThread("Error running file copy task",
                () -> executeRunnerAndHandleException(() -> moveOrCopy(container, source, progress),
                        (Exception e) -> handleCopyError(container, error, progress, source, target, e)));

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
                                 ExplorerItemModel source, File target, Exception e) {
        error.set(true);
        String sourcePath = source.getFile().getAbsolutePath();
        writeErrorLog("Error when copying file", e);

        runInMainThread("Error handling copy error", () -> {
            // ask for confirmation to continue when there is an error
            if (showConfirmation("Error when copying " + sourcePath + " to " + target.getAbsolutePath() + "\n" +
                    e.getMessage() + "\nDo you want to continue?")) {
                // consider the file with the error as finished
                progress.set(1);
            } else {
                container.operation.setStopped(true);
            }
        });
    }

    private void monitorFileProgress(CopyParameterContainer container, DoubleProperty progress, BooleanProperty error,
                                     ExplorerItemModel source, File target) throws InterruptedException {
        double currentCompletedAmount = container.operation.getCompletedAmount();

        // calculate the amount of contribution this file has to the total size of the operation
        double contribution = source.getSize() / container.totalSize;

        while (progress.get() < 1 && !container.operation.isStopped()) {
            setCompletedAmount(container, currentCompletedAmount + contribution * progress.get());
            sleep(THREAD_SLEEP_TIME);
        }

        finishCopyFileProgress(container, error, target, currentCompletedAmount, contribution);
    }

    private void setCompletedAmount(CopyParameterContainer container, double percent) {
        runInMainThread("Error updating completed amount",
                () -> container.operation.setCompletedAmount(percent));
    }

    private void finishCopyFileProgress(CopyParameterContainer container, BooleanProperty error, File target,
                                        double currentPercentageDone, double contribution) {
        setCompletedAmount(container, currentPercentageDone + contribution);

        runInMainThread("Error creating reload event", () -> {
            // reload source folder if operation is move and there was no error
            if (container.mode == CopyMode.MOVE && !container.operation.isStopped() && !error.get()) {
                postEvent(new ReloadEvent(container.model.getFolder().getAbsolutePath()));
            }

            // reload target folder each time a file process is finished
            postEvent(new ReloadEvent(target.getParent()));
        });
    }

    @Subscribe
    public void onCopyToClipboardEvent(CopyToClipboardEvent event) {
        executeRunner("Error handling copy to clipboard event", () -> {
            List<File> fileList = new LinkedList<>();

            for (ExplorerItemModel item : event.getSourceList()) {
                fileList.add(item.getFile());
            }

            ClipboardLogic.copyToClipboard(fileList);
        });
    }

    @Subscribe
    public void onPasteEvent(PasteEvent event) {
        executeRunner("Error handling paste event", () -> {
            List<File> fileList = ClipboardLogic.getFiles();

            if (fileList != null && !fileList.isEmpty()) {
                List<ExplorerItemModel> sourceList = convertFileListToSourceList(fileList);

                if (!sourceList.isEmpty()) {
                    writeInfoLog("Pasting files from clipboard");
                    ExplorerModel model = event.getSourceModel();
                    copy(event.getSourceModel(), sourceList, model.getFolder().getAbsolutePath(), CopyMode.PASTE);
                }
            }
        });
    }

    private List<ExplorerItemModel> convertFileListToSourceList(List<File> fileList) {
        List<ExplorerItemModel> sourceList = new LinkedList<>();

        for (File source : fileList) {
            // only paste if source file still exists
            if (source.exists()) {
                ExplorerItemModel item = new ExplorerItemModel(source);
                sourceList.add(item);
            }
        }

        return sourceList;
    }

    @Subscribe
    public void onMoveEvent(MoveEvent event) {
        executeRunner("Error handling move event", () -> copy(event.getSourceModel(), event.getSourceList(),
                getTargetPath(event.getSourceModel()), CopyMode.MOVE));
    }

    @Subscribe
    public void onDeleteEvent(DeleteEvent event) {
        executeRunner("Error handling delete event", () -> {
            List<ExplorerItemModel> sourceList = event.getSourceList();
            BooleanProperty breakLoop = new SimpleBooleanProperty();

            if (showConfirmation("Are you sure you want to delete selected files/folders (" + sourceList.size() + ")" + "?")) {
                for (ExplorerItemModel item : sourceList) {
                    writeInfoLog("Deleting file " + item.getFile().getAbsolutePath());

                    executeRunnerAndHandleException(() -> FileLogic.delete(item.getFile()), (Exception e) -> {
                        writeErrorLog("Error when deleting file", e);

                        // ask for confirmation to continue when there is an error
                        if (!showConfirmation("Error when deleting " + item.getFile().getAbsolutePath() + "\n" + e.getMessage() + "\nDo you want to continue?")) {
                            breakLoop.set(true);
                        }
                    });

                    if (breakLoop.get()) {
                        break;
                    }
                }

                postEvent(new ReloadEvent(event.getSourceModel().getFolder().getAbsolutePath()));
            }
        });
    }

    @Subscribe
    public void onRenameEvent(SimpleRenameEvent event) {
        executeRunner("Error handling simple rename event", () -> showCustomDialog("Simple rename",
                simpleRenameDialog, ResourceManager.getSimpleRenameIcon()));
    }

    @Subscribe
    public void onEditAsTextEvent(EditAsTextEvent event) {
        executeRunner("Error handling edit as text event", () -> showCustomDialog("Edit as text", textEditorDialog,
                ResourceManager.getEditAsTextIcon()));
    }

    @Subscribe
    public void onAddNewFolderEvent(AddNewFolderEvent event) {
        executeRunner("Error handling add new folder event", () -> {
            File folder = FileLogic.addNewFolder(selectedModel.getFolder().getAbsolutePath());
            reloadAndRenameNewFileFolder(folder);
        });
    }

    private void reloadAndRenameNewFileFolder(File fileOrFolder) {
        postEvent(new ReloadEvent(selectedModel.getFolder().getAbsolutePath()));
        postEvent(new SimpleRenameEvent(fileOrFolder));
    }

    @Subscribe
    public void onAddNewFileEvent(AddNewFileEvent event) {
        executeRunner("Error handling add new file event", () -> {
            File file = FileLogic.addNewFile(selectedModel.getFolder().getAbsolutePath());
            reloadAndRenameNewFileFolder(file);
        });
    }

    @FXML
    private void initialize() {
        executeRunnerOrExit("Could not initialise app", () -> {
            EventBus.getDefault().register(this);
            handleCloseEvent();
            scpOperation.setManaged(false);
            scpOperation.managedProperty().bind(scpOperation.visibleProperty());

            // only show scroll pane if there are running operations
            operationList.addListener(
                    (ListChangeListener<Operation>) l -> executeRunner("Error handling operation  list change event",
                            () -> scpOperation.setVisible(!operationList.isEmpty())));

            initialiseDialogs();
            initialiseExplorers();
        });
    }

    private void handleCloseEvent() {
        WindowEventUtility.bindWindowEventHandler(getClass(), hbxExplorer, new WindowEventUtility.WindowEventHandler() {
            @Override
            public void handleShownEvent() {
            }

            @Override
            public void handleFocusedEvent() {
            }

            @Override
            public void handleCloseEvent(WindowEvent windowEvent) {
                executeRunnerOrExit("Error handling window exit event", () -> {
                    // show confirmation is there are running processes
                    if (!operationList.isEmpty()) {
                        if (showConfirmation("There are running operations, are you sure you want to exit?")) {
                            for (Operation operation : operationList) {
                                operation.setStopped(true);
                            }
                        } else {
                            windowEvent.consume();
                        }
                    }
                });
            }
        });
    }

    private void initialiseDialogs() throws IOException {
        FXMLLoader loader = new FXMLLoader(ResourceManager.getSimpleRenameFXML());
        simpleRenameDialog = loader.load();
        loader = new FXMLLoader(ResourceManager.getEditAsTextFXML());
        textEditorDialog = loader.load();
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
        hbxExplorer.getChildren().add(explorer);
        postEvent(new InitialiseExplorerEvent(model, isLeft));
    }

    @FXML
    private void simpleRename(ActionEvent event) {
        executeRunner("Could not perform simple rename",
                () -> postEvent(new SimpleRenameEvent(selectedItemList.get(0).getFile())));
    }

    @FXML
    private void advancedRename(ActionEvent event) {
    }

    @FXML
    private void editAsText(ActionEvent event) {
        executeRunner("Could not edit as text",
                () -> postEvent(new EditAsTextEvent(selectedItemList.get(0).getFile())));
    }

    @FXML
    private void copy(ActionEvent event) {
        executeRunner("Could not copy to other tab", () -> postEvent(new CopyToOtherEvent(selectedModel,
                selectedItemList)));
    }

    @FXML
    private void move(ActionEvent event) {
        executeRunner("Could not move files", () -> postEvent(new MoveEvent(selectedModel, selectedItemList)));
    }

    @FXML
    private void addNewFolder(ActionEvent event) {
        executeRunner("Could not add new folder", () -> postEvent(new AddNewFolderEvent(selectedModel)));
    }

    @FXML
    private void addNewFile(ActionEvent event) {
        executeRunner("Could not add new file", () -> postEvent(new AddNewFileEvent(selectedModel)));
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