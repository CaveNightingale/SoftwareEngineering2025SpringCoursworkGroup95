package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.schema.Entity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.layout.VBox;

import java.util.SequencedCollection;

public class EntityList extends VBox {
    private Storage storage;
    private SequencedCollection<Reference<Entity>> entities;

    private final ObjectProperty<EventHandler<EntityEditClickedEvent>> onEntityEditClicked = new SimpleObjectProperty<>();

    public final ObjectProperty<EventHandler<EntityEditClickedEvent>> onEntityEditClickedProperty() {
        return onEntityEditClicked;
    }

    public final EventHandler<EntityEditClickedEvent> getOnEntityEditClicked() {
        return onEntityEditClickedProperty().get();
    }

    public final void setOnEntityEditClicked(EventHandler<EntityEditClickedEvent> value) {
        onEntityEditClickedProperty().set(value);
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
    }

    public void setStorage(Storage storage) {
        if (this.storage == storage) {
            return;
        }
        this.storage = storage;
        load();
    }

    public void setEntities(SequencedCollection<Reference<Entity>> entity) {
        if (this.entities == entity) {
            return;
        }
        this.entities = entity;
        load();
    }

    public void load() {
        this.getChildren().clear();
        if (storage == null || entities == null) {
            return;
        }
        for (Reference<Entity> reference : entities) {
            EntityItem item = new EntityItem();
            item.setOnMouseClicked(event -> {
                this.fireEvent(new EntityEditClickedEvent(reference));
            });
            item.setStorage(storage);
            item.setEntity(reference);
            this.getChildren().add(item);
        }
    }

    public static class EntityEditClickedEvent extends Event {
        public static final EventType<EntityEditClickedEvent> CLICKED = new EventType<>(Event.ANY, "ENTITY_EDIT_CLICKED");

        private final Reference<Entity> reference;

        public EntityEditClickedEvent(Reference<Entity> reference) {
            super(CLICKED);
            this.reference = reference;
        }

        public Reference<Entity> getReference() {
            return reference;
        }
    }
}
