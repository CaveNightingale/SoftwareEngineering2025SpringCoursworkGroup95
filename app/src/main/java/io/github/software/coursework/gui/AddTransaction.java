package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AddTransaction extends VBox {
    @FXML
    private TextField title;

    @FXML
    private TextArea description;

    @FXML
    private DatePicker time;

    @FXML
    private TextField amount;

    @FXML
    private ComboBox<ReferenceItemPair<Entity>> entity;

    @FXML
    private TextField category;

    @FXML
    private TextField tags;

    @FXML
    private Label message;

    @FXML
    private Button submit;

    @FXML
    private Button delete;

    @FXML
    public Region deleteSpace;

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

    public final ObjectProperty<ObservableList<ReferenceItemPair<Entity>>> entityItemsProperty() {
        return entity.itemsProperty();
    }

    public final ObservableList<ReferenceItemPair<Entity>> getEntityItems() {
        return entityItemsProperty().get();
    }

    public final void setEntityItems(ObservableList<ReferenceItemPair<Entity>> value) {
        entityItemsProperty().set(value);
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
            protected void updateItem(ReferenceItemPair<Entity> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.item().name());
                }
            }
        });

        entity.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ReferenceItemPair<Entity> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.item().name());
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
    }

    public Transaction getTransaction() {
        return new Transaction(
                title.getText(),
                description.getText(),
                time.getValue().toEpochDay() * 86400000,
                new BigDecimal(amount.getText()).multiply(BigDecimal.valueOf(100)).longValue(),
                category.getText(),
                entity.getValue().reference(),
                ImmutableList.copyOf(tags.getText().split("\\s+"))
        );
    }

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    public void setTransaction(ImmutablePair<Transaction, Entity> transaction) {
        title.setText(transaction.getLeft().title());
        description.setText(transaction.getLeft().description());
        time.setValue(LocalDate.ofEpochDay(transaction.getLeft().time() / 86400000));
        amount.setText(BigDecimal.valueOf(transaction.getLeft().amount()).divide(BigDecimal.valueOf(100)).toString());
        entity.setValue(new ReferenceItemPair<>(transaction.getLeft().entity(), transaction.getRight()));
        category.setText(transaction.getLeft().category());
        tags.setText(String.join(" ", transaction.getLeft().tags()));
        submit.setText("Update");
        deleteSpace.setManaged(true);
        deleteSpace.setVisible(true);
        delete.setManaged(true);
        delete.setVisible(true);
    }

    public void handleMouseClick() {
        if (title.getText().isEmpty()) {
            message.setText("Title is required");
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
        if (time.getValue() == null) {
            message.setText("Time is required");
            return;
        }
        setDisable(true);
        fireEvent(new SubmitEvent(this, this, false));
    }

    public void handleDelete() {
        fireEvent(new SubmitEvent(this, this, true));
    }
}
