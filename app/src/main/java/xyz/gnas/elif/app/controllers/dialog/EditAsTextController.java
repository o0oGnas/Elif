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
import xyz.gnas.elif.app.common.utility.LogUtility;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.common.utility.window.WindowEventHandler;
import xyz.gnas.elif.app.events.dialog.DialogEvent.DialogType;
import xyz.gnas.elif.app.events.dialog.SingleFileDialogEvent;
import xyz.gnas.elif.core.logic.FileLogic;

import java.io.File;

import static xyz.gnas.elif.app.common.utility.DialogUtility.showConfirmation;
import static xyz.gnas.elif.app.common.utility.window.WindowEventUtility.bindWindowEventHandler;

public class EditAsTextController {
    @FXML
    private TextArea ttaContent;

    @FXML
    private Button btnSave;

    private File file;

    private ChangeListener<String> textAreaListener;

    private BooleanProperty hasNewContent = new SimpleBooleanProperty();

    private void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(getClass(), errorMessage, runner);
    }

    private void writeInfoLog(String log) {
        LogUtility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onSingleFileDialogEvent(SingleFileDialogEvent event) {
        executeRunner("Error when handling edit as text event", () -> {
            if (event.getType() == DialogType.EditAsText) {
                file = event.getFile();
                ttaContent.textProperty().removeListener(textAreaListener);
                ttaContent.setText(FileLogic.readFileAsText(file));
                ttaContent.textProperty().addListener(textAreaListener);
                btnSave.disableProperty().bind(hasNewContent.not());
            }
        });
    }

    @FXML
    private void initialize() {
        executeRunner("Could not initialise edit as text dialog", () -> {
            EventBus.getDefault().register(this);
            textAreaListener = (observableValue, s, t1) -> executeRunner("Error when handling text change event",
                    () -> hasNewContent.set(true));
            bindCloseEventHandler();
        });
    }

    private void bindCloseEventHandler() {
        bindWindowEventHandler(getClass(), ttaContent, new WindowEventHandler() {
            @Override
            public void handleShownEvent() {
            }

            @Override
            public void handleFocusedEvent() {
            }

            @Override
            public void handleCloseEvent(WindowEvent windowEvent) {
                // show confirm if there are unsaved changes and prevent closing if user chooses no
                if (!checkCloseDialog()) {
                    windowEvent.consume();
                }
            }
        });
    }

    /**
     * check if there are unsaved changes and show confirmation if there are
     *
     * @return true if there are no unsaved changes or user chooses to close without saving, false otherwise
     */
    private boolean checkCloseDialog() {
        if (hasNewContent.get()) {
            return showConfirmation("There are unsaved changes, are you sure you want close without saving?");
        } else {
            return true;
        }
    }

    @FXML
    private void keyReleased(KeyEvent keyEvent) {
        executeRunner("Could not handle key event", () -> {
            if (keyEvent.isControlDown()) {
                if (keyEvent.getCode() == KeyCode.S) {
                    save(null);
                    hasNewContent.set(false);
                }
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                hideDialog();
            }
        });
    }

    private void hideDialog() {
        if (checkCloseDialog()) {
            ttaContent.getScene().getWindow().hide();
        }
    }

    @FXML
    private void save(ActionEvent event) {
        executeRunner("Could not save text edit", () -> {
            writeInfoLog("Saving text to file " + file.getAbsolutePath());
            FileLogic.saveTextToFile(file, ttaContent.getText());
            hasNewContent.set(false);

            // hide dialog if use clicks on save button
            if (event != null) {
                hideDialog();
            }
        });
    }

    @FXML
    private void cancel(ActionEvent event) {
        executeRunner("Could not cancel edit as text", () -> hideDialog());
    }
}
