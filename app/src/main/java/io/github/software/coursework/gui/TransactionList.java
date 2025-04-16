package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class TransactionList extends VBox {
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

    private final SimpleObjectProperty<ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>>> items = new SimpleObjectProperty<>(this, "items");

    public final SimpleObjectProperty<ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>>> itemsProperty() {
        return items;
    }

    public final ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> getItems() {
        return itemsProperty().get();
    }

    public final void setItems(ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> value) {
        itemsProperty().set(value);
    }

    private TransactionItem createItem(ImmutablePair<ReferenceItemPair<Transaction>, Entity> pair) {
        TransactionItem item = new TransactionItem();
        item.setTransaction(ImmutablePair.of(pair.getLeft().item(), pair.getRight().name()));
        item.setFocusTraversable(true);
        item.setOnMouseClicked(event -> fireEvent(new TransactionEditClickedEvent(pair.getLeft().reference(), pair.getLeft().item(), pair.getRight())));
        item.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                fireEvent(new TransactionEditClickedEvent(pair.getLeft().reference(), pair.getLeft().item(), pair.getRight()));
            }
        });
        item.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                item.getStyleClass().add("focused-item");
            } else {
                item.getStyleClass().remove("focused-item");
            }
        });
        return item;
    }

    private void onListContentChange(ListChangeListener.Change<? extends ImmutablePair<ReferenceItemPair<Transaction>, Entity>> change) {
        while (change.next()) {
            if (change.wasAdded()) {
                int index = 0;
                for (ImmutablePair<ReferenceItemPair<Transaction>, Entity> pair : change.getAddedSubList()) {
                    getChildren().add(change.getFrom() + (index++), createItem(pair));
                }
            } else if (change.wasRemoved()) {
                getChildren().remove(change.getFrom(), change.getFrom() + change.getRemovedSize());
            }
        }
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
        this.itemsProperty().addListener((observable, oldValue, newValue) -> {
            getChildren().clear();
            if (oldValue != null) {
                oldValue.removeListener(this::onListContentChange);
            }
            if (newValue != null) {
                newValue.forEach(pair -> getChildren().add(createItem(pair)));
                newValue.addListener(this::onListContentChange);
            }
        });
        setItems(FXCollections.observableArrayList());
    }

    public static class TransactionEditClickedEvent extends Event {
        public static final EventType<TransactionEditClickedEvent> CLICKED = new EventType<>(Event.ANY, "TRANSACTION_EDIT_CLICKED");

        private final Reference<Transaction> reference;
        private final Transaction transaction;
        private final Entity entity;

        public TransactionEditClickedEvent(Reference<Transaction> reference, Transaction transaction, Entity entity) {
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
