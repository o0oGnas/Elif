package xyz.gnas.elif.app.events.dialog;

import java.io.File;

public class SimpleRenameEvent extends SingleFileDialogEvent {
    public SimpleRenameEvent(File file) {
        super(file);
    }
}
