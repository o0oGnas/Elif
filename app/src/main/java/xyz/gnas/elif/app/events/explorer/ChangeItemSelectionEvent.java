package xyz.gnas.elif.app.events.explorer;

import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;

import java.util.List;

public class ChangeItemSelectionEvent {
    private List<ExplorerItemModel> itemList;

    public List<ExplorerItemModel> getItemList() {
        return itemList;
    }

    public ChangeItemSelectionEvent(List<ExplorerItemModel> itemList) {
        this.itemList = itemList;
    }
}
