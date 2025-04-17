package io.github.software.coursework.gui;

import javafx.beans.property.*;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class EditItem extends AnchorPane {
    @FXML
    private Node name;

    @FXML
    private Button submit;

    public StringProperty textProperty() {
        return name instanceof TextField field ? field.textProperty() : ((Label) name).textProperty();
    }

    public String getText() {
        return textProperty().get();
    }

    public void setText(String text) {
        textProperty().set(text);
    }

    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);
    public final BooleanProperty editableProperty() {
        return editable;
    }

    public final boolean isEditable() {
        return editableProperty().get();
    }

    public final void setEditable(boolean value) {
        editableProperty().set(value);
    }

    private final ObjectProperty<EventHandler<SubmitEvent>> onSubmit = new SimpleObjectProperty<>();

    public final ObjectProperty<EventHandler<SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public final EventHandler<SubmitEvent> getOnSubmit() {
        return onSubmitProperty().get();
    }

    public final void setOnSubmit(EventHandler<SubmitEvent> value) {
        onSubmitProperty().set(value);
    }

    public EditItem(boolean insertMode) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource(insertMode ? "EditItemInsertMode.fxml" : "EditItemDeleteMode.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        onSubmitProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                removeEventHandler(SubmitEvent.SUBMIT, oldValue);
            }
            if (newValue != null) {
                addEventHandler(SubmitEvent.SUBMIT, newValue);
            }
        });
        editableProperty().addListener((observable, oldValue, newValue) -> {
            submit.setDisable(!newValue);
        });
    }

    public void handleAction() {
        setDisable(true);
        Event.fireEvent(this, new SubmitEvent(name instanceof TextField, getText()));
    }

    public class SubmitEvent extends Event {
        public static final EventType<SubmitEvent> SUBMIT = new EventType<>(Event.ANY, "EDIT_ITEM_SUBMIT");
        private final String name;
        private final boolean insertMode;

        public SubmitEvent(boolean insertMode, String name) {
            super(SUBMIT);
            this.name = name;
            this.insertMode = insertMode;
        }

        public String getName() {
            return name;
        }

        public void reset() {
            setDisable(false);
            setText("");
        }

        public boolean isInsertMode() {
            return insertMode;
        }
    }
}
