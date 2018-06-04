package org.isegodin.git.tool;

import com.jcraft.jsch.Session;
import javafx.application.Application;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.isegodin.git.tool.context.ApplicationContext;
import org.isegodin.git.tool.ui.GitToolUiApplication;
import org.isegodin.git.tool.concurrent.GitRepositoryPollingThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * @author isegodin
 */
public class BootApp {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public void boot(String[] args, File appDir) {
        String repoUrl = System.getProperty("repoUrl");
        String branch = Optional.ofNullable(System.getProperty("branch")).orElse("master");

        Git repository = initRepository(appDir, repoUrl, branch);

        new GitRepositoryPollingThread(ApplicationContext.eventQueue, repository, branch).start();

        Application.launch(GitToolUiApplication.class, args);
    }

    private Git initRepository(final File appDir, String repoUrl, String branch) {
        String projectName = repoUrl.substring(repoUrl.lastIndexOf("/") + 1, repoUrl.lastIndexOf(".git"));

        File workDir = new File(Optional.ofNullable(System.getProperty("workingFolder"))
                .orElseGet(() -> appDir.getAbsolutePath()));

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

                logger.info("Repository \"{}\" branch \"{}\" cloned to \"{}\"", repoUrl, branch, projectDir);
            } else {
                git = Git.open(projectDir);
                logger.info("Repository branch \"{}\" opened \"{}\"", branch, projectDir);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return git;
    }
}
