package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class TransactionListModel extends VBox {
    private final ObjectProperty<EventHandler<TransactionActionEvent>> onAction = new SimpleObjectProperty<>();
    public ObjectProperty<EventHandler<TransactionActionEvent>> onActionProperty() {
        return onAction;
    }

    public EventHandler<TransactionActionEvent> getOnAction() {
        return onActionProperty().get();
    }

    public void setOnAction(EventHandler<TransactionActionEvent> value) {
        onActionProperty().set(value);
    }

    private final SimpleObjectProperty<ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>>> items = new SimpleObjectProperty<>(this, "items");

    public SimpleObjectProperty<ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>>> itemsProperty() {
        return items;
    }

    public ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> getItems() {
        return itemsProperty().get();
    }

    public void setItems(ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> value) {
        itemsProperty().set(value);
    }

    public void action(Reference<Transaction> reference, Transaction transaction, Entity entity) {
        if (getOnAction() != null) {
            getOnAction().handle(new TransactionActionEvent(reference, transaction, entity));
        }
    }

    public static class TransactionActionEvent extends Event {
        public static final EventType<TransactionActionEvent> CLICKED = new EventType<>(Event.ANY, "TRANSACTION_EDIT_CLICKED");

        private final Reference<Transaction> reference;
        private final Transaction transaction;
        private final Entity entity;

        public TransactionActionEvent(Reference<Transaction> reference, Transaction transaction, Entity entity) {
            super(CLICKED);
            this.reference = reference;
            this.transaction = transaction;
            this.entity = entity;
        }

        public Reference<Transaction> getReference() {
            return reference;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public Entity getEntity() {
            return entity;
        }
    }
}