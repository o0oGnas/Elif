package xyz.gnas.elif.core.logic;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class ClipboardLogic {
    private static List<File> cutList = new LinkedList<>();

    private static final Clipboard clipboard = Clipboard.getSystemClipboard();

    public static boolean clipboardHasFiles() {
        return clipboard.hasFiles();
    }

    public static void copyToClipboard(List<File> fileList) {
        ClipboardContent content = new ClipboardContent();
        content.putFiles(fileList);
        clipboard.setContent(content);
        cutList.clear();
    }

    public static List<File> getFiles() {
        return clipboard.getFiles();
    }

    public static void clear() {
        clipboard.clear();
        cutList.clear();
    }
}
