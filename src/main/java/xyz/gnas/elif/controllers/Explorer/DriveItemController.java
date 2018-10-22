package main.java.xyz.gnas.elif.controllers.Explorer;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import main.java.xyz.gnas.elif.common.CommonUtility;

public class DriveItemController {
	@FXML
	private ImageView imvDrive;

	@FXML
	private Label lblDrive;

	private void showError(Exception e, String message, boolean exit) {
		CommonUtility.showError(getClass(), e, message, exit);
	}

	public void initialiseDrive(File drive) {
		FileSystemView fsv = FileSystemView.getFileSystemView();
		Icon icon = fsv.getSystemIcon(drive);
		BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.createGraphics();
		icon.paintIcon(null, g, 0, 0);
		imvDrive.setImage(SwingFXUtils.toFXImage(bi, null));
		lblDrive.setText(fsv.getSystemDisplayName(drive));
		lblDrive.setTextFill(Color.BLACK);
	}

	@FXML
	private void initialize() {
		try {
		} catch (Exception e) {
			showError(e, "Could not initialise drive item", true);
		}
	}
}