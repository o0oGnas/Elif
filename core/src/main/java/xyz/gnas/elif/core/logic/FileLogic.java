package xyz.gnas.elif.core.logic;

import javafx.beans.property.DoubleProperty;
import xyz.gnas.elif.core.models.Operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
    public static void copy(File source, File target, Operation operation, DoubleProperty progress) throws IOException, InterruptedException {
        try (FileChannel inputChannel = new FileInputStream(source).getChannel()) {
            long sourceSize = inputChannel.size();

            try (FileChannel outputChannel = new FileOutputStream(target).getChannel()) {
                long stepSize = 1024 * 1024;

                for (long i = 0; i < sourceSize; i = i + stepSize) {
                    while (operation.isPaused()) {
                        Thread.sleep(500);
                    }

                    if (operation.isStopped()) {
                        completeProgress(progress);
                        break;
                    } else {
                        copyChunk(i, stepSize, sourceSize, inputChannel, outputChannel, progress);
                    }
                }
            }

            completeProgress(progress);
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

    public static void move(File source, File target, Operation operation, DoubleProperty progress) throws IOException, InterruptedException {
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
                source.delete();
            }
        }
    }

    private static String getRootPath(File file) {
        String path = file.getAbsolutePath();
        return path.substring(0, path.indexOf("\\") + 1);
    }

    public static void delete(File file) {
        file.delete();
    }

    public static void rename(File source, File target) {
        source.renameTo(target);
    }
}