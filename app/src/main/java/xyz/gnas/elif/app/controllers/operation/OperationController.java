package xyz.gnas.elif.app.controllers.operation;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import xyz.gnas.elif.app.common.utility.DialogUtility;
import xyz.gnas.elif.app.common.utility.LogUtility;
import xyz.gnas.elif.app.common.utility.runner.RunnerUtility;
import xyz.gnas.elif.app.common.utility.runner.VoidRunner;
import xyz.gnas.elif.app.events.operation.InitialiseOperationEvent;
import xyz.gnas.elif.core.models.Operation;

public class OperationController {
    @FXML
    private MaterialIconView mivPauseResume;

    @FXML
    private Label lblName;

    @FXML
    private Label lblStatus;

    @FXML
    private HBox hbxActions;

    @FXML
    private ProgressIndicator pgiProgress;

    @FXML
    private Button btnPauseResume;

    private Operation operation;

    private void executeRunner(String errorMessage, VoidRunner runner) {
        RunnerUtility.executeVoidrunner(getClass(), errorMessage, runner);
    }

    private void writeInfoLog(String log) {
        LogUtility.writeInfoLog(getClass(), log);
    }

    @Subscribe
    public void onInitialiseOperationEvent(InitialiseOperationEvent event) {
        executeRunner("Error when handling initialise operation event", () -> {
            if (operation == null) {
                operation = event.getOperation();
                lblName.setText(operation.getName());
                hbxActions.disableProperty().bind(operation.stoppedProperty());
                hbxActions.disableProperty().bind(operation.completeProperty());

                operation.suboperationNameProperty().addListener(
                        l -> executeRunner("Error when handling suboperation  name change event",
                                () -> lblStatus.setText(operation.getSuboperationName())));

                operation.completedAmountProperty().addListener(
                        l -> executeRunner("Error when handling completed amount change event",
                                () -> pgiProgress.setProgress(operation.getCompletedAmount())));

                addPauseListener(operation);
            }
        });
    }

    private void addPauseListener(Operation operation) {
        operation.pausedProperty().addListener(l -> executeRunner("Error when handling paused status change event",
                () -> {
                    if (!operation.isStopped()) {
                        boolean pause = operation.isPaused();
                        mivPauseResume.setIcon(pause ? MaterialIcon.MAIL.PLAY_ARROW : MaterialIcon.PAUSE);
                        btnPauseResume.setText(pause ? "Resume" : "Pause");
                        String status = "";

                        if (pause) {
                            status += "(Paused) ";
                        }

                        status += operation.getSuboperationName();
                        lblStatus.setText(status);
                    }
                }));
    }

    @FXML
    private void initialize() {
        executeRunner("Could not initialise operation item", () -> EventBus.getDefault().register(this));
    }

    @FXML
    private void pauseOrResume(ActionEvent event) {
        executeRunner("Could not pause/resume operation " + getOperationNameForError(), () -> {
            operation.setPaused(!operation.isPaused());
            String pauseResume = operation.isPaused() ? "Pausing" : "Resuming";
            writeInfoLog(pauseResume + " operation [" + operation.getName() + "]");
        });
    }

    private String getOperationNameForError() {
        return "[" + operation.getName() + "]";
    }

    @FXML
    private void stop(ActionEvent event) {
        executeRunner("Could not stop operation " + getOperationNameForError(), () -> {
            if (DialogUtility.showConfirmation("Are you sure you want to stop this operation?")) {
                operation.setStopped(true);
                writeInfoLog("Stopping operation [" + operation.getName() + "]");
            }
        });
    }
}