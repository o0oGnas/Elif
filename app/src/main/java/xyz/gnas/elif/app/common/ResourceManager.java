package xyz.gnas.elif.app.common;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javafx.scene.image.Image;
import xyz.gnas.elif.app.FXMain;

/**
 * @author Gnas
 * @Description Manage resources, including lazy initialisation
 * @Date Oct 10, 2018
 */
public class ResourceManager {
	private static Image appIcon;

	private static List<String> cssList;

	private static URL appFXML;
	private static URL explorerFXML;
	private static URL driveItemFXML;

	public static Image getAppIcon() {
		if (appIcon == null) {
			appIcon = new Image(getClassLoader().getResourceAsStream("icon.png"));
		}

		return appIcon;
	}

	private static ClassLoader getClassLoader() {
		return FXMain.class.getClassLoader();
	}

	public static List<String> getCSSList() {
		if (cssList == null) {
			cssList = new LinkedList<String>();
			cssList.add(getCSS("app"));
			cssList.add(getCSS("theme"));
		}

		return cssList;
	}

	private static String getCSS(String cssName) {
		return getResourceString("css/" + cssName + ".css");
	}

	private static String getResourceString(String resource) {
		return getResourceURL(resource).toExternalForm();
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
			explorerFXML = getExplorerXMLWrapper("Explorer");
		}

		return explorerFXML;
	}

	private static URL getExplorerXMLWrapper(String fxml) {
		return getFXML("explorer/" + fxml);
	}

	public static URL getDriveItemFXML() {
		if (driveItemFXML == null) {
			driveItemFXML = getExplorerXMLWrapper("DriveItem");
		}

		return driveItemFXML;
	}
}
