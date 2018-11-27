package xyz.gnas.elif.app.common.utility;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static javafx.application.Platform.runLater;

public final class DialogUtility {
    public static void showError(Class callingClass, String message, Throwable e, boolean exit) {
        runLater(() -> {
            try {
                String stackTrace = getStrackTrace(e);
                GridPane expContent = getExpandableContent(stackTrace);
                Alert alert = new Alert(AlertType.ERROR);
                initialiseAlert(alert, "Error", "An error has occurred!", message + ". See details below");
                alert.getDialogPane().setExpandableContent(expContent);
                alert.showAndWait();
                writeErrorLog(callingClass, message, e);
            } catch (Exception ex) {
                writeErrorLog(DialogUtility.class, "Could not display error", ex);
            }

            if (exit) {
                System.exit(1);
            }
        });
    }

    private static String getStrackTrace(Throwable e) throws IOException {
        try (StringWriter sw = new StringWriter()) {
            try (PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);
                return sw.toString();
            }
        }
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

    private static void initialiseAlert(Alert alert, String title, String headerText, String contentText) {
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
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
                initialiseAlert(alert, "Message", headerText, message);
                alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                alert.showAndWait();
            } catch (Exception e) {
                writeErrorLog(DialogUtility.class, "Could not display alert", e);
            }
        });
    }

    public static void showCustomDialog(String dialogName, Node content, Image icon) {
        runLater(() -> {
            try {
                Alert alert = new Alert(AlertType.NONE);
                alert.setTitle(dialogName);
                initialiseCustomDialogPane(alert, content, icon);
                alert.showAndWait();
            } catch (Exception e) {
                writeErrorLog(DialogUtility.class, "Could not display custom dialog", e);
            }
        });
    }

    private static void initialiseCustomDialogPane(Alert alert, Node content, Image icon) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(content);

        if (icon != null) {
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.getIcons().add(icon);
        }

        addCloseButton(dialogPane);
    }


    // add hidden close button so "X" button works
    // https://stackoverflow.com/questions/32048348/javafx-scene-control-dialogr-wont-close-on
    // -pressing-x
    private static void addCloseButton(DialogPane dialogPane) {
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = dialogPane.lookupButton(ButtonType.CLOSE);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);
    }

    public static boolean showConfirmation(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        initialiseAlert(alert, "Confirmation", "Please confirm this action", message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static String showOptions(String message, String... options) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        initialiseAlert(alert, "Option selection required", message, "Please select an option below");
        alert.getButtonTypes().clear();

        for (String option : options) {
            ButtonType button = new ButtonType(option);
            alert.getButtonTypes().add(button);
        }

        addCloseButton(alert.getDialogPane());
        Optional<ButtonType> result = alert.showAndWait();
        return getOptionResult(result);
    }


    private static String getOptionResult(Optional<ButtonType> result) {
        if (result.isPresent()) {
            ButtonType selectedOption = result.get();

            if (selectedOption == ButtonType.CLOSE) {
                return null;
            } else {
                return selectedOption.getText();
            }
        } else {
            return null;
        }
    }
}