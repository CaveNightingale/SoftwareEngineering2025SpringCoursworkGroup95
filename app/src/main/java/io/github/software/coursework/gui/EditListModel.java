package io.github.software.coursework.gui;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;

public final class EditListModel {

    private final ObjectProperty<ObservableList<String>> names = new SimpleObjectProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<ObservableList<String>> protectedNames = new SimpleObjectProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<EventHandler<EditItemModel.SubmitEvent>> onSubmit = new SimpleObjectProperty<>();

    public ObservableList<String> getNames() {
        return names.get();
    }

    public void setNames(ObservableList<String> value) {
        names.set(value);
    }

    public ObjectProperty<ObservableList<String>> namesProperty() {
        return names;
    }

    public ObservableList<String> getProtectedNames() {
        return protectedNames.get();
    }

    public void setProtectedNames(ObservableList<String> value) {
        protectedNames.set(value);
    }

    public ObjectProperty<ObservableList<String>> protectedNamesProperty() {
        return protectedNames;
    }

    public EventHandler<EditItemModel.SubmitEvent> getOnSubmit() {
        return onSubmit.get();
    }

    public void setOnSubmit(EventHandler<EditItemModel.SubmitEvent> value) {
        onSubmit.set(value);
    }

    public ObjectProperty<EventHandler<EditItemModel.SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }
}