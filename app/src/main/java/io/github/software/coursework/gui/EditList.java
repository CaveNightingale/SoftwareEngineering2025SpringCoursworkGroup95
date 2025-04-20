package io.github.software.coursework.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class EditList extends FlowPane {
    private final ObjectProperty<EventHandler<EditItem.SubmitEvent>> onSubmit = new SimpleObjectProperty<>(this, "onSubmit");
    public final ObjectProperty<EventHandler<EditItem.SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public final EventHandler<EditItem.SubmitEvent> getOnSubmit() {
        return onSubmitProperty().get();
    }

    public final void setOnSubmit(EventHandler<EditItem.SubmitEvent> value) {
        onSubmitProperty().set(value);
    }

    private final ObjectProperty<ObservableList<String>> names = new SimpleObjectProperty<>(this, "names", FXCollections.observableArrayList());
    public final ObjectProperty<ObservableList<String>> namesProperty() {
        return names;
    }

    public final ObservableList<String> getNames() {
        return namesProperty().get();
    }

    public final void setNames(ObservableList<String> value) {
        namesProperty().set(value);
    }

    private final ObjectProperty<ObservableList<String>> protectedNames = new SimpleObjectProperty<>(this, "protectedNames", FXCollections.observableArrayList());
    public final ObjectProperty<ObservableList<String>> protectedNamesProperty() {
        return protectedNames;
    }

    public final ObservableList<String> getProtectedNames() {
        return protectedNamesProperty().get();
    }

    public final void setProtectedNames(ObservableList<String> value) {
        protectedNamesProperty().set(value);
    }

    private final EditItem insertModeItem = new EditItem(true);

    private void buildChildren() {
        getChildren().clear();
        ArrayList<String> names = new ArrayList<>(getNames());
        names.sort(String::compareTo);
        HashSet<String> protectedNames = new HashSet<>(getProtectedNames());
        for (String name : names) {
            EditItem item = new EditItem(false);
            item.setText(name);
            item.setEditable(!protectedNames.contains(name));
            getChildren().add(item);
        }
        getChildren().add(insertModeItem);
        insertModeItem.setEditable(!names.contains(insertModeItem.getText()) && !insertModeItem.getText().isBlank());
    }

    private final ListChangeListener<String> namesListener = change -> buildChildren();

    public EditList() {
        getChildren().add(new EditItem(true));
        onSubmitProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                removeEventHandler(EditItem.SubmitEvent.SUBMIT, oldValue);
            }
            if (newValue != null) {
                addEventHandler(EditItem.SubmitEvent.SUBMIT, newValue);
            }
        });
        namesProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(namesListener);
            }
            if (newValue != null) {
                newValue.addListener(namesListener);
            }
            buildChildren();
        });
        protectedNamesProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(namesListener);
            }
            if (newValue != null) {
                newValue.addListener(namesListener);
            }
            buildChildren();
        });
        insertModeItem.textProperty().addListener((observable, oldValue, newValue) -> {
            insertModeItem.setEditable(!getNames().contains(newValue) && !newValue.isBlank());
        });
        getNames().addListener(namesListener);
        getProtectedNames().addListener(namesListener);
        buildChildren();
    }
}
