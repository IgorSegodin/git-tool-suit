package org.isegodin.git.tool.ui.alert;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @author isegodin
 */
public class AlertHelper {

    private static final Timer timer = new Timer();

    public static void showAlert(String text) {
        Platform.runLater(() -> {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);

            alert.setTitle("Git tool suit");
            alert.setHeaderText("New commits");
            alert.setContentText(text);

            stage.setOpacity(0);

            alert.show();

            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

            stage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - alert.getWidth() - 10);
            stage.setY(primaryScreenBounds.getMinY() + primaryScreenBounds.getHeight() - alert.getHeight() - 10);

            TimerTask closeTask = new TimerTask() {
                @Override
                public void run() {
                    boolean wasFocused = stage.focusedProperty().get();
                    if (!wasFocused) {
                        TimerTask outAnimationTask = new OpacityAnimationTask(false, stage, () -> {
                            Platform.runLater(alert::close);
                        });
                        timer.schedule(outAnimationTask, 0, 100);
                    }
                }
            };

            TimerTask inAnimationTask = new OpacityAnimationTask(true, stage, () -> {
                timer.schedule(closeTask, TimeUnit.SECONDS.toMillis(3));
            });

            timer.schedule(inAnimationTask, 0, 100);

            stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    closeTask.cancel();
                }
            });
        });
    }

    private static class OpacityAnimationTask extends TimerTask {

        private final double DELTA = 0.05;

        private final boolean in;
        private double opacity = 0;
        private final Window window;
        private final Runnable next;

        public OpacityAnimationTask(boolean in, Window window, Runnable next) {
            this.in = in;
            if (!in) {
                this.opacity = 1;
            }
            this.window = window;
            this.next = next;
        }

        @Override
        public void run() {
            if (in) {
                opacity += DELTA;
            } else {
                opacity -= DELTA;
            }

            if (opacity > 1) {
                opacity = 1;
            } else if (opacity < 0) {
                opacity = 0;
            }

            Platform.runLater(() -> {
                window.setOpacity(opacity);
            });

            if ((in && opacity == 1) || (!in && opacity == 0)) {
                if (next != null) {
                    next.run();
                }
                cancel();
            }
        }
    }
}
