package xyz.gnas.elif.app.events.explorer;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public abstract class ExplorerEvent {
    private ExplorerModel model;

    public ExplorerEvent(ExplorerModel model) {
        this.model = model;
    }

    public ExplorerModel getModel() {
        return model;
    }
}
