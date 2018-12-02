package xyz.gnas.elif.app.controllers.dialogs.advanced_rename;

import javafx.scene.control.cell.CheckBoxTreeCell;
import xyz.gnas.elif.app.common.utility.runner.RunnerUtility;
import xyz.gnas.elif.app.common.utility.runner.VoidRunner;

import java.io.File;

class AdvancedRenameCheckBoxTreeCell extends CheckBoxTreeCell<File> {
    private void executeRunner(String errorMessage, VoidRunner runner) {
        RunnerUtility.executeVoidrunner(getClass(), errorMessage, runner);
    }

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
}
