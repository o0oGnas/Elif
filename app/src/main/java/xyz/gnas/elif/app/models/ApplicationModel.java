package xyz.gnas.elif.app.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.models.explorer.ExplorerItemModel;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;

import java.io.File;

import static xyz.gnas.elif.app.common.utility.DialogUtility.showError;
import static xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility.executeRunnerAndHandleException;

public class ApplicationModel {
    private static ApplicationModel instance = null;

    public static ApplicationModel getInstance() {
        if (instance == null) {
            instance = new ApplicationModel();
        }

        return instance;
    }

    private ObjectProperty<SettingModel> setting = new SimpleObjectProperty<>();

    /**
     * The model of the currently selected tab
     */
    private ObjectProperty<ExplorerModel> selectedExplorerModel = new SimpleObjectProperty<>();

    /**
     * List of selected items in the last active tab
     */
    private ObjectProperty<ObservableList<ExplorerItemModel>> selectedItemList = new SimpleObjectProperty<>();

    public SettingModel getSetting() {
        if (setting.get() == null) {
            executeRunnerAndHandleException(() -> {
                ObjectMapper mapper = new ObjectMapper();
                File file = new File(Configurations.SETTING_FILE);

                if (file.exists()) {
                    setting.set(mapper.readValue(file, SettingModel.class));
                } else {
                    initialiseDefaultSetting();
                }
            }, (Exception e) -> {
                showError(ApplicationModel.class, "Error getting setting", e, false);
                initialiseDefaultSetting();
            });
        }

        return setting.get();
    }

    public ExplorerModel getSelectedExplorerModel() {
        return selectedExplorerModel.get();
    }

    public ObjectProperty<ExplorerModel> selectedExplorerModelProperty() {
        return selectedExplorerModel;
    }

    public void setSelectedExplorerModel(ExplorerModel selectedExplorerModel) {
        this.selectedExplorerModel.set(selectedExplorerModel);
    }

    public ObservableList<ExplorerItemModel> getSelectedItemList() {
        return selectedItemList.get();
    }

    public ObjectProperty<ObservableList<ExplorerItemModel>> selectedItemListProperty() {
        return selectedItemList;
    }

    public void setSelectedItemList(ObservableList<ExplorerItemModel> selectedItemList) {
        this.selectedItemList.set(selectedItemList);
    }

    private void initialiseDefaultSetting() {
        setting.set(new SettingModel());
        SettingModel settingModel = setting.get();
        settingModel.setLeftModel(new ExplorerModel());
        settingModel.setRightModel(new ExplorerModel());
    }

    private static void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(SettingModel.class, errorMessage, runner);
    }
}
