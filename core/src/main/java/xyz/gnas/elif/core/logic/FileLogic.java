package xyz.gnas.elif.core.logic;

import javafx.beans.property.DoubleProperty;
import xyz.gnas.elif.core.models.Operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static java.lang.Thread.sleep;

public class FileLogic {
    /**
     * Copy to from source to target
     *
     * @param source    the source file
     * @param target    the target file
     * @param operation the operation object that is running
     * @param progress  the progress wrapper to track the progress of this method, value is from 0 to 1, it's 1 once
     *                  the method finishes
     * @throws IOException the io exception
     */
    public static void copy(File source, File target, Operation operation, DoubleProperty progress)
            throws IOException, InterruptedException {
        try (FileChannel inputChannel = new FileInputStream(source).getChannel()) {
            performCopy(inputChannel, target, operation, progress);
            completeProgress(progress);
        }
    }

    private static void performCopy(FileChannel inputChannel, File target, Operation operation,
                                    DoubleProperty progress) throws IOException, InterruptedException {
        long sourceSize = inputChannel.size();

        try (FileChannel outputChannel = new FileOutputStream(target).getChannel()) {
            long stepSize = 1024 * 1024;

            for (long i = 0; i < sourceSize; i = i + stepSize) {
                while (operation.isPaused()) {
                    sleep(500);
                }

                if (operation.isStopped()) {
                    completeProgress(progress);
                    break;
                } else {
                    copyChunk(i, stepSize, sourceSize, inputChannel, outputChannel, progress);
                }
            }
        }
    }

    private static void completeProgress(DoubleProperty progress) {
        progress.set(1);
    }

    private static void copyChunk(long i, long stepSize, long sourceSize, FileChannel inputChannel,
                                  FileChannel outputChannel, DoubleProperty progress) throws IOException {
        long chunkSize = stepSize;

        // the size of the last chunk is the remaining bytes to copy
        if (i + stepSize > sourceSize) {
            chunkSize = sourceSize - i;
        }

        outputChannel.transferFrom(inputChannel, i, chunkSize);
        progress.set(i * 1.0 / sourceSize);
    }

    public static void move(File source, File target, Operation operation, DoubleProperty progress)
            throws IOException, InterruptedException {
        String sourceRoot = getRootPath(source);
        String targetRoot = getRootPath(target);

        // use java.nio.file.Files.move when moving in the same drive because it's much faster, otherwise use copy
        // function and delete the source file after finish
        if (sourceRoot.equalsIgnoreCase(targetRoot)) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            completeProgress(progress);
        } else {
            copy(source, target, operation, progress);

            // delete source after copying
            if (!operation.isStopped()) {
                delete(source);
            }
        }
    }

    private static String getRootPath(File file) {
        String path = file.getAbsolutePath();
        return path.substring(0, path.indexOf("\\") + 1);
    }

    /**
     * Delete a file
     *
     * @param file the file
     */
    public static void delete(File file) throws IOException {
        file.delete();
    }

    /**
     * Rename a file
     *
     * @param source the file to rename
     * @param target the file object representing result of renaming
     */
    public static void rename(File source, File target) throws IOException {
        source.renameTo(target);
    }

    /**
     * create a new folder
     *
     * @param parent the parent path of the folder
     * @return the file object representing the new folder
     */
    public static File addNewFolder(String parent) {
        File folder = getNewFileOrFolder(parent, false);
        folder.mkdir();
        return folder;
    }

    /**
     * Read file as text string.
     *
     * @param file the file to read
     * @return the content of the file as a String
     * @throws IOException the io exception
     */
    public static String readFileAsText(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, Charset.defaultCharset());
    }

    /**
     * Save text as file content
     *
     * @param file the file
     * @param text the text content
     * @throws FileNotFoundException the file not found exception
     */
    public static void saveTextToFile(File file, String text) throws FileNotFoundException {
        try (PrintStream ps = new PrintStream(new FileOutputStream(file))) {
            ps.print(text);
        }
    }

    private static File getNewFileOrFolder(String parent, boolean isFile) {
        String parentPath = parent + "\\";
        String fileFolder = isFile ? "file" : "folder";
        String newFileFolderPath = parentPath + "New " + fileFolder;
        File fileOrFolder = new File(newFileFolderPath);
        int index = 2;

        while (fileOrFolder.exists()) {
            fileOrFolder = new File(newFileFolderPath + " (" + index + ")");
            ++index;
        }

        return fileOrFolder;
    }

    /**
     * create a new file
     *
     * @param parent the parent path of the file
     * @return the file object representing the new file
     * @throws IOException the io exception
     */
    public static File addNewFile(String parent) throws IOException {
        File file = getNewFileOrFolder(parent, true);
        file.createNewFile();
        return file;
    }
}