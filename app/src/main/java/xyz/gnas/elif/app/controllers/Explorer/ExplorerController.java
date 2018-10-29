package xyz.gnas.elif.app.controllers.Explorer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.ResourceManager;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.LoadDriveEvent;
import xyz.gnas.elif.app.events.LoadRootsEvent;
import xyz.gnas.elif.app.models.ExplorerItem;

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
	private MaterialIconView mivName;

	@FXML
	private MaterialIconView mivExtension;

	@FXML
	private MaterialIconView mivSize;

	@FXML
	private MaterialIconView mivDate;

	@FXML
	private TableView<ExplorerItem> tbvTable;

	@FXML
	private TableColumn<ExplorerItem, ExplorerItem> tbcName;

	@FXML
	private TableColumn<ExplorerItem, ExplorerItem> tbcExtension;

	@FXML
	private TableColumn<ExplorerItem, ExplorerItem> tbcSize;

	@FXML
	private TableColumn<ExplorerItem, ExplorerItem> tbcDate;

	private ObjectProperty<File> currentPath = new SimpleObjectProperty<File>();

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
	public void onLoadRootsEvent(LoadRootsEvent event) throws IOException {
		ObservableList<File> driveList = cboDrive.getItems();
		driveList.clear();

		// for each pathname in pathname array
		for (File root : File.listRoots()) {
			driveList.add(root);
		}

		cboDrive.getSelectionModel().select(0);
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

			currentPath.addListener((ObservableValue<? extends File> observable, File oldValue, File newValue) -> {
				try {
					String path = newValue.getAbsolutePath();
					writeInfoLog("Navigating to " + path);
					lblFolderPath.setText(path);
					btnBack.setDisable(newValue.getParentFile() == null);
					updateItemList();
				} catch (Exception e) {
					showError(e, "Error when changing path", false);
				}
			});

			initialiseDriveComboBox();
			initialiseSortImages();
			initialiseTable();
		} catch (Exception e) {
			showError(e, "Could not initialise explorer", true);
		}
	}

	private void initialiseDriveComboBox() {
		// SET THE VALUE NEXTSTEP TO THE BUTTONCELL
		cboDrive.setButtonCell(getListCell());

		cboDrive.setCellFactory(f -> {
			return getListCell();
		});

		addHandlerToDriveComboBox();
	}

	/**
	 * @description Wrapper method for displaying both lists of drives and selected
	 *              drive
	 * @return the ListCell used by ComboBox for displaying
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
						currentPath.set(newValue);
					} catch (Exception e) {
						showError(e, "Error handling drive selection", false);
					}
				});
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
		ObservableList<ExplorerItem> itemList = tbvTable.getItems();
		itemList.clear();

		for (File child : currentPath.get().listFiles()) {
			itemList.add(new ExplorerItem(child));
		}

		sortItemList();
		tbvTable.refresh();
	}

	private void sortItemList() {
		writeInfoLog("Sorting item list");
		tbvTable.getItems().sort((ExplorerItem o1, ExplorerItem o2) -> {
			boolean descending = isDescending.get();
			boolean IsDirectory1 = o1.getFile().isDirectory();

			if (IsDirectory1 == o2.getFile().isDirectory()) {
				if (currentSortLabel == lblName) {
					String name1 = o1.getName();
					String name2 = o2.getName();
					return descending ? name2.compareTo(name1) : name1.compareTo(name2);
				} else if (currentSortLabel == lblExtension) {
					String extension1 = o1.getExtension();
					String extension2 = o2.getExtension();
					return descending ? extension2.compareTo(extension1) : extension1.compareTo(extension2);
				} else if (currentSortLabel == lblSize) {
					Long size1 = o1.getSize();
					Long size2 = o2.getSize();
					return descending ? size2.compareTo(size1) : size1.compareTo(size2);
				} else {
					Calendar date1 = o1.getDate();
					Calendar date2 = o2.getDate();
					return descending ? date2.compareTo(date1) : date1.compareTo(date2);
				}
			} else {
				return IsDirectory1 ? -1 : 1;
			}
		});
	}

	private void initialiseTable() {
		tbvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		setDoubleClickHandlerToTable();
		initialiseColumn(tbcName, lblName, Column.Name);
		initialiseColumn(tbcExtension, lblExtension, Column.Extension);
		initialiseColumn(tbcSize, lblSize, Column.Size);
		initialiseColumn(tbcDate, lblDate, Column.Date);
	}

	private void setDoubleClickHandlerToTable() {
		tbvTable.setRowFactory(tv -> {
			TableRow<ExplorerItem> row = new TableRow<ExplorerItem>();

			row.setOnMouseClicked(event -> {
				try {
					if (event.getClickCount() == 2 && (!row.isEmpty())) {
						File file = row.getItem().getFile();

						// navigate if it's a folder, run if a file
						if (file.isDirectory()) {
							currentPath.set(file);
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
				} catch (Exception e) {
					showError(e, "Error handling double click", false);
				}
			});

			return row;
		});
	}

	/**
	 * @description Wrapper to reduce copy paste
	 * @param tbc    column to initialise
	 * @param lbl    label header
	 * @param column column type
	 */
	private void initialiseColumn(TableColumn<ExplorerItem, ExplorerItem> tbc, Label lbl, Column column) {
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
			currentPath.set(currentPath.get().getParentFile());
		} catch (Exception e) {
			showError(e, "Could not navigate back", false);
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

			String sortOrder = isDescending.get() ? "Descending" : "Ascending";
			writeInfoLog("Sorting list by " + currentSortLabel.getText() + " - " + sortOrder);
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

	/**
	 * @author Gnas
	 * @date Oct 22, 2018
	 * @description custom value for table columns
	 */
	private class ExplorerTableCellValue implements
			Callback<TableColumn.CellDataFeatures<ExplorerItem, ExplorerItem>, ObservableValue<ExplorerItem>> {
		@Override
		public ObservableValue<ExplorerItem> call(CellDataFeatures<ExplorerItem, ExplorerItem> param) {
			return new ObservableValue<ExplorerItem>() {
				@Override
				public void removeListener(InvalidationListener listener) {
					// TODO Auto-generated method stub

				}

				@Override
				public void addListener(InvalidationListener listener) {
					// TODO Auto-generated method stub

				}

				@Override
				public void removeListener(ChangeListener<? super ExplorerItem> listener) {
					// TODO Auto-generated method stub

				}

				@Override
				public ExplorerItem getValue() {
					try {
						return param.getValue();
					} catch (Exception e) {
						showError(e, "Error getting value for table cell", false);
						return null;
					}
				}

				@Override
				public void addListener(ChangeListener<? super ExplorerItem> listener) {
					// TODO Auto-generated method stub

				}
			};
		}
	}

	/**
	 * @author Gnas
	 * @date Oct 22, 2018
	 * @description Column enum, used by ExplorerTableCell to determine how to
	 *              display the data
	 */
	private enum Column {
		Name, Extension, Size, Date
	}

	/**
	 * @author Gnas
	 * @date Oct 22, 2018
	 * @description custom cell display for table columns
	 */
	private class ExplorerTableCellCallback
			implements Callback<TableColumn<ExplorerItem, ExplorerItem>, TableCell<ExplorerItem, ExplorerItem>> {
		private Column column;

		public ExplorerTableCellCallback(Column column) {
			this.column = column;
		}

		@Override
		public TableCell<ExplorerItem, ExplorerItem> call(TableColumn<ExplorerItem, ExplorerItem> param) {
			return new ExplorerTableCell() {
			};
		}

		private class ExplorerTableCell extends TableCell<ExplorerItem, ExplorerItem> {
			@Override
			protected void updateItem(ExplorerItem item, boolean empty) {
				try {
					super.updateItem(item, empty);

					if (empty || item == null) {
						setGraphic(null);
					} else {
						display(item);
					}
				} catch (Exception e) {
					showError(e, "Error when displaying item", true);
				}
			}

			private void display(ExplorerItem item) {
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

			private void setIcon(ExplorerItem item) {
				// show icon depending on file or folder
				File file = item.getFile();

				if (file.isDirectory()) {
					MaterialIconView miv = new MaterialIconView();
					miv.setGlyphName("FOLDER_OPEN");
					miv.setGlyphSize(16);
					setGraphic(miv);
				} else {
					ImageView imv = new ImageView(Utility.getFileIcon(file));
					setGraphic(imv);
				}
			}
		}
	}
}