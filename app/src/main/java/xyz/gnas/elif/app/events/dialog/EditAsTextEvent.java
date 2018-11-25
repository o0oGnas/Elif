package xyz.gnas.elif.app.events.dialog;

import java.io.File;

public class EditAsTextEvent extends SingleFileDialogEvent {
    public EditAsTextEvent(File file) {
        super(file);
    }
}
