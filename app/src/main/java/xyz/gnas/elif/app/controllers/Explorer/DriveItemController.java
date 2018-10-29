package xyz.gnas.elif.app.controllers.Explorer;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.LoadDriveEvent;

public class DriveItemController {
	@FXML
	private ImageView imvDrive;

	@FXML
	private Label lblDrive;

	private File drive;

	private void showError(Exception e, String message, boolean exit) {
		Utility.showError(getClass(), e, message, exit);
	}

	@Subscribe
	public void onLoadDriveEvent(LoadDriveEvent event) {
		// prevent reloading
		if (drive == null) {
			drive = event.getDrive();
			imvDrive.setImage(Utility.getFileIcon(drive));
			lblDrive.setText(FileSystemView.getFileSystemView().getSystemDisplayName(drive));
			lblDrive.setTextFill(Color.BLACK);
		}
	}

	@FXML
	private void initialize() {
		try {
			EventBus.getDefault().register(this);
		} catch (Exception e) {
			showError(e, "Could not initialise drive item", true);
		}
	}
}