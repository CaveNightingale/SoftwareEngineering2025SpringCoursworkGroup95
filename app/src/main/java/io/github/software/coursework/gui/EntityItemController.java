package io.github.software.coursework.gui;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public final class EntityItemController {
    @FXML
    private Text name;

    private final EntityItemModel model = new EntityItemModel();

    public EntityItemModel getModel() {
        return model;
    }

    @FXML
    public void initialize() {
        model.entityProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                name.setText(newValue.name());
            } else {
                name.setText("");
            }
        });
    }
}
