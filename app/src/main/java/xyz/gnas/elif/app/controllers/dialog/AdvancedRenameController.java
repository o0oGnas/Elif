package xyz.gnas.elif.app.controllers.dialog;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.apache.commons.io.FilenameUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.ImageUtility;
import xyz.gnas.elif.app.common.utility.LogUtility;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.common.utility.code.RunnerWithIntReturn;
import xyz.gnas.elif.app.events.dialog.DialogEvent;
import xyz.gnas.elif.app.events.dialog.DialogEvent.DialogType;
import xyz.gnas.elif.app.models.ApplicationModel;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static xyz.gnas.elif.app.common.utility.LogUtility.writeErrorLog;
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
    private TreeView<File> tvwMain;

    @FXML
    private TreeView<File> tvwPreview;

    @FXML
    private Button btnRename;

    private ApplicationModel applicationModel = ApplicationModel.getInstance();

    private Thread treeThread;

    /**
     * keep mapping between search and preview nodes
     */
    private Map<TreeItem<File>, TreeItem<File>> searchPreviewNodeMap = new HashMap<>();

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

    private void writeInfoLog(String log) {
        LogUtility.writeInfoLog(getClass(), log);
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

                updateTrees();
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

    private void updateTrees() {
        hbxGenerating.setVisible(true);

        runInSideThread("Error when creating new thread to manage trees building", () -> {
            synchronized (this) {
                // wait until previous build is complete
                if (treeThread != null && treeThread.isAlive()) {
                    wait();
                }

                runBuildThread();

                // wait until current build is complete
                wait();

                runInMainThread("Error when displaying trees", () -> hbxGenerating.setVisible(false));
                stopBuildingTrees = false;
            }
        });
    }

    private void runBuildThread() {
        runInSideThread("Error when creating side thread to build trees",
                () -> treeThread = runInSideThread("Error when building trees", () -> {
                    synchronized (this) {
                        writeInfoLog("Building trees");
                        searchPreviewNodeMap.clear();
                        buildTrees(null, null, applicationModel.getSelectedExplorerModel().getFolder());
                        writeInfoLog("Finished building trees");
                        notify();
                    }
                }));
    }

    /**
     * recursively build the trees, build both at the same time to reduce folder loops, synchronized for thread-safety
     *
     * @param mainTreeParent    previous node of the main tree, null if current is root
     * @param previewTreeParent same with above but for preview tree
     * @param file              the current file to build node
     */
    private synchronized void buildTrees(TreeItem<File> mainTreeParent, TreeItem<File> previewTreeParent, File file) {
        CheckBoxTreeItem<File> mainTreeNode = new CheckBoxTreeItem<>(file, getIcon(file));
        mainTreeNode.selectedProperty().addListener(
                l -> executeRunner("Error when handling main tree node selection", this::updatePreviewTree));
        TreeItem<File> previewTreeNode = new TreeItem<>(file, getIcon(file));
        searchPreviewNodeMap.put(mainTreeNode, previewTreeNode);

        if (file.isDirectory()) {
            traverseFolder(file, mainTreeNode, previewTreeNode);
        }

        setRootOrAttachToParent(tvwMain, mainTreeParent, mainTreeNode);
        setRootOrAttachToParent(tvwPreview, previewTreeParent, previewTreeNode);
        checkAndSelectFile(mainTreeNode, file);
    }

    private Node getIcon(File file) {
        HBox hbx = new HBox();
        List<Node> childrenList = hbx.getChildren();
        Node icon;

        if (file.isDirectory()) {
            icon = new MaterialIconView(MaterialIcon.FOLDER_OPEN, Configurations.ICON_SIZE);
        } else {
            icon = new ImageView(ImageUtility.getFileIcon(file, true));
        }

        childrenList.add(icon);

        // add left margin to icon
        HBox.setMargin(icon, new Insets(0, 0, 0, 5));
        return hbx;
    }

    private void traverseFolder(File file, TreeItem<File> mainTreeNode, TreeItem<File> previewTreeNode) {
        executeRunnerAndHandleException(() -> {
            DirectoryStream<Path> stream = Files.newDirectoryStream(file.toPath());

            for (Path entry : stream) {
                buildTrees(mainTreeNode, previewTreeNode, entry.toFile());

                if (stopBuildingTrees) {
                    break;
                }
            }

            sortChildren(mainTreeNode);
            sortChildren(previewTreeNode);
        }, (Exception e) -> writeErrorLog(getClass(), "Error when traversing " + file.getAbsolutePath(), e));
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

    private void checkAndSelectFile(CheckBoxTreeItem<File> node, File file) {
        List<ExplorerItemModel> selectedList = new LinkedList<>(applicationModel.getSelectedItemList());

        for (ExplorerItemModel item : selectedList) {
            if (file.getAbsolutePath().equalsIgnoreCase(item.getFile().getAbsolutePath())) {
                node.setSelected(true);
                break;
            }
        }
    }

    @FXML
    private void initialize() {
        executeRunner("Could not initialise advanced rename dialog", () -> {
            EventBus.getDefault().register(this);
            initialiseModeComboBox();
            initialiseNameExtensionComboBox();
            initialiseMainTree();
            initialisePreviewTree();
            bindControlsVisibility();
            addListenerToTextField(ttfSearch);
            addListenerToTextField(ttfReplace);
            hbxReplace.disableProperty().bind(hbxNameExtension.disableProperty());
        });
    }

    private void initialiseModeComboBox() {
        cbbMode.getItems().addAll(Configurations.REPLACE, Configurations.KEEP);
        SingleSelectionModel<String> selectionModel = cbbMode.getSelectionModel();
        selectionModel.select(Configurations.REPLACE);

        selectionModel.selectedItemProperty().addListener(
                l -> executeRunner("Error when handling replace mode selection", () -> {
                    // Disable name/extension box if rename mode is not replace
                    hbxNameExtension.setDisable(!selectionModel.getSelectedItem().equalsIgnoreCase(Configurations.REPLACE));
                    updateTextFieldsBySelectedItem();
                }));
    }

    private void updateTextFieldsBySelectedItem() {
        TreeItem<File> selectedItem = tvwMain.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            updateTextFields(selectedItem.getValue());
        }
    }

    private void initialiseNameExtensionComboBox() {
        cbbNameExtension.getItems().addAll(Configurations.NAME, Configurations.EXTENSION,
                Configurations.NAME_EXTENSION);
        SingleSelectionModel<String> selectionModel = cbbNameExtension.getSelectionModel();
        selectionModel.select(Configurations.NAME);
        selectionModel.selectedItemProperty().addListener(
                l -> executeRunner("Error when handling name/extension selection",
                        this::updateTextFieldsBySelectedItem));
    }

    private void initialiseMainTree() {
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

    private void initialisePreviewTree() {
        tvwPreview.setCellFactory(f -> new TreeCell<>() {
            @Override
            public void updateItem(File item, boolean empty) {
                executeRunner("Error when displaying item", () -> {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                });
            }
        });
    }

    private void bindControlsVisibility() {
        hbxGenerating.setManaged(false);
        hbxGenerating.managedProperty().bind(hbxGenerating.visibleProperty());
        tvwMain.disableProperty().bind(hbxGenerating.visibleProperty());
        tvwPreview.disableProperty().bind(tvwMain.disableProperty());
        btnRename.disableProperty().bind(tvwPreview.disableProperty());
    }

    private void addListenerToTextField(TextField ttf) {
        ttf.textProperty().addListener(l -> executeRunner("Error when handling change in replace text",
                () -> {
                    TreeItem<File> root = tvwMain.getRoot();

                    if (root != null) {
                        updatePreviewTree();
                    }
                }));
    }

    private void updatePreviewTree() {
        for (TreeItem<File> mainNode : searchPreviewNodeMap.keySet()) {
            File file = mainNode.getValue();
            String name = file.getName();
            TreeItem<File> previewNode = searchPreviewNodeMap.get(mainNode);

            // only update for nodes that are selected and contains the search string
            if (name.contains(ttfSearch.getText()) && ((CheckBoxTreeItem) mainNode).isSelected()) {
                updatePreviewNode(previewNode, file);
            } else {
                previewNode.setValue(file);
            }
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

        if (newName.equalsIgnoreCase("")) {
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
                    return nameWithoutExtension.replace(searchText, replaceText) + "." + extension;

                case Configurations.EXTENSION:
                    return nameWithoutExtension + "." + extension.replace(searchText, replaceText);

                default:
                    return name.replace(searchText, replaceText);
            }
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
                    rename(null);
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
        });
    }

    @FXML
    private void cancel(ActionEvent event) {
        executeRunner("Could not cancel advanced rename", () -> tvwMain.getScene().getWindow().hide());
    }

    private class AdvancedRenameCheckBoxTreeCell extends CheckBoxTreeCell<File> {
        @Override
        public void updateItem(File item, boolean empty) {
            executeRunner("Error when displaying item", () -> {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    setText(item.getName());
                }
            });
        }
    }
}
