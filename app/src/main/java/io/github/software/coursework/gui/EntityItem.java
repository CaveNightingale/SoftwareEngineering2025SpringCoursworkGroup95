package io.github.software.coursework.gui;

import io.github.software.coursework.data.schema.Entity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.io.IOException;

public class EntityItem extends AnchorPane {
    @FXML
    private Text name;

    private final ObjectProperty<Entity> entity = new SimpleObjectProperty<>();

    public final ObjectProperty<Entity> entityProperty() {
        return entity;
    }

    public final void setEntity(Entity entity) {
        entityProperty().set(entity);
    }

    public final Entity getEntity() {
        return entityProperty().get();
    }

    public EntityItem() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("EntityItem.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        entityProperty().addListener((observable, oldValue, newValue) -> name.setText(newValue.name()));
    }
}
