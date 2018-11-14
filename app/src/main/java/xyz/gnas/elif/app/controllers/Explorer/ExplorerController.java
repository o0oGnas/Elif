package xyz.gnas.elif.app.controllers.Explorer;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.explorer.ChangePathEvent;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.explorer.LoadDriveEvent;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.events.operation.AddOperationEvent;
import xyz.gnas.elif.app.models.Operation;
import xyz.gnas.elif.core.FileLogic;
import xyz.gnas.elif.core.models.Progress;
import xyz.gnas.elif.core.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.core.models.explorer.ExplorerModel;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Thread.sleep;
import static javafx.application.Platform.runLater;

public class ExplorerController {
    @FXML
    private ComboBox<File> cboDrive;

    @FXML
    private Button btnBack;

    @FXML
    private Label lblFolderPath;

    @FXML
    private Label lblName;

    @FXML
    private Label lblExtension;

    @FXML
    private Label lblSize;

    @FXML
    private Label lblDate;

    @FXML
    private Label lblCopyToOther;

    @FXML
    private MaterialIconView mivName;

    @FXML
    private MaterialIconView mivExtension;

    @FXML
    private MaterialIconView mivSize;

    @FXML
    private MaterialIconView mivDate;

    @FXML
    private TableView<ExplorerItemModel> tbvTable;

    @FXML
    private TableColumn<ExplorerItemModel, ExplorerItemModel> tbcName;

    @FXML
    private TableColumn<ExplorerItemModel, ExplorerItemModel> tbcExtension;

    @FXML
    private TableColumn<ExplorerItemModel, ExplorerItemModel> tbcSize;

    @FXML
    private TableColumn<ExplorerItemModel, ExplorerItemModel> tbcDate;

    @FXML
    private CustomMenuItem cmiCopyToOther;

    private ExplorerModel model;

    /**
     * Flag to tell the current sorting columns
     */
    private Label currentSortLabel;

    /**
     * Flag to tell current sorting order
     */
    private BooleanProperty isDescending = new SimpleBooleanProperty();

    /**
     * Flag to tell if user is editing a file
     */
    private boolean isEditing;

    @Subscribe
    public void onInitialiseExplorerEvent(InitialiseExplorerEvent event) throws IOException {
        try {
            if (model == null) {
                model = event.getModel();

                ObservableList<File> driveList = cboDrive.getItems();
                driveList.clear();

                // for each pathname in pathname array
                for (File root : File.listRoots()) {
                    driveList.add(root);
                }

                selectInitialDrive();
                updateOperationTargets();
            }
        } catch (Exception e) {
            showError(e, "Could not initialise explorer", true);
        }
    }

    private void selectInitialDrive() {
        ObservableList<File> driveList = cboDrive.getItems();

        // select first drive by default
        File path = model.getPath();

        if (path == null) {
            if (!driveList.isEmpty()) {
                cboDrive.getSelectionModel().select(0);
            }
        } else { // select drive that is chosen by the current path
            // get the drive path of the current path
            String driveOfPath = path.getAbsolutePath().substring(0, path.getAbsolutePath().indexOf("\\") + 1);
            boolean validPath = false;

            for (int i = 0; i < driveList.size(); ++i) {
                if (driveOfPath.equalsIgnoreCase(driveList.get(i).getAbsolutePath())) {
                    cboDrive.getSelectionModel().select(i);
                    changeCurrentPath(path);
                    validPath = true;
                    break;
                }
            }

            if (!validPath) {
                cboDrive.getSelectionModel().select(0);
            }
        }
    }

    @Subscribe
    public void onChangePathEvent(ChangePathEvent event) {
        try {
            ExplorerModel eventModel = event.getModel();

            if (model != eventModel) {
                updateOperationTargets();
            }
        } catch (Exception e) {
            showError(e, "Error handling change path event", false);
        }
    }

    @Subscribe
    public void onReloadEvent(ReloadEvent event) {
        try {
            if (event.getPath().equalsIgnoreCase(model.getPath().getAbsolutePath())) {
                updateItemList();
            }
        } catch (Exception e) {
            showError(e, "Error handling reload  event", false);
        }
    }

    private void updateOperationTargets() {
        File path = model.getOtherModel().getPath();
        cmiCopyToOther.setDisable(path == null);

        if (path != null) {
            lblCopyToOther.setText("Copy to " + path.getAbsolutePath());
        }
    }

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        Utility.writeInfoLog(getClass(), log);
    }

    @FXML
    private void initialize() {
        try {
            EventBus.getDefault().register(this);
            currentSortLabel = lblName;
            initialiseDriveComboBox();
            initialiseSortImages();
            initialiseTable();
        } catch (Exception e) {
            showError(e, "Could not initialise explorer", true);
        }
    }

    private void initialiseDriveComboBox() {
        cboDrive.setButtonCell(getListCell());

        cboDrive.setCellFactory(f -> {
            return getListCell();
        });

        addHandlerToDriveComboBox();
    }

    /**
     * Wrapper method for displaying both lists of drives and selected
     * drive
     */
    private ListCell<File> getListCell() {
        return new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                try {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setGraphic(null);
                    } else {
                        FXMLLoader loader = new FXMLLoader(ResourceManager.getDriveItemFXML());
                        setGraphic(loader.load());
                        EventBus.getDefault().post(new LoadDriveEvent(item));
                    }
                } catch (Exception e) {
                    showError(e, "Error displaying drives", false);
                }
            }
        };
    }

    private void addHandlerToDriveComboBox() {
        cboDrive.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends File> observable, File oldValue, File newValue) -> {
                    try {
                        changeCurrentPath(newValue);
                    } catch (Exception e) {
                        showError(e, "Error handling drive selection", false);
                    }
                });
    }

    /**
     * Change current path and generate change path event
     */
    private void changeCurrentPath(File path) {
        model.setPath(path);
        String absolutePath = path.getAbsolutePath();
        writeInfoLog("Navigating to " + absolutePath);
        lblFolderPath.setText(absolutePath);
        btnBack.setDisable(path.getParentFile() == null);
        updateItemList();
        EventBus.getDefault().post(new ChangePathEvent(model));
    }

    private void initialiseSortImages() {
        hideSortImages();
        mivName.setVisible(true);

        isDescending
                .addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    try {
                        String glyph = newValue ? Configurations.DESCENDING : Configurations.ASCENDING;
                        mivName.setGlyphName(glyph);
                        mivExtension.setGlyphName(glyph);
                        mivSize.setGlyphName(glyph);
                        mivDate.setGlyphName(glyph);
                    } catch (Exception e) {
                        showError(e, "Error when handling change to sort order", false);
                    }
                });
    }

    private void hideSortImages() {
        mivName.setVisible(false);
        mivExtension.setVisible(false);
        mivSize.setVisible(false);
        mivDate.setVisible(false);
    }

    private void updateItemList() {
        writeInfoLog("Updating item list");
        ObservableList<ExplorerItemModel> itemList = tbvTable.getItems();
        itemList.clear();

        File[] children = model.getPath().listFiles();

        if (children != null) {
            for (File child : children) {
                itemList.add(new ExplorerItemModel(child));
            }

            sortItemList();
        }

        tbvTable.refresh();
        scrollToTop(null);
    }

    private void sortItemList() {
        String sortOrder = isDescending.get() ? "Descending" : "Ascending";
        writeInfoLog("Sorting list by " + currentSortLabel.getText() + " - " + sortOrder);

        tbvTable.getItems().sort((ExplorerItemModel o1, ExplorerItemModel o2) -> {
            boolean descending = isDescending.get();
            boolean IsDirectory1 = o1.getFile().isDirectory();

            // only sort if both are files or folders, otherwise folder comes first
            if (IsDirectory1 == o2.getFile().isDirectory()) {
                if (currentSortLabel == lblName) {
                    return getSortResult(descending, o1.getName(), o2.getName());
                } else if (currentSortLabel == lblExtension) {
                    return getSortResult(descending, o1.getExtension(), o2.getExtension());
                } else if (currentSortLabel == lblSize) {
                    return getSortResult(descending, o1.getSize(), o2.getSize());
                } else {
                    return getSortResult(descending, o1.getDate(), o2.getDate());
                }
            } else {
                return IsDirectory1 ? -1 : 1;
            }
        });
    }

    /**
     * Wrapper to return the result of comparison
     */
    private int getSortResult(boolean descending, Comparable o1, Comparable o2) {
        return descending ? o2.compareTo(o1) : o1.compareTo(o2);
    }

    private void initialiseTable() {
        tbvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setDoubleClickHandler();
        initialiseColumn(tbcName, lblName, Column.Name);
        initialiseColumn(tbcExtension, lblExtension, Column.Extension);
        initialiseColumn(tbcSize, lblSize, Column.Size);
        initialiseColumn(tbcDate, lblDate, Column.Date);
    }

    private void setDoubleClickHandler() {
        tbvTable.setRowFactory(f -> {
            TableRow<ExplorerItemModel> row = new TableRow<ExplorerItemModel>();

            row.setOnMouseClicked(event -> {
                try {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        navigateOrRun(row.getItem());
                    }
                } catch (Exception e) {
                    showError(e, "Error handling double click", false);
                }
            });

            return row;
        });
    }

    /**
     * Navigate into selected folder or run if it's a file
     */
    private void navigateOrRun(ExplorerItemModel item) {
        File file = item.getFile();

        // navigate if it's a folder, run if a file
        if (file.isDirectory()) {
            changeCurrentPath(file);
        } else {
            String path = file.getAbsolutePath();

            try {
                writeInfoLog("Running file " + path);
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                showError(e, "Error opening file " + path, false);
            }
        }
    }

    /**
     * Wrapper to reduce copy paste
     */
    private void initialiseColumn(TableColumn<ExplorerItemModel, ExplorerItemModel> tbc, Label lbl, Column column) {
        tbc.setCellValueFactory(new ExplorerTableCellValue());
        tbc.setCellFactory(new ExplorerTableCellCallback(column));

        tbc.widthProperty()
                .addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    try {
                        lbl.setPrefWidth(newValue.doubleValue());
                    } catch (Exception e) {
                        showError(e, "Error when listening to changes to column width", false);
                    }
                });
    }

    @FXML
    private void back(ActionEvent event) {
        try {
            File parent = model.getPath().getParentFile();

            if (parent != null) {
                changeCurrentPath(parent);
            }
        } catch (Exception e) {
            showError(e, "Could not navigate back", false);
        }
    }

    @FXML
    private void scrollToTop(ActionEvent event) {
        try {
            writeInfoLog("Scrolling to top");
            ScrollBar verticalBar = getVerticalBar();

            if (verticalBar != null) {
                verticalBar.setValue(verticalBar.getMin());
            }
        } catch (Exception e) {
            showError(e, "Could not scroll to top", false);
        }
    }

    private ScrollBar getVerticalBar() {
        return (ScrollBar) tbvTable.lookup(".scroll-bar:vertical");
    }

    @FXML
    private void scrollToBottom(ActionEvent event) {
        try {
            writeInfoLog("Scrolling to bottom");
            ScrollBar verticalBar = getVerticalBar();

            if (verticalBar != null) {
                verticalBar.setValue(verticalBar.getMax());
            }
        } catch (Exception e) {
            showError(e, "Could not scroll to bottom", false);
        }
    }

    @FXML
    private void sort(MouseEvent event) {
        try {
            hideSortImages();
            Label previousLabel = currentSortLabel;
            currentSortLabel = (Label) event.getSource();

            // switch between ascending/descending if user clicks again on the same column,
            // otherwise set order to ascending
            if (previousLabel == currentSortLabel) {
                isDescending.set(!isDescending.get());
            } else {
                isDescending.set(false);
            }

            showSortImage();
            sortItemList();
        } catch (Exception e) {
            showError(e, "Could not sort items", false);
        }
    }

    private void showSortImage() {
        if (currentSortLabel == lblName) {
            mivName.setVisible(true);
        } else if (currentSortLabel == lblExtension) {
            mivExtension.setVisible(true);
        } else if (currentSortLabel == lblSize) {
            mivSize.setVisible(true);
        } else {
            mivDate.setVisible(true);
        }
    }

    @FXML
    private void copyToOther(MouseEvent event) {
        try {
            String targetPath = model.getOtherModel().getPath().getAbsolutePath() + "\\";
            List<ExplorerItemModel> sourceList = tbvTable.getSelectionModel().getSelectedItems();
            boolean hasDuplicate = false;
            Map<ExplorerItemModel, File> sourceTargetMap = new TreeMap<>();
            double totalSize = 0;

            for (ExplorerItemModel source : sourceList) {
                File targetFile = new File(targetPath + source.getFile().getName());

                if (targetFile.exists()) {
                    hasDuplicate = true;
                }

                totalSize += source.getSize();
                sourceTargetMap.put(source, targetFile);
            }

            performCopyToOther(sourceTargetMap, hasDuplicate, targetPath, totalSize);
        } catch (Exception e) {
            showError(e, "Could not copy to other tab", false);
        }
    }

    private void performCopyToOther(Map<ExplorerItemModel, File> sourceTargetMap, boolean hasDuplicate,
                                    String targetPath, double totalSize) throws IOException {
        String replace = "Replace existing files";
        String duplicate = "Copy as duplicates";
        String skip = "Skip existing files";
        String cancel = "Cancel";
        String result = replace;

        if (hasDuplicate) {
            result = Utility.showOptions("There are files in the target folder with the same name", replace, skip,
                    duplicate, cancel);
        }

        if (result != null && !result.equalsIgnoreCase(cancel)) {
            if (result.equalsIgnoreCase(skip)) {
                totalSize = removeExistingFiles(sourceTargetMap, totalSize);
            }

            Operation operation = new Operation("Copy files to " + targetPath);
            EventBus.getDefault().post(new AddOperationEvent(operation));
            runCopyMasterThread(operation, sourceTargetMap, targetPath, result, duplicate, totalSize);
        }
    }

    private double removeExistingFiles(Map<ExplorerItemModel, File> sourceTargetMap, double totalSize) {
        Map<ExplorerItemModel, File> temp = new HashMap<>(sourceTargetMap);
        sourceTargetMap.clear();

        for (ExplorerItemModel source : temp.keySet()) {
            File target = temp.get(source);

            if (target.exists()) {
                totalSize -= source.getSize();
            } else {
                sourceTargetMap.put(source, target);
            }
        }

        return totalSize;
    }

    private void runCopyMasterThread(Operation operation, Map<ExplorerItemModel, File> sourceTargetMap,
                                     String targetPath, String result,
                                     String duplicate, double totalSize) {
        runNewThread(new Task<>() {
            @Override
            protected Integer call() {
                try {
                    for (ExplorerItemModel source : sourceTargetMap.keySet()) {
                        runLater(() -> {
                            operation.setSuboperationName("Copying " + source.getFile().getAbsolutePath());
                        });

                        checkAndUpdateSourceTargetMap(sourceTargetMap, source, targetPath, result, duplicate);
                        runCopyFileThread(operation, sourceTargetMap, source, totalSize);
                    }

                    runLater(() -> {
                        operation.setIsComplete(true);
                    });
                } catch (Exception e) {
                    showError(e, "Error when copying files", false);
                }

                return 1;
            }
        });
    }

    private void runNewThread(Task<Integer> task) {
        new Thread(task).start();
    }

    private void checkAndUpdateSourceTargetMap(Map<ExplorerItemModel, File> sourceTargetMap, ExplorerItemModel source
            , String targetPath, String result, String duplicate) {
        if (sourceTargetMap.get(source).exists()) {
            if (result.equalsIgnoreCase(duplicate)) {
                // update target name to prevent overwriting existing file
                updateSourceTargetMap(sourceTargetMap, targetPath, source);
            }
        }
    }

    private void updateSourceTargetMap(Map<ExplorerItemModel, File> sourceTargetMap, String targetPath,
                                       ExplorerItemModel source) {
        int index = 2;

        // add suffix to target name until there's no more duplicate
        do {
            File target = new File(targetPath + source.getName() + " (" + index + ")." + source.getExtension());
            sourceTargetMap.put(source, target);
            ++index;
        } while (sourceTargetMap.get(source).exists());
    }

    private void runCopyFileThread(Operation operation, Map<ExplorerItemModel, File> sourceTargetMap,
                                   ExplorerItemModel source, double totalSize) throws InterruptedException {
        Progress progress = new Progress();
        operation.getProgressList().add(progress);
        File target = sourceTargetMap.get(source);

        runNewThread(new Task<>() {
            @Override
            protected Integer call() {
                File sourceFile = source.getFile();

                try {
                    FileLogic.copyToPath(sourceFile, target, progress);
                } catch (Exception e) {
                    showError(e,
                            "Error when copying " + sourceFile.getAbsolutePath() + " to " + target.getAbsolutePath()
                            , false);
                }

                return 1;
            }
        });

        monitorFileCopyProgress(operation, progress, source, target, totalSize);
    }

    private void monitorFileCopyProgress(Operation operation, Progress progress, ExplorerItemModel source, File target,
                                         double totalSize) throws InterruptedException {
        double currentPercentageDone = operation.getPercentageDone();

        // calculate the amount of contribution this file has to the total size of the operation
        double contribution = source.getSize() / totalSize;

        while (!progress.isComplete()) {
            setPercentageDone(operation, currentPercentageDone + contribution * progress.getPercentageDone());
            sleep(500);
        }

        setPercentageDone(operation, currentPercentageDone + contribution);

        runLater(() -> {
            try {
                EventBus.getDefault().post(new ReloadEvent(target.getParent()));
            } catch (Exception e) {
                showError(e, "Error updating finished progress", false);
            }
        });
    }

    private void setPercentageDone(Operation operation, double percent) {
        runLater(() -> {
            try {
                operation.setPercentageDone(percent);
            } catch (Exception e) {
                showError(e, "Error updating operation progress", false);
            }
        });
    }

    @FXML
    public void copyToClipboard(MouseEvent mouseEvent) {
    }

    @FXML
    private void tableKeyReleased(KeyEvent event) {
        try {
            handleKeyEvent(event);
        } catch (Exception e) {
            showError(e, "Could not handle key event", false);
        }
    }

    private void handleKeyEvent(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER:
                handleEnterKey();
                break;

            case BACK_SPACE:
                back(null);
                break;

            case F5:
                copyToOther(null);
                break;

            default:
                break;
        }
    }

    private void handleEnterKey() {
        List<ExplorerItemModel> folderList = new ArrayList<>();
        List<ExplorerItemModel> fileList = new ArrayList<>();
        ObservableList<ExplorerItemModel> itemList = tbvTable.getSelectionModel().getSelectedItems();

        for (ExplorerItemModel item : itemList) {
            if (item.getFile().isDirectory()) {
                folderList.add(item);
            } else {
                fileList.add(item);
            }
        }

        // navigate into top folder is no files are selected
        if (fileList.isEmpty()) {
            if (!folderList.isEmpty()) {
                navigateOrRun(folderList.get(0));
            }
        } else {
            for (ExplorerItemModel item : fileList) {
                navigateOrRun(item);
            }
        }
    }

    /**
     * Column enum, used by ExplorerTableCell to determine how to
     * display the data
     */
    private enum Column {
        Name, Extension, Size, Date
    }

    /**
     * Custom value for table columns
     */
    private class ExplorerTableCellValue implements
            Callback<TableColumn.CellDataFeatures<ExplorerItemModel, ExplorerItemModel>,
                    ObservableValue<ExplorerItemModel>> {
        @Override
        public ObservableValue<ExplorerItemModel> call(CellDataFeatures<ExplorerItemModel, ExplorerItemModel> param) {
            return new ObservableValue<>() {
                @Override
                public void removeListener(InvalidationListener listener) {
                }

                @Override
                public void addListener(InvalidationListener listener) {
                }

                @Override
                public void removeListener(ChangeListener<? super ExplorerItemModel> listener) {
                }

                @Override
                public ExplorerItemModel getValue() {
                    try {
                        return param.getValue();
                    } catch (Exception e) {
                        showError(e, "Error getting value for table cell", false);
                        return null;
                    }
                }

                @Override
                public void addListener(ChangeListener<? super ExplorerItemModel> listener) {
                }
            };
        }
    }

    /**
     * Custom cell display for table columns
     */
    private class ExplorerTableCellCallback implements
            Callback<TableColumn<ExplorerItemModel, ExplorerItemModel>, TableCell<ExplorerItemModel,
                    ExplorerItemModel>> {

        private Column column;

        public ExplorerTableCellCallback(Column column) {
            this.column = column;
        }

        @Override
        public TableCell<ExplorerItemModel, ExplorerItemModel> call(
                TableColumn<ExplorerItemModel, ExplorerItemModel> param) {
            return new ExplorerTableCell() {
            };
        }

        private class ExplorerTableCell extends TableCell<ExplorerItemModel, ExplorerItemModel> {
            @Override
            protected void updateItem(ExplorerItemModel item, boolean empty) {
                try {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        display(item);
                    }
                } catch (Exception e) {
                    showError(e, "Error when displaying item", false);
                }
            }

            private void display(ExplorerItemModel item) {
                switch (column) {
                    case Name:
                        setIcon(item);
                        setText(item.getName());
                        break;

                    case Extension:
                        setText(item.getExtension());
                        break;

                    case Size:
                        DecimalFormat format = new DecimalFormat("#,###");
                        setText(format.format(item.getSize()));
                        break;

                    case Date:
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
                        setText(dateFormat.format(item.getDate().getTime()));
                        break;

                    default:
                        break;
                }
            }

            private void setIcon(ExplorerItemModel item) {
                // show icon depending on file or folder
                File file = item.getFile();

                if (file.isDirectory()) {
                    MaterialIconView mivFolder = new MaterialIconView();
                    mivFolder.setGlyphName("FOLDER_OPEN");
                    mivFolder.setGlyphSize(16);
                    setGraphic(mivFolder);
                } else {
                    ImageView imv = new ImageView(Utility.getFileIcon(file, true));
                    setGraphic(imv);
                }
            }
        }
    }
}