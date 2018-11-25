package xyz.gnas.elif.app.events.dialog;

import java.io.File;

public abstract class SingleFileDialogEvent {
    private File file;

    public File getFile() {
        return file;
    }

    public SingleFileDialogEvent(File file) {
        this.file = file;
    }
}
