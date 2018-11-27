package xyz.gnas.elif.app.models;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.utility.code.CodeRunnerUtility;
import xyz.gnas.elif.app.common.utility.code.Runner;
import xyz.gnas.elif.app.events.explorer.ChangePathEvent;
import xyz.gnas.elif.app.models.explorer.ExplorerModel;

import java.io.File;

import static xyz.gnas.elif.app.common.utility.LogUtility.writeInfoLog;

public class SettingModel {
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

    public SettingModel() {
        EventBus.getDefault().register(this);
    }

    private static void executeRunner(String errorMessage, Runner runner) {
        CodeRunnerUtility.executeRunner(SettingModel.class, errorMessage, runner);
    }

    @Subscribe
    public void onChangePathEvent(ChangePathEvent event) {
        executeRunner("Error when handling change path event", () -> {
            writeInfoLog(getClass(), "Saving settings to file");
            File file = new File(Configurations.SETTING_FILE);
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
            mapper.writeValue(file, this);
        });
    }
}