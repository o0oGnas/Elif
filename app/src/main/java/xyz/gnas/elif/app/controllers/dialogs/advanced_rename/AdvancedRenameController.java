package xyz.gnas.elif.app.controllers.dialogs.advanced_rename;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
    private enum AlphaMode {
        Both, Upper, Lower, Non
    }

    private enum NormalOrNonMode {
        Normal, Non
    }

    @FXML
    private TextField ttfSearch;

    @FXML
    private TextField ttfReplace;

    @FXML
    private ComboBox<String> cbbRenameMode;

    @FXML
    private ComboBox<String> cbbNameExtension;

    @FXML
    private HBox hbxNameExtension;

    @FXML
    private HBox hbxReplace;

    @FXML
    private HBox hbxPattern;

    @FXML
    private HBox hbxGenerating;

    @FXML
    private ComboBox<String> cbbSearchMode;

    @FXML
    private SplitMenuButton smbAlpha;

    @FXML
    private SplitMenuButton smbNumeric;

    @FXML
    private SplitMenuButton smbAlphaNumeric;

    @FXML
    private SplitMenuButton smbSpace;

    @FXML
    private TreeView<File> tvwPreview;

    @FXML
    private CheckTreeView<File> tvwMain;

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

    private ObjectProperty<AlphaMode> alphaButtonMode = new SimpleObjectProperty<>(AlphaMode.Both);
    private ObjectProperty<NormalOrNonMode> numericButtonMode = new SimpleObjectProperty<>(NormalOrNonMode.Normal);
    private ObjectProperty<AlphaMode> alphaNumericButtonMode = new SimpleObjectProperty<>(AlphaMode.Both);
    private ObjectProperty<NormalOrNonMode> spaceButtonMode = new SimpleObjectProperty<>(NormalOrNonMode.Normal);

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
            if (cbbRenameMode.getSelectionModel().getSelectedItem().equalsIgnoreCase(Configurations.KEEP)) {
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

                // remove listener after check model is initialised to prevent rechecking
                tvwMain.checkModelProperty().removeListener(this);
            }
        };
    }

    @FXML
    private void initialize() {
        executeRunner("Could not initialise advanced rename dialogs", () -> {
            EventBus.getDefault().register(this);
            initialiseComboBoxes();
            initialisePatternButtons();
            initialiseMainTree();
            tvwPreview.setCellFactory(f -> new AdvancedRenameTreeCell(mainPreviewNodeMap, newNameItemMap));
            bindControlsVisibility();
            addListenerToTextField(ttfSearch);
            addListenerToTextField(ttfReplace);
            hbxReplace.disableProperty().bind(hbxNameExtension.disableProperty());
        });
    }

    private void initialiseComboBoxes() {
        initialiseRenameModeModeComboBox();
        initialiseNameExtensionComboBox();
        initialiseSearchModeComboBox();
    }

    private void initialiseRenameModeModeComboBox() {
        cbbRenameMode.getItems().addAll(Configurations.REPLACE, Configurations.KEEP);
        SelectionModel<String> selectionModel = cbbRenameMode.getSelectionModel();
        selectionModel.select(Configurations.REPLACE);

        selectionModel.selectedItemProperty().addListener(
                l -> executeRunner("Error when handling rename mode selection", () -> {
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

    private void initialiseSearchModeComboBox() {
        cbbSearchMode.getItems().addAll(Configurations.TEXT, Configurations.PATTERN);
        SelectionModel<String> selectionModel = cbbSearchMode.getSelectionModel();
        selectionModel.select(Configurations.TEXT);

        selectionModel.selectedItemProperty().addListener(
                l -> executeRunner("Error when handling search mode selection",
                        () -> {
                            // disable pattern if search mode is text
                            hbxPattern.setDisable(selectionModel.getSelectedItem().equalsIgnoreCase(Configurations.TEXT));
                        }));
    }

    private void initialisePatternButtons() {
        initialiseAlphaButton(smbAlpha, alphaButtonMode, "Alpha", 1);
        initialiseNormalOrNonButton(smbNumeric, numericButtonMode, "Numeric", 2);
        initialiseAlphaButton(smbAlphaNumeric, alphaNumericButtonMode, "Alphanumeric", 3);
        initialiseNormalOrNonButton(smbSpace, spaceButtonMode, "Space", 4);
    }

    private void initialiseAlphaButton(SplitMenuButton smb, ObjectProperty<AlphaMode> mode, String text, int number) {
        mode.addListener(l -> executeRunner("Error when handling change to alpha button mode", () -> {
            switch (mode.get()) {
                case Both:
                    updateAlphaButtonCase(smb, text, "[U] & [l]", number);
                    break;

                case Upper:
                    updateAlphaButtonCase(smb, text, "[U]", number);
                    break;

                case Lower:
                    updateAlphaButtonCase(smb, text, "[l]", number);
                    break;

                default:
                    updateAlphaButton(smb, "Non-" + text.toLowerCase(), number);
                    break;
            }
        }));
    }

    private void updateAlphaButtonCase(SplitMenuButton smb, String normal, String s, int number) {
        updateAlphaButton(smb, normal + " " + s, number);
    }

    private void updateAlphaButton(SplitMenuButton smb, String text, int number) {
        updatePatternButton(smb, text, number);
    }

    private void updatePatternButton(SplitMenuButton smb, String text, int number) {
        smb.setText(text + "  (Ctrl + " + number + ")");
    }

    private void initialiseNormalOrNonButton(SplitMenuButton smb, ObjectProperty<NormalOrNonMode> mode, String text,
                                             int number) {
        mode.addListener(l -> executeRunner("Error when handling change to numeric button mode", () -> {
            switch (mode.get()) {
                case Normal:
                    updateNumericButton(smb, text, number);
                    break;

                default:
                    updateNumericButton(smb, "Non-" + text.toLowerCase(), number);
                    break;
            }
        }));
    }

    private void updateNumericButton(SplitMenuButton smb, String text, int number) {
        updatePatternButton(smb, text, number);
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
                        btnRename.setDisable(CollectionUtils.isEmpty(tvwMain.getCheckModel().getCheckedItems()));
                    }
                }));
    }

    private void addListenerToMainTreeSelection() {
        bindMainTreeCheckListener();

        tvwMain.checkModelProperty().addListener(
                c -> executeRunner("Error when handling change in check model", this::bindMainTreeCheckListener));
    }

    private void bindMainTreeCheckListener() {
        CheckModel<TreeItem<File>> checkModel = tvwMain.getCheckModel();

        if (checkModel != null) {
            ObservableList<TreeItem<File>> selectedItemList = tvwMain.getCheckModel().getCheckedItems();

            selectedItemList.addListener((ListChangeListener<TreeItem<File>>) l ->
                    executeRunner("Error when handling changes in main tree node selection", () -> {
                        updatePreviewTree();

                        // disable rename button if nothing is selected
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

        if (cbbRenameMode.getSelectionModel().getSelectedItem().equalsIgnoreCase(Configurations.REPLACE)) {
            newName = getNewNameForReplaceMode(file);
        } else {
            newName = ttfSearch.getText();
        }

        if (StringUtils.isEmpty(newName)) {
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

                    if (StringUtils.isEmpty(extension)) {
                        return newNameWithoutExtension;
                    } else {
                        return newNameWithoutExtension + "." + extension;
                    }

                case Configurations.EXTENSION:
                    if (StringUtils.isEmpty(extension)) {
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
    private void addAlpha(ActionEvent event) {
        executeRunner("Could not add alpha character", () -> {

                }
        );
    }

    @FXML
    private void selectBothAlpha(ActionEvent actionEvent) {
        executeRunner("Could not select both alpha", () -> alphaButtonMode.set(AlphaMode.Both));
    }

    @FXML
    private void selectUpperAlpha(ActionEvent actionEvent) {
        executeRunner("Could not select upper alpha", () -> alphaButtonMode.set(AlphaMode.Upper));
    }

    @FXML
    private void selectLowerAlpha(ActionEvent actionEvent) {
        executeRunner("Could not select lower alpha", () -> alphaButtonMode.set(AlphaMode.Lower));
    }

    @FXML
    private void selectNonAlpha(ActionEvent actionEvent) {
        executeRunner("Could not select non alpha", () -> alphaButtonMode.set(AlphaMode.Non));
    }

    @FXML
    private void selectNumeric(ActionEvent actionEvent) {
        executeRunner("Could not select numeric", () -> numericButtonMode.set(NormalOrNonMode.Normal));
    }

    @FXML
    private void selectNonNumeric(ActionEvent actionEvent) {
        executeRunner("Could not select non numeric", () -> numericButtonMode.set(NormalOrNonMode.Non));
    }

    @FXML
    private void selectBothAlphaNumeric(ActionEvent actionEvent) {
        executeRunner("Could not select both alpha numeric", () -> alphaNumericButtonMode.set(AlphaMode.Both));
    }

    @FXML
    private void selectUpperAlphaNumeric(ActionEvent actionEvent) {
        executeRunner("Could not select upper alpha numeric", () -> alphaNumericButtonMode.set(AlphaMode.Upper));
    }

    @FXML
    private void selectLowerAlphaNumeric(ActionEvent actionEvent) {
        executeRunner("Could not select lower alpha numeric", () -> alphaNumericButtonMode.set(AlphaMode.Lower));
    }

    @FXML
    private void selectNonAlphaNumeric(ActionEvent actionEvent) {
        executeRunner("Could not select non alpha numeric", () -> alphaNumericButtonMode.set(AlphaMode.Non));
    }

    @FXML
    private void selectSpace(ActionEvent actionEvent) {
        executeRunner("Could not select space", () -> spaceButtonMode.set(NormalOrNonMode.Normal));
    }

    @FXML
    private void selectNonSpace(ActionEvent actionEvent) {
        executeRunner("Could not select non space", () -> spaceButtonMode.set(NormalOrNonMode.Non));
    }

    @FXML
    private void stopBuildingTrees(ActionEvent event) {
        executeRunner("Could not stop building trees", () -> stopBuildingTrees = true);
    }

    @FXML
    private void dialogKeyReleased(KeyEvent keyEvent) {
        executeRunner("Could not handle dialog key event", () -> {
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

            if (StringUtils.isEmpty(extension)) {
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
        executeRunner("Could not cancel advanced rename", () -> ttfSearch.getScene().getWindow().hide());
    }
}