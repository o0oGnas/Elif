package xyz.gnas.elif.app.controllers.dialog;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.ImageUtility;
import xyz.gnas.elif.app.common.utility.LogUtility;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.events.dialog.DialogEvent;
import xyz.gnas.elif.app.events.dialog.DialogEvent.DialogType;
import xyz.gnas.elif.app.models.ApplicationModel;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdvancedRenameController {
    @FXML
    private TreeView<File> tvwMain;

    @FXML
    private TreeView<File> tvwPreview;

    private ApplicationModel applicationModel = ApplicationModel.getInstance();

    private void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(getClass(), errorMessage, runner);
    }

    private void writeInfoLog(String log) {
        LogUtility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onDialogEvent(DialogEvent event) {
        executeRunner("Error when handling edit as text event", () -> {
            if (event.getType() == DialogType.AdvancedRename) {
                File folder = applicationModel.getSelectedExplorerModel().getFolder();
                tvwMain.setRoot(buildTree(null, folder));
                initialiseMainTreeViewCell();
                tvwPreview.setRoot(buildTree(null, folder));
            }
        });
    }

    private void initialiseMainTreeViewCell() {
        tvwMain.setCellFactory(f -> new CheckBoxTreeCell<>() {
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
        });
    }

    /**
     * recursively build the tree
     *
     * @param parent the parent node of the current branch
     * @param file   the file to build the branch on
     * @return
     */
    private TreeItem<File> buildTree(TreeItem<File> parent, File file) {
        CheckBoxTreeItem<File> node = new CheckBoxTreeItem<>(file, getIcon(file));

        if (file.isDirectory()) {
            List<File> childrenList = getSortedChildrenList(file);

            for (File child : childrenList) {
                buildTree(node, child);
            }
        }

        if (parent == null) {
            // expand root node by default
            node.setExpanded(true);
        } else {
            parent.getChildren().add(node);
        }

        checkAndSelectFile(node, file);
        return node;
    }

    private Node getIcon(File file) {
        if (file.isDirectory()) {
            MaterialIconView mivFolder = new MaterialIconView();
            mivFolder.setGlyphName(Configurations.FOLDER_GlYPH);
            mivFolder.setGlyphSize(16);
            return mivFolder;
        } else {
            return new ImageView(ImageUtility.getFileIcon(file, true));
        }
    }

    private List<File> getSortedChildrenList(File file) {
        List<File> childrenList = Arrays.asList(file.listFiles());

        Collections.sort(childrenList, ((o1, o2) -> {
            // only sort by name if both are files or folders
            if (o1.isDirectory() == o2.isDirectory()) {
                return o1.getName().compareTo(o2.getName());
            } else {// folders come first
                return o1.isDirectory() ? -1 : 1;
            }
        }));

        return childrenList;
    }

    private void checkAndSelectFile(CheckBoxTreeItem<File> node, File file) {
        for (ExplorerItemModel item : applicationModel.getSelectedItemList()) {
            if (file.getAbsolutePath().equalsIgnoreCase(item.getFile().getAbsolutePath())) {
                node.setSelected(true);
                break;
            }
        }
    }

    @FXML
    private void initialize() {
        executeRunner("Could not initialise advanced rename dialog", () -> EventBus.getDefault().register(this));
    }

    @FXML
    private void keyReleased(KeyEvent keyEvent) {
        executeRunner("Could not handle key event", () -> {
            switch (keyEvent.getCode()) {
                case ENTER:
                    rename(null);
                    break;

                case ESCAPE:
                    System.out.println("esc");
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
}
