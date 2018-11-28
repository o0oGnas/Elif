package xyz.gnas.elif.app.events.operation;

public class PerformOperationEvent {
    public enum OperationType {
        CopyToOther,
        CopyToClipboard,
        CutToClipboard,
        Paste,
        Move,
        Delete,
        AddNewFolder,
        AddNewFile
    }

    private OperationType type;

    public OperationType getType() {
        return type;
    }

    public PerformOperationEvent(OperationType type) {
        this.type = type;
    }
}
