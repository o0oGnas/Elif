package xyz.gnas.elif.app.controllers.explorer;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.ImageUtility;
import xyz.gnas.elif.app.common.utility.LogUtility;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.common.utility.code.RunnerWithIntReturn;
import xyz.gnas.elif.app.common.utility.window.WindowEventHandler;
import xyz.gnas.elif.app.common.utility.window.WindowEventUtility;
import xyz.gnas.elif.app.controllers.explorer.ExplorerTableCellCallback.Column;
import xyz.gnas.elif.app.events.dialog.DialogEvent.DialogType;
import xyz.gnas.elif.app.events.dialog.SingleFileDialogEvent;
import xyz.gnas.elif.app.events.explorer.ChangePathEvent;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.events.explorer.SwitchTabEvent;
import xyz.gnas.elif.app.events.operation.PerformOperationEvent;
import xyz.gnas.elif.app.events.operation.PerformOperationEvent.OperationType;
import xyz.gnas.elif.app.models.ApplicationModel;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;
import xyz.gnas.elif.core.logic.ClipboardLogic;

import javax.swing.filechooser.FileSystemView;
import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ExplorerController {
    @FXML
    private AnchorPane acpMain;

    @FXML
    private ComboBox<File> cbbDrive;

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
    private Label lblCutToClipboard;

    @FXML
    private Label lblPaste;

    @FXML
    private Label lblMove;

    @FXML
    private Label lblDelete;

    @FXML
    private Label lblSimpleRename;

    @FXML
    private Label lblAdvancedRename;

    @FXML
    private Label lblEditAsText;

    @FXML
    private Label lblNewFolder;

    @FXML
    private Label lblNewFile;

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
    private CustomMenuItem cmiSimpleRename;

    @FXML
    private CustomMenuItem cmiEditAsText;

    @FXML
    private CustomMenuItem cmiPaste;

    @FXML
    private SeparatorMenuItem smiRunOrGoTo;

    @FXML
    private SeparatorMenuItem smiEditAsText;

    private final int MIN_TABLE_CONTEXT_MENU_ITEM_WIDTH = 150;

    private ApplicationModel applicationModel = ApplicationModel.getInstance();

    private ExplorerModel explorerModel;

    private Label currentSortLabel;

    /**
     * current sorting order
     */
    private BooleanProperty isDescending = new SimpleBooleanProperty();

    private List<ExplorerItemModel> selectedFolderList = new ArrayList<>();
    private List<ExplorerItemModel> selectedFileList = new ArrayList<>();

    private void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(getClass(), errorMessage, runner);
    }

    private void executeRunnerOrExit(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunnerOrExit(getClass(), errorMessage, runner);
    }

    private int executeRunnerWithIntReturn(String errorMessage, int errorReturnValue, RunnerWithIntReturn runner) {
        return CodeRunnerUtility.executeRunnerWithIntReturn(getClass(), errorMessage, errorReturnValue, runner);
    }

    private void writeInfoLog(String log) {
        LogUtility.writeInfoLog(getClass(), log);
    }

    private void postEvent(Object object) {
        EventBus.getDefault().post(object);
    }

    @Subscribe
    public void onInitialiseExplorerEvent(InitialiseExplorerEvent event) {
        executeRunnerOrExit("Could not initialise explorer", () -> {
            if (explorerModel == null) {
                explorerModel = event.getModel();

                // add right padding if explorerModel is left, left padding if explorerModel is right
                acpMain.setPadding(event.isLeft() ? new Insets(0, 5, 0, 0) : new Insets(0, 0, 0, 5));
                loadDrives();
            }
        });
    }

    private void loadDrives() {
        ObservableList<File> driveList = cbbDrive.getItems();
        driveList.clear();
        Collections.addAll(driveList, File.listRoots());
        selectInitialDrive();
        cbbDrive.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends File> observable, File oldValue, File newValue) ->
                        executeRunner("Error when handling drive selection", () -> changeCurrentPath(newValue)));
    }

    private void selectInitialDrive() {
        if (explorerModel.getFolder() == null) {
            // select first drive by default
            if (!cbbDrive.getItems().isEmpty()) {
                cbbDrive.getSelectionModel().select(0);
            }
        } else {
            selectDriveFromSetting();
        }
    }

    private void selectDriveFromSetting() {
        ObservableList<File> driveList = cbbDrive.getItems();
        String driveOfPath = getRootPath();
        boolean validPath = false;
        SingleSelectionModel selectionModel = cbbDrive.getSelectionModel();

        for (int i = 0; i < driveList.size(); ++i) {
            if (driveOfPath.equalsIgnoreCase(driveList.get(i).getAbsolutePath())) {
                selectionModel.select(i);
                changeCurrentPath(explorerModel.getFolder());
                validPath = true;
                break;
            }
        }

        if (!validPath) {
            selectionModel.select(0);
        }
    }

    private String getRootPath() {
        String path = explorerModel.getFolder().getAbsolutePath();
        return path.substring(0, path.indexOf("\\") + 1);
    }

    /**
     * Change current path, update list and generate change path event
     */
    private void changeCurrentPath(File path) {
        String absolutePath = path.getAbsolutePath();
        writeInfoLog("Navigating to " + absolutePath);
        explorerModel.setFolder(path);
        lblFolderPath.setText(absolutePath);
        boolean isRoot = path.getParentFile() == null;
        btnBack.setDisable(isRoot);
        btnRoot.setDisable(isRoot);
        updateItemList();
        scrollToTop(null);
        postEvent(new ChangePathEvent(explorerModel));
    }

    private void updateItemList() {
        writeInfoLog("Updating item list");
        ObservableList<ExplorerItemModel> itemList = tbvTable.getItems();
        itemList.clear();
        File[] children = explorerModel.getFolder().listFiles();

        if (children != null) {
            for (File child : children) {
                itemList.add(new ExplorerItemModel(child));
            }

            sortItemList();
        }

        tbvTable.refresh();
    }

    @Subscribe
    public void onChangePathEvent(ChangePathEvent event) {
        executeRunner("Error when handling change path event", () -> {
            ExplorerModel eventModel = event.getModel();

            if (explorerModel != eventModel) {
                File otherFile = eventModel.getFolder();
                cmiCopyToOther.setDisable(otherFile == null);

                if (otherFile != null) {
                    String otherPath = "\"" + otherFile.getAbsolutePath() + "\"";
                    lblCopyToOther.setText("Copy to " + otherPath);
                    lblMove.setText("Move to " + otherPath);
                }
            }
        });
    }

    @Subscribe
    public void onReloadEvent(ReloadEvent event) {
        executeRunner("Error when handling reload event", () -> {
            if (event.getPath().equalsIgnoreCase(explorerModel.getFolder().getAbsolutePath())) {
                updateAndReselect();
            }
        });
    }

    @Subscribe
    public void onSwitchTabEvent(SwitchTabEvent event) {
        executeRunner("Error when handling switch tab event", () -> {
            if (event.getModel() != explorerModel) {
                tbvTable.requestFocus();
            }
        });
    }

    @FXML
    private void initialize() {
        executeRunnerOrExit("Could not initialise explorer", () -> {
            EventBus.getDefault().register(this);
            handleWindowFocusedEvent();
            initialiseDriveComboBox();
            initialiseSortImages();
            initialiseTable();
        });
    }

    private void handleWindowFocusedEvent() {
        WindowEventUtility.bindWindowEventHandler(getClass(), acpMain, new WindowEventHandler() {
            @Override
            public void handleShownEvent() {
            }

            @Override
            public void handleFocusedEvent() {
                executeRunner("Error when handling window focused event", () -> {
                    // only reselect for the focused tab
                    if (applicationModel.getSelectedExplorerModel() == explorerModel) {
                        updateAndReselect();
                    } else {
                        updateItemList();
                    }
                });
            }

            @Override
            public void handleCloseEvent(WindowEvent windowEvent) {
            }
        });
    }

    private void updateAndReselect() {
        TableViewSelectionModel<ExplorerItemModel> selectionModel = tbvTable.getSelectionModel();
        List<ExplorerItemModel> oldSelectedList = new LinkedList<>(selectionModel.getSelectedItems());
        updateItemList();

        for (ExplorerItemModel selectedItem : oldSelectedList) {
            for (ExplorerItemModel item : tbvTable.getItems()) {
                if (selectedItem.getFile().getAbsolutePath().equalsIgnoreCase(item.getFile().getAbsolutePath())) {
                    selectionModel.select(item);
                }
            }
        }
    }

    private void initialiseDriveComboBox() {
        cbbDrive.setButtonCell(getListCell());
        cbbDrive.setCellFactory(f -> getListCell());
    }

    /**
     * Wrapper method for displaying both lists of drives and selected drive
     */
    private ListCell<File> getListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                executeRunner("Error displaying drives", () -> {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(getDriveItem(item));
                    }
                });
            }
        };
    }

    /**
     * get a Node to show the drive
     *
     * @param item the File object representing the drive
     * @return the Node object
     */
    private HBox getDriveItem(File item) {
        ImageView imv = new ImageView(ImageUtility.getFileIcon(item, false));
        Label lbl = new Label(FileSystemView.getFileSystemView().getSystemDisplayName(item));
        lbl.setTextFill(Color.BLACK);
        HBox hbo = new HBox(imv, lbl);
        HBox.setMargin(lbl, new Insets(0, 0, 0, 10));

        // make the icon vertically center
        HBox.setMargin(imv, new Insets(0, 0, 5, 0));
        HBox.setHgrow(lbl, Priority.ALWAYS);
        hbo.setAlignment(Pos.CENTER_LEFT);
        hbo.setPadding(new Insets(5, 5, 5, 5));
        return hbo;
    }

    private void initialiseSortImages() {
        hideSortImages();
        mivName.setVisible(true);

        isDescending.addListener(l -> executeRunner("Error when handling change to sort order", () -> {
            String glyph = isDescending.get() ? Configurations.DESCENDING_GLYPH : Configurations.ASCENDING_GLYPH;
            mivName.setGlyphName(glyph);
            mivExtension.setGlyphName(glyph);
            mivSize.setGlyphName(glyph);
            mivDate.setGlyphName(glyph);
        }));
    }

    private void hideSortImages() {
        mivName.setVisible(false);
        mivExtension.setVisible(false);
        mivSize.setVisible(false);
        mivDate.setVisible(false);
    }

    private void sortItemList() {
        String sortOrder = isDescending.get() ? "Descending" : "Ascending";
        writeInfoLog("Sorting list by " + currentSortLabel.getText() + " - " + sortOrder);

        tbvTable.getItems().sort((ExplorerItemModel o1, ExplorerItemModel o2) ->
                executeRunnerWithIntReturn("Error when sorting table", 0, () -> {
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
                }));
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

        tbvTable.focusedProperty().addListener(l ->
                executeRunner("Error when handling table focus change event", () -> {
                    if (tbvTable.isFocused()) {
                        applicationModel.setSelectedExplorerModel(explorerModel);
                        applicationModel.setSelectedItemList(getSelectedItems());
                    }
                }));

        addListenerToSelectedItems();
        initialiseTableContextMenu();
        tbvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setDoubleClickHandler();
        initialiseColumn(tbcName, lblName, Column.Name);
        initialiseColumn(tbcExtension, lblExtension, Column.Extension);
        initialiseColumn(tbcSize, lblSize, Column.Size);
        initialiseColumn(tbcDate, lblDate, Column.Date);
    }

    private void addListenerToSelectedItems() {
        tbvTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ExplorerItemModel>) l ->
                executeRunner("Error when handling change to item selection", () -> {
                    updateSelectedFoldersAndFiles();
                    updateTableContextMenu();
                }));
    }

    private void updateTableContextMenu() {
        cmiRunOrGoto.setVisible(false);
        cmiSimpleRename.setVisible(false);
        cmiEditAsText.setVisible(false);

        if (selectedFolderList.isEmpty()) {
            if (!selectedFileList.isEmpty()) {
                cmiRunOrGoto.setVisible(true);
                setRunContextMenuItem();

                // show edit as text if exactly 1 file is selected
                if (selectedFileList.size() == 1) {
                    cmiSimpleRename.setVisible(true);
                    cmiEditAsText.setVisible(true);
                }
            }
        } else if (selectedFolderList.size() == 1) {
            // only show rename and go to if exactly 1 folder is selected
            if (selectedFileList.isEmpty()) {
                cmiSimpleRename.setVisible(true);
                cmiRunOrGoto.setVisible(true);
                setGoToContextMenuItem();
            }
        }
    }

    private void initialiseTableContextMenu() {
        ctmTable.setOnShowing(l -> executeRunner("Error when handling table context menu shown event",
                () -> cmiPaste.setVisible(ClipboardLogic.clipboardHasFiles()))
        );

        smiRunOrGoTo.visibleProperty().bind(cmiRunOrGoto.visibleProperty());
        smiEditAsText.visibleProperty().bindBidirectional(cmiEditAsText.visibleProperty());
        setRunOrGoToLabelWidth();
        bindTableMenuItemsWidthProperty();
    }

    private void setRunOrGoToLabelWidth() {
        lblRunOrGoTo.setMinWidth(MIN_TABLE_CONTEXT_MENU_ITEM_WIDTH);

        lblRunOrGoTo.widthProperty().addListener(l ->
                executeRunner("Error when handling run or go to label width change", () -> {
                    // resize label to minimum width if it's less than minimum
                    if (lblRunOrGoTo.getWidth() < MIN_TABLE_CONTEXT_MENU_ITEM_WIDTH) {
                        lblRunOrGoTo.autosize();
                    }
                }));
    }

    private void bindTableMenuItemsWidthProperty() {
        lblCopyToOther.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblCopyToClipboard.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblCutToClipboard.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblPaste.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblMove.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblDelete.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblSimpleRename.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblAdvancedRename.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblEditAsText.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblNewFolder.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
        lblNewFile.prefWidthProperty().bind(lblRunOrGoTo.widthProperty());
    }

    private void updateSelectedFoldersAndFiles() {
        selectedFolderList.clear();
        selectedFileList.clear();
        List<ExplorerItemModel> itemList = getSelectedItems();

        for (ExplorerItemModel item : itemList) {
            if (item.getFile().isDirectory()) {
                selectedFolderList.add(item);
            } else {
                selectedFileList.add(item);
            }
        }
    }

    private ObservableList<ExplorerItemModel> getSelectedItems() {
        return tbvTable.getSelectionModel().getSelectedItems();
    }

    private void setRunContextMenuItem() {
        mivRunOrGoTo.setGlyphName(Configurations.RUN_FILE_GLYPH);
        String run = "Run ";
        int size = selectedFileList.size();

        if (size > 1) {
            run += size + " selected files";
        } else {
            run += "\"" + selectedFileList.get(0).getFile().getName() + "\"";
        }

        lblRunOrGoTo.setText(run);
    }

    private void setGoToContextMenuItem() {
        mivRunOrGoTo.setGlyphName(Configurations.GO_TO_GLYPH);
        ExplorerItemModel item = selectedFolderList.get(0);
        lblRunOrGoTo.setText("Go to " + item.getFile().getAbsolutePath());
    }

    private void setDoubleClickHandler() {
        tbvTable.setRowFactory(f -> {
            TableRow<ExplorerItemModel> row = new TableRow<>();

            row.setOnMouseClicked(event -> executeRunner("Error when handling double click", () -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && (!row.isEmpty())) {
                    runOrGoToByItem(row.getItem());
                }
            }));

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

            executeRunner("Error opening file " + path, () -> {
                writeInfoLog("Running file " + path);
                Desktop.getDesktop().open(file);
            });
        }
    }

    /**
     * Wrapper to initialise column
     */
    private void initialiseColumn(TableColumn<ExplorerItemModel, ExplorerItemModel> tbc, Label lbl, Column column) {
        tbc.setCellValueFactory(new ExplorerTableCellValue());
        tbc.setCellFactory(new ExplorerTableCellCallback(column));

        tbc.widthProperty().addListener(l -> executeRunner("Error when handling change to column width",
                () -> lbl.setPrefWidth(tbc.getWidth())));
    }

    @FXML
    private void back(ActionEvent event) {
        executeRunner("Could not navigate back", () -> {
            File parent = explorerModel.getFolder().getParentFile();

            if (parent != null) {
                changeCurrentPath(parent);
            }
        });
    }

    @FXML
    private void goToRoot(ActionEvent event) {
        executeRunner("Could not go to root", () -> changeCurrentPath(new File(getRootPath())));
    }

    @FXML
    private void reload(ActionEvent event) {
        executeRunner("Could not reload", this::updateAndReselect);
    }

    @FXML
    private void scrollToTop(ActionEvent event) {
        executeRunner("Could not scroll to top", () -> tbvTable.scrollTo(0));
    }

    @FXML
    private void scrollToBottom(ActionEvent event) {
        executeRunner("Could not scroll to bottom", () -> tbvTable.scrollTo(tbvTable.getItems().size() - 1));
    }

    @FXML
    private void sort(MouseEvent event) {
        executeRunner("Could not sort items", () -> {
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
        });
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
        executeRunner("Could not run or go to selection", () -> {
            // navigate into selected folder
            if (selectedFileList.isEmpty()) {
                runOrGoToByItem(selectedFolderList.get(0));
            } else { // otherwise run the files
                for (ExplorerItemModel item : selectedFileList) {
                    runOrGoToByItem(item);
                }
            }
        });
    }

    @FXML
    private void copyToOther(ActionEvent event) {
        executeRunner("Could not copy to other tab",
                () -> checkEmptyAndPostEvent(new PerformOperationEvent(OperationType.CopyToOther)));
    }

    private void checkEmptyAndPostEvent(Object event) {
        if (!getSelectedItems().isEmpty()) {
            postEvent(event);
        }
    }

    @FXML
    private void copyToClipboard(ActionEvent event) {
        executeRunner("Could not copy to clipboard",
                () -> checkEmptyAndPostEvent(new PerformOperationEvent(OperationType.CopyToClipboard)));
    }

    @FXML
    private void cutToClipboard(ActionEvent event) {
        executeRunner("Could not cut to clipboard",
                () -> checkEmptyAndPostEvent(new PerformOperationEvent(OperationType.CutToClipboard)));
    }

    @FXML
    private void paste(ActionEvent event) {
        executeRunner("Could not paste", () -> postEvent(new PerformOperationEvent(OperationType.Paste)));
    }

    @FXML
    private void move(ActionEvent event) {
        executeRunner("Could not copy to clipboard",
                () -> checkEmptyAndPostEvent(new PerformOperationEvent(OperationType.Move)));
    }

    @FXML
    private void delete(ActionEvent event) {
        executeRunner("Could not delete",
                () -> checkEmptyAndPostEvent(new PerformOperationEvent(OperationType.Delete)));
    }

    @FXML
    private void simpleRename(ActionEvent event) {
        executeRunner("Could not perform simple rename",
                () -> postEvent(new SingleFileDialogEvent(DialogType.SimpleRename,
                        getSelectedItems().get(0).getFile())));
    }

    @FXML
    private void advancedRename(ActionEvent event) {
    }

    @FXML
    private void editAsText(ActionEvent event) {
        executeRunner("Could not edit as text",
                () -> postEvent(new SingleFileDialogEvent(DialogType.EditAsText, getSelectedItems().get(0).getFile())));
    }

    @FXML
    private void addNewFolder(ActionEvent event) {
        executeRunner("Could not add new folder",
                () -> postEvent(new PerformOperationEvent(OperationType.AddNewFolder)));
    }

    @FXML
    private void addNewFile(ActionEvent event) {
        executeRunner("Could not add new file", () -> postEvent(new PerformOperationEvent(OperationType.AddNewFile)));
    }

    @FXML
    private void tableKeyPressed(KeyEvent event) {
        executeRunner("Could not handle key event", () -> {
            if (event.isControlDown()) {
                handleControlModifierEvent(event);
            } else {
                handleNoModifierEvent(event);
            }
        });
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

            case X:
                cutToClipboard(null);
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
                postEvent(new SwitchTabEvent(explorerModel));
                break;

            case ENTER:
                if (cmiRunOrGoto.isVisible()) {
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
                if (cmiSimpleRename.isVisible()) {
                    simpleRename(null);
                }

                break;

            case F4:
                if (cmiEditAsText.isVisible()) {
                    editAsText(null);
                }

                break;

            case F7:
                addNewFolder(null);
                break;

            case F8:
                addNewFile(null);
                break;

            default:
                break;
        }
    }
}