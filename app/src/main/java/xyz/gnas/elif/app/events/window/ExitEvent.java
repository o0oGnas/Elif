package xyz.gnas.elif.app.events.window;

import javafx.stage.WindowEvent;

public class ExitEvent {
    private WindowEvent windowEvent;

    public ExitEvent(WindowEvent windowEvent) {
        this.windowEvent = windowEvent;
    }

    public WindowEvent getWindowEvent() {
        return windowEvent;
    }
}