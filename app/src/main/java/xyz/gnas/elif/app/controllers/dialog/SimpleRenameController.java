package xyz.gnas.elif.app.controllers.dialog;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;
import org.apache.commons.io.FilenameUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility.Runner;
import xyz.gnas.elif.app.common.utility.DialogUtility;
import xyz.gnas.elif.app.common.utility.ImageUtility;
import xyz.gnas.elif.app.common.utility.WindowEventUtility;
import xyz.gnas.elif.app.events.dialog.SimpleRenameEvent;
import xyz.gnas.elif.core.logic.FileLogic;

import java.io.File;

import static xyz.gnas.elif.app.common.utility.DialogUtility.showConfirmation;

public class SimpleRenameController {
    @FXML
    private MaterialIconView mivFolder;

    @FXML
    private ImageView imvFile;

    @FXML
    private Label lblFile;

    @FXML
    private TextField txtName;

    private File file;

    private void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(getClass(), errorMessage, runner);
    }

    private void writeInfoLog(String log) {
        DialogUtility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onSimpleRenameEvent(SimpleRenameEvent event) {
        executeRunner("Error handling simple rename event", () -> {
            file = event.getFile();
            mivFolder.setVisible(file.isDirectory());

            if (!file.isDirectory()) {
                imvFile.setImage(ImageUtility.getFileIcon(file, true));
            }

            lblFile.setText(file.getAbsolutePath());
            txtName.setText(file.getName());
        });
    }

    @FXML
    private void initialize() {
        executeRunner("Could not initialise simple rename dialog", () -> {
            EventBus.getDefault().register(this);
            mivFolder.managedProperty().bind(mivFolder.visibleProperty());
            imvFile.managedProperty().bind(imvFile.visibleProperty());
            imvFile.visibleProperty().bind(mivFolder.visibleProperty().not());
            addHandlerToSceneAndWindow();
        });
    }

    private void addHandlerToSceneAndWindow() {
        WindowEventUtility.bindWindowEventHandler(getClass(), txtName, new WindowEventUtility.WindowEventHandler() {
            @Override
            public void handleShownEvent() {
                // select only the name of the file by default
                txtName.requestFocus();
                String name = FilenameUtils.removeExtension(file.getName());
                txtName.selectRange(0, name.length());
            }

            @Override
            public void handleFocusedEvent() {
            }

            @Override
            public void handleCloseEvent(WindowEvent windowEvent) {
            }
        });
    }

    @FXML
    private void keyPressed(KeyEvent keyEvent) {
        executeRunner("Could not handle key event", () -> {
            switch (keyEvent.getCode()) {
                case ENTER:
                    rename(null);
                    break;

                case ESCAPE:
                    cancel(null);
                    break;

                default:
                    break;
            }
        });
    }

    @FXML
    private void rename(ActionEvent event) {
        executeRunner("Could not apply rename", () -> {
            String newName = txtName.getText() == null ? "" : txtName.getText();
            File target = new File(file.getParent() + "\\" + newName);
            boolean result = true;

            // do not ask for confirm if use old name
            if (target.exists() && !newName.equalsIgnoreCase(file.getName())) {
                result = showConfirmation("There is already a file or folder with the same name, do you want to " +
                        "overwrite it?");
            }

            if (result) {
                writeInfoLog("Raname file " + file.getAbsolutePath() + " to " + target.getAbsolutePath());
                FileLogic.rename(file, target);
                hideDialog();
            }
        });
    }

    private void hideDialog() {
        txtName.getScene().getWindow().hide();
    }

    @FXML
    private void cancel(ActionEvent event) {
        executeRunner("Could not cancel rename", () -> hideDialog());
    }
}