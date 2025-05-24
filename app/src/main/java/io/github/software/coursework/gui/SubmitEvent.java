package io.github.software.coursework.gui;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Event that is fired when the user submits a form.
 */
public final class SubmitEvent extends Event {
    public static final EventType<SubmitEvent> SUBMIT = new EventType<>(Event.ANY, "SUBMIT");
    private final boolean delete;

    /**
     * Creates a new SubmitEvent.
     * @param delete true if the user clicked the delete button, false if the user clicked the submit/update/add button.
     */
    public SubmitEvent(boolean delete) {
        super(SUBMIT);
        this.delete = delete;
    }

    public SubmitEvent(Object source, EventTarget target, boolean delete) {
        super(source, target, SUBMIT);
        this.delete = delete;
    }

    /**
     * Returns true if the user clicked the delete button, false if the user clicked the submit/update/add button.
     * @return If the user clicked the delete button.
     */
    public boolean isDelete() {
        return delete;
    }
}
