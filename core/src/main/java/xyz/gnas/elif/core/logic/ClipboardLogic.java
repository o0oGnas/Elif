package xyz.gnas.elif.core.logic;

import javafx.beans.property.BooleanProperty;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class ClipboardLogic {
    /**
     * list of files cut to clipboard
     */
    private static List<File> cutList = new LinkedList<>();

    private static Clipboard clipboard = Clipboard.getSystemClipboard();

    public static boolean clipboardHasFiles() {
        return clipboard.hasFiles();
    }

    public static void copyToClipboard(List<File> fileList) {
        setFileListContent(fileList);
        cutList.clear();
    }

    private static void setFileListContent(List<File> fileList) {
        ClipboardContent content = new ClipboardContent();
        content.putFiles(fileList);
        clipboard.setContent(content);
        cutList.clear();
    }

    public static void cutToClipboard(List<File> fileList) {
        setFileListContent(fileList);
        cutList.addAll(fileList);
    }

    public static List<File> getFiles(BooleanProperty isCut) {
        List<File> fileList = clipboard.getFiles();
        setIsCut(fileList, isCut);
        clipboard.clear();
        cutList.clear();
        return fileList;
    }

    private static void setIsCut(List<File> fileList, BooleanProperty isCut) {
        isCut.set(true);

        // check that all files in return list are in cut list, if one of them isn't then set cut flag to false
        for (File file : fileList) {
            boolean exists = false;

            for (File cut : cutList) {
                if (file.getAbsolutePath().equalsIgnoreCase(cut.getAbsolutePath())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                isCut.set(false);
                break;
            }
        }
    }
}
