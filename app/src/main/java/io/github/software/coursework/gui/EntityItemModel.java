package io.github.software.coursework.gui;

import io.github.software.coursework.data.schema.Entity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class EntityItemModel {
    private final ObjectProperty<Entity> entity = new SimpleObjectProperty<>();

    public ObjectProperty<Entity> entityProperty() {
        return entity;
    }

    public void setEntity(Entity entity) {
        entityProperty().set(entity);
    }

    public Entity getEntity() {
        return entityProperty().get();
    }
}
