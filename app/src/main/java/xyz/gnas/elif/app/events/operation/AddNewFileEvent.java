package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public class AddNewFileEvent extends OperationEvent {
    public AddNewFileEvent(ExplorerModel sourceModel) {
        super(sourceModel);
    }
}
