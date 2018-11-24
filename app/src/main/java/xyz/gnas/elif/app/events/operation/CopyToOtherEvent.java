package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;

import java.util.List;

public class CopyToOtherEvent extends OperationWithSourceEvent {
    public CopyToOtherEvent(ExplorerModel sourceModel, List<ExplorerItemModel> sourceList) {
        super(sourceModel, sourceList);
    }
}
