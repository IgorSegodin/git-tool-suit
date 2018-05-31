package org.isegodin.git.tool.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.isegodin.git.tool.concurrent.QueuePollingThread;
import org.isegodin.git.tool.context.ApplicationContext;
import org.isegodin.git.tool.data.Event;
import org.isegodin.git.tool.ui.alert.AlertHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.util.stream.Collectors;

/**
 * @author isegodin
 */
public class GitToolUiApplication extends Application {

    private static Logger logger = LoggerFactory.getLogger(GitToolUiApplication.class);

    @Override
    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(false);

        initTray();

        QueuePollingThread pollingThread = new QueuePollingThread(ApplicationContext.eventQueue, events -> {
            String message = events.stream().map(Event::getMessage).collect(Collectors.joining(System.lineSeparator()));
            logger.info("Received events: {}", message);
            AlertHelper.showAlert(message);
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

        MenuItem testItem = new MenuItem("Test");
        testItem.addActionListener(e -> {
            AlertHelper.showAlert("Show dialog");
        });

        PopupMenu popup = new PopupMenu();
        popup.add(testItem);
        popup.add(exitItem);

        Image trayImage = Toolkit.getDefaultToolkit().getImage(System.class.getResource("/ui/images/git-16x16.png"));
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

}
