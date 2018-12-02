package xyz.gnas.elif.app.common.utility;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.utility.runner.RunnerUtility;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static xyz.gnas.elif.app.common.utility.runner.RunnerUtility.executeMainThreadAndExceptionRunner;

public final class DialogUtility {
    private static void writeErrorLog(String message, Throwable e) {
        LogUtility.writeErrorLog(DialogUtility.class, message, e);
    }

    /**
     * Show error message with the exception stack trace and write a log, additionally exit the application if exit
     * flag is true
     *
     * @param callingClass the calling class
     * @param message      the message to display to user
     * @param e            the exception
     * @param exit         flag to exit the application
     */
    public static void showError(Class callingClass, String message, Throwable e, boolean exit) {
        executeMainThreadAndExceptionRunner(() -> {
            String stackTrace = getStackTrace(e);
            GridPane expContent = getExpandableContent(stackTrace);
            Alert alert = new Alert(AlertType.ERROR);
            initialiseAlert(alert, "Error", "An error has occurred!", message + ". See details below");
            alert.getDialogPane().setExpandableContent(expContent);
            alert.showAndWait();
            LogUtility.writeErrorLog(callingClass, message, e);
            checkExitFlagAndExit(exit);
        }, (Exception ex) -> {
            writeErrorLog("Could not display error", ex);
            checkExitFlagAndExit(exit);
        });
    }

    private static void checkExitFlagAndExit(boolean exit) {
        if (exit) {
            Platform.exit();
        }
    }

    private static String getStackTrace(Throwable e) throws IOException {
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

    /**
     * Show warning dialogs
     *
     * @param headerText the header text
     * @param message    the message
     */
    public static void showWarning(String headerText, String message) {
        executeMainThreadAndExceptionRunner(() -> {
            Alert alert = new Alert(AlertType.WARNING);
            initialiseAlert(alert, "Message", headerText, message);
            alert.showAndWait();
        }, (Exception e) -> writeErrorLog("Could not display warning", e));
    }

    /**
     * Show a custom dialogs
     *
     * @param dialogName the dialogs name
     * @param content    the node object representing the content
     * @param icon       the icon of the dialogs (can be null)
     */
    public static void showCustomDialog(String dialogName, Node content, Image icon) {
        executeMainThreadAndExceptionRunner(() -> {
            Alert alert = new Alert(AlertType.NONE);
            alert.setTitle(dialogName);
            initialiseCustomDialogPane(alert, content, icon);
            alert.showAndWait();
        }, (Exception e) -> writeErrorLog("Could not display custom dialogs", e));
    }

    private static void initialiseCustomDialogPane(Alert alert, Node content, Image icon) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getScene().getStylesheets().addAll(ResourceManager.getCSSList());
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

    /**
     * Show confirmation dialogs, must be called in main thread
     *
     * @param message the message
     * @return the boolean result - true is OK, false is cancel
     */
    public static boolean showConfirmation(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        initialiseAlert(alert, "Confirmation", "Please confirm this action", message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Show dialogs with options, must be called in main thread
     *
     * @param message the message
     * @param options the options
     * @return the string result, which is one of the options
     */
    public static String showOptions(String message, String... options) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        initialiseAlert(alert, "Option selection required", message, "Please select an option below");
        alert.getButtonTypes().clear();

        for (String option : options) {
            ButtonType button = new ButtonType(option);
            alert.getButtonTypes().add(button);
        }

        DialogPane dialogPane = alert.getDialogPane();
        addKeyEventHandlerForOptionDialog(dialogPane);
        addCloseButton(dialogPane);
        Optional<ButtonType> result = alert.showAndWait();
        return getOptionResult(result);
    }

    private static void addKeyEventHandlerForOptionDialog(DialogPane dialogPane) {
        // close dialogs if escape key is pressed
        dialogPane.addEventHandler(KeyEvent.KEY_PRESSED,
                (KeyEvent event) -> RunnerUtility.executeVoidrunner(DialogUtility.class,
                        "Error when handling key event for dialogs", () -> {
                            if (event.getCode() == KeyCode.ESCAPE) {
                                dialogPane.getScene().getWindow().hide();
                            }
                        }));
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