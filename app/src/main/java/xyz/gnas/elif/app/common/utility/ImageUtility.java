package xyz.gnas.elif.app.common.utility;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.commons.io.FilenameUtils;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImageUtility {
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

    /**
     * Gets icon of a File object
     *
     * @param file     the file
     * @param useCache flag for using cache or not
     * @return the icon
     */
    public static WritableImage getFileIcon(File file, boolean useCache) {
        String fileName = file.getName();
        String extension = FilenameUtils.getExtension(fileName).toUpperCase();
        String name = FilenameUtils.removeExtension(fileName).toUpperCase();
        boolean checkCacheByName = CACHE_BY_NAME_EXTENSION.contains("," + extension + ",");
        boolean checkHasNameCache = useCache && checkCacheByName && nameIconMap.containsKey(name);
        boolean checkHasExtensionCache = useCache && !checkCacheByName && extensionIconMap.containsKey(extension);
        return checkCacheAndGetWritableImage(checkCacheByName, checkHasNameCache, checkHasExtensionCache, name,
                extension, file, useCache);
    }

    private static WritableImage checkCacheAndGetWritableImage(boolean checkCacheByName, boolean checkHasNameCache,
                                                               boolean checkHasExtensionCache, String name,
                                                               String extension, File file, boolean useCache) {
        if (checkHasNameCache) {
            return nameIconMap.get(name);
        } else if (checkHasExtensionCache) {
            return extensionIconMap.get(extension);
        } else {
            WritableImage wr = getWritableImage(file);

            if (wr != null) {
                if (useCache) {
                    if (checkCacheByName) {
                        nameIconMap.put(name, wr);
                    } else {
                        extensionIconMap.put(extension, wr);
                    }
                }
            }

            return wr;
        }
    }

    private static WritableImage getWritableImage(File file) {
        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);

        if (icon == null) {
            return null;
        } else {
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
}
