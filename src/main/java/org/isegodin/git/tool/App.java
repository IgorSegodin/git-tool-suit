package org.isegodin.git.tool;

import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author isegodin
 */
public class App {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
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

        Repository repository;

        if (!projectDir.exists()) {
            Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(projectDir)
                    .setBranch(branch)
                    .call();

            repository = git.getRepository();

            logger.info("Repository \"{}\" cloned to \"{}\"", repoUrl, projectDir);
        } else {
            Git git = Git.open(projectDir);
            repository = git.getRepository();
            logger.info("Repository opened \"{}\"", projectDir);
        }

        System.out.println(repository.getBranch());
        System.out.println(repository.getFullBranch());
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
