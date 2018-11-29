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
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.utility.DialogUtility;
import xyz.gnas.elif.app.common.utility.LogUtility;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.common.utility.window.WindowEventHandler;
import xyz.gnas.elif.app.events.dialog.DialogEvent;
import xyz.gnas.elif.app.events.dialog.DialogEvent.DialogType;
import xyz.gnas.elif.app.events.dialog.SingleFileDialogEvent;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.events.operation.InitialiseOperationEvent;
import xyz.gnas.elif.app.events.operation.PerformOperationEvent;
import xyz.gnas.elif.app.events.operation.PerformOperationEvent.OperationType;
import xyz.gnas.elif.app.models.ApplicationModel;
import xyz.gnas.elif.app.models.SettingModel;
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
import static xyz.gnas.elif.app.common.utility.DialogUtility.showConfirmation;
import static xyz.gnas.elif.app.common.utility.DialogUtility.showCustomDialog;
import static xyz.gnas.elif.app.common.utility.DialogUtility.showWarning;
import static xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility.executeRunnerAndHandleException;
import static xyz.gnas.elif.app.common.utility.window.WindowEventUtility.bindWindowEventHandler;

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

    private ApplicationModel applicationModel = ApplicationModel.getInstance();

    private Node simpleRenameDialog;
    private Node advancedRenameDialog;
    private Node textEditorDialog;

    private ObservableList<Operation> operationList = FXCollections.observableArrayList();

    private void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(getClass(), errorMessage, runner);
    }

    private void executeRunnerOrExit(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunnerOrExit(getClass(), errorMessage, runner);
    }

    private void runInSideThread(String errorMessage, Runner runner) {
        CodeRunnerUtility.runInSideThread(getClass(), errorMessage, runner);
    }

    private void runInMainThread(String errorMessage, Runner runner) {
        CodeRunnerUtility.runInMainThread(getClass(), errorMessage, runner);
    }

    private void writeErrorLog(String message, Throwable e) {
        LogUtility.writeErrorLog(getClass(), message, e);
    }

    private void writeInfoLog(String log) {
        LogUtility.writeInfoLog(getClass(), log);
    }

    private void postEvent(Object object) {
        EventBus.getDefault().post(object);
    }

    @Subscribe
    public void onOperationEvent(PerformOperationEvent event) {
        executeRunner("Error when handling operation event", () -> {
            switch (event.getType()) {
                case CopyToOther:
                    copy(applicationModel.getSelectedItemList(), getTargetPath(), CopyMode.COPY);
                    break;

                case CopyToClipboard:
                    copyToClipboard();
                    break;

                case CutToClipboard:
                    cutToClipboard();
                    break;

                case Paste:
                    paste();
                    break;

                case Move:
                    copy(applicationModel.getSelectedItemList(), getTargetPath(), CopyMode.CUT);
                    break;

                case Delete:
                    delete();
                    break;

                case AddNewFolder:
                    addNewFolder();
                    break;

                case AddNewFile:
                    addNewFile();
                    break;

                default:
                    break;
            }
        });
    }

    @Subscribe
    public void onDialogEvent(DialogEvent event) {
        executeRunner("Error when handling single file dialog event", () -> {
            switch (event.getType()) {
                case SimpleRename:
                    showCustomDialog("Simple rename", simpleRenameDialog, ResourceManager.getSimpleRenameIcon());
                    break;

                case AdvancedRename:
                    showCustomDialog("Advanced rename", advancedRenameDialog, ResourceManager.getAdvancedRenameIcon());
                    break;

                case EditAsText:
                    showCustomDialog("Edit as text", textEditorDialog, ResourceManager.getEditAsTextIcon());
                    break;
            }
        });
    }

    /**
     * get the target path of an operation, i.e. path of the other tab
     *
     * @return
     */
    private String getTargetPath() {
        SettingModel settingModel = ApplicationModel.getInstance().getSetting();
        ExplorerModel leftModel = settingModel.getLeftModel();
        ExplorerModel targetModel = applicationModel.getSelectedExplorerModel() == leftModel ?
                settingModel.getRightModel() : leftModel;
        return targetModel.getFolder().getAbsolutePath();
    }

    private void copy(List<ExplorerItemModel> sourceList, String targetPath, CopyMode mode) throws IOException {
        CopyParameterContainer container = new CopyParameterContainer();
        container.sourceList = sourceList;
        container.mode = mode;
        container.targetPath = targetPath + "\\";
        copyFromSourceList(container);
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
                container.targetPath.equalsIgnoreCase(applicationModel.getSelectedExplorerModel().getFolder().getAbsolutePath());

        // show confirm if copy mode is copy or source and target paths are different
        if ((checkDifferentPath || container.mode == CopyMode.COPY) && container.hasDuplicate) {
            confirmResult = DialogUtility.showOptions("There are files in the target folder with the same name",
                    replace, skip, duplicate, cancel);
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

        String operationName = container.mode == CopyMode.CUT ? "Move" : "Copy";
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
                l -> executeRunner("Error when handling operation complete property change", () -> {
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

    private void processSourceTargetMap(CopyParameterContainer container, String duplicate) throws InterruptedException {
        for (ExplorerItemModel source : container.sourceTargetMap.keySet()) {
            File file = source.getFile();

            if (container.mode == CopyMode.CUT && container.targetPath.equalsIgnoreCase(file.getParent() + "\\")) {
                runInMainThread("Error showing invalid operation", () ->
                        showWarning("Invalid operation", "Cannot move file/folder \"" + getFilePathInQuote(file) +
                                "\" to its current folder!"));
            } else {
                while (container.operation.isPaused()) {
                    sleep(Configurations.THREAD_SLEEP_TIME);
                }

                if (container.operation.isStopped()) {
                    break;
                } else {
                    updateSuboperationAndCopy(source, container, duplicate);
                }
            }
        }
    }

    private String getFilePathInQuote(File file) {
        return "\"" + file.getAbsolutePath() + "\"";
    }

    private void updateSuboperationAndCopy(ExplorerItemModel source, CopyParameterContainer container, String duplicate)
            throws InterruptedException {
        runInMainThread("Error creating new operation", () -> {
            String name = container.mode == CopyMode.CUT ? "Moving" : "Copying";
            container.operation.setSuboperationName(name + " \"" + getFilePathInQuote(source.getFile()) + "\"");
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

        runInSideThread("Error running file copy task",
                () -> executeRunnerAndHandleException(() -> moveOrCopy(container, source, progress),
                        (Exception e) -> handleCopyError(container, error, progress, source, target, e)));
        monitorFileProgress(container, progress, error, source, target);
    }

    private void moveOrCopy(CopyParameterContainer container, ExplorerItemModel source, DoubleProperty progress)
            throws IOException, InterruptedException {
        File sourceFile = source.getFile();

        if (container.mode == CopyMode.CUT) {
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

        runInMainThread("Error when handling copy error", () -> {
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
            sleep(Configurations.THREAD_SLEEP_TIME);
        }

        finishCopyFileProgress(container, error, source, target, currentCompletedAmount, contribution);
    }

    private void setCompletedAmount(CopyParameterContainer container, double percent) {
        runInMainThread("Error updating completed amount", () -> container.operation.setCompletedAmount(percent));
    }

    private void finishCopyFileProgress(CopyParameterContainer container, BooleanProperty error,
                                        ExplorerItemModel source, File target, double currentPercentageDone,
                                        double contribution) {
        setCompletedAmount(container, currentPercentageDone + contribution);

        runInMainThread("Error creating reload event", () -> {
            if (!container.operation.isStopped() && !error.get()) {
                // reload source folder if operation is move or cut & paste
                postEvent(new ReloadEvent(source.getFile().getParent()));
            }

            // reload target folder each time a file process is finished
            postEvent(new ReloadEvent(target.getParent()));
        });
    }


    private void copyToClipboard() {
        List<File> fileList = getFileListFromSelectedItems();
        ClipboardLogic.copyToClipboard(fileList);
    }

    private List<File> getFileListFromSelectedItems() {
        List<File> fileList = new LinkedList<>();

        for (ExplorerItemModel item : applicationModel.getSelectedItemList()) {
            fileList.add(item.getFile());
        }

        return fileList;
    }

    private void cutToClipboard() {
        List<File> fileList = getFileListFromSelectedItems();
        ClipboardLogic.cutToClipboard(fileList);
    }

    private void paste() throws IOException {
        BooleanProperty isCut = new SimpleBooleanProperty();
        List<File> fileList = ClipboardLogic.getFiles(isCut);

        if (fileList != null && !fileList.isEmpty()) {
            List<ExplorerItemModel> sourceList = convertFileListToSourceList(fileList);

            if (!sourceList.isEmpty()) {
                writeInfoLog("Pasting files from clipboard");
                copy(sourceList, applicationModel.getSelectedExplorerModel().getFolder().getAbsolutePath(),
                        isCut.get() ? CopyMode.CUT : CopyMode.COPY);
            }
        }
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

    private void delete() {
        List<ExplorerItemModel> sourceList = new LinkedList<>(applicationModel.getSelectedItemList());

        // cannot use normal boolean in lambda
        BooleanProperty breakLoop = new SimpleBooleanProperty();

        if (showConfirmation("Are you sure you want to delete selected files/folders (" + sourceList.size() + ")?")) {
            for (ExplorerItemModel item : sourceList) {
                deleteItem(item, breakLoop);

                if (breakLoop.get()) {
                    break;
                }
            }
        }
    }

    private void deleteItem(ExplorerItemModel item, BooleanProperty breakLoop) {
        writeInfoLog("Deleting file " + item.getFile().getAbsolutePath());

        executeRunnerAndHandleException(() -> {
            File file = item.getFile();
            FileLogic.delete(file);
            postEvent(new ReloadEvent(file.getParent()));
        }, (Exception e) -> {
            writeErrorLog("Error when deleting file", e);

            // ask for confirmation to continue when there is an error
            if (!showConfirmation("Error when deleting " + item.getFile().getAbsolutePath() + "\n" + e.getMessage() + "\nDo you want to continue?")) {
                breakLoop.set(true);
            }
        });
    }

    private void addNewFolder() {
        List<ExplorerItemModel> selectedItemList = applicationModel.getSelectedItemList();
        String newFolder = applicationModel.getSelectedExplorerModel().getFolder().getAbsolutePath() + "\\";

        if (selectedItemList.isEmpty()) {
            newFolder += "New folder";
        } else {
            newFolder += selectedItemList.get(0).getName();
        }

        File folder = FileLogic.addNewFolder(newFolder);
        reloadAndRenameNewFileFolder(folder);
    }

    private void reloadAndRenameNewFileFolder(File file) {
        postEvent(new ReloadEvent(file.getParent()));
        postEvent(new SingleFileDialogEvent(DialogType.SimpleRename, file));
    }

    private void addNewFile() throws IOException {
        String newFile = applicationModel.getSelectedExplorerModel().getFolder().getAbsolutePath() + "\\New file";
        File file = FileLogic.addNewFile(newFile);
        reloadAndRenameNewFileFolder(file);
    }

    @FXML
    private void initialize() {
        executeRunnerOrExit("Could not initialise app", () -> {
            EventBus.getDefault().register(this);
            addListenerToSelectedModelAndItemList();
            handleCloseEvent();
            scpOperation.setManaged(false);
            scpOperation.managedProperty().bind(scpOperation.visibleProperty());

            // only show scroll pane if there are running operations
            operationList.addListener((ListChangeListener<Operation>) l ->
                    executeRunner("Error when handling operation  list change event",
                            () -> scpOperation.setVisible(!operationList.isEmpty())));
            initialiseDialogs();
            initialiseExplorers();
        });
    }

    private void addListenerToSelectedModelAndItemList() {
        applicationModel.selectedExplorerModelProperty().addListener(l -> {
            if (applicationModel.getSelectedExplorerModel() != null) {
                hbxNewFolderFile.setDisable(false);
            }
        });

        bindListenerToSelectedItemList();

        applicationModel.selectedItemListProperty().addListener(
                l -> executeRunner("Error when handling item selection change event", () -> {
                    updateFunctionButtons();
                    bindListenerToSelectedItemList();
                }));
    }

    private void bindListenerToSelectedItemList() {
        ObservableList<ExplorerItemModel> selectedItemList = applicationModel.getSelectedItemList();

        if (selectedItemList != null) {
            selectedItemList.addListener((ListChangeListener<? super ExplorerItemModel>) l ->
                    executeRunner("Error when handling changes to selected item list",
                            this::updateFunctionButtons));
        }
    }

    private void updateFunctionButtons() {
        ObservableList<ExplorerItemModel> selectedItemList = applicationModel.getSelectedItemList();

        if (selectedItemList.isEmpty()) {
            hbxRenameEditCopyMove.setDisable(true);
        } else {
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
    }

    private void handleCloseEvent() {
        bindWindowEventHandler(getClass(), hbxExplorer, new WindowEventHandler() {
            @Override
            public void handleShownEvent() {
            }

            @Override
            public void handleFocusedEvent() {
            }

            @Override
            public void handleCloseEvent(WindowEvent event) {
                executeRunnerOrExit("Error when handling window exit event",
                        () -> checkRunningOperationsAndConfirmClose(event));
            }
        });
    }

    private void checkRunningOperationsAndConfirmClose(WindowEvent event) {
        if (!operationList.isEmpty()) {
            if (showConfirmation("There are running operations, are you sure you want to exit?")) {
                for (Operation operation : operationList) {
                    operation.setStopped(true);
                }
            } else {
                event.consume();
            }
        }
    }

    private void initialiseDialogs() throws IOException {
        // simple rename
        FXMLLoader loader = new FXMLLoader(ResourceManager.getSimpleRenameFXML());
        simpleRenameDialog = loader.load();

        // advanced rename
        loader = new FXMLLoader(ResourceManager.getAdvancedRenameFXML());
        advancedRenameDialog = loader.load();

        // edit as text
        loader = new FXMLLoader(ResourceManager.getEditAsTextFXML());
        textEditorDialog = loader.load();
    }

    private void initialiseExplorers() throws IOException {
        SettingModel settingModel = applicationModel.getSetting();

        // load both sides
        writeInfoLog("Loading left side");
        loadExplorer(settingModel.getLeftModel(), true);
        writeInfoLog("Loading right side");
        loadExplorer(settingModel.getRightModel(), false);
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
                () -> postEvent(new SingleFileDialogEvent(DialogType.SimpleRename,
                        applicationModel.getSelectedItemList().get(0).getFile())));
    }

    @FXML
    private void advancedRename(ActionEvent event) {
    }

    @FXML
    private void editAsText(ActionEvent event) {
        executeRunner("Could not edit as text", () -> postEvent(new SingleFileDialogEvent(DialogType.EditAsText,
                applicationModel.getSelectedItemList().get(0).getFile())));
    }

    @FXML
    private void copy(ActionEvent event) {
        executeRunner("Could not copy to other tab",
                () -> postEvent(new PerformOperationEvent(OperationType.CopyToOther)));
    }

    @FXML
    private void move(ActionEvent event) {
        executeRunner("Could not move files", () -> postEvent(new PerformOperationEvent(OperationType.Move)));
    }

    @FXML
    private void addNewFolder(ActionEvent event) {
        executeRunner("Could not add new folder",
                () -> postEvent(new PerformOperationEvent(OperationType.AddNewFolder)));
    }

    @FXML
    private void addNewFile(ActionEvent event) {
        executeRunner("Could not add new file", () -> postEvent(new PerformOperationEvent(OperationType.AddNewFile)));
    }

    /**
     * convenient enum for copy method
     */
    private enum CopyMode {
        COPY, CUT
    }

    /**
     * Convenient class that contains parameters used by copy and move operation
     */
    private class CopyParameterContainer {
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