package xyz.gnas.elif.app.controllers.explorer;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.CodeRunnerUtility.Runner;
import xyz.gnas.elif.app.common.utility.DialogUtility;
import xyz.gnas.elif.app.common.utility.ImageUtility;
import xyz.gnas.elif.app.common.utility.WindowEventUtility;
import xyz.gnas.elif.app.events.dialog.EditAsTextEvent;
import xyz.gnas.elif.app.events.dialog.SimpleRenameEvent;
import xyz.gnas.elif.app.events.explorer.ChangeItemSelectionEvent;
import xyz.gnas.elif.app.events.explorer.ChangePathEvent;
import xyz.gnas.elif.app.events.explorer.FocusExplorerEvent;
import xyz.gnas.elif.app.events.explorer.InitialiseExplorerEvent;
import xyz.gnas.elif.app.events.explorer.ReloadEvent;
import xyz.gnas.elif.app.events.explorer.SwitchTabEvent;
import xyz.gnas.elif.app.events.operation.AddNewFileEvent;
import xyz.gnas.elif.app.events.operation.AddNewFolderEvent;
import xyz.gnas.elif.app.events.operation.CopyToClipboardEvent;
import xyz.gnas.elif.app.events.operation.CopyToOtherEvent;
import xyz.gnas.elif.app.events.operation.DeleteEvent;
import xyz.gnas.elif.app.events.operation.MoveEvent;
import xyz.gnas.elif.app.events.operation.PasteEvent;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;
import xyz.gnas.elif.core.logic.ClipboardLogic;

import javax.swing.filechooser.FileSystemView;
import java.awt.Desktop;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

    private ExplorerModel model;

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

    private int executeRunnerWithIntReturn(String errorMessage, int errorReturnValue,
                                           CodeRunnerUtility.RunnerWithIntReturn runner) {
        return CodeRunnerUtility.executeRunnerWithIntReturn(getClass(), errorMessage, errorReturnValue, runner);
    }

    private Object executeRunnerWithObjectReturn(String errorMessage, CodeRunnerUtility.RunnerWithObjectReturn runner) {
        return CodeRunnerUtility.executeRunnerWithObjectReturn(getClass(), errorMessage, runner);
    }

    private void writeInfoLog(String log) {
        DialogUtility.writeInfoLog(getClass(), log);
    }

    private void postEvent(Object object) {
        EventBus.getDefault().post(object);
    }

    @Subscribe
    public void onInitialiseExplorerEvent(InitialiseExplorerEvent event) {
        executeRunnerOrExit("Could not initialise explorer", () -> {
            if (model == null) {
                model = event.getModel();

                // add right padding if model is left, left padding if model is right
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
                        executeRunner("Error handling drive selection", () -> changeCurrentPath(newValue)));
    }

    private void selectInitialDrive() {
        if (model.getFolder() == null) {
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

        for (int i = 0; i < driveList.size(); ++i) {
            if (driveOfPath.equalsIgnoreCase(driveList.get(i).getAbsolutePath())) {
                cbbDrive.getSelectionModel().select(i);
                changeCurrentPath(model.getFolder());
                validPath = true;
                break;
            }
        }

        if (!validPath) {
            cbbDrive.getSelectionModel().select(0);
        }
    }

    private String getRootPath() {
        String path = model.getFolder().getAbsolutePath();
        return path.substring(0, path.indexOf("\\") + 1);
    }

    /**
     * Change current path, update list and generate change path event
     */
    private void changeCurrentPath(File path) {
        String absolutePath = path.getAbsolutePath();
        writeInfoLog("Navigating to " + absolutePath);
        model.setFolder(path);
        lblFolderPath.setText(absolutePath);
        boolean isRoot = path.getParentFile() == null;
        btnBack.setDisable(isRoot);
        btnRoot.setDisable(isRoot);
        updateItemListAndScrollToTop();
        postEvent(new ChangePathEvent(model));
    }

    private void updateItemListAndScrollToTop() {
        updateItemList();
        scrollToTop(null);
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
    }

    @Subscribe
    public void onChangePathEvent(ChangePathEvent event) {
        executeRunner("Error handling change path event", () -> {
            ExplorerModel eventModel = event.getModel();

            if (model != eventModel) {
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
        executeRunner("Error handling reload event", () -> {
            if (event.getPath().equalsIgnoreCase(model.getFolder().getAbsolutePath())) {
                updateItemList();
            }
        });
    }

    @Subscribe
    public void onSwitchTabEvent(SwitchTabEvent event) {
        executeRunner("Error handling switch tab event", () -> {
            if (event.getModel() != model) {
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
        WindowEventUtility.bindWindowEventHandler(getClass(), acpMain, new WindowEventUtility.WindowEventHandler() {
            @Override
            public void handleShownEvent() {
            }

            @Override
            public void handleFocusedEvent() {
                executeRunner("Error handling window focused event", () -> updateItemList());
            }

            @Override
            public void handleCloseEvent(WindowEvent windowEvent) {
            }
        });
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
                        postEvent(new FocusExplorerEvent(model));
                        postEvent(new ChangeItemSelectionEvent(getSelectedItems()));
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
                    postEvent(new ChangeItemSelectionEvent(getSelectedItems()));
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
        ctmTable.setOnShowing(l -> executeRunner("Error handling table context menu shown event",
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

    private List<ExplorerItemModel> getSelectedItems() {
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

            row.setOnMouseClicked(event -> executeRunner("Error handling double click", () -> {
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

        tbc.widthProperty().addListener(l -> executeRunner("Error handling change to column width",
                () -> lbl.setPrefWidth(tbc.getWidth())));
    }

    @FXML
    private void back(ActionEvent event) {
        executeRunner("Could not navigate back", () -> {
            File parent = model.getFolder().getParentFile();

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
        executeRunner("Could not reload", this::updateItemListAndScrollToTop);
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
        executeRunner("Could not copy to other tab", () -> checkEmptyAndPostEvent(new CopyToOtherEvent(model,
                getSelectedItems())));
    }

    private void checkEmptyAndPostEvent(Object event) {
        if (!getSelectedItems().isEmpty()) {
            postEvent(event);
        }
    }

    @FXML
    private void copyToClipboard(ActionEvent event) {
        executeRunner("Could not copy to clipboard", () -> checkEmptyAndPostEvent(new CopyToClipboardEvent(model,
                getSelectedItems())));
    }

    @FXML
    private void paste(ActionEvent event) {
        executeRunner("Could not paste", () -> postEvent(new PasteEvent(model)));
    }

    @FXML
    private void move(ActionEvent event) {
        executeRunner("Could not copy to clipboard", () -> checkEmptyAndPostEvent(new MoveEvent(model,
                getSelectedItems())));
    }

    @FXML
    private void delete(ActionEvent event) {
        executeRunner("Could not delete", () -> checkEmptyAndPostEvent(new DeleteEvent(model, getSelectedItems())));
    }

    @FXML
    private void simpleRename(ActionEvent event) {
        executeRunner("Could not perform simple rename",
                () -> postEvent(new SimpleRenameEvent(tbvTable.getSelectionModel().getSelectedItem().getFile())));
    }

    @FXML
    private void advancedRename(ActionEvent event) {
    }

    @FXML
    private void editAsText(ActionEvent event) {
        executeRunner("Could not edit as text",
                () -> postEvent(new EditAsTextEvent(tbvTable.getSelectionModel().getSelectedItem().getFile())));
    }

    @FXML
    private void addNewFolder(ActionEvent event) {
        executeRunner("Could not add new folder", () -> postEvent(new AddNewFolderEvent(model)));
    }

    @FXML
    private void addNewFile(ActionEvent event) {
        executeRunner("Could not add new file", () -> postEvent(new AddNewFileEvent(model)));
    }

    @FXML
    private void tableKeyReleased(KeyEvent event) {
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

    /**
     * Column enum, used by ExplorerTableCell to determine how to display the data
     */
    private enum Column {
        Name, Extension, Size, Date
    }

    /**
     * Custom value for table columns
     */
    private class ExplorerTableCellValue implements Callback<TableColumn.CellDataFeatures<ExplorerItemModel,
            ExplorerItemModel>, ObservableValue<ExplorerItemModel>> {
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
                    return (ExplorerItemModel) executeRunnerWithObjectReturn("Error getting value for table cell",
                            () -> param.getValue());
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
    private class ExplorerTableCellCallback implements Callback<TableColumn<ExplorerItemModel, ExplorerItemModel>,
            TableCell<ExplorerItemModel, ExplorerItemModel>> {
        private Column column;

        public ExplorerTableCellCallback(Column column) {
            this.column = column;
        }

        @Override
        public TableCell<ExplorerItemModel, ExplorerItemModel> call(TableColumn<ExplorerItemModel, ExplorerItemModel> param) {
            return new ExplorerTableCell() {
            };
        }

        private class ExplorerTableCell extends TableCell<ExplorerItemModel, ExplorerItemModel> {
            @Override
            protected void updateItem(ExplorerItemModel item, boolean empty) {
                executeRunner("Error when displaying item", () -> {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        display(item);
                    }
                });
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
                    mivFolder.setGlyphName(Configurations.FOLDER_GRYPH);
                    mivFolder.setGlyphSize(16);
                    setGraphic(mivFolder);
                } else {
                    ImageView imv = new ImageView(ImageUtility.getFileIcon(file, true));
                    setGraphic(imv);
                }
            }
        }
    }
}