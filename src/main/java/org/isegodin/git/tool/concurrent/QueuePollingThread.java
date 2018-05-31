package org.isegodin.git.tool.concurrent;

import org.isegodin.git.tool.data.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author isegodin
 */
public class QueuePollingThread extends Thread {

    private final Queue<Event> eventQueue;

    private final Consumer<List<Event>> eventConsumer;

    public QueuePollingThread(Queue<Event> eventQueue, Consumer<List<Event>> eventConsumer) {
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
