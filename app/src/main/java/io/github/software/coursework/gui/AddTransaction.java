package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.util.Bitmask;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.controlsfx.control.SearchableComboBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.software.coursework.gui.Helper.*;

public class AddTransaction extends VBox {
    @FXML
    private TextField title;

    @FXML
    private TextArea description;

    @FXML
    private DatePicker time;

    @FXML
    private TextField amount;

    @FXML
    private SearchableComboBox<ReferenceItemPair<Entity>> entity;

    @FXML
    private SearchableComboBox<String> category;

    @FXML
    private SearchableComboBox<String> addTags;

    @FXML
    private Label message;

    @FXML
    private Button submit;

    @FXML
    private Button delete;

    @FXML
    private Region deleteSpace;

    @FXML
    private HBox tagsRow;

    @FXML
    private HBox auxTagsRow;

    @FXML
    private Label clickToRemove;

    private final ArrayList<String> selectedTags = new ArrayList<>();

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

    public final ObjectProperty<ObservableList<ReferenceItemPair<Entity>>> entityItemsProperty() {
        return entity.itemsProperty();
    }

    public final ObservableList<ReferenceItemPair<Entity>> getEntityItems() {
        return entityItemsProperty().get();
    }

    public final void setEntityItems(ObservableList<ReferenceItemPair<Entity>> value) {
        entityItemsProperty().set(value);
    }

    private final SimpleObjectProperty<ObservableList<String>> categoryItems = new SimpleObjectProperty<>(this, "categoryItems");
    public final ObjectProperty<ObservableList<String>> categoryItemsProperty() {
        return categoryItems;
    }

    public final ObservableList<String> getCategoryItems() {
        return categoryItemsProperty().get();
    }

    public final void setCategoryItems(ObservableList<String> value) {
        categoryItemsProperty().set(value);
    }

    private final SimpleObjectProperty<ObservableList<String>> tagsItems = new SimpleObjectProperty<>(this, "tagsItems");
    public final ObjectProperty<ObservableList<String>> tagItemsProperty() {
        return tagsItems;
    }

    public final ObservableList<String> getTagItems() {
        return tagItemsProperty().get();
    }

    public final void setTagItems(ObservableList<String> value) {
        tagItemsProperty().set(value);
    }

    // A workaround for ControlsFX's ArrayIndexOutOfBoundsException
    // Just avoid emitting the list change listener on the list by directly setting a new list
    private void updateCategoryItems() {
        String oldValue = category.getValue();
        category.setValue(null);
        category.setItems(FXCollections.observableArrayList(getCategoryItems()));
        if (oldValue != null && getCategoryItems().contains(oldValue)) {
            category.setValue(oldValue);
        }
    }

    private void updateTagItems() {
        ArrayList<String> tagItems = new ArrayList<>(getTagItems());
        tagItems.removeAll(selectedTags);
        addTags.setItems(FXCollections.observableArrayList(tagItems));
        boolean shouldUseAux = !selectedTags.isEmpty();
        boolean usingAux = auxTagsRow.getChildren().contains(addTags);
        if (shouldUseAux && !usingAux) {
            tagsRow.getChildren().remove(addTags);
            auxTagsRow.getChildren().add(addTags);
            auxTagsRow.setVisible(true);
            auxTagsRow.setManaged(true);
            clickToRemove.setVisible(true);
            clickToRemove.setManaged(true);
        } else if (!shouldUseAux && usingAux) {
            auxTagsRow.getChildren().remove(addTags);
            tagsRow.getChildren().add(addTags);
            auxTagsRow.setVisible(false);
            auxTagsRow.setManaged(false);
            clickToRemove.setVisible(false);
            clickToRemove.setManaged(false);
        }
        tagsRow.getChildren().removeIf(node -> node.getStyleClass().contains("add-transaction-tag"));
        for (String tag : selectedTags) {
            VBox box = new VBox();
            box.getStyleClass().add("add-transaction-tag");
            Button button = new Button(tag);
            button.setOnAction(event -> {
                selectedTags.remove(tag);
                handleTagInput();
                updateTagItems();
            });
            box.getChildren().add(button);
            tagsRow.getChildren().add(box);
        }
    }

    private boolean categoryPresent = false;
    private boolean tagPresent = false;
    private CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>> predictionTask = null;
    private final Helper.Debounce predict;
    private boolean suppressUpdate = false;

    private final ListChangeListener<String> categoryItemsListener = change -> {
        updateCategoryItems();
        if (predictionTask != null) {
            handlePredictorInput(); // Restart prediction
        }
    };
    private final ListChangeListener<String> tagItemsListener = change -> {
        updateTagItems();
        if (predictionTask != null) {
            handlePredictorInput(); // Restart prediction
        }
    };

    public AddTransaction(@Nullable Transaction transaction, @Nullable Entity entity1, Model model) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("AddTransaction.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        entity.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ReferenceItemPair<Entity> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.item().name());
                }
            }
        });

        entity.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ReferenceItemPair<Entity> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.item().name());
                }
            }
        });

        onSubmitProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(SubmitEvent.SUBMIT, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(SubmitEvent.SUBMIT, newValue);
            }
        });

        categoryItemsProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(categoryItemsListener);
                category.setValue(null);
                category.getItems().clear();
                if (predictionTask != null) {
                    handlePredictorInput();
                }
            }
            if (newValue != null) {
                newValue.addListener(categoryItemsListener);
                updateCategoryItems();
                if (predictionTask != null) {
                    handlePredictorInput();
                }
            }
        });

        tagItemsProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(tagItemsListener);
                addTags.getItems().clear();
                if (predictionTask != null) {
                    predictionTask.cancel(true);
                    predictionTask = null;
                }
            }
            if (newValue != null) {
                newValue.addListener(tagItemsListener);
                updateTagItems();
                if (predictionTask != null) {
                    predictionTask.cancel(true);
                    predictionTask = null;
                }
            }
        });

        addTags.setPromptText("Add...");
        setupTagsInput();

        if (transaction != null) {
            title.setText(transaction.title());
            description.setText(transaction.description());
            time.setValue(LocalDate.ofEpochDay(Math.floorDiv(transaction.time(), 86400000)));
            amount.setText(BigDecimal.valueOf(transaction.amount()).divide(BigDecimal.valueOf(100)).toString());
            entity.setValue(new ReferenceItemPair<>(transaction.entity(), Objects.requireNonNull(entity1)));
            category.setValue(transaction.category());
            selectedTags.addAll(transaction.tags());
            submit.setText("Update");
            deleteSpace.setManaged(true);
            deleteSpace.setVisible(true);
            delete.setManaged(true);
            delete.setVisible(true);
            categoryPresent = true;
            tagPresent = true;
        }

        predict = debounce(() -> {
            if ((!tagPresent || !categoryPresent) && validatePredictor() == null && !getCategoryItems().isEmpty()) {
                CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>> task = predictionTask = model.predictCategoriesAndTags(
                        ImmutableList.of(getTransaction()),
                        ImmutableList.copyOf(getCategoryItems()),
                        ImmutableList.copyOf(getTagItems())
                );
                task.thenAccept(result -> Platform.runLater(() -> {
                    if (predictionTask == task) {
                        if (!categoryPresent) {
                            suppressUpdate = true;
                            category.setValue(getCategoryItems().get(result.getLeft().get(0)));
                            suppressUpdate = false;
                        }
                        if (!tagPresent) {
                            Bitmask.View2D mask = result.getRight();
                            selectedTags.clear();
                            for (int i = 0; i < getTagItems().size(); i++) {
                                if (mask.get(i, 0)) {
                                    selectedTags.add(getTagItems().get(i));
                                }
                            }
                            updateTagItems();
                        }
                    }
                }));
            }
        });
    }

    public Transaction getTransaction() {
        return new Transaction(
                title.getText(),
                description.getText(),
                time.getValue().toEpochDay() * 86400000,
                new BigDecimal(amount.getText()).multiply(BigDecimal.valueOf(100)).longValue(),
                category.getValue(),
                entity.getValue().reference(),
                ImmutableList.copyOf(selectedTags)
        );
    }

    public void handleMouseClick() {
        String message = validate();
        if (message != null) {
            this.message.setText(message);
            return;
        }
        setDisable(true);
        fireEvent(new SubmitEvent(this, this, false));
    }

    private String validatePredictor() {
        if (title.getText().isEmpty()) {
            return "Title is required";
        }
        if (amount.getText().isEmpty()) {
            return "Amount is required";
        }
        if (!amount.getText().matches("[+\\-]?\\d+\\.?\\d{0,2}")) {
            return "Amount is invalid";
        }
        if (entity.getValue() == null) {
            return "Entity is required";
        }
        if (time.getValue() == null) {
            return "Time is required";
        }
        return null;
    }

    private String validate() {
        String predictorMessage = validatePredictor();
        if (predictorMessage != null) {
            return predictorMessage;
        }
        if (category.getValue() == null) {
            return "Category is required";
        }
        return null;
    }

    public void handleDelete() {
        fireEvent(new SubmitEvent(this, this, true));
    }

    public void handleAddTag(String tag) {
        selectedTags.add(tag);
        Platform.runLater(() -> {
            handleTagInput();
            updateTagItems();
        });
    }

    private void setupTagsInput() {
        addTags.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleAddTag(newValue);
                SearchableComboBox<String> next = new SearchableComboBox<>();
                next.setPromptText(addTags.getPromptText());
                next.getStyleClass().addAll(addTags.getStyleClass());
                next.setItems(getTagItems());
                if (tagsRow.getChildren().contains(addTags)) {
                    tagsRow.getChildren().set(tagsRow.getChildren().indexOf(addTags), next);
                } else {
                    auxTagsRow.getChildren().set(auxTagsRow.getChildren().indexOf(addTags), next);
                }
                addTags = next;
                setupTagsInput();
            }
        });
    }

    public void handlePredictorInput() {
        if (suppressUpdate) {
            return;
        }
        if (predictionTask != null) {
            predictionTask.cancel(true);
            predictionTask = null;
        }
        predict.run();
    }

    public void handleTagInput() {
        if (suppressUpdate) {
            return;
        }
        tagPresent = true;
        handlePredictorInput();
    }

    public void handleCategoryInput() {
        if (suppressUpdate) {
            return;
        }
        categoryPresent = true;
        handlePredictorInput();
    }
}
