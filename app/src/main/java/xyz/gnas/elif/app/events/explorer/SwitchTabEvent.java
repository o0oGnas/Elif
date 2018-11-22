package xyz.gnas.elif.app.events.explorer;

import xyz.gnas.elif.app.models.explorer.ExplorerModel;

public class SwitchTabEvent extends ExplorerEvent {
    public SwitchTabEvent(ExplorerModel model) {
        super(model);
    }
}
