package io.github.software.coursework.gui;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class SubmitEvent extends Event {
    public static final EventType<SubmitEvent> SUBMIT = new EventType<>(Event.ANY, "SUBMIT");
    private final boolean delete;

    public SubmitEvent(boolean delete) {
        super(SUBMIT);
        this.delete = delete;
    }

    public SubmitEvent(Object source, EventTarget target, boolean delete) {
        super(source, target, SUBMIT);
        this.delete = delete;
    }

    public boolean isDelete() {
        return delete;
    }
}
