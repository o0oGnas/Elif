package xyz.gnas.elif.app.controllers.Explorer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import org.greenrobot.eventbus.EventBus;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.events.operation.AddOperationEvent;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;
import xyz.gnas.elif.core.logic.ClipboardLogic;
import xyz.gnas.elif.core.logic.FileLogic;
import xyz.gnas.elif.core.models.Operation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Thread.sleep;
import static javafx.application.Platform.runLater;
import static xyz.gnas.elif.app.common.Utility.showAlert;
import static xyz.gnas.elif.app.common.Utility.showConfirmation;

/**
 * Handles operations
 */
public class OperationHandler {
    public enum CopyMode {
        COPY, MOVE, PASTE
    }

    private static void postEvent(Object object) {
        EventBus.getDefault().post(object);
    }

    private static void showError(Exception e, String message, boolean exit) {
        Utility.showError(OperationHandler.class, e, message, exit);
    }

    private static void writeInfoLog(String log) {
        Utility.writeInfoLog(OperationHandler.class, log);
    }

    public static void copy(ExplorerModel model, String targetPath, List<ExplorerItemModel> sourceList, CopyMode mode) {
        if (mode == CopyMode.MOVE && model.getFolder().getAbsolutePath().equalsIgnoreCase(model.getOtherModel().getFolder().getAbsolutePath())) {
            showAlert("Invalid operation", "Cannot move files to their current folder!");
        } else {
            ParameterContainer container = new ParameterContainer();
            container.model = model;
            container.sourceList = sourceList;
            container.mode = mode;
            container.targetPath = targetPath + "\\";
            copyFromSourceList(container);
        }
    }

    private static void copyFromSourceList(ParameterContainer container) {
        if (!container.sourceList.isEmpty()) {
            Map<ExplorerItemModel, File> sourceTargetMap = new TreeMap<>();

            for (ExplorerItemModel source : container.sourceList) {
                File targetFile = new File(container.targetPath + source.getFile().getName());

                if (targetFile.exists()) {
                    container.hasDuplicate = true;
                }

                container.totalSize += source.getSize();
                sourceTargetMap.put(source, targetFile);
            }

            container.sourceTargetMap = sourceTargetMap;
            confirmDuplicateAndCopy(container);
        }
    }

    private static void confirmDuplicateAndCopy(ParameterContainer container) {
        String replace = "Replace existing files";
        String duplicate = "Copy as duplicates";
        String skip = "Skip existing files";
        String cancel = "Cancel";
        String confirmResult = duplicate;
        boolean checkDifferentPath =
                !container.model.getFolder().getAbsolutePath().equalsIgnoreCase(container.model.getOtherModel().getFolder().getAbsolutePath());

        // Ask for choice if mode is copy and paths are different, for other modes always ask
        if (((checkDifferentPath && container.mode == CopyMode.COPY) || container.mode != CopyMode.COPY) && container.hasDuplicate) {
            confirmResult = Utility.showOptions("There are files in the target folder with the same name, please " +
                            "choose " +
                            "one " + "of the options below", replace, skip,
                    duplicate, cancel);
        }

        if (confirmResult != null && !confirmResult.equalsIgnoreCase(cancel)) {
            container.confirmResult = confirmResult;
            performCopy(container, skip, duplicate);
        }
    }

    private static void performCopy(ParameterContainer container, String skip, String duplicate) {
        if (container.confirmResult.equalsIgnoreCase(skip)) {
            removeExistingFiles(container);
        }

        String operationName = container.mode == CopyMode.MOVE ? "Move" : "Copy";
        container.operation = new Operation(operationName + " files to " + container.targetPath);
        postEvent(new AddOperationEvent(container.operation));
        runMasterThread(container, duplicate);
    }

    private static void removeExistingFiles(ParameterContainer container) {
        Map<ExplorerItemModel, File> temp = new HashMap<>(container.sourceTargetMap);
        container.sourceTargetMap.clear();

        for (ExplorerItemModel source : temp.keySet()) {
            File target = temp.get(source);

            if (target.exists()) {
                container.totalSize -= source.getSize();
            } else {
                container.sourceTargetMap.put(source, target);
            }
        }
    }

    private static void runMasterThread(ParameterContainer container, String duplicate) {
        runNewThread(new Task<>() {
            @Override
            protected Integer call() {
                try {
                    for (ExplorerItemModel source : container.sourceTargetMap.keySet()) {
                        while (container.operation.isPaused()) {
                            Thread.sleep(500);
                        }

                        if (container.operation.isStopped()) {
                            break;
                        } else {
                            createSuboperation(container, source, duplicate);
                        }
                    }

                    runLater(() -> {
                                try {
                                    container.operation.setComplete(true);
                                } catch (Exception e) {
                                    showError(e, "Error when running master thread", false);
                                }
                            }
                    );
                } catch (Exception e) {
                    showError(e, "Error when copying files", false);
                }

                return 1;
            }
        });
    }

    private static void runNewThread(Task<Integer> task) {
        new Thread(task).start();
    }

    private static void createSuboperation(ParameterContainer container, ExplorerItemModel source, String duplicate) throws InterruptedException {
        runLater(() -> {
            try {
                String name = container.mode == CopyMode.MOVE ? "Moving" : "Copying";
                container.operation.setSuboperationName(name + " " + source.getFile().getAbsolutePath());
            } catch (Exception e) {
                showError(e, "Error when changing sub operation name", false);
            }
        });

        checkAndUpdateSourceTargetMap(container, source, duplicate);
        runFileThread(container, source);
    }

    private static void checkAndUpdateSourceTargetMap(ParameterContainer container, ExplorerItemModel source,
                                                      String duplicate) {
        if (container.sourceTargetMap.get(source).exists()) {
            if (container.confirmResult.equalsIgnoreCase(duplicate)) {
                // update target name to prevent overwriting existing file
                updateSourceTargetMap(container, source);
            }
        }
    }

    private static void updateSourceTargetMap(ParameterContainer container, ExplorerItemModel source) {
        int index = 2;

        // add suffix to target name until there's no more duplicate
        do {
            File target =
                    new File(container.targetPath + source.getName() + " (" + index + ")." + source.getExtension());
            container.sourceTargetMap.put(source, target);
            ++index;
        } while (container.sourceTargetMap.get(source).exists());
    }

    private static void runFileThread(ParameterContainer container, ExplorerItemModel source) throws InterruptedException {
        DoubleProperty progress = new SimpleDoubleProperty();
        BooleanProperty error = new SimpleBooleanProperty();
        File target = container.sourceTargetMap.get(source);

        runNewThread(new Task<>() {
            @Override
            protected Integer call() {
                try {
                    moveOrCopy(container, source, progress);
                } catch (Exception e) {
                    error.set(true);

                    showError(e,
                            "Error when copying " + source.getFile().getAbsolutePath() + " to " + target.getAbsolutePath()
                            , false);
                }

                return 1;
            }
        });

        monitorFileProgress(container, progress, error, source, target);
    }

    private static void moveOrCopy(ParameterContainer container, ExplorerItemModel source, DoubleProperty progress) throws IOException, InterruptedException {
        File sourceFile = source.getFile();

        if (container.mode == CopyMode.MOVE) {
            FileLogic.move(sourceFile, container.sourceTargetMap.get(source), container.operation, progress);
        } else {
            FileLogic.copy(sourceFile, container.sourceTargetMap.get(source), container.operation, progress);
        }
    }

    private static void monitorFileProgress(ParameterContainer container, DoubleProperty progress,
                                            BooleanProperty error,
                                            ExplorerItemModel source, File target) throws InterruptedException {
        double currentPercentageDone = container.operation.getPercentageDone();

        // calculate the amount of contribution this file has to the total size of the operation
        double contribution = source.getSize() / container.totalSize;

        while (progress.get() < 1) {
            setPercentageDone(container, currentPercentageDone + contribution * progress.get());
            sleep(500);
        }

        finishFileProgress(container, error, target, currentPercentageDone, contribution);
    }

    private static void setPercentageDone(ParameterContainer container, double percent) {
        runLater(() -> {
            try {
                container.operation.setPercentageDone(percent);
            } catch (Exception e) {
                showError(e, "Error updating operation progress", false);
            }
        });
    }

    private static void finishFileProgress(ParameterContainer container, BooleanProperty error, File
            target, double currentPercentageDone, double contribution) {
        setPercentageDone(container, currentPercentageDone + contribution);

        runLater(() -> {
            try {
                // reload source folder if operation is move
                if (container.mode == CopyMode.MOVE && !container.operation.isStopped() && !error.get()) {
                    postEvent(new ReloadEvent(container.model.getFolder().getAbsolutePath()));
                }

                // reload target folder each time a file process is finished
                postEvent(new ReloadEvent(target.getParent()));
            } catch (Exception e) {
                showError(e, "Error updating finished progress", false);
            }
        });
    }

    public static void copyToClipboard(List<ExplorerItemModel> sourceList) {
        List<File> fileList = new LinkedList<>();

        for (ExplorerItemModel item : sourceList) {
            fileList.add(item.getFile());
        }

        ClipboardLogic.copyToClipboard(fileList);
    }

    public static void paste(ExplorerModel model) {
        List<File> fileList = ClipboardLogic.getFiles();

        if (fileList != null && !fileList.isEmpty()) {
            writeInfoLog("Pasting files from clipboard");
            List<ExplorerItemModel> sourceList = new LinkedList<>();

            for (File source : fileList) {
                ExplorerItemModel item = new ExplorerItemModel(source);
                sourceList.add(item);
            }

            copy(model, model.getFolder().getAbsolutePath(), sourceList,
                    OperationHandler.CopyMode.PASTE);
        }
    }

    public static void delete(ExplorerModel model, List<ExplorerItemModel> sourceList) {
        if (showConfirmation("Are you sure you want to delete selected files (" + sourceList.size() + ")")) {
            writeInfoLog("Deleting files");

            for (ExplorerItemModel item : sourceList) {
                FileLogic.delete(item.getFile());
            }

            // reload after deleting
            postEvent(new ReloadEvent(model.getFolder().getAbsolutePath()));
        }
    }

    /**
     * Convenient class that contains parameters used by copy and move operation
     */
    private static class ParameterContainer {
        private ExplorerModel model;

        private List<ExplorerItemModel> sourceList;

        private CopyMode mode;

        private Map<ExplorerItemModel, File> sourceTargetMap;

        private Operation operation;

        private String targetPath;
        private String confirmResult;

        private boolean hasDuplicate;

        private double totalSize;
    }
}
