package org.isegodin.git.tool.concurrent;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.isegodin.git.tool.data.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author isegodin
 */
public class GitRepositoryPollingThread extends Thread {

    private static Logger logger = LoggerFactory.getLogger(GitRepositoryPollingThread.class);

    private final Queue<Event> eventQueue;

    private final Git git;
    private final String branch;

    private volatile RevCommit lastCommit;

    public GitRepositoryPollingThread(Queue<Event> eventQueue, Git git, String branch) {
        this.eventQueue = eventQueue;
        this.git = git;
        this.branch = branch;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            List<Event> events = checkEvents();

            if (!events.isEmpty()) {
                eventQueue.addAll(events);
            }

            synchronized (this) {
                try {
                    wait(TimeUnit.SECONDS.toMillis(50));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<Event> checkEvents() {
        try {
            if (lastCommit == null) {
                lastCommit = getHeadCommit();
                return Collections.emptyList();
            }

            git.fetch().call();

            RevCommit headCommit = getHeadCommit();

            if (headCommit.getId().equals(lastCommit.getId())) {
                return Collections.emptyList();
            }

            Iterable<RevCommit> commitIterator = git.log().addRange(lastCommit.getId(), headCommit).call();

            LinkedList<RevCommit> commits = new LinkedList<>();
            commitIterator.forEach(commits::add);

            lastCommit = headCommit;

            return commits.stream().map(this::convertCommitToEvent).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Failed to check events", e);
        }

        return Collections.emptyList();
    }

    private Event convertCommitToEvent(RevCommit commit) {
        StringBuilder sb = new StringBuilder();
        sb.append(commit.getFullMessage());

        if (commit.getAuthorIdent() != null) {
            sb.append(" (").append(commit.getAuthorIdent().getName()).append(")");
        }
        return new Event(sb.toString());
    }

    private RevCommit getHeadCommit() {
        try {
            String branch = "refs/remotes/origin/" + this.branch;
            return git.log().add(git.getRepository().resolve(branch)).setMaxCount(1).call().iterator().next();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
