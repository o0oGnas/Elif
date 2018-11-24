package xyz.gnas.elif.app.events.explorer;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public class FocusExplorerEvent extends ExplorerEvent {
    public FocusExplorerEvent(ExplorerModel model) {
        super(model);
    }
}
