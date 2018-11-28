package xyz.gnas.elif.app.events.dialog;

public class DialogEvent {
    public enum DialogType {
        SimpleRename, AdvancedRename, EditAsText
    }

    private DialogType type;

    public DialogType getType() {
        return type;
    }

    public DialogEvent(DialogType type) {
        this.type = type;
    }
}
