package xyz.gnas.elif.app.controllers.Operation;

import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.operation.InitialiseOperationEvent;
import xyz.gnas.elif.app.models.Operation;

public class OperationController {
    @FXML
    private MaterialIconView mivPauseResume;

    @FXML
    private Label lblName;

    @FXML
    private Label lblStatus;

    @FXML
    private HBox hboProcess;

    @FXML
    private HBox hboActions;

    @FXML
    private ProgressIndicator pgiProgress;

    @FXML
    private Button btnStop;

    @FXML
    private Button btnPauseResume;

    private Operation operation;

    private void showError(Exception e, String message, boolean exit) {
        Utility.showError(getClass(), e, message, exit);
    }

    private void writeInfoLog(String log) {
        Utility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onInitialiseOperationEvent(InitialiseOperationEvent event) {
        try {
            if (operation == null) {
                operation = event.getOperation();
                lblName.setText(operation.getName());

                operation.percentageDoneProperty().addListener(l -> {
                    try {
                        pgiProgress.setProgress(operation.getPercentageDone());
                    } catch (Exception e) {
                        showError(e, "Could not update progress indicator", false);
                    }
                });

                operation.suboperationNameProperty().addListener(l -> {
                    try {
                        lblStatus.setText(operation.getSuboperationName());
                    } catch (Exception e) {
                        showError(e, "Could not update suboperation name", false);
                    }
                });
            }
        } catch (Exception e) {
            showError(e, "Error handling initialise operation event", false);
        }
    }

    @FXML
    private void initialize() {
        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {
            showError(e, "Could not initialise operation item", false);
        }
    }

    @FXML
    private void pauseOrResume(ActionEvent event) {

    }

    @FXML
    private void stop(ActionEvent event) {

    }
}