package io.github.software.coursework.gui;

import javafx.beans.property.*;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

public final class EditItemModel {

    private final StringProperty text = new SimpleStringProperty("");
    private final BooleanProperty editable = new SimpleBooleanProperty(true);
    private final ObjectProperty<EventHandler<SubmitEvent>> onSubmit = new SimpleObjectProperty<>();

    public StringProperty textProperty() {
        return text;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public BooleanProperty editableProperty() {
        return editable;
    }

    public boolean isEditable() {
        return editable.get();
    }

    public void setEditable(boolean editable) {
        this.editable.set(editable);
    }

    public ObjectProperty<EventHandler<SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public EventHandler<SubmitEvent> getOnSubmit() {
        return onSubmit.get();
    }

    public void setOnSubmit(EventHandler<SubmitEvent> handler) {
        this.onSubmit.set(handler);
    }

    public BooleanProperty insertMode = new SimpleBooleanProperty(false);

    public BooleanProperty insertModeProperty() {
        return insertMode;
    }

    public boolean isInsertMode() {
        return insertMode.get();
    }

    public void setInsertMode(boolean insertMode) {
        this.insertMode.set(insertMode);
    }

    public void submit() {
        if (getOnSubmit() != null) {
            getOnSubmit().handle(new SubmitEvent(isInsertMode(), getText(), this));
        }
    }

    public void reset() {
        setText("");
        setEditable(true);
    }

    public static final class SubmitEvent extends Event {
        public static final EventType<SubmitEvent> SUBMIT = new EventType<>(Event.ANY, "EDIT_ITEM_SUBMIT");
        private final String name;
        private final boolean insertMode;
        private final EditItemModel editItemModel;

        public SubmitEvent(boolean insertMode, String name, EditItemModel editItemModel) {
            super(SUBMIT);
            this.name = name;
            this.insertMode = insertMode;
            this.editItemModel = editItemModel;
        }

        public String getName() {
            return name;
        }

        public boolean isInsertMode() {
            return insertMode;
        }

        public EditItemModel getEditItemModel() {
            return editItemModel;
        }
    }
}