package xyz.gnas.elif.core.logic;

import javafx.beans.property.DoubleProperty;
import xyz.gnas.elif.core.models.Operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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
    public static void copyToPath(File source, File target, Operation operation, DoubleProperty progress) throws IOException, InterruptedException {
        try (FileChannel inputChannel = new FileInputStream(source).getChannel()) {
            long sourceSize = inputChannel.size();

            try (FileChannel outputChannel = new FileOutputStream(target).getChannel()) {
                long stepSize = 1024 * 1024;

                for (long i = 0; i < sourceSize; i = i + stepSize) {
                    while (operation.getPaused()) {
                        Thread.sleep(500);
                    }

                    if (operation.getStopped()) {
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
                                  FileChannel outputChannel
            , DoubleProperty progress) throws IOException {
        long chunkSize = stepSize;

        // the size of the last chunk is the remaining bytes to copy
        if (i + stepSize > sourceSize) {
            chunkSize = sourceSize - i;
        }

        outputChannel.transferFrom(inputChannel, i, chunkSize);
        progress.set(i * 1.0 / sourceSize);
    }
}