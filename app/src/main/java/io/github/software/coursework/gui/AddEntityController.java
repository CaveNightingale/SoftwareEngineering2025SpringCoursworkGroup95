package io.github.software.coursework.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.controlsfx.control.SearchableComboBox;

public final class AddEntityController {
    @FXML
    private VBox root;

    @FXML
    private TextField name;

    @FXML
    private TextField telephone;

    @FXML
    private TextField email;

    @FXML
    private TextField address;

    @FXML
    private TextField website;

    @FXML
    private SearchableComboBox<String> type;

    @FXML
    private Label message;

    @FXML
    private Button submit;

    private final AddEntityModel model;

    public AddEntityModel getModel() {
        return model;
    }

    public AddEntityController() {
        model = new AddEntityModel();
    }

    @FXML
    public void initialize() {
        name.textProperty().bindBidirectional(model.nameProperty());
        telephone.textProperty().bindBidirectional(model.telephoneProperty());
        email.textProperty().bindBidirectional(model.emailProperty());
        address.textProperty().bindBidirectional(model.addressProperty());
        website.textProperty().bindBidirectional(model.websiteProperty());
        type.valueProperty().bindBidirectional(model.typeProperty());
        message.textProperty().bindBidirectional(model.messageProperty());
        model.updatingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                submit.setText("Update");
            } else {
                submit.setText("Add");
            }
        });
    }

    public void handleMouseClick() {
        if (model.validate()) {
            root.setDisable(true);
            model.submit();
        }
    }

    public void handleTypeInput() {
        model.setTypePresent(true);
        model.runPrediction();
    }

    public void handlePredictorInput() {
        model.runPrediction();
    }
}
