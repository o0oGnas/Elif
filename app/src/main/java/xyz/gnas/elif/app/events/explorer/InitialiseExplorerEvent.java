package xyz.gnas.elif.app.events.explorer;

import xyz.gnas.elif.core.models.explorer.ExplorerModel;

public class InitialiseExplorerEvent extends ExplorerEvent {
    public InitialiseExplorerEvent(ExplorerModel model) {
        super(model);
    }
}