package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

public class EntityItem extends AnchorPane {
    @FXML
    private Text name;

    private Storage storage;
    private Reference<Entity> entity;

    public EntityItem() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("EntityItem.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setStorage(Storage storage) {
        if (this.storage == storage) {
            return;
        }
        this.storage = storage;
        load();
    }

    public void setEntity(Reference<Entity> entity) {
        if (Objects.equals(this.entity, entity)) {
            return;
        }
        this.entity = entity;
        load();
    }

    public void load() {
        if (storage == null || entity == null) {
            return;
        }
        Entity entityValue = storage.getEntity(entity);
        name.setText(entityValue.name());
    }
}
