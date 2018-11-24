package xyz.gnas.elif.app.events.dialog;

import java.io.File;

public class SimpleRenameEvent {
    private File file;

    public File getFile() {
        return file;
    }

    public SimpleRenameEvent(File file) {
        this.file = file;
    }
}
