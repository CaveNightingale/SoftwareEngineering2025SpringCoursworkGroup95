package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.util.Bitmask;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class AddTransactionModel {
    private final ObjectProperty<String> title = new SimpleObjectProperty<>("");

    public ObjectProperty<String> titleProperty() {
        return title;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    private final ObjectProperty<String> time = new SimpleObjectProperty<>(null);

    public ObjectProperty<String> timeProperty() {
        return time;
    }

    public String getTime() {
        return time.get();
    }

    public void setTime(String time) {
        this.time.set(time);
    }

    private final ObjectProperty<String> amount = new SimpleObjectProperty<>("");

    public ObjectProperty<String> amountProperty() {
        return amount;
    }

    public String getAmount() {
        return amount.get();
    }

    public void setAmount(String amount) {
        this.amount.set(amount);
    }

    private final ObjectProperty<String> titleError = new SimpleObjectProperty<>("");

    public ObjectProperty<String> titleErrorProperty() {
        return titleError;
    }

    public String getTitleError() {
        return titleError.get();
    }

    public void setTitleError(String titleError) {
        this.titleError.set(titleError);
    }

    private final ObjectProperty<String> timeError = new SimpleObjectProperty<>("");

    public ObjectProperty<String> timeErrorProperty() {
        return timeError;
    }

    public String getTimeError() {
        return timeError.get();
    }

    public void setTimeError(String timeError) {
        this.timeError.set(timeError);
    }

    private final ObjectProperty<String> amountError = new SimpleObjectProperty<>("");

    public ObjectProperty<String> amountErrorProperty() {
        return amountError;
    }

    public String getAmountError() {
        return amountError.get();
    }

    public void setAmountError(String amountError) {
        this.amountError.set(amountError);
    }

    private final ObjectProperty<String> entityError = new SimpleObjectProperty<>("");

    public ObjectProperty<String> entityErrorProperty() {
        return entityError;
    }

    public String getEntityError() {
        return entityError.get();
    }

    public void setEntityError(String entityError) {
        this.entityError.set(entityError);
    }

    private final ObjectProperty<ObservableList<ReferenceItemPair<Entity>>> availableEntities = new SimpleObjectProperty<>(null);

    public ObjectProperty<ObservableList<ReferenceItemPair<Entity>>> availableEntitiesProperty() {
        return availableEntities;
    }

    public ObservableList<ReferenceItemPair<Entity>> getAvailableEntities() {
        return availableEntities.get();
    }

    public void setAvailableEntities(ObservableList<ReferenceItemPair<Entity>> availableEntities) {
        this.availableEntities.set(availableEntities);
    }

    private final ObjectProperty<ObservableList<String>> availableCategories = new SimpleObjectProperty<>(null);

    public ObjectProperty<ObservableList<String>> availableCategoriesProperty() {
        return availableCategories;
    }

    public ObservableList<String> getAvailableCategories() {
        return availableCategories.get();
    }

    public void setAvailableCategories(ObservableList<String> availableCategories) {
        this.availableCategories.set(availableCategories);
    }

    private final ObjectProperty<ObservableList<String>> availableTags = new SimpleObjectProperty<>(null);

    public ObjectProperty<ObservableList<String>> availableTagsProperty() {
        return availableTags;
    }

    public ObservableList<String> getAvailableTags() {
        return availableTags.get();
    }

    public void setAvailableTags(ObservableList<String> availableTags) {
        this.availableTags.set(availableTags);
    }

    private final ObjectProperty<ReferenceItemPair<Entity>> entity = new SimpleObjectProperty<>(null);

    public ObjectProperty<ReferenceItemPair<Entity>> entityProperty() {
        return entity;
    }

    public ReferenceItemPair<Entity> getEntity() {
        return entity.get();
    }

    public void setEntity(ReferenceItemPair<Entity> entity) {
        this.entity.set(entity);
    }

    private final ObjectProperty<String> category = new SimpleObjectProperty<>("");

    public ObjectProperty<String> categoryProperty() {
        return category;
    }

    public String getCategory() {
        return category.get();
    }

    public void setCategory(String category) {
        this.category.set(category);
    }

    private final ObjectProperty<ObservableList<String>> tags = new SimpleObjectProperty<>(FXCollections.observableArrayList());

    public ObjectProperty<ObservableList<String>> tagsProperty() {
        return tags;
    }

    public ObservableList<String> getTags() {
        return tags.get();
    }

    public void setTags(ObservableList<String> tags) {
        this.tags.set(tags);
    }

    private final BooleanProperty categoryPresent = new SimpleBooleanProperty(false);

    public BooleanProperty categoryPresentProperty() {
        return categoryPresent;
    }

    public boolean isCategoryPresent() {
        return categoryPresent.get();
    }

    public void setCategoryPresent(boolean categoryPresent) {
        this.categoryPresent.set(categoryPresent);
    }

    private final BooleanProperty tagPresent = new SimpleBooleanProperty(false);

    public BooleanProperty tagPresentProperty() {
        return tagPresent;
    }

    public boolean isTagPresent() {
        return tagPresent.get();
    }

    public void setTagPresent(boolean tagPresent) {
        this.tagPresent.set(tagPresent);
    }

    private final ObjectProperty<String> note = new SimpleObjectProperty<>("");

    public ObjectProperty<String> noteProperty() {
        return note;
    }

    public String getNote() {
        return note.get();
    }

    public void setNote(String note) {
        this.note.set(note);
    }

    private final ObjectProperty<EventHandler<SubmitEvent>> onSubmit = new SimpleObjectProperty<>();

    public ObjectProperty<EventHandler<SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public EventHandler<SubmitEvent> getOnSubmit() {
        return onSubmit.get();
    }

    public void setOnSubmit(EventHandler<SubmitEvent> onSubmit) {
        this.onSubmit.set(onSubmit);
    }

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

    private LocalDate parseDate() {
        String dateStr = getTime();
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(dateStr.replace('/', '-'));
        } catch (DateTimeParseException ignored) {
        }
        try {
            String[] parts = dateStr.split("[/-]");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public Transaction getTransaction() {
        return new Transaction(
                getTitle(),
                getNote(),
                Objects.requireNonNull(parseDate()).toEpochDay() * 86400000,
                new BigDecimal(getAmount()).multiply(BigDecimal.valueOf(100)).longValue(),
                getCategory(),
                getEntity().reference(),
                ImmutableList.copyOf(getTags())
        );
    }

    private boolean validatePredictor() {
        return !getTitle().isEmpty() && !getCategory().isEmpty() && getEntity() != null &&
                parseDate() != null && getAmount().matches("[+\\-]?\\d+\\.?\\d{0,2}");
    }

    private boolean suppressUpdate = false;
    private CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>> predictionTask = null;
    private final Helper.Debounce predict = Helper.debounce(() -> {
        if (getModel() == null) {
            return;
        }
        if ((!isTagPresent() || !isCategoryPresent()) && validatePredictor() && !getAvailableCategories().isEmpty()) {
            CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>> task = predictionTask = getModel().predictCategoriesAndTags(
                    ImmutableList.of(getTransaction()),
                    ImmutableList.copyOf(getAvailableCategories()),
                    ImmutableList.copyOf(getAvailableTags())
            );
            task.thenAccept(result -> Platform.runLater(() -> {
                if (predictionTask == task) {
                    if (!isCategoryPresent()) {
                        suppressUpdate = true;
                        setCategory(getAvailableCategories().get(result.getLeft().get(0)));
                        suppressUpdate = false;
                    }
                    if (!isTagPresent()) {
                        Bitmask.View2D mask = result.getRight();
                        getTags().clear();
                        ArrayList<String> newTags = new ArrayList<>();
                        ObservableList<String> availableTags = getTags();
                        for (int i = 0; i < availableTags.size(); i++) {
                            if (mask.get(i, 0)) {
                                newTags.add(availableTags.get(i));
                            }
                        }
                        getTags().addAll(newTags);
                    }
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

    public boolean validateTitle() {
        if (getTitle().isEmpty()) {
            setTitleError("Title is required");
            return false;
        } else {
            setTitleError("");
            return true;
        }
    }

    public boolean validateTime() {
        if (getTime().isEmpty()) {
            setTimeError("Date is required");
            return false;
        } else {
            LocalDate date = parseDate();
            if (date == null) {
                setTimeError("Invalid date format");
                return false;
            } else {
                setTimeError("");
                return true;
            }
        }
    }

    public boolean validateAmount() {
        if (getAmount().isEmpty()) {
            setAmountError("Amount is required");
            return false;
        } else if (!getAmount().matches("[+\\-]?\\d+\\.?\\d{0,2}")) {
            setAmountError("Invalid amount format");
            return false;
        } else {
            setAmountError("");
            return true;
        }
    }

    public boolean validateEntity() {
        if (getEntity() == null) {
            setEntityError("Transaction Party is required");
            return false;
        } else {
            setEntityError("");
            return true;
        }
    }

    public boolean validate() {
        boolean hasError = !validateTitle();
        hasError |= !validateTime();
        hasError |= !validateAmount();
        hasError |= !validateEntity();
        return !hasError;
    }

    public void submit() {
        if (getOnSubmit() != null) {
            getOnSubmit().handle(new SubmitEvent(false));
        }
    }

    public void delete() {
        if (getOnSubmit() != null) {
            getOnSubmit().handle(new SubmitEvent(true));
        }
    }

    public void setTransaction(Transaction transaction) {
        setTitle(transaction.title());
        setNote(transaction.description());
        setTime(LocalDate.ofEpochDay(transaction.time() / 86400000).toString());
        setAmount(String.valueOf(transaction.amount() / 100.0));
        setCategory(transaction.category());
        setEntity(getAvailableEntities().stream().filter(e -> e.reference().equals(transaction.entity())).findFirst().orElse(null));
        setTags(FXCollections.observableArrayList(transaction.tags()));
        setUpdating(true);
        setTagPresent(true);
        setCategoryPresent(true);
    }
}