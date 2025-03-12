package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.math.BigDecimal;

public class AddTransaction extends VBox {
    private record Option(Reference<Entity> ref, String value) {}

    @FXML
    private TextField title;

    @FXML
    private TextField description;

    @FXML
    private DatePicker time;

    @FXML
    private TextField amount;

    @FXML
    private ComboBox<Option> entity;

    @FXML
    private TextField category;

    @FXML
    private TextField tags;

    @FXML
    private Text message;

    private Storage storage;

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

    public AddTransaction() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("AddTransaction.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        entity.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Option item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.value());
                }
            }
        });

        entity.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Option item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.value());
                }
            }
        });

        this.onSubmit.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(SubmitEvent.SUBMIT, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(SubmitEvent.SUBMIT, newValue);
            }
        });

        load();
    }

    public void setStorage(Storage storage) {
        if (this.storage == storage) {
            return;
        }
        this.storage = storage;
        load();
    }

    public void load() {
        entity.getItems().clear();
        if (storage == null) {
            return;
        }
        for (Reference<Entity> reference : storage.getEntities()) {
            entity.getItems().add(new Option(reference, storage.getEntity(reference).name()));
        }
    }

    public Transaction getTransaction() {
        return new Transaction(
                title.getText(),
                description.getText(),
                time.getValue().toEpochDay() * 86400000,
                new BigDecimal(amount.getText()).multiply(BigDecimal.valueOf(100)).longValue(),
                category.getText(),
                entity.getValue().ref(),
                ImmutableList.copyOf(tags.getText().split("\\s+"))
        );
    }

    public void handleMouseClick() {
        if (storage == null) {
            return;
        }
        if (title.getText().isEmpty()) {
            message.setText("Title is required");
            return;
        }
        if (time.getValue() == null) {
            message.setText("Time is required");
            return;
        }
        if (amount.getText().isEmpty()) {
            message.setText("Amount is required");
            return;
        }
        if (!amount.getText().matches("[+\\-]?\\d+\\.?\\d{0,2}")) {
            message.setText("Amount is invalid");
            return;
        }
        if (entity.getValue() == null) {
            message.setText("Entity is required");
            return;
        }
    fireEvent(new SubmitEvent(this, this, SubmitEvent.SUBMIT));
    }
}
