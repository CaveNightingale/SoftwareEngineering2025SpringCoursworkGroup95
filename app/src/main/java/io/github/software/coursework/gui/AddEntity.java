package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.data.schema.Entity;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AddEntity extends VBox {

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
    private ComboBox<String> type;

    @FXML
    private Label message;

    @FXML
    private Button submit;

    private final Model model;

    private boolean typePresent = false;

    private CompletableFuture<ImmutableIntArray> typePredictionTask = null;

    private final ObjectProperty<EventHandler<SubmitEvent>> onSubmit = new SimpleObjectProperty<>();

    public final ObjectProperty<EventHandler<SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public final EventHandler<SubmitEvent> getOnSubmit() {
        return onSubmitProperty().get();
    }

    public final void setOnSubmit(EventHandler<SubmitEvent> value) {
        onSubmitProperty().set(value);
    }

    public AddEntity(@Nullable Entity entity, Model model) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("AddEntity.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.model = model;

        this.onSubmit.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(SubmitEvent.SUBMIT, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(SubmitEvent.SUBMIT, newValue);
            }
        });

        if (entity != null) {
            name.setText(entity.name());
            telephone.setText(entity.telephone());
            email.setText(entity.email());
            address.setText(entity.address());
            website.setText(entity.website());
            type.setValue(getTypeDisplayName(entity.type()));
            submit.setText("Update");
        }
    }

    private static String getTypeDisplayName(Entity.Type entity) {
        return switch (entity) {
            case UNKNOWN -> "";
            case INDIVIDUAL -> "Individual";
            case EDUCATION -> "Education";
            case GOVERNMENT -> "Government";
            case COMMERCIAL -> "Commercial";
            case NONPROFIT -> "Non-profit";
        };
    }

    public Entity getEntity() {
        String typeName = type.getValue().replaceAll("-", "").toUpperCase();
        return new Entity(
                name.getText(),
                telephone.getText(),
                email.getText(),
                address.getText(),
                website.getText(),
                typeName.isEmpty() ? Entity.Type.UNKNOWN : Entity.Type.valueOf(typeName)
        );
    }

    public void handleMouseClick() {
        if (name.getText().isBlank()) {
            message.setText("Name is required");
            return;
        }
        setDisable(true);
        fireEvent(new SubmitEvent(this, this, false));
    }

    public void handleTypeInput() {
        if (typePredictionTask != null) {
            typePredictionTask.cancel(true);
            typePredictionTask = null;
        }
        typePresent = true;
    }

    public void handleNonTypeInput() {
        if (!typePresent && !name.getText().isBlank()) {
            if (typePredictionTask != null) {
                typePredictionTask.cancel(true);
            }
            CompletableFuture<ImmutableIntArray> predictionTask = typePredictionTask = model.predictEntityTypes(ImmutableList.of(getEntity()));
            typePredictionTask.thenAccept(result -> {
                Platform.runLater(() -> {
                    if (predictionTask == typePredictionTask) {
                        System.out.println("predict " + result.get(0));
                        type.setValue(getTypeDisplayName(Entity.Type.values()[result.get(0)]));
                    }
                });
            });
        }
    }
}
