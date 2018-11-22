package xyz.gnas.elif.app.controllers.Dialog;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import org.apache.commons.io.FilenameUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.dialog.SingleRenameEvent;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.core.logic.FileLogic;

import java.io.File;

import static xyz.gnas.elif.app.common.Utility.showConfirmation;

public class SingleRenameController {
    @FXML
    private MaterialIconView mivFolder;

    @FXML
    private ImageView imvFile;

    @FXML
    private Label lblFile;

    @FXML
    private TextField txtName;

    private ExplorerItemModel item;

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        Utility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onSingleRenameEvent(SingleRenameEvent event) {
        try {
            item = event.getItem();
            File file = item.getFile();
            mivFolder.setVisible(file.isDirectory());

            if (!file.isDirectory()) {
                imvFile.setImage(Utility.getFileIcon(file, true));
            }

            lblFile.setText(file.getAbsolutePath());
            txtName.setText(file.getName());
        } catch (Exception e) {
            showError(e, "Error handling single rename event", false);
        }
    }

    @FXML
    private void initialize() {
        try {
            EventBus.getDefault().register(this);
            mivFolder.managedProperty().bind(mivFolder.visibleProperty());
            imvFile.managedProperty().bind(imvFile.visibleProperty());
            imvFile.visibleProperty().bind(mivFolder.visibleProperty().not());

            txtName.sceneProperty().addListener(s -> {
                Scene scene = txtName.getScene();

                if (scene != null) {
                    scene.getWindow().setOnShown(l -> {
                        // select only the name of the file by default
                        txtName.requestFocus();
                        String name = FilenameUtils.removeExtension(item.getFile().getName());
                        txtName.selectRange(0, name.length());
                    });
                }
            });
        } catch (Exception e) {
            showError(e, "Could not initialise single rename dialog", true);
        }
    }

    @FXML
    private void keyPressed(KeyEvent keyEvent) {
        try {
            switch (keyEvent.getCode()) {
                case ENTER:
                    apply(null);
                    break;

                case ESCAPE:
                    cancel(null);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            showError(e, "Could not handle key event", false);
        }
    }

    @FXML
    private void apply(ActionEvent event) {
        try {
            File source = item.getFile();
            File target = new File(source.getParent() + "\\" + txtName.getText());
            boolean result = true;

            if (target.exists()) {
                result = showConfirmation("There is already a file or folder with the same name, do you want to " +
                        "overwrite it?");
            }

            if (result) {
                writeInfoLog("Raname file " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
                FileLogic.rename(source, target);
                EventBus.getDefault().post(new ReloadEvent(target.getParent()));
                hideDialog();
            }
        } catch (Exception e) {
            showError(e, "Could not apply rename", false);
        }
    }

    private void hideDialog() {
        txtName.getScene().getWindow().hide();
    }

    @FXML
    private void cancel(ActionEvent event) {
        try {
            hideDialog();
        } catch (Exception e) {
            showError(e, "Could not cancel rename", false);
        }
    }
}