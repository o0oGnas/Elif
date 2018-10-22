package main.java.xyz.gnas.elif.controllers;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import main.java.xyz.gnas.elif.common.CommonUtility;
import main.java.xyz.gnas.elif.common.ResourceManager;
import main.java.xyz.gnas.elif.models.ExplorerItem;

public class ExplorerController {
	@FXML
	private ComboBox<File> cboDrive;

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
	private ImageView imvName;

	@FXML
	private ImageView imvExtension;

	@FXML
	private ImageView imvSize;

	@FXML
	private ImageView imvDate;

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

	private File currentPath;

	/**
	 * Flag to tell the current sorting columns
	 */
	private Label currentSortLabel;

	/**
	 * Flag to tell current sorting order
	 */
	private BooleanProperty isDescending = new SimpleBooleanProperty();

	/**
	 * Flag to tell if File is editing a file
	 */
	private boolean isEditing;

	public void initialiseAll(File[] rootList) throws IOException {
		ObservableList<File> driveList = cboDrive.getItems();
		driveList.clear();

		// for each pathname in pathname array
		for (File root : rootList) {
			driveList.add(root);
		}

		cboDrive.getSelectionModel().select(0);
	}

	private void showError(Exception e, String message, boolean exit) {
		CommonUtility.showError(getClass(), e, message, exit);
	}

	private void writeInfoLog(String log) {
		CommonUtility.writeInfoLog(getClass(), log);
	}

	@FXML
	private void initialize() {
		try {
			currentSortLabel = lblName;
			initialiseDriveComboBox();
			initialiseSortImages();
			initialiseTable();
		} catch (Exception e) {
			showError(e, "Could not initialise explorer", true);
		}
	}

	private void initialiseDriveComboBox() {
		Callback<ListView<File>, ListCell<File>> factory = (ListView<File> l) -> {
			return new ListCell<File>() {
				@Override
				protected void updateItem(File item, boolean empty) {
					super.updateItem(item, empty);

					if (item == null || empty) {
						setGraphic(null);
					} else {
						setText(item.getAbsolutePath());
					}
				}
			};
		};

		// Just set the button cell here:
		cboDrive.setButtonCell(factory.call(null));
		cboDrive.setCellFactory(factory);
		addHandlerToDriveComboBox();
	}

	private void addHandlerToDriveComboBox() {
		cboDrive.getSelectionModel().selectedItemProperty()
				.addListener((ObservableValue<? extends File> observable, File oldValue, File newValue) -> {
					try {
						currentPath = newValue;
						updateItemList();
					} catch (Exception e) {
						showError(e, "Error handling drive selection", false);
					}
				});
	}

	private void initialiseSortImages() {
		hideSortImages();
		imvName.setVisible(true);

		isDescending
				.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
					Image image = newValue ? ResourceManager.getDescendingIcon() : ResourceManager.getAscendingIcon();
					imvName.setImage(image);
					imvExtension.setImage(image);
					imvSize.setImage(image);
					imvDate.setImage(image);
				});
	}

	private void hideSortImages() {
		imvName.setVisible(false);
		imvExtension.setVisible(false);
		imvSize.setVisible(false);
		imvDate.setVisible(false);
	}

	private void updateItemList() {
		for (File child : currentPath.listFiles()) {
			tbvTable.getItems().add(new ExplorerItem(child));
		}

		sortItemList();
	}

	private void sortItemList() {
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
		initialiseColumn(tbcName, lblName, Column.Name);
		initialiseColumn(tbcExtension, lblExtension, Column.Extension);
		initialiseColumn(tbcSize, lblSize, Column.Size);
		initialiseColumn(tbcDate, lblDate, Column.Date);
	}

	/**
	 * @description Wrapper to reduce copy paste
	 * @param tbc    column to initialise
	 * @param lbl    label header
	 * @param column column type
	 */
	private void initialiseColumn(TableColumn<ExplorerItem, ExplorerItem> tbc, Label lbl, Column column) {
		tbc.setCellValueFactory(new ExplorerTableCellValue());
		tbc.setCellFactory(new ExplorerTableCell(column));

		tbc.widthProperty()
				.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
					lbl.setPrefWidth(newValue.doubleValue());
				});
	}

	@FXML
	private void sort(MouseEvent event) {
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

		if (currentSortLabel == lblName) {
			imvName.setVisible(true);
		} else if (currentSortLabel == lblExtension) {
			imvExtension.setVisible(true);
		} else if (currentSortLabel == lblSize) {
			imvSize.setVisible(true);
		} else {
			imvDate.setVisible(true);
		}

		sortItemList();
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
					// TODO Auto-generated method stub
					return param.getValue();
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
	private class ExplorerTableCell
			implements Callback<TableColumn<ExplorerItem, ExplorerItem>, TableCell<ExplorerItem, ExplorerItem>> {
		private Column column;

		public ExplorerTableCell(Column column) {
			this.column = column;
		}

		@Override
		public TableCell<ExplorerItem, ExplorerItem> call(TableColumn<ExplorerItem, ExplorerItem> param) {
			return new TableCell<ExplorerItem, ExplorerItem>() {
				@Override
				protected void updateItem(ExplorerItem item, boolean empty) {
					try {
						super.updateItem(item, empty);

						if (empty) {
							setGraphic(null);
						} else {
							setTextFill(item.getFile().isDirectory() ? Color.MAROON : Color.BLACK);
							display(item);
						}
					} catch (Exception e) {
						showError(e, "Error when displaying item", true);
					}
				}

				private void display(ExplorerItem item) {
					switch (column) {
					case Name:
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
			};
		}
	}
}