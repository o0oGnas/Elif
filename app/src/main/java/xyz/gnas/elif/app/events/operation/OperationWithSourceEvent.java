package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;

import java.util.List;

public class OperationWithSourceEvent extends OperationEvent {
    private List<ExplorerItemModel> sourceList;

    public List<ExplorerItemModel> getSourceList() {
        return sourceList;
    }

    public OperationWithSourceEvent(ExplorerModel sourceModel, List<ExplorerItemModel> sourceList) {
        super(sourceModel);
        this.sourceList = sourceList;
    }
}
