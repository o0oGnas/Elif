package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public class PasteEvent extends OperationEvent {
    public PasteEvent(ExplorerModel sourceModel) {
        super(sourceModel);
    }
}
