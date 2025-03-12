package io.github.software.coursework.gui;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class SubmitEvent extends Event {
    public static final EventType<SubmitEvent> SUBMIT = new EventType<>(Event.ANY, "SUBMIT");

    public SubmitEvent() {
        super(SUBMIT);
    }

    public SubmitEvent(Object source, EventTarget target) {
        super(source, target, SUBMIT);
    }

    public SubmitEvent(Object source, EventTarget target, EventType<? extends Event> eventType) {
        super(source, target, eventType);
    }
}
