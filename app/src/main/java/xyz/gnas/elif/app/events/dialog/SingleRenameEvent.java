package xyz.gnas.elif.app.events.dialog;

import xyz.gnas.elif.core.models.explorer.ExplorerItemModel;

public class SingleRenameEvent {
    private ExplorerItemModel item;

    public ExplorerItemModel getItem() {
        return item;
    }

    public SingleRenameEvent(ExplorerItemModel file) {
        this.item = file;
    }
}
