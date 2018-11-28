package xyz.gnas.elif.app.events.explorer;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public class ChangePathEvent extends ExplorerEvent {
    public ChangePathEvent(ExplorerModel model) {
        super(model);
    }
}