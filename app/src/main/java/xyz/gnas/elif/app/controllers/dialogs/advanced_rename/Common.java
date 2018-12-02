package xyz.gnas.elif.app.controllers.dialogs.advanced_rename;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.ImageUtility;

import java.io.File;
import java.util.List;

class Common {
    public static Node getIcon(File file) {
        HBox hbx = new HBox();
        List<Node> childrenList = hbx.getChildren();
        Node icon;

        if (file.isDirectory()) {
            icon = new MaterialIconView(MaterialIcon.FOLDER_OPEN, Configurations.ICON_SIZE);
        } else {
            icon = new ImageView(ImageUtility.getFileIcon(file, true));
        }

        childrenList.add(icon);

        // add left margin to icon
        HBox.setMargin(icon, new Insets(0, 10, 0, 10));
        return hbx;
    }
}
