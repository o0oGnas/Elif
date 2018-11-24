package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;

import java.util.List;

public class DeleteEvent extends OperationWithSourceEvent {
    public DeleteEvent(ExplorerModel sourceModel, List<ExplorerItemModel> sourceList) {
        super(sourceModel, sourceList);
    }
}
