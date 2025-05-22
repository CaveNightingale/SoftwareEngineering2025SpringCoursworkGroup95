package io.github.software.coursework.gui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public final class EditItemController {

    @FXML
    private Node nameField;

    @FXML
    private Button submitButton;

    private final EditItemModel model;

    public EditItemController() {
        this.model = new EditItemModel();
    }

    private StringProperty nameFieldTextProperty() {
        return switch (nameField) {
            case TextField textField -> textField.textProperty();
            case Label label -> label.textProperty();
            default -> throw new IllegalStateException("Unexpected value: " + nameField);
        };
    }

    @FXML
    public void initialize() {
        nameFieldTextProperty().bindBidirectional(model.textProperty());
        submitButton.disableProperty().bind(Bindings.not(model.editableProperty()));

        submitButton.setOnAction(event -> handleSubmit());
    }

    public EditItemModel getModel() {
        return model;
    }

    @FXML
    private void handleSubmit() {
        if (model.getOnSubmit() != null) {
            model.getOnSubmit().handle(new EditItemModel.SubmitEvent(true, model.getText(), model));
        }
    }
}