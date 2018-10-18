package main.java.xyz.gnas.elif.controllers;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import main.java.xyz.gnas.elif.common.CommonUtility;
import main.java.xyz.gnas.elif.models.ExplorerItem;

public class ExplorerController {
	@FXML
	private ComboBox<File> cboDrive;

	@FXML
	private Label lblFolderPath;

	@FXML
	private TableView<ExplorerItem> tbvTable;

	@FXML
	private TableColumn<ExplorerItem, String> tbcName;

	@FXML
	private TableColumn<ExplorerItem, String> tbcExtension;

	@FXML
	private TableColumn<ExplorerItem, Long> tbcSize;

	@FXML
	private TableColumn<ExplorerItem, Calendar> tbcDate;

	private File currentPath;

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

	@FXML
	private void initialize() {
		try {
			initialiseDriveComboBox();
			tbvTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			initialiseStringColumn(tbcName, "name");
			initialiseStringColumn(tbcExtension, "extension");
			initialiseSizeColumn();
			initialiseDateColumn();
		} catch (Exception e) {
			CommonUtility.showError(e, "Could not initialise explorer", true);
		}
	}

	private void initialiseDriveComboBox() {
		Callback<ListView<File>, ListCell<File>> factory = new Callback<ListView<File>, ListCell<File>>() {
			@Override
			public ListCell<File> call(ListView<File> l) {
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
			}
		};

		// Just set the button cell here:
		cboDrive.setButtonCell(factory.call(null));
		cboDrive.setCellFactory(factory);
		addHandlerToDriveComboBox();
	}

	private void addHandlerToDriveComboBox() {
		cboDrive.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<File>() {
			@Override
			public void changed(ObservableValue<? extends File> observable, File oldValue, File newValue) {
				try {
					currentPath = newValue;
					update();
				} catch (Exception e) {
					CommonUtility.showError(e, "Error handling drive selection", false);
				}
			}
		});
	}

	private void update() {
		ObservableList<ExplorerItem> itemList = tbvTable.getItems();
		itemList.clear();

		for (File child : currentPath.listFiles()) {
			itemList.add(new ExplorerItem(child));
		}
	}

	/**
	 * @Description Wrapper to reduce copy paste
	 * @Date Oct 18, 2018
	 * @param column       TableColumn object
	 * @param propertyName name of the property to bind to column
	 */
	private void initialiseStringColumn(TableColumn<ExplorerItem, String> column, String propertyName) {
		column.setCellValueFactory(new PropertyValueFactory<ExplorerItem, String>(propertyName));
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		
		column.setCellFactory(new Callback<TableColumn<ExplorerItem, String>, TableCell<ExplorerItem, String>>() {
			@Override
			public TableCell<ExplorerItem, String> call(TableColumn<ExplorerItem, String> param) {
				return new TableCell<ExplorerItem, String>() {
					@Override
					protected void updateItem(String item, boolean empty) {
						try {
							super.updateItem(item, empty);

							if (empty || item < 0) {
								setGraphic(null);
							} else {
								setText(item);
								
								if ()
							}
						} catch (Exception e) {
							CommonUtility.showError(e, "Error when displaying date column", false);
						}
					}
				};
			}
		});
	}

	private void initialiseSizeColumn() {
		tbcSize.setCellFactory(new Callback<TableColumn<ExplorerItem, Long>, TableCell<ExplorerItem, Long>>() {
			@Override
			public TableCell<ExplorerItem, Long> call(TableColumn<ExplorerItem, Long> param) {
				return new TableCell<ExplorerItem, Long>() {
					@Override
					protected void updateItem(Long item, boolean empty) {
						try {
							super.updateItem(item, empty);

							if (empty || item < 0) {
								setGraphic(null);
							} else {
								DecimalFormat format = new DecimalFormat("#,###");
								setText(format.format(item));
							}
						} catch (Exception e) {
							CommonUtility.showError(e, "Error when displaying date column", false);
						}
					}
				};
			}
		});

		tbcSize.setCellValueFactory(new PropertyValueFactory<ExplorerItem, Long>("size"));
	}

	private void initialiseDateColumn() {
		tbcDate.setCellFactory(new Callback<TableColumn<ExplorerItem, Calendar>, TableCell<ExplorerItem, Calendar>>() {
			@Override
			public TableCell<ExplorerItem, Calendar> call(TableColumn<ExplorerItem, Calendar> param) {
				return new TableCell<ExplorerItem, Calendar>() {
					@Override
					protected void updateItem(Calendar item, boolean empty) {
						try {
							super.updateItem(item, empty);

							if (empty) {
								setGraphic(null);
							} else {
								SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
								setText(dateFormat.format(item.getTime()));
							}
						} catch (Exception e) {
							CommonUtility.showError(e, "Error when displaying date column", false);
						}
					}
				};
			}
		});

		tbcDate.setCellValueFactory(new PropertyValueFactory<ExplorerItem, Calendar>("date"));
	}
}