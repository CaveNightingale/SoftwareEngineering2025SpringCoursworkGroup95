package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.schema.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.layout.VBox;

import java.util.SequencedCollection;

public class TransactionList extends VBox {
    private Storage storage;
    private SequencedCollection<Reference<Transaction>> transactions;

    private final ObjectProperty<EventHandler<TransactionEditClickedEvent>> onTransactionEditClicked = new SimpleObjectProperty<>();

    public final ObjectProperty<EventHandler<TransactionEditClickedEvent>> onTransactionEditClickedProperty() {
        return onTransactionEditClicked;
    }

    public final EventHandler<TransactionEditClickedEvent> getOnTransactionEditClicked() {
        return onTransactionEditClickedProperty().get();
    }

    public final void setOnTransactionEditClicked(EventHandler<TransactionEditClickedEvent> value) {
        onTransactionEditClickedProperty().set(value);
    }

    public TransactionList() {
        this.onTransactionEditClicked.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(TransactionEditClickedEvent.CLICKED, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(TransactionEditClickedEvent.CLICKED, newValue);
            }
        });
    }

    public void setStorage(Storage storage) {
        if (this.storage == storage) {
            return;
        }
        this.storage = storage;
        load();
    }

    public void setTransactions(SequencedCollection<Reference<Transaction>> transactions) {
        if (this.transactions == transactions) {
            return;
        }
        this.transactions = transactions;
        load();
    }

    public void load() {
        this.getChildren().clear();
        if (storage == null || transactions == null) {
            return;
        }
        for (Reference<Transaction> reference : transactions) {
            TransactionItem item = new TransactionItem();
            item.setOnMouseClicked(event -> {
                this.fireEvent(new TransactionEditClickedEvent(reference));
            });
            item.setStorage(storage);
            item.setTransaction(reference);
            this.getChildren().add(item);
        }
    }

    public static class TransactionEditClickedEvent extends Event {
        public static final EventType<TransactionEditClickedEvent> CLICKED = new EventType<>(Event.ANY, "TRANSACTION_EDIT_CLICKED");

        private final Reference<Transaction> reference;

        public TransactionEditClickedEvent(Reference<Transaction> reference) {
            super(CLICKED);
            this.reference = reference;
        }

        public Reference<Transaction> getReference() {
            return reference;
        }
    }
}
