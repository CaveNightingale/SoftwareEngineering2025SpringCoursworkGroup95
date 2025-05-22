package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

public final class EntityListModel {
    private final SimpleObjectProperty<ObservableList<ReferenceItemPair<Entity>>> items = new SimpleObjectProperty<>(this, "items");

    private final ObjectProperty<EventHandler<EntityActionEvent>> onAction = new SimpleObjectProperty<>(this, "onEntityEditClicked");

    public ObjectProperty<EventHandler<EntityActionEvent>> onActionProperty() {
        return onAction;
    }

    public EventHandler<EntityActionEvent> getOnAction() {
        return onActionProperty().get();
    }

    public void setOnAction(EventHandler<EntityActionEvent> value) {
        onActionProperty().set(value);
    }

    public SimpleObjectProperty<ObservableList<ReferenceItemPair<Entity>>> itemsProperty() {
        return items;
    }

    public ObservableList<ReferenceItemPair<Entity>> getItems() {
        return itemsProperty().get();
    }

    public void setItems(ObservableList<ReferenceItemPair<Entity>> value) {
        itemsProperty().set(value);
    }

    public void action(Reference<Entity> reference, Entity entity) {
        if (getOnAction() != null) {
            getOnAction().handle(new EntityActionEvent(reference, entity));
        }
    }

    public static class EntityActionEvent extends Event {
        public static final EventType<EntityActionEvent> CLICKED = new EventType<>(Event.ANY, "ENTITY_EDIT_ACTION");

        private final Reference<Entity> reference;
        private final Entity entity;

        public EntityActionEvent(Reference<Entity> reference, Entity entity) {
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
