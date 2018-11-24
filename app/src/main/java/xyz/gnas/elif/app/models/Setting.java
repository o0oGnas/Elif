package xyz.gnas.elif.app.models;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.explorer.ChangePathEvent;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;

import java.io.File;

public class Setting {
    private static Setting instance;
    private ExplorerModel leftModel;
    private ExplorerModel rightModel;

    public ExplorerModel getLeftModel() {
        return leftModel;
    }

    public void setLeftModel(ExplorerModel leftModel) {
        this.leftModel = leftModel;
    }

    public ExplorerModel getRightModel() {
        return rightModel;
    }

    public void setRightModel(ExplorerModel rightModel) {
        this.rightModel = rightModel;
    }

    public Setting() {
    }

    public static Setting getInstance() {
        try {
            if (instance == null) {
                ObjectMapper mapper = new ObjectMapper();
                File file = new File(Configurations.SETTING_FILE);

                if (file.exists()) {
                    instance = mapper.readValue(file, Setting.class);
                    subscribeInstance();
                } else {
                    initialiseDefaultSetting();
                }
            }
        } catch (Exception e) {
            Utility.showError(Setting.class, e, "Error getting setting", false);
            initialiseDefaultSetting();
        }

        ExplorerModel leftModel = instance.leftModel;
        ExplorerModel rightModel = instance.rightModel;
        return instance;
    }

    private static void subscribeInstance() {
        EventBus.getDefault().register(instance);
    }

    private static void initialiseDefaultSetting() {
        instance = new Setting();
        instance.leftModel = new ExplorerModel();
        instance.rightModel = new ExplorerModel();
        subscribeInstance();
    }

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        Utility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onChangePathEvent(ChangePathEvent event) {
        try {
            writeInfoLog("Saving settings to file");
            File file = new File(Configurations.SETTING_FILE);
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
            mapper.writeValue(file, this);
        } catch (Exception e) {
            showError(e, "Error saving paths to file", false);
        }
    }
}