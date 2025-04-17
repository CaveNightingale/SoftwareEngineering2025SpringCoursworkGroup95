package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;

public class EntityList extends FlowPane {
    private final SimpleObjectProperty<ObservableList<ReferenceItemPair<Entity>>> items = new SimpleObjectProperty<>(this, "items");

    private final ObjectProperty<EventHandler<EntityEditClickedEvent>> onEntityEditClicked = new SimpleObjectProperty<>(this, "onEntityEditClicked");

    public final ObjectProperty<EventHandler<EntityEditClickedEvent>> onEntityEditClickedProperty() {
        return onEntityEditClicked;
    }

    public final EventHandler<EntityEditClickedEvent> getOnEntityEditClicked() {
        return onEntityEditClickedProperty().get();
    }

    public final void setOnEntityEditClicked(EventHandler<EntityEditClickedEvent> value) {
        onEntityEditClickedProperty().set(value);
    }

    public final SimpleObjectProperty<ObservableList<ReferenceItemPair<Entity>>> itemsProperty() {
        return items;
    }

    public final ObservableList<ReferenceItemPair<Entity>> getItems() {
        return itemsProperty().get();
    }

    public final void setItems(ObservableList<ReferenceItemPair<Entity>> value) {
        itemsProperty().set(value);
    }

    private EntityItem createItem(ReferenceItemPair<Entity> pair) {
        EntityItem item = new EntityItem();
        item.setFocusTraversable(true);
        item.setEntity(pair.item());
        item.setOnMouseClicked(event -> fireEvent(new EntityEditClickedEvent(pair.reference(), pair.item())));
        item.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                fireEvent(new EntityEditClickedEvent(pair.reference(), pair.item()));
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

    private void onListContentChange(ListChangeListener.Change<? extends ReferenceItemPair<Entity>> change) {
        while (change.next()) {
            if (change.wasAdded()) {
                int index = 0;
                for (ReferenceItemPair<Entity> pair : change.getAddedSubList()) {
                    getChildren().add(change.getFrom() + (index++), createItem(pair));
                }
            } else if (change.wasRemoved()) {
                getChildren().remove(change.getFrom(), change.getFrom() + change.getRemovedSize());
            }
        }
    }
    public EntityList() {
        this.onEntityEditClicked.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(EntityEditClickedEvent.CLICKED, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(EntityEditClickedEvent.CLICKED, newValue);
            }
        });
        itemsProperty().addListener((observable, oldValue, newValue) -> {
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

    public static class EntityEditClickedEvent extends Event {
        public static final EventType<EntityEditClickedEvent> CLICKED = new EventType<>(Event.ANY, "ENTITY_EDIT_CLICKED");

        private final Reference<Entity> reference;
        private final Entity entity;

        public EntityEditClickedEvent(Reference<Entity> reference, Entity entity) {
            super(CLICKED);
            this.reference = reference;
            this.entity = entity;
        }

        public Reference<Entity> getReference() {
            return reference;
        }

        public Entity getEntity() {
            return entity;
        }
    }
}
