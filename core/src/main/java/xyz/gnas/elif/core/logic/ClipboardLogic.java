package xyz.gnas.elif.core.logic;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import xyz.gnas.elif.core.models.explorer.ExplorerItemModel;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class ClipboardLogic {
    private static final Clipboard clipboard = Clipboard.getSystemClipboard();

    public static boolean clipboardHasFiles() {
        return clipboard.hasFiles();
    }

    public static void copyToClipboard(List<ExplorerItemModel> itemList) {
        ClipboardContent content = new ClipboardContent();
        List<File> fileList = new LinkedList<>();

        for (ExplorerItemModel item : itemList) {
            fileList.add(item.getFile());
        }

        content.putFiles(fileList);
        clipboard.setContent(content);
    }

    public static List<File> getFiles() {
        return clipboard.getFiles();
    }

    public static void clear() {
        clipboard.clear();
    }
}
