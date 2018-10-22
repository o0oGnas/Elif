package main.java.xyz.gnas.elif.models;

import java.io.File;
import java.util.Calendar;

import org.apache.commons.io.FilenameUtils;

public class ExplorerItem {
	private File file;
	private String name;
	private String extension;
	private long size;
	private Calendar date;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public Calendar getDate() {
		return date;
	}

	public void setDate(Calendar date) {
		this.date = date;
	}

	public ExplorerItem(File file) {
		this.file = file;
		String fileName = file.getName();
		name = FilenameUtils.removeExtension(fileName);
		extension = file.isDirectory() ? "[Folder]" : FilenameUtils.getExtension(fileName);
		long fileSize = file.length();
		size = fileSize >= 0 ? fileSize : -1;
		date = Calendar.getInstance();
		date.setTimeInMillis(file.lastModified());
	}
}
