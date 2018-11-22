package xyz.gnas.elif.app.controllers.Explorer;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.dialog.SingleRenameEvent;
import xyz.gnas.elif.app.events.explorer.ChangePathEvent;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.explorer.LoadDriveEvent;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.events.explorer.SwitchTabEvent;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;
import xyz.gnas.elif.core.logic.ClipboardLogic;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ExplorerController {
    @FXML
    private AnchorPane acpMain;

    @FXML
    private ComboBox<File> cboDrive;

    @FXML
    private Button btnBack;

    @FXML
    private Button btnRoot;

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
    private Label lblRunOrGoTo;

    @FXML
    private Label lblCopyToOther;

    @FXML
    private Label lblCopyToClipboard;

    @FXML
    private Label lblMove;

    @FXML
    private Label lblDelete;

    @FXML
    private Label lblRename;

    @FXML
    private MaterialIconView mivName;

    @FXML
    private MaterialIconView mivExtension;

    @FXML
    private MaterialIconView mivSize;

    @FXML
    private MaterialIconView mivDate;

    @FXML
    private MaterialIconView mivRunOrGoTo;

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
    private ContextMenu ctmTable;

    @FXML
    private CustomMenuItem cmiRunOrGoto;

    @FXML
    private CustomMenuItem cmiCopyToOther;

    @FXML
    private CustomMenuItem cmiRename;

    @FXML
    private SeparatorMenuItem smiRunOrGoTo;

    @FXML
    private SeparatorMenuItem smiDelete;

    @FXML
    private CustomMenuItem cmiPaste;

    private final int MIN_TABLE_CONTEXT_MENU_ITEM_WIDTH = 150;

    private ExplorerModel model;

    /**
     * Flag to tell the current sorting columns
     */
    private Label currentSortLabel;

    /**
     * Flag to tell current sorting order
     */
    private BooleanProperty isDescending = new SimpleBooleanProperty();

    private List<ExplorerItemModel> selectedFolderList = new ArrayList<>();
    private List<ExplorerItemModel> selectedFileList = new ArrayList<>();

    @Subscribe
    public void onInitialiseExplorerEvent(InitialiseExplorerEvent event) {
        try {
            if (model == null) {
                model = event.getModel();

                // add right padding if model is left, left padding if model is right
                acpMain.setPadding(event.isLeft() ? new Insets(0, 5, 0, 0) : new Insets(0, 0, 0, 5));
                loadDrives();
                updateOperationTargets();
            }
        } catch (Exception e) {
            showError(e, "Could not initialise explorer", true);
        }
    }

    private void loadDrives() {
        ObservableList<File> driveList = cboDrive.getItems();
        driveList.clear();

        // for each pathname in pathname array
        for (File root : File.listRoots()) {
            driveList.add(root);
        }

        selectInitialDrive();

        // add listener to drive combo box after initialising
        addListenerToDriveComboBox();
    }

    private void selectInitialDrive() {
        if (model.getFolder() == null) {
            // select first drive by default
            if (!cboDrive.getItems().isEmpty()) {
                cboDrive.getSelectionModel().select(0);
            }
        } else {
            selectDriveFromSetting();
        }
    }

    private void selectDriveFromSetting() {
        ObservableList<File> driveList = cboDrive.getItems();
        // select drive that is chosen by the current path
        // get the drive path of the current path
        String driveOfPath = getRootPath();
        boolean validPath = false;

        for (int i = 0; i < driveList.size(); ++i) {
            if (driveOfPath.equalsIgnoreCase(driveList.get(i).getAbsolutePath())) {
                cboDrive.getSelectionModel().select(i);
                changeCurrentPath(model.getFolder());
                validPath = true;
                break;
            }
        }

        if (!validPath) {
            cboDrive.getSelectionModel().select(0);
        }
    }

    private String getRootPath() {
        String path = model.getFolder().getAbsolutePath();
        return path.substring(0, path.indexOf("\\") + 1);
    }

    private void addListenerToDriveComboBox() {
        cboDrive.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends File> observable, File oldValue, File newValue) -> {
                    try {
                        changeCurrentPath(newValue);
                    } catch (Exception e) {
                        showError(e, "Error handling drive selection", false);
                    }
                });
    }

    private void updateOperationTargets() {
        File path = model.getOtherModel().getFolder();
        cmiCopyToOther.setDisable(path == null);

        if (path != null) {
            lblCopyToOther.setText("Copy to " + path.getAbsolutePath());
            lblMove.setText("Move to " + path.getAbsolutePath());
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
            if (event.getPath().equalsIgnoreCase(model.getFolder().getAbsolutePath())) {
                updateItemList();
            }
        } catch (Exception e) {
            showError(e, "Error handling reload  event", false);
        }
    }

    @Subscribe
    public void onSwitchTabEvent(SwitchTabEvent event) {
        try {
            if (event.getModel() != model) {
                tbvTable.requestFocus();
            }
        } catch (Exception e) {
            showError(e, "Error handling switch tab event", false);
        }
    }

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        Utility.writeInfoLog(getClass(), log);
    }

    private void postEvent(Object object) {
        EventBus.getDefault().post(object);
    }

    @FXML
    private void initialize() {
        try {
            EventBus.getDefault().register(this);
            initialiseDriveComboBox();
            initialiseSortImages();
            initialiseTable();

            ctmTable.setOnShowing(l -> {
                cmiPaste.setVisible(ClipboardLogic.clipboardHasFiles());
            });
        } catch (Exception e) {
            showError(e, "Could not initialise explorer", true);
        }
    }

    private void initialiseDriveComboBox() {
        cboDrive.setButtonCell(getListCell());
        cboDrive.setCellFactory(f -> getListCell());
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
                        postEvent(new LoadDriveEvent(item));
                    }
                } catch (Exception e) {
                    showError(e, "Error displaying drives", false);
                }
            }
        };
    }

    /**
     * Change current path and generate change path event
     */
    private void changeCurrentPath(File path) {
        String absolutePath = path.getAbsolutePath();
        writeInfoLog("Navigating to " + absolutePath);
        model.setFolder(path);
        lblFolderPath.setText(absolutePath);
        boolean isRoot = path.getParentFile() == null;
        btnBack.setDisable(isRoot);
        btnRoot.setDisable(isRoot);
        updateItemList();
        postEvent(new ChangePathEvent(model));
    }

    private void initialiseSortImages() {
        hideSortImages();
        mivName.setVisible(true);

        isDescending.addListener(l -> {
            try {
                String glyph = isDescending.get() ? Configurations.DESCENDING : Configurations.ASCENDING;
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

        File[] children = model.getFolder().listFiles();

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
        // sort by name initially
        currentSortLabel = lblName;
        initialiseTableContextMenu();
        tbvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setDoubleClickHandler();
        initialiseColumn(tbcName, lblName, Column.Name);
        initialiseColumn(tbcExtension, lblExtension, Column.Extension);
        initialiseColumn(tbcSize, lblSize, Column.Size);
        initialiseColumn(tbcDate, lblDate, Column.Date);
    }

    private void initialiseTableContextMenu() {
        smiRunOrGoTo.visibleProperty().bind(cmiRunOrGoto.visibleProperty());
        smiDelete.visibleProperty().bind(cmiRename.visibleProperty());
        lblRunOrGoTo.setMinWidth(MIN_TABLE_CONTEXT_MENU_ITEM_WIDTH);

        lblRunOrGoTo.widthProperty().addListener(l -> {
            // resize label to minimum width if it's less than minimum
            if (lblRunOrGoTo.getWidth() < MIN_TABLE_CONTEXT_MENU_ITEM_WIDTH) {
                lblRunOrGoTo.autosize();
            }
        });

        lblCopyToOther.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblCopyToClipboard.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblMove.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblDelete.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblRename.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        addListenerToSelectedItems();
    }

    private void addListenerToSelectedItems() {
        tbvTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ExplorerItemModel>) l -> {
            try {
                updateSelectedFoldersAndFiles();
                cmiRunOrGoto.setVisible(true);

                // if only files are selected
                if (selectedFolderList.isEmpty()) {
                    if (!selectedFileList.isEmpty()) {
                        setRunContextMenuItem();

                        // show "rename" if only 1 file is selected
                        cmiRename.setVisible(selectedFileList.size() == 1);
                    }
                } else {
                    // hide "run or go to" if there are both files and folder or multiple folders are selected
                    if (!selectedFileList.isEmpty() || selectedFolderList.size() > 1) {
                        cmiRunOrGoto.setVisible(false);
                        cmiRename.setVisible(false);
                    } else {
                        setGoToContextMenuItem();

                        // show rename if only 1 folder is selected
                        cmiRename.setVisible(true);
                    }
                }
            } catch (Exception e) {
                showError(e, "Error when handling change to item selection", false);
            }
        });
    }

    private void updateSelectedFoldersAndFiles() {
        selectedFolderList.clear();
        selectedFileList.clear();
        ObservableList<ExplorerItemModel> itemList = tbvTable.getSelectionModel().getSelectedItems();

        for (ExplorerItemModel item : itemList) {
            if (item.getFile().isDirectory()) {
                selectedFolderList.add(item);
            } else {
                selectedFileList.add(item);
            }
        }
    }

    private void setRunContextMenuItem() {
        mivRunOrGoTo.setGlyphName("POWER_SETTINGS_NEW");
        String run = "Run ";
        int size = selectedFileList.size();

        if (size > 1) {
            run += size + " selected files";
        } else {
            run += selectedFileList.get(0).getFile().getName();
        }

        lblRunOrGoTo.setText(run);
    }

    private void setGoToContextMenuItem() {
        mivRunOrGoTo.setGlyphName("OPEN_IN_BROWSER");
        ExplorerItemModel item = selectedFolderList.get(0);
        lblRunOrGoTo.setText("Go to " + item.getFile().getAbsolutePath());
    }

    private void setDoubleClickHandler() {
        tbvTable.setRowFactory(f -> {
            TableRow<ExplorerItemModel> row = new TableRow<ExplorerItemModel>();

            row.setOnMouseClicked(event -> {
                try {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && (!row.isEmpty())) {
                        runOrGoToByItem(row.getItem());
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
    private void runOrGoToByItem(ExplorerItemModel item) {
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
    private void initialiseColumn(TableColumn<ExplorerItemModel, ExplorerItemModel> tbc, Label lbl, Column
            column) {
        tbc.setCellValueFactory(new ExplorerTableCellValue());
        tbc.setCellFactory(new ExplorerTableCellCallback(column));

        tbc.widthProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue,
                                         Number newValue) -> {
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
            File parent = model.getFolder().getParentFile();

            if (parent != null) {
                changeCurrentPath(parent);
            }
        } catch (Exception e) {
            showError(e, "Could not navigate back", false);
        }
    }

    @FXML
    private void goToRoot(ActionEvent event) {
        try {
            changeCurrentPath(new File(getRootPath()));
        } catch (Exception e) {
            showError(e, "Could not go to root", false);
        }
    }

    @FXML
    private void reload(ActionEvent event) {
        try {
            updateItemList();
        } catch (Exception e) {
            showError(e, "Could not reload", false);
        }
    }

    @FXML
    private void scrollToTop(ActionEvent event) {
        try {
            writeInfoLog("Scrolling to top");
            tbvTable.scrollTo(0);
        } catch (Exception e) {
            showError(e, "Could not scroll to top", false);
        }
    }

    @FXML
    private void scrollToBottom(ActionEvent event) {
        try {
            writeInfoLog("Scrolling to bottom");
            tbvTable.scrollTo(tbvTable.getItems().size() - 1);
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
    private void runOrGoTo(ActionEvent event) {
        try {
            // navigate into top folder if no files are selected
            if (selectedFileList.isEmpty()) {
                runOrGoToByItem(selectedFolderList.get(0));
            } else { // otherwise run the files
                for (ExplorerItemModel item : selectedFileList) {
                    runOrGoToByItem(item);
                }
            }
        } catch (Exception e) {
            showError(e, "Could not run or go to selection", false);
        }
    }

    @FXML
    private void copyToOther(ActionEvent event) {
        try {
            OperationHandler.copy(model, model.getOtherModel().getFolder().getAbsolutePath(),
                    tbvTable.getSelectionModel().getSelectedItems(), OperationHandler.CopyMode.COPY);
        } catch (Exception e) {
            showError(e, "Could not copy to other tab", false);
        }
    }

    @FXML
    private void copyToClipboard(ActionEvent event) {
        try {
            OperationHandler.copyToClipboard(tbvTable.getSelectionModel().getSelectedItems());
        } catch (Exception e) {
            showError(e, "Could not copy to clipboard", false);
        }
    }

    @FXML
    private void paste(ActionEvent event) {
        try {
            OperationHandler.paste(model);
        } catch (Exception e) {
            showError(e, "Could not paste", false);
        }
    }

    @FXML
    private void move(ActionEvent event) {
        try {
            OperationHandler.copy(model, model.getOtherModel().getFolder().getAbsolutePath(),
                    tbvTable.getSelectionModel().getSelectedItems(), OperationHandler.CopyMode.MOVE);
        } catch (Exception e) {
            showError(e, "Could not copy to clipboard", false);
        }
    }

    @FXML
    private void delete(ActionEvent event) {
        try {
            OperationHandler.delete(model, tbvTable.getSelectionModel().getSelectedItems());
        } catch (Exception e) {
            showError(e, "Could not delete", false);
        }
    }

    @FXML
    private void rename(ActionEvent event) {
        try {
            postEvent(new SingleRenameEvent(tbvTable.getSelectionModel().getSelectedItem()));
        } catch (Exception e) {
            showError(e, "Could not rename", false);
        }
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
        if (event.isControlDown()) {
            handleControlModifierEvent(event);
        } else {
            handleNoModifierEvent(event);
        }
    }

    private void handleControlModifierEvent(KeyEvent event) {
        switch (event.getCode()) {
            case H:
                goToRoot(null);
                break;

            case R:
                reload(null);
                break;

            case C:
                copyToClipboard(null);
                break;

            case V:
                paste(null);
                break;

            default:
                break;
        }
    }

    private void handleNoModifierEvent(KeyEvent event) {
        switch (event.getCode()) {
            case TAB:
                postEvent(new SwitchTabEvent(model));
                break;

            case ENTER:
                // only call run or go to if list of selected files is not empty or only 1 folder is selected
                if (!selectedFileList.isEmpty() || selectedFolderList.size() == 1) {
                    runOrGoTo(null);
                }

                break;

            case BACK_SPACE:
                back(null);
                break;

            case F5:
                copyToOther(null);
                break;

            case F6:
                move(null);
                break;

            case DELETE:
                delete(null);
                break;

            case F2:
                rename(null);
                break;

            default:
                break;
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