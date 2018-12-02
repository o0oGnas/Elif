package xyz.gnas.elif.app.controllers.explorer;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import xyz.gnas.elif.app.common.utility.runner.RunnerUtility;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;

class ExplorerTableCellValue implements Callback<TableColumn.CellDataFeatures<ExplorerItemModel,
        ExplorerItemModel>, ObservableValue<ExplorerItemModel>> {
    @Override
    public ObservableValue<ExplorerItemModel> call(TableColumn.CellDataFeatures<ExplorerItemModel, ExplorerItemModel> param) {
        return new ObservableValue<>() {
            @Override
            public void removeListener(InvalidationListener listener) {
            }

            @Override
            public void addListener(InvalidationListener listener) {
            }

            @Override
            public void removeListener(ChangeListener<? super ExplorerItemModel> listener) {
            }

            @Override
            public ExplorerItemModel getValue() {
                return (ExplorerItemModel) RunnerUtility.executeObjectRunner(getClass(),
                        "Error getting value for table cell", param::getValue);
            }

            @Override
            public void addListener(ChangeListener<? super ExplorerItemModel> listener) {
            }
        };
    }
}
