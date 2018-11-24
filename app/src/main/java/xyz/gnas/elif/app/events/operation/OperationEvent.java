package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public abstract class OperationEvent {
    private ExplorerModel sourceModel;

    public ExplorerModel getSourceModel() {
        return sourceModel;
    }

    public OperationEvent(ExplorerModel sourceModel) {
        this.sourceModel = sourceModel;
    }
}
