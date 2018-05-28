package org.isegodin.git.tool;

import com.jcraft.jsch.Session;
import javafx.application.Application;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.isegodin.git.tool.data.Event;
import org.isegodin.git.tool.ui.GitToolUiApplication;
import org.isegodin.git.tool.worker.GitRepositoryPollingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author isegodin
 */
public class App {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static final ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        Git repository = initRepository();

        new GitRepositoryPollingThread(eventQueue, repository).start();

        Application.launch(GitToolUiApplication.class, args);
    }

    public static Git initRepository() {
        String repoUrl = System.getProperty("repoUrl");
        String branch = Optional.ofNullable(System.getProperty("branch")).orElse("master");
        String projectName = repoUrl.substring(repoUrl.lastIndexOf("/") + 1, repoUrl.lastIndexOf(".git"));

        File workDir = new File(Optional.ofNullable(System.getProperty("workingFolder"))
                .orElseGet(() -> resolveAppDir().getAbsolutePath()));

        if (!workDir.exists() && !workDir.mkdir()) {
            throw new RuntimeException("Can not create folder: " + workDir.getAbsolutePath());
        }

        File projectsDir = new File(workDir, "projects");
        if (!projectsDir.exists() && !projectsDir.mkdir()) {
            throw new RuntimeException("Can not create folder: " + projectsDir.getAbsolutePath());
        }

        File projectDir = new File(projectsDir, projectName);

        SshSessionFactory.setInstance(new JschConfigSessionFactory() {
            public void configure(OpenSshConfig.Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
        });

        Git git;

        try {
            if (!projectDir.exists()) {
                git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(projectDir)
                        .setBranch(branch)
                        .call();

                logger.info("Repository \"{}\" cloned to \"{}\"", repoUrl, projectDir);
            } else {
                git = Git.open(projectDir);
                logger.info("Repository opened \"{}\"", projectDir);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return git;
    }

    private static File resolveAppDir() {
        try {
            URL jarLocation = App.class.getProtectionDomain().getCodeSource().getLocation();
            String path = jarLocation.getPath();
            return Paths.get(path.substring(0, path.lastIndexOf("/"))).toFile();
        } catch (Exception e) {
            throw new RuntimeException("Can not resolve app folder location.", e);
        }
    }
}
