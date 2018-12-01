package xyz.gnas.elif.app.controllers.dialogs.advanced_rename;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.CheckModel;
import org.controlsfx.control.CheckTreeView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.LogUtility;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.common.utility.code.RunnerWithIntReturn;
import xyz.gnas.elif.app.events.dialogs.DialogEvent;
import xyz.gnas.elif.app.events.dialogs.DialogEvent.DialogType;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.models.ApplicationModel;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.core.logic.FileLogic;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;
import static xyz.gnas.elif.app.common.utility.DialogUtility.showConfirmation;
import static xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility.executeRunnerAndHandleException;

public class AdvancedRenameController {
    @FXML
    private ComboBox<String> cbbMode;

    @FXML
    private ComboBox<String> cbbNameExtension;

    @FXML
    private HBox hbxNameExtension;

    @FXML
    private HBox hbxReplace;

    @FXML
    private HBox hbxGenerating;

    @FXML
    private TextField ttfSearch;

    @FXML
    private TextField ttfReplace;

    @FXML
    private CheckTreeView<File> tvwMain;

    @FXML
    private TreeView<File> tvwPreview;

    @FXML
    private Button btnRename;

    private ApplicationModel applicationModel = ApplicationModel.getInstance();

    private Thread treeBuildThread;

    /**
     * keep mapping between search and preview nodes
     */
    private BidiMap<TreeItem<File>, TreeItem<File>> mainPreviewNodeMap = new DualHashBidiMap();

    /**
     * keep mapping between new name and nodes with file that will be renamed to this name
     */
    private Map<String, List<TreeItem<File>>> newNameItemMap = new HashMap<>();

    private List<File> oldCheckList = new LinkedList<>();

    /**
     * flag to stop building trees
     */
    private boolean stopBuildingTrees;

    private void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(getClass(), errorMessage, runner);
    }

    private int executeRunnerWithIntReturn(String errorMessage, int errorReturnValue, RunnerWithIntReturn runner) {
        return CodeRunnerUtility.executeRunnerWithIntReturn(getClass(), errorMessage, errorReturnValue, runner);
    }

    private Thread runInSideThread(String errorMessage, Runner runner) {
        return CodeRunnerUtility.runInSideThread(getClass(), errorMessage, runner);
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
    public void onDialogEvent(DialogEvent event) {
        executeRunner("Error when handling edit as text event", () -> {
            if (event.getType() == DialogType.AdvancedRename) {
                // use first selection as default for text fields
                List<ExplorerItemModel> selectedList = applicationModel.getSelectedItemList();

                if (!selectedList.isEmpty()) {
                    updateTextFields(selectedList.get(0).getFile());
                }

                updateTrees(false);
            }
        });
    }

    private void updateTextFields(File file) {
        String nameWithExtension = file.getName();

        if (file.isDirectory()) {
            ttfSearch.setText(nameWithExtension);
        } else {
            if (cbbMode.getSelectionModel().getSelectedItem().equalsIgnoreCase(Configurations.KEEP)) {
                ttfSearch.setText(FilenameUtils.removeExtension(nameWithExtension));
            } else {
                updateSearchTextByNameExtensionSelection(file);
            }
        }

        ttfReplace.setText(ttfSearch.getText());
    }

    private void updateSearchTextByNameExtensionSelection(File file) {
        String nameWithExtension = file.getName();

        switch (cbbNameExtension.getSelectionModel().getSelectedItem()) {
            case Configurations.NAME:
                ttfSearch.setText(FilenameUtils.removeExtension(nameWithExtension));
                break;

            case Configurations.EXTENSION:
                ttfSearch.setText(FilenameUtils.getExtension(nameWithExtension));
                break;

            default:
                ttfSearch.setText(nameWithExtension);
                break;
        }
    }

    /**
     * @param reselect flag to tell if the new tree should use selection from previous tree
     */
    private void updateTrees(boolean reselect) {
        hbxGenerating.setVisible(true);

        runInSideThread("Error when creating new thread to manage trees building", () -> {
            // wait until previous build is complete
            waitForThread();
            runBuildThread(reselect);

            // wait until current build is complete
            waitForThread();
            runInMainThread("Error when displaying trees", () -> hbxGenerating.setVisible(false));
            stopBuildingTrees = false;
        });
    }

    private void waitForThread() throws InterruptedException {
        while (treeBuildThread != null && treeBuildThread.isAlive()) {
            sleep(Configurations.THREAD_SLEEP_TIME);
        }
    }

    private void runBuildThread(boolean reselect) {
        treeBuildThread = runInSideThread("Error when building trees", () -> {
            writeInfoLog("Building trees");
            mainPreviewNodeMap.clear();
            buildTrees(null, null, applicationModel.getSelectedExplorerModel().getFolder(), reselect);
            writeInfoLog("Finished building trees");
        });
    }

    /**
     * recursively build the trees, build both at the same time to reduce folder loops, synchronized for thread-safety
     *
     * @param mainTreeParent    previous node of the main tree, null if current is root
     * @param previewTreeParent same with above but for preview tree
     * @param file              the current file to build node
     */
    private synchronized void buildTrees(CheckBoxTreeItem<File> mainTreeParent, TreeItem<File> previewTreeParent,
                                         File file, boolean reselect) {
        CheckBoxTreeItem<File> mainTreeNode = new CheckBoxTreeItem<>(file, Common.getIcon(file));
        TreeItem<File> previewTreeNode = new TreeItem<>(file);
        mainPreviewNodeMap.put(mainTreeNode, previewTreeNode);
        System.out.println(file.getAbsolutePath());

        if (file.isDirectory()) {
            traverseFolder(file, mainTreeNode, previewTreeNode, reselect);
            sortChildren(mainTreeNode);
            sortChildren(previewTreeNode);
        }

        setRootOrAttachToParent(tvwMain, mainTreeParent, mainTreeNode);
        setRootOrAttachToParent(tvwPreview, previewTreeParent, previewTreeNode);
        checkAndSelectFile(mainTreeNode, file, reselect);
    }

    private void traverseFolder(File file, CheckBoxTreeItem<File> mainTreeNode, TreeItem<File> previewTreeNode,
                                boolean reselect) {
        executeRunnerAndHandleException(() -> {
            DirectoryStream<Path> stream = Files.newDirectoryStream(file.toPath());

            for (Path entry : stream) {
                buildTrees(mainTreeNode, previewTreeNode, entry.toFile(), reselect);

                if (stopBuildingTrees) {
                    break;
                }
            }
        }, (Exception e) -> writeErrorLog("Error when traversing " + file.getAbsolutePath(), e));
    }

    private void sortChildren(TreeItem<File> node) {
        node.getChildren().sort((o1, o2) -> executeRunnerWithIntReturn("Error when sorting children", 0, () -> {
            File f1 = o1.getValue();
            File f2 = o2.getValue();
            boolean isDirectory = f1.isDirectory();

            if (isDirectory == f2.isDirectory()) {
                return f1.getName().compareTo(f2.getName());
            } else {
                return isDirectory ? -1 : 1;
            }
        }));
    }

    private void setRootOrAttachToParent(TreeView<File> tree, TreeItem<File> parent, TreeItem<File> node) {
        if (parent == null) {
            runInMainThread("Error when setting root of tree", () -> {
                tree.setRoot(node);
                // expand root
                node.setExpanded(true);
            });
        } else {
            parent.getChildren().add(node);
        }
    }

    private void checkAndSelectFile(CheckBoxTreeItem<File> node, File file, boolean reselect) {
        List<File> selectedFileList = getSelectedFileList(reselect);

        for (File selectedFile : selectedFileList) {
            if (file.getAbsolutePath().equalsIgnoreCase(selectedFile.getAbsolutePath())) {
                CheckModel<TreeItem<File>> checkModel = tvwMain.getCheckModel();

                if (checkModel == null) {
                    ChangeListener<CheckModel<TreeItem<File>>> listener = getListenerForCheckModel(node);
                    tvwMain.checkModelProperty().addListener(listener);
                } else {
                    checkModel.check(node);
                }

                break;
            }
        }
    }

    private List<File> getSelectedFileList(boolean reselect) {
        if (reselect) {
            return oldCheckList;
        } else {
            List<File> selectedFileList = new LinkedList<>();
            List<ExplorerItemModel> selectedItemModelList = new LinkedList<>(applicationModel.getSelectedItemList());

            for (ExplorerItemModel itemModel : selectedItemModelList) {
                selectedFileList.add(itemModel.getFile());
            }

            return selectedFileList;
        }
    }

    private ChangeListener<CheckModel<TreeItem<File>>> getListenerForCheckModel(CheckBoxTreeItem<File> node) {
        return new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends CheckModel<TreeItem<File>>> observableValue,
                                CheckModel<TreeItem<File>> treeItemCheckModel,
                                CheckModel<TreeItem<File>> t1) {
                executeRunner("Error when handling change to check model",
                        () -> tvwMain.getCheckModel().check(node));
                tvwMain.checkModelProperty().removeListener(this);
            }
        };
    }

    @FXML
    private void initialize() {
        executeRunner("Could not initialise advanced rename dialogs", () -> {
            EventBus.getDefault().register(this);
            initialiseModeComboBox();
            initialiseNameExtensionComboBox();
            initialiseMainTree();
            tvwPreview.setCellFactory(f -> new AdvancedRenameTreeCell(mainPreviewNodeMap, newNameItemMap));
            bindControlsVisibility();
            addListenerToTextField(ttfSearch);
            addListenerToTextField(ttfReplace);
            hbxReplace.disableProperty().bind(hbxNameExtension.disableProperty());
        });
    }

    private void initialiseModeComboBox() {
        cbbMode.getItems().addAll(Configurations.REPLACE, Configurations.KEEP);
        SelectionModel<String> selectionModel = cbbMode.getSelectionModel();
        selectionModel.select(Configurations.REPLACE);

        selectionModel.selectedItemProperty().addListener(
                l -> executeRunner("Error when handling replace mode selection", () -> {
                    // Disable name/extension box if rename mode is not replace
                    hbxNameExtension.setDisable(!selectionModel.getSelectedItem().equalsIgnoreCase(Configurations.REPLACE));
                }));
    }

    private void initialiseNameExtensionComboBox() {
        cbbNameExtension.getItems().addAll(Configurations.NAME, Configurations.EXTENSION,
                Configurations.NAME_EXTENSION);
        SelectionModel<String> selectionModel = cbbNameExtension.getSelectionModel();
        selectionModel.select(Configurations.NAME);

        selectionModel.selectedItemProperty().addListener(
                l -> executeRunner("Error when handling name/extension selection",
                        () -> {
                            TreeItem<File> selectedItem = tvwMain.getSelectionModel().getSelectedItem();

                            if (selectedItem != null) {
                                updateTextFields(selectedItem.getValue());
                            }
                        }));
    }

    private void initialiseMainTree() {
        addListenerToMainTreeDisabledState();
        addListenerToMainTreeSelection();
        setCellFactoryForMainTree();
    }

    private void addListenerToMainTreeDisabledState() {
        tvwMain.disabledProperty().addListener(l ->
                executeRunner("Error when handling change to main tree disable state", () -> {
                    if (tvwMain.isDisabled()) {
                        btnRename.setDisable(true);
                    } else {
                        List<TreeItem<File>> selectedItemList = tvwMain.getCheckModel().getCheckedItems();
                        btnRename.setDisable(selectedItemList == null || selectedItemList.isEmpty());
                    }
                }));
    }

    private void addListenerToMainTreeSelection() {
        bindMainTreeCheckListener();

        tvwMain.checkModelProperty().addListener(
                c -> executeRunner("Error when handling change in check model", () -> bindMainTreeCheckListener()));
    }

    private void bindMainTreeCheckListener() {
        CheckModel<TreeItem<File>> checkModel = tvwMain.getCheckModel();

        if (checkModel != null) {
            ObservableList<TreeItem<File>> selectedItemList = tvwMain.getCheckModel().getCheckedItems();

            selectedItemList.addListener((ListChangeListener<TreeItem<File>>) l ->
                    executeRunner("Error when handling changes in main tree node selection", () -> {
                        updatePreviewTree();
                        // disable rename button if no file is selected
                        btnRename.setDisable(tvwPreview.isDisabled() || selectedItemList.isEmpty());
                    }));
        }
    }

    private void setCellFactoryForMainTree() {
        tvwMain.setCellFactory(f -> {
            TreeCell<File> cell = new AdvancedRenameCheckBoxTreeCell();

            cell.setOnMouseClicked(event -> executeRunner("Error when handling cell click event", () -> {
                if (!cell.isEmpty()) {
                    updateTextFields(cell.getItem());
                }
            }));

            return cell;
        });
    }

    private void bindControlsVisibility() {
        hbxGenerating.setManaged(false);
        hbxGenerating.managedProperty().bind(hbxGenerating.visibleProperty());
        tvwMain.disableProperty().bind(hbxGenerating.visibleProperty());
        tvwPreview.disableProperty().bind(tvwMain.disableProperty());
    }

    private void addListenerToTextField(TextField ttf) {
        ttf.textProperty().addListener(l -> executeRunner("Error when handling change in text field",
                () -> {
                    TreeItem<File> root = tvwMain.getRoot();

                    if (root != null) {
                        updatePreviewTree();
                    }
                }));
    }

    private void updatePreviewTree() {
        newNameItemMap.clear();

        for (TreeItem<File> mainNode : mainPreviewNodeMap.keySet()) {
            File file = mainNode.getValue();
            String name = file.getName();
            TreeItem<File> previewNode = mainPreviewNodeMap.get(mainNode);

            // only update for nodes that are selected and contains the search string
            if (((CheckBoxTreeItem) mainNode).isSelected() && name.contains(ttfSearch.getText())) {
                updatePreviewNode(previewNode, file);
            } else {
                previewNode.setValue(file);
            }

            updateNewNameItemMap(previewNode);
        }

        tvwPreview.refresh();
    }

    private void updatePreviewNode(TreeItem<File> previewNode, File file) {
        String name = file.getName();
        String newName;

        if (cbbMode.getSelectionModel().getSelectedItem().equalsIgnoreCase(Configurations.REPLACE)) {
            newName = getNewNameForReplaceMode(file);
        } else {
            newName = ttfSearch.getText();
        }

        if (newName.isEmpty()) {
            newName = name;
        }

        previewNode.setValue(new File(file.getParent() + "\\" + newName));
    }

    private String getNewNameForReplaceMode(File file) {
        String name = file.getName();
        String searchText = ttfSearch.getText();
        String replaceText = ttfReplace.getText();

        if (file.isDirectory()) {
            return name.replace(searchText, replaceText);
        } else {
            String nameWithoutExtension = FilenameUtils.removeExtension(name);
            String extension = FilenameUtils.getExtension(name);

            switch (cbbNameExtension.getSelectionModel().getSelectedItem()) {
                case Configurations.NAME:
                    String newNameWithoutExtension = nameWithoutExtension.replace(searchText, replaceText);

                    if (extension == null || extension.isEmpty()) {
                        return newNameWithoutExtension;
                    } else {
                        return newNameWithoutExtension + "." + extension;
                    }

                case Configurations.EXTENSION:
                    if (extension == null || extension.isEmpty()) {
                        return nameWithoutExtension;
                    } else {
                        return nameWithoutExtension + "." + extension.replace(searchText, replaceText);
                    }

                default:
                    return name.replace(searchText, replaceText);
            }
        }
    }

    private void updateNewNameItemMap(TreeItem<File> previewNode) {
        String path = previewNode.getValue().getAbsolutePath();

        if (newNameItemMap.containsKey(path)) {
            newNameItemMap.get(path).add(previewNode);
        } else {
            List<TreeItem<File>> list = new LinkedList<>();
            list.add(previewNode);
            newNameItemMap.put(path, list);
        }
    }

    @FXML
    private void stopBuildingTrees(ActionEvent event) {
        executeRunner("Could not stop building trees", () -> stopBuildingTrees = true);
    }

    @FXML
    private void keyReleased(KeyEvent keyEvent) {
        executeRunner("Could not handle key event", () -> {
            switch (keyEvent.getCode()) {
                case ENTER:
                    if (!btnRename.isDisabled()) {
                        rename(null);
                    }

                    break;

                case ESCAPE:
                    cancel(null);
                    break;

                default:
                    break;
            }
        });
    }

    @FXML
    private void rename(ActionEvent event) {
        executeRunner("Could not apply advanced rename", () -> {
            boolean hasDuplicate = checkHasDuplicate();

            if (!hasDuplicate || showConfirmation("There are files with duplicate name, do you want to continue?")) {
                // cannot use normal boolean in lambda
                BooleanProperty breakLoop = new SimpleBooleanProperty();

                for (TreeItem<File> node : tvwMain.getCheckModel().getCheckedItems()) {
                    // ignore root node
                    if (node != tvwMain.getRoot()) {
                        processNode(node, breakLoop);

                        if (breakLoop.get()) {
                            break;
                        }
                    }
                }

                // reload both trees after rename is finished
                reloadTreesAfterRename();
            }
        });
    }

    private boolean checkHasDuplicate() {
        for (String name : newNameItemMap.keySet()) {
            if (newNameItemMap.get(name).size() > 1) {
                return true;
            }
        }

        return false;
    }

    private void processNode(TreeItem<File> node, BooleanProperty breakLoop) {
        executeRunnerAndHandleException(() -> renameFile(node), (Exception e) -> {
            String sourcePath = node.getValue().getAbsolutePath();
            writeErrorLog("Error when renaming file " + sourcePath, e);
            breakLoop.set(!showConfirmation("Error when renaming " + sourcePath + "\n" +
                    e.getMessage() + "\nDo you want to continue?"));
        });
    }

    private void renameFile(TreeItem<File> mainNode) {
        File source = mainNode.getValue();
        TreeItem<File> targetNode = mainPreviewNodeMap.get(mainNode);
        File target = targetNode.getValue();
        int index = 2;

        while (target.exists()) {
            target = getFileWithSuffix(source, target, index);
            ++index;
        }

        writeInfoLog("Renaming " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
        FileLogic.rename(source, target);

        // update file of target node so we know which new nodes to select based on old nodes
        targetNode.setValue(target);
        postEvent(new ReloadEvent(source.getParent()));
    }

    private File getFileWithSuffix(File source, File target, int index) {
        String nameWithExtension = target.getName();
        String nameWithoutExtension = FilenameUtils.removeExtension(nameWithExtension);
        String extension = FilenameUtils.getExtension(nameWithExtension);
        String newName;

        if (source.isDirectory()) {
            newName = nameWithExtension + getNameSuffix(index);
        } else {
            newName = nameWithoutExtension + getNameSuffix(index);

            if (extension != null && !extension.isEmpty()) {
                newName += "." + extension;
            }
        }

        return new File(target.getParent() + "\\" + newName);
    }

    private String getNameSuffix(int index) {
        return " (" + index + ")";
    }

    private void reloadTreesAfterRename() {
        oldCheckList.clear();

        for (TreeItem<File> item : tvwMain.getCheckModel().getCheckedItems()) {
            // use new file for old selection
            oldCheckList.add(mainPreviewNodeMap.get(item).getValue());
        }

        tvwMain.getCheckModel().clearChecks();
        updateTrees(true);
    }

    @FXML
    private void cancel(ActionEvent event) {
        executeRunner("Could not cancel advanced rename", () -> cbbMode.getScene().getWindow().hide());
    }
}