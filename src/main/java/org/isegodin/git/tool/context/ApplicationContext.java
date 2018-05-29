package org.isegodin.git.tool.context;

import org.isegodin.git.tool.data.Event;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author i.segodin
 */
public class ApplicationContext {

    public static final ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
}
