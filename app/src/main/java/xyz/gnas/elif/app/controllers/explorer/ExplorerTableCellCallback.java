package xyz.gnas.elif.app.controllers.explorer;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;

class ExplorerTableCellCallback implements Callback<TableColumn<ExplorerItemModel, ExplorerItemModel>,
        TableCell<ExplorerItemModel, ExplorerItemModel>> {
    /**
     * Column enum, used by ExplorerTableCell to determine how to display the data
     */
    public enum Column {
        Name, Extension, Size, Date
    }

    private Column column;

    public ExplorerTableCellCallback(Column column) {
        this.column = column;
    }

    @Override
    public TableCell<ExplorerItemModel, ExplorerItemModel> call(TableColumn<ExplorerItemModel, ExplorerItemModel> param) {
        return new ExplorerTableCell(column) {
        };
    }
}
