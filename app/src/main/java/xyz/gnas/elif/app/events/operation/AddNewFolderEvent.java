package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public class AddNewFolderEvent extends OperationEvent {
    public AddNewFolderEvent(ExplorerModel sourceModel) {
        super(sourceModel);
    }
}
