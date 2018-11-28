package xyz.gnas.elif.app.events.dialog;

import java.io.File;

public class SingleFileDialogEvent extends DialogEvent {
    private File file;

    public File getFile() {
        return file;
    }

    public SingleFileDialogEvent(DialogType type, File file) {
        super(type);
        this.file = file;
    }
}
