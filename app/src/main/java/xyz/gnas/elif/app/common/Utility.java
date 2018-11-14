package xyz.gnas.elif.app.common;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static javafx.application.Platform.runLater;

public final class Utility {
    /**
     * Cache by file name for files of these extensions
     */
    private static final String CACHE_BY_NAME_EXTENSION = ",EXE,MSI,";

    /**
     * cache icons based on extensions
     */
    private static Map<String, WritableImage> extensionIconMap = new HashMap<>();

    /**
     * cache icons based on extensions
     */
    private static Map<String, WritableImage> nameIconMap = new HashMap<>();

    public static void showError(Class callingClass, Throwable e, String message, boolean exit) {
        runLater(() -> {
            try {
                // Get stack trace as string
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String stackTrace = sw.toString();

                GridPane expContent = getExpandableContent(stackTrace);

                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("An error has occurred!");
                alert.setContentText(message + ". See details below");
                alert.getDialogPane().setExpandableContent(expContent);
                alert.showAndWait();

                writeErrorLog(callingClass, message, e);
            } catch (Exception ex) {
                writeErrorLog(Utility.class, "Could not display error", ex);
            }

            if (exit) {
                System.exit(1);
            }
        });
    }

    private static GridPane getExpandableContent(String sStackTrace) {
        TextArea textArea = new TextArea(sStackTrace);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);
        return expContent;
    }

    public static void writeErrorLog(Class callingClass, String message, Throwable e) {
        try {
            Logger logger = LoggerFactory.getLogger(callingClass);
            logger.error(message, e);
        } catch (Exception ex) {
            System.out.println("Error writing error log");
        }
    }

    public static void writeInfoLog(Class callingClass, String log) {
        try {
            Logger logger = LoggerFactory.getLogger(callingClass);
            logger.info(log);
        } catch (Exception ex) {
            System.out.println("Error writing info log");
        }
    }

    public static void showAlert(String headerText, String message) {
        runLater(() -> {
            try {
                Alert alert = new Alert(AlertType.NONE);
                alert.setTitle("Message");
                alert.setHeaderText(headerText);
                alert.setContentText(message);
                alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                alert.showAndWait();
            } catch (Exception e) {
                writeErrorLog(Utility.class, "Could not display alert", e);
            }
        });
    }

    public static boolean showConfirmation(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Please confirm this action");
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static String showOptions(String message, String... options) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Select option");
        alert.setHeaderText("Please select an option");
        alert.setContentText(message);
        alert.getButtonTypes().clear();

        for (String option : options) {
            ButtonType button = new ButtonType(option);
            alert.getButtonTypes().add(button);
        }

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            return result.get().getText();
        } else {
            return null;
        }
    }

    public static WritableImage getFileIcon(File file, boolean useCache) {
        String fileName = file.getName();
        String extension = FilenameUtils.getExtension(fileName).toUpperCase();
        String name = FilenameUtils.removeExtension(fileName).toUpperCase();
        boolean checkCacheByName = CACHE_BY_NAME_EXTENSION.contains("," + extension + ",");
        boolean checkHasNameCache = useCache && checkCacheByName && nameIconMap.containsKey(name);
        boolean checkHasExtensionCache = useCache && !checkCacheByName && extensionIconMap.containsKey(extension);

        if (checkHasNameCache) {
            return nameIconMap.get(name);
        } else if (checkHasExtensionCache) {
            return extensionIconMap.get(extension);
        } else {
            WritableImage wr = getWritableImage(file);

            if (useCache) {
                if (checkCacheByName) {
                    nameIconMap.put(name, wr);
                } else {
                    extensionIconMap.put(extension, wr);
                }
            }

            return wr;
        }
    }

    private static WritableImage getWritableImage(File file) {
        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        WritableImage wr = new WritableImage(bi.getWidth(), bi.getHeight());
        PixelWriter pw = wr.getPixelWriter();

        for (int x = 0; x < bi.getWidth(); x++) {
            for (int y = 0; y < bi.getHeight(); y++) {
                pw.setArgb(x, y, bi.getRGB(x, y));
            }
        }

        return wr;
    }
}