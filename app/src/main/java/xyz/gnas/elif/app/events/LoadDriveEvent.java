package xyz.gnas.elif.app.events;

import java.io.File;

public class LoadDriveEvent {
	private File drive;

	public File getDrive() {
		return drive;
	}

	public LoadDriveEvent(File drive) {
		this.drive = drive;
	}
}
