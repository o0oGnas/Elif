package xyz.gnas.elif.app.common;

import javafx.scene.image.Image;
import javafx.scene.media.Media;
import xyz.gnas.elif.app.main.FXMain;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Manage resources, including lazy initialisation
 */
public class ResourceManager {
    private static Image appIcon;
    private static Image simpleRenameIcon;
    private static Image editAsTextIcon;

    private static Media notificationSound;

    private static List<String> cssList;

    private static URL appFXML;
    private static URL operationFXML;
    private static URL explorerFXML;
    private static URL simpleRenameFXML;
    private static URL editAsTextFXML;

    public static Image getAppIcon() {
        if (appIcon == null) {
            appIcon = getIcon("app.png");
        }

        return appIcon;
    }

    private static Image getIcon(String iconName) {
        return new Image(getClassLoader().getResourceAsStream("icons/" + iconName));
    }

    private static ClassLoader getClassLoader() {
        return FXMain.class.getClassLoader();
    }

    public static Image getSimpleRenameIcon() {
        if (simpleRenameIcon == null) {
            simpleRenameIcon = getIcon("simple_rename.png");
        }

        return simpleRenameIcon;
    }

    public static Image getEditAsTextIcon() {
        if (editAsTextIcon == null) {
            editAsTextIcon = getIcon("edit_as_text.png");
        }

        return editAsTextIcon;
    }

    public static Media getNotificationSound() {
        if (notificationSound == null) {
            notificationSound = new Media(getResourceString("notification.mp3"));
        }

        return notificationSound;
    }

    private static String getResourceString(String resource) {
        return getResourceURL(resource).toExternalForm();
    }

    public static List<String> getCSSList() {
        if (cssList == null) {
            cssList = new LinkedList<>();
            cssList.add(getCSS("app"));
            cssList.add(getCSS("theme"));
        }

        return cssList;
    }

    private static String getCSS(String cssName) {
        return getResourceString("css/" + cssName + ".css");
    }

    private static URL getResourceURL(String resource) {
        return getClassLoader().getResource(resource);
    }

    public static URL getAppFXML() {
        if (appFXML == null) {
            appFXML = getFXML("App");
        }

        return appFXML;
    }

    private static URL getFXML(String fxml) {
        return getResourceURL("fxml/" + fxml + ".fxml");
    }

    public static URL getExplorerFXML() {
        if (explorerFXML == null) {
            explorerFXML = getFXML("explorer/explorer");
        }

        return explorerFXML;
    }

    public static URL getOperationFXML() {
        if (operationFXML == null) {
            operationFXML = getFXML("operation/operation");
        }

        return operationFXML;
    }

    public static URL getSimpleRenameFXML() {
        if (simpleRenameFXML == null) {
            simpleRenameFXML = getDialogFXML("SimpleRename");
        }

        return simpleRenameFXML;
    }

    private static URL getDialogFXML(String fxml) {
        return getFXML("dialog/" + fxml);
    }

    public static URL getEditAsTextFXML() {
        if (editAsTextFXML == null) {
            editAsTextFXML = getDialogFXML("EditAsText");
        }

        return editAsTextFXML;
    }
}
