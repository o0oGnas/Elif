package xyz.gnas.elif.app.controllers.dialog;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.utility.DialogUtility;
import xyz.gnas.elif.app.common.utility.WindowEventUtility;
import xyz.gnas.elif.app.events.dialog.EditAsTextEvent;
import xyz.gnas.elif.core.logic.FileLogic;

import java.io.File;

import static xyz.gnas.elif.app.common.utility.DialogUtility.showConfirmation;

public class EditAsTextController {
    @FXML
    private TextArea ttaContent;

    @FXML
    private Button btnSave;

    private File file;

    private ChangeListener<String> textAreaListener;

    private BooleanProperty hasNewContent = new SimpleBooleanProperty();

    private void showError(Exception e, String message, boolean exit) {
        DialogUtility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        DialogUtility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onEditAsTextEvent(EditAsTextEvent event) {
        try {
            file = event.getFile();
            ttaContent.textProperty().removeListener(textAreaListener);
            ttaContent.setText(FileLogic.readFileAsText(file));
            ttaContent.textProperty().addListener(textAreaListener);
            btnSave.disableProperty().bind(hasNewContent.not());
        } catch (Exception e) {
            showError(e, "Error handling edit as text event", false);
        }
    }

    @FXML
    private void initialize() {
        try {
            EventBus.getDefault().register(this);

            textAreaListener = (observableValue, s, t1) -> {
                try {
                    hasNewContent.set(true);
                } catch (Exception e) {
                    showError(e, "Error handling text change event", false);
                }
            };

            handleCloseEvent();
        } catch (Exception e) {
            showError(e, "Could not initialise edit as text dialog", true);
        }
    }

    private void handleCloseEvent() {
        WindowEventUtility.bindWindowEventHandler(ttaContent, new WindowEventUtility.WindowEventHandler() {
            @Override
            public void handleShownEvent() {
            }

            @Override
            public void handleFocusedEvent() {
            }

            @Override
            public void handleCloseEvent(WindowEvent windowEvent) {
                // show confirm if there are unsaved changes and prevent closing if user chooses no
                if (hasNewContent.get() && !showConfirmation("There are unsaved changes, are you sure you want close " +
                        "without saving first?")) {
                    windowEvent.consume();
                }
            }
        });
    }

    @FXML
    private void keyReleased(KeyEvent keyEvent) {
        try {
            if (keyEvent.isControlDown()) {
                if (keyEvent.getCode() == KeyCode.S) {
                    save(null);
                    hasNewContent.set(false);
                }
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                cancel(null);
            }
        } catch (Exception e) {
            showError(e, "Could not handle key event", false);
        }
    }

    @FXML
    private void save(ActionEvent event) {
        try {
            writeInfoLog("Saving text to file " + file.getAbsolutePath());
            FileLogic.saveTextToFile(file, ttaContent.getText());

            // hide dialog if use clicks on save button
            if (event != null) {
                hideDialog();
            }
        } catch (Exception e) {
            showError(e, "Could not save text edit", false);
        }
    }

    private void hideDialog() {
        ttaContent.getScene().getWindow().hide();
    }

    @FXML
    private void cancel(ActionEvent event) {
        try {
            // show confirm if there are unsaved changes
            if (!hasNewContent.get() || showConfirmation("There are unsaved changes, are you sure you want to close " +
                    "without saving first?")) {
                hideDialog();
            }
        } catch (Exception e) {
            showError(e, "Could not cancel edit as text", false);
        }
    }
}
