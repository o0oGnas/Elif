package main.java.xyz.gnas.elif.common;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javafx.scene.image.Image;
import main.java.xyz.gnas.elif.Main;

/**
 * @author Gnas
 * @Description Manage resources, including lazy load and caching
 * @Date Oct 10, 2018
 */
public class ResourceManager {
	private static final String RESOURCE_FOLDER = "main/resources/";
	private static final String CSS_FOLDER = RESOURCE_FOLDER + "css/";
	private static final String FXML_FOLDER = RESOURCE_FOLDER + "fxml/";
	private static final String ICON_FOLDER = RESOURCE_FOLDER + "icons/";

	private static Image appIcon;

	private static List<String> cssList;

	private static URL appFXML;
	private static URL explorerFXML;

	public static Image getAppIcon() {
		if (appIcon == null) {
			appIcon = new Image(Main.class.getClassLoader().getResourceAsStream(ICON_FOLDER + "app.png"));
		}

		return appIcon;
	}

	public static List<String> getCSSList() {
		if (cssList == null) {
			cssList = new LinkedList<String>();
			cssList.add(Main.class.getClassLoader().getResource(CSS_FOLDER + "app.css").toExternalForm());
			cssList.add(Main.class.getClassLoader().getResource(CSS_FOLDER + "theme.css").toExternalForm());
		}

		return cssList;
	}

	public static URL getAppFXML() {
		if (appFXML == null) {
			appFXML = Main.class.getClassLoader().getResource(FXML_FOLDER + "App.fxml");
		}

		return appFXML;
	}

	public static URL getExplorerFXML() {
		if (explorerFXML == null) {
			explorerFXML = Main.class.getClassLoader().getResource(FXML_FOLDER + "Explorer.fxml");
		}

		return explorerFXML;
	}
}
