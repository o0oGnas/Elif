package xyz.gnas.elif.app.events.explorer;

public class ReloadEvent {
    private String path;

    public String getPath() {
        return path;
    }

    public ReloadEvent(String path) {
        this.path = path;
    }
}
