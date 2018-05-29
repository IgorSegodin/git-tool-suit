package org.isegodin.git.tool;

import java.io.File;
import java.net.URL;

/**
 * @author isegodin
 */
public class App {

    public static void main(String[] args) {
        File appDir = resolveAppDir();
        System.setProperty("logFilePath", new File(appDir, "logs/").getAbsolutePath());

        new BootApp().boot(args, appDir);
    }

    private static File resolveAppDir() {
        try {
            URL jarLocation = App.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(jarLocation.toURI());
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Can not resolve app folder location.", e);
        }
    }
}
