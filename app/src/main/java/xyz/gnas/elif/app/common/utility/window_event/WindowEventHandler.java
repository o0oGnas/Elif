package xyz.gnas.elif.app.common.utility.window_event;

import javafx.stage.WindowEvent;

public interface WindowEventHandler {
    void handleShownEvent();

    void handleFocusedEvent();

    void handleCloseEvent(WindowEvent windowEvent);
}
