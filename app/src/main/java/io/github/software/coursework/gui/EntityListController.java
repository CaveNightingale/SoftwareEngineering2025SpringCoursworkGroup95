package io.github.software.coursework.gui;

import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;

import java.io.IOException;

public final class EntityListController {
    @FXML
    private FlowPane root;

    private final EntityListModel model = new EntityListModel();

    public EntityListModel getModel() {
        return model;
    }

    private Node createItem(ReferenceItemPair<Entity> pair) {
        FXMLLoader loader = new FXMLLoader(EntityListController.class.getResource("EntityItem.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        EntityItemController itemController = loader.getController();
        EntityItemModel itemModel = itemController.getModel();
        itemModel.setEntity(pair.item());
        node.setFocusTraversable(true);
        node.setOnMouseClicked(event -> model.action(pair.reference(), pair.item()));
        node.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                model.action(pair.reference(), pair.item());
            }
        });
        node.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                node.getStyleClass().add("focused-item");
            } else {
                node.getStyleClass().remove("focused-item");
            }
        });
        return node;
    }

    private final ListChangeListener<ReferenceItemPair<Entity>> onListContentChange = change -> rebuildChildren();

    private void rebuildChildren() {
        root.getChildren().clear();
        model.getItems().forEach(pair -> root.getChildren().add(createItem(pair)));
    }

    @FXML
    public void initialize() {
        model.itemsProperty().addListener((observable, oldValue, newValue) -> {
            root.getChildren().clear();
            if (oldValue != null) {
                oldValue.removeListener(onListContentChange);
            }
            if (newValue != null) {
                newValue.forEach(pair -> root.getChildren().add(createItem(pair)));
                newValue.addListener(onListContentChange);
            }
        });
        model.setItems(FXCollections.observableArrayList());
    }
}
