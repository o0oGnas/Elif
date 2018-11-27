package xyz.gnas.elif.app.models.explorer;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Calendar;

public class ExplorerItemModel implements Comparable<ExplorerItemModel> {
    private File file;

    /**
     * name without extension
     */
    private String name;
    private String extension;

    private long size;

    private Calendar date;

    public ExplorerItemModel(File file) {
        this.file = file;
        String fileName = file.getName();

        if (file.isDirectory()) {
            name = fileName;
            extension = "[Folder]";
        } else {
            name = FilenameUtils.removeExtension(fileName);
            extension = FilenameUtils.getExtension(fileName);
        }

        long fileSize = file.length();
        size = fileSize >= 0 ? fileSize : -1;
        date = Calendar.getInstance();
        date.setTimeInMillis(file.lastModified());
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    public Calendar getDate() {
        return date;
    }

    @Override
    public int compareTo(ExplorerItemModel o) {
        if (file.isDirectory() == o.file.isDirectory()) {
            return file.getName().compareTo(o.file.getName());
        } else {
            return file.isDirectory() ? -1 : 1;
        }
    }
}
