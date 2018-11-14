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
import xyz.gnas.elif.core.models.explorer.ExplorerModel;

import java.io.File;
import java.util.ArrayList;

public class Setting {
    private static Setting instance;
    private ArrayList<ExplorerModel> explorerModelList;

    public Setting() {
    }

    public static Setting getInstance() {
        try {
            if (instance == null) {
                ObjectMapper mapper = new ObjectMapper();
                File file = new File(Configurations.SETTING_FILE);

                if (file.exists()) {
                    instance = mapper.readValue(file, Setting.class);
                } else {
                    instance = new Setting();
                    instance.explorerModelList = new ArrayList<ExplorerModel>();
                }

                EventBus.getDefault().register(instance);
            }
        } catch (Exception e) {
            Utility.showError(Setting.class, e, "Error getting setting", false);
        }

        return instance;
    }

    public ArrayList<ExplorerModel> getExplorerModelList() {
        return explorerModelList;
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