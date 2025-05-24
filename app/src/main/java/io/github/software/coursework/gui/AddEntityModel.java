package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.data.schema.Entity;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;

import java.util.concurrent.CompletableFuture;

public final class AddEntityModel {
    private final ObjectProperty<Model> model = new SimpleObjectProperty<>();

    public ObjectProperty<Model> modelProperty() {
        return model;
    }

    public Model getModel() {
        return model.get();
    }

    public void setModel(Model model) {
        this.model.set(model);
    }

    private final ObjectProperty<String> message = new SimpleObjectProperty<>("");

    public ObjectProperty<String> messageProperty() {
        return message;
    }

    public String getMessage() {
        return message.get();
    }

    private final ObjectProperty<EventHandler<SubmitEvent>> onSubmit = new SimpleObjectProperty<>();

    public ObjectProperty<EventHandler<SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public EventHandler<SubmitEvent> getOnSubmit() {
        return onSubmit.get();
    }

    public void setOnSubmit(EventHandler<SubmitEvent> handler) {
        onSubmit.set(handler);
    }

    private final ObjectProperty<String> name = new SimpleObjectProperty<>("");

    public ObjectProperty<String> nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    private final ObjectProperty<String> telephone = new SimpleObjectProperty<>("");

    public ObjectProperty<String> telephoneProperty() {
        return telephone;
    }

    public String getTelephone() {
        return telephone.get();
    }

    public void setTelephone(String telephone) {
        this.telephone.set(telephone);
    }

    private final ObjectProperty<String> email = new SimpleObjectProperty<>("");

    public ObjectProperty<String> emailProperty() {
        return email;
    }

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    private final ObjectProperty<String> address = new SimpleObjectProperty<>("");

    public ObjectProperty<String> addressProperty() {
        return address;
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    private final ObjectProperty<String> website = new SimpleObjectProperty<>("");

    public ObjectProperty<String> websiteProperty() {
        return website;
    }

    public String getWebsite() {
        return website.get();
    }

    public void setWebsite(String website) {
        this.website.set(website);
    }

    private final ObjectProperty<String> type = new SimpleObjectProperty<>("");

    public ObjectProperty<String> typeProperty() {
        return type;
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    private final BooleanProperty updating = new SimpleBooleanProperty(false);

    public BooleanProperty updatingProperty() {
        return updating;
    }

    public boolean isUpdating() {
        return updating.get();
    }

    public void setUpdating(boolean updating) {
        this.updating.set(updating);
    }

    private final BooleanProperty typePresent = new SimpleBooleanProperty(false);

    public BooleanProperty typePresentProperty() {
        return typePresent;
    }

    public boolean isTypePresent() {
        return typePresent.get();
    }

    public void setTypePresent(boolean typePresent) {
        if (suppressUpdate) {
            return;
        }
        this.typePresent.set(typePresent);
    }

    public void setEntity(Entity entity) {
        setName(entity.name());
        setTelephone(entity.telephone());
        setEmail(entity.email());
        setAddress(entity.address());
        setWebsite(entity.website());
        setType(getTypeDisplayName(entity.type()));
        setTypePresent(true);
        setUpdating(true);
    }

    public Entity getEntity() {
        String typeName = type.getValue().replaceAll("-", "").toUpperCase();
        return new Entity(
                getName(),
                getTelephone(),
                getEmail(),
                getAddress(),
                getWebsite(),
                typeName.isEmpty() ? Entity.Type.UNKNOWN : Entity.Type.valueOf(typeName)
        );
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

    private boolean suppressUpdate = false;
    private CompletableFuture<ImmutableIntArray> predictionTask;
    private final Helper.Debounce predict = Helper.debounce(() -> {
        if (getModel() == null) {
            return;
        }
        if (!isTypePresent() && !getName().isBlank()) {
            CompletableFuture<ImmutableIntArray> predictionTask0 = predictionTask = getModel().predictEntityTypes(ImmutableList.of(getEntity()));
            predictionTask0.thenAccept(result -> Platform.runLater(() -> {
                if (predictionTask == predictionTask0) {
                    suppressUpdate = true;
                    type.setValue(getTypeDisplayName(Entity.Type.values()[result.get(0)]));
                    suppressUpdate = false;
                }
            }));
        }
    });

    public void runPrediction() {
        if (suppressUpdate) {
            return;
        }
        if (predictionTask != null) {
            predictionTask.cancel(true);
            predictionTask = null;
        }
        predict.run();
    }

    public boolean validate() {
        if (getName().isBlank()) {
            message.set("Name is required");
            return false;
        }
        return true;
    }

    public void submit() {
        EventHandler<SubmitEvent> onSubmit = getOnSubmit();
        if (onSubmit != null) {
            onSubmit.handle(new SubmitEvent(false));
        }
    }
}