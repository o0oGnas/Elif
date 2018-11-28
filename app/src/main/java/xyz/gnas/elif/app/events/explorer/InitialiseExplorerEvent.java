package xyz.gnas.elif.app.events.explorer;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public class InitialiseExplorerEvent extends ExplorerEvent {
    private boolean isLeft;

    public boolean isLeft() {
        return isLeft;
    }

    public InitialiseExplorerEvent(ExplorerModel model, boolean isLeft) {
        super(model);
        this.isLeft = isLeft;
    }
}