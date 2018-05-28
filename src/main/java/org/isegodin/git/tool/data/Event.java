package org.isegodin.git.tool.data;

/**
 * @author isegodin
 */
public class Event {

    private final String message;

    public Event(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
