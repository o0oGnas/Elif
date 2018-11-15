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
import xyz.gnas.elif.app.common.Configurations;
import xyz.gnas.elif.app.common.Utility;
import xyz.gnas.elif.app.events.operation.InitialiseOperationEvent;
import xyz.gnas.elif.core.models.Operation;

import static xyz.gnas.elif.app.common.Utility.showConfirmation;

public class OperationController {
    @FXML
    private MaterialIconView mivPauseResume;

    @FXML
    private Label lblName;

    @FXML
    private Label lblStatus;

    @FXML
    private HBox hboActions;

    @FXML
    private ProgressIndicator pgiProgress;

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
                hboActions.disableProperty().bind(operation.stoppedProperty());
                hboActions.disableProperty().bind(operation.completeProperty());
                addSuboperationNameListener(operation);
                addPercentageDoneListner(operation);
                addPauseListener(operation);
            }
        } catch (Exception e) {
            showError(e, "Error handling initialise operation event", false);
        }
    }

    private void addSuboperationNameListener(Operation operation) {
        operation.suboperationNameProperty().addListener(l -> {
            try {
                lblStatus.setText(operation.getSuboperationName());
            } catch (Exception e) {
                showError(e, "Could not update suboperation name", false);
            }
        });
    }

    private void addPercentageDoneListner(Operation operation) {
        lblName.setText(operation.getName());

        operation.percentageDoneProperty().addListener(l -> {
            try {
                pgiProgress.setProgress(operation.getPercentageDone());
            } catch (Exception e) {
                showError(e, "Could not update progress indicator", false);
            }
        });
    }

    private void addPauseListener(Operation operation) {
        operation.pausedProperty().addListener(l -> {
            boolean pause = operation.getPaused();
            mivPauseResume.setGlyphName(pause ? Configurations.RESUME_GLYPH : Configurations.PAUSE_GLYPH);
            btnPauseResume.setText(pause ? "Resume" : "Pause");
            String status = "";

            if (pause) {
                status += "(Paused) ";
            }

            status += operation.getSuboperationName();
            lblStatus.setText(status);
        });
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
        try {
            operation.setPaused(!operation.getPaused());
            String pauseResume = operation.getPaused() ? "Pausing" : "Resuming";
            writeInfoLog(pauseResume + " operation [" + operation.getName() + "]");
        } catch (Exception e) {
            showError(e, "Could not pause/resume operation [" + operation.getName() + "]", false);
        }
    }

    @FXML
    private void stop(ActionEvent event) {
        try {
            if (showConfirmation("Are you sure you want to stop this operation?")) {
                operation.setStopped(true);
                writeInfoLog("Stopping operation [" + operation.getName() + "]");
            }
        } catch (Exception e) {
            showError(e, "Could not stop operation [" + operation.getName() + "]", false);
        }
    }
}