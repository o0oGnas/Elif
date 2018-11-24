package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;

import java.util.List;

public class CopyToClipboardEvent extends OperationWithSourceEvent {
    public CopyToClipboardEvent(ExplorerModel sourceModel, List<ExplorerItemModel> sourceList) {
        super(sourceModel, sourceList);
    }
}