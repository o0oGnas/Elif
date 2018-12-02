package xyz.gnas.elif.app.controllers.dialogs.advanced_rename;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.apache.commons.collections4.BidiMap;
import xyz.gnas.elif.app.common.utility.runner.RunnerUtility;
import xyz.gnas.elif.app.common.utility.runner.VoidRunner;

import java.io.File;
import java.util.List;
import java.util.Map;

class AdvancedRenameTreeCell extends TreeCell<File> {
    private BidiMap<TreeItem<File>, TreeItem<File>> mainPreviewNodeMap;
    private Map<String, List<TreeItem<File>>> newNameItemMap;

    private void executeRunner(String errorMessage, VoidRunner runner) {
        RunnerUtility.executeVoidrunner(getClass(), errorMessage, runner);
    }

    public AdvancedRenameTreeCell(BidiMap<TreeItem<File>, TreeItem<File>> mainPreviewNodeMap,
                                  Map<String, List<TreeItem<File>>> newNameItemMap) {
        this.mainPreviewNodeMap = mainPreviewNodeMap;
        this.newNameItemMap = newNameItemMap;
    }

    @Override
    public void updateItem(File item, boolean empty) {
        executeRunner("Error when displaying item", () -> {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setGraphic(null);
            } else {
                display(item);
            }
        });
    }

    private void display(File item) {
        Node icon = Common.getIcon(item);
        Label lblNewName = new Label(item.getName());
        setTextColor(item, lblNewName);
        HBox hbx = new HBox(icon, lblNewName);
        hbx.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lblNewName, Priority.ALWAYS);
        checkAndDisplayOldName(hbx, item);
        setGraphic(hbx);
    }

    private void setTextColor(File item, Label lblNewName) {
        lblNewName.setTextFill(Color.BLACK);
        TreeItem<File> mainNode = mainPreviewNodeMap.getKey(getTreeItem());

        if (mainNode != null) {
            String path = item.getAbsolutePath();
            List<TreeItem<File>> itemList = newNameItemMap.get(path);

            // if there are multiple files with the same new name
            if (itemList != null && itemList.size() > 1) {
                lblNewName.setTextFill(Color.RED);
            } else if (!mainNode.getValue().getAbsolutePath().equalsIgnoreCase(path)) { // if new new is
                // different from old name
                lblNewName.setTextFill(Color.BLUE);
            }
        }
    }

    private void checkAndDisplayOldName(HBox hbx, File item) {
        TreeItem<File> mainNode = mainPreviewNodeMap.getKey(getTreeItem());

        if (mainNode != null) {
            File mainFile = mainNode.getValue();

            if (mainFile != null) {
                Label lblOldName = new Label(mainFile.getName());
                Font font = lblOldName.getFont();
                lblOldName.setFont(Font.font(font.getName(), FontWeight.BOLD, FontPosture.ITALIC, font.getSize()));
                lblOldName.setTextFill(Color.GREEN);
                lblOldName.managedProperty().bind(lblOldName.visibleProperty());
                lblOldName.setVisible(!mainFile.getAbsolutePath().equalsIgnoreCase(item.getAbsolutePath()));
                hbx.getChildren().add(lblOldName);
                HBox.setMargin(lblOldName, new Insets(0, 0, 0, 10));
                HBox.setHgrow(lblOldName, Priority.ALWAYS);
            }
        }
    }
}
