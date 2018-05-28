package org.isegodin.git.tool.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.isegodin.git.tool.App;
import org.isegodin.git.tool.data.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author isegodin
 */
public class GitToolUiApplication extends Application {

    private static Logger logger = LoggerFactory.getLogger(GitToolUiApplication.class);

    private Timer timer = new Timer();

    @Override
    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(false);

        initTray();

        PollingThread pollingThread = new PollingThread(App.eventQueue, events -> {
            String message = events.stream().map(Event::getMessage).collect(Collectors.joining(System.lineSeparator()));
            logger.info("Received events: {}", message);
            showDialog(message);
        });

        pollingThread.start();
    }

    private void initTray() {
        if (!SystemTray.isSupported()) {
            logger.error("SystemTray is not supported");
            System.exit(0);
            return;
        }

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            System.exit(0);
        });

        PopupMenu popup = new PopupMenu();
        popup.add(exitItem);

        Image trayImage = Toolkit.getDefaultToolkit().getImage(System.class.getResource("/ui/images/tray.png"));
        TrayIcon trayIcon = new TrayIcon(trayImage);
        trayIcon.setPopupMenu(popup);

        try {
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(trayIcon);
        } catch (AWTException e) {
            logger.error("TrayIcon could not be added.", e);
            System.exit(0);
        }
    }

    private void showDialog(String text) {
        Platform.runLater(() -> {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);

            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);

            alert.setTitle("Git tool suit");
            alert.setHeaderText("Received events");
            alert.setContentText(text);
            alert.show();

            TimerTask closeTask = new TimerTask() {
                @Override
                public void run() {
                    boolean wasFocused = stage.focusedProperty().get();
                    if (!wasFocused) {
                        Platform.runLater(alert::close);
                    }
                }
            };

            stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    closeTask.cancel();
                }
            });

            timer.schedule(closeTask, TimeUnit.SECONDS.toMillis(3));
        });
    }

    private class PollingThread extends Thread {

        private final Queue<Event> eventQueue;

        private final Consumer<List<Event>> eventConsumer;

        public PollingThread(Queue<Event> eventQueue, Consumer<List<Event>> eventConsumer) {
            this.eventQueue = eventQueue;
            this.eventConsumer = eventConsumer;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                List<Event> events = new LinkedList<>();

                while (events.size() <= 10 && !eventQueue.isEmpty()) {
                    events.add(eventQueue.poll());
                }

                if (!events.isEmpty()) {
                    eventConsumer.accept(events);
                }

                if (!eventQueue.isEmpty()) {
                    continue;
                }

                synchronized (this) {
                    try {
                        wait(TimeUnit.MINUTES.toMillis(1));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
