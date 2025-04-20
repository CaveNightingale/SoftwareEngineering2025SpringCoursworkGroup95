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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.controlsfx.control.ToggleSwitch;
import javafx.scene.layout.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.controlsfx.control.SearchableComboBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.software.coursework.gui.Helper.*;

public class AddTransaction extends VBox {
    @FXML
    private TextField title;

    @FXML
    private TextArea note;

    @FXML
    private DatePicker time;

    @FXML
    private TextField amount;

    @FXML
    private Label amountError;

    @FXML
    private Label timeError;

    @FXML
    private Label titleError;

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
    private WebView notePreview;

    @FXML
    private ToggleSwitch previewSwitch;

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
        FXMLLoader fxmlLoader = new FXMLLoader(AddTransaction.class.getResource("AddTransaction.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 初始化Markdown预览
        note.textProperty().addListener((observable, oldValue, newValue) -> {
            updateMarkdownPreview(newValue);
        });

        // 原有初始化逻辑
        amount.textProperty().addListener((observable, oldValue, newValue) -> validateAmount(newValue));
        time.valueProperty().addListener((observable, oldValue, newValue) -> validateTime(newValue));
        time.getEditor().textProperty().addListener((observable, oldValue, newValue) -> validateTime(null));
        title.textProperty().addListener((observable, oldValue, newValue) -> validateTitle(newValue));

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

        this.previewSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                notePreview.setVisible(true);
                notePreview.setManaged(true);
                note.setVisible(false);
                note.setManaged(false);
            } else {
                notePreview.setVisible(false);
                notePreview.setManaged(false);
                note.setVisible(true);
                note.setManaged(true);
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
            note.setText(transaction.description());
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

    // MARK: Markdown预览方法
    private void updateMarkdownPreview(String markdown) {
        if (notePreview == null) return;

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);

        String styledHtml = "<html><head><style>"
                + "body { font-family: sans-serif; margin: 10px; line-height: 1.5; }"
                + "h1, h2, h3 { color: #333; }"
                + "code { background: #f0f0f0; padding: 2px 5px; border-radius: 3px; }"
                + "pre { background: #f8f8f8; padding: 10px; border-radius: 5px; }"
                + "a { color: #0066cc; text-decoration: none; }"
                + "</style></head><body>" + html + "</body></html>";

        notePreview.getEngine().loadContent(styledHtml);
    }

    // MARK: 工具栏方法
    @FXML
    private void insertBold() {
        wrapSelectionWith("**", "**");
    }

    @FXML
    private void insertItalic() {
        wrapSelectionWith("*", "*");
    }

    @FXML
    private void insertLink() {
        wrapSelectionWith("[", "](https://)");
    }

    @FXML
    private void insertCode() {
        wrapSelectionWith("`", "`");
    }

    private void wrapSelectionWith(String prefix, String suffix) {
        int start = note.getSelection().getStart();
        int end = note.getSelection().getEnd();
        String selected = note.getSelectedText();

        if (start == end) {
            note.insertText(start, prefix + suffix);
            note.positionCaret(start + prefix.length());
        } else {
            note.replaceText(start, end, prefix + selected + suffix);
            note.selectRange(start, end + prefix.length() + suffix.length());
        }
    }

    // MARK: 数据获取和设置方法
    public Transaction getTransaction() {
        return new Transaction(
                title.getText(),
                note.getText(),
                time.getValue().toEpochDay() * 86400000,
                new BigDecimal(amount.getText()).multiply(BigDecimal.valueOf(100)).longValue(),
                category.getValue(),
                entity.getValue().reference(),
                ImmutableList.copyOf(selectedTags)
        );
    }

    // MARK: 验证方法
    private void validateAmount(String newValue) {
        if (newValue.isEmpty()) {
            amountError.setText("Amount is required");
            amount.setStyle("-fx-border-color: red;");
        } else if (!newValue.matches("[+\\-]?\\d+(\\.\\d{0,2})?")) {
            amountError.setText("Amount is invalid");
            amount.setStyle("-fx-border-color: red;");
        } else {
            amountError.setText("");
            amount.setStyle("");
        }
    }

    private void validateTime(LocalDate newValue) {
        if (newValue == null) {
            String text = time.getEditor().getText();
            if (!text.isEmpty()) {
                if (isValidDate(text)) {
                    timeError.setText("");
                    time.setStyle("");
                } else {
                    timeError.setText("Invalid date format (e.g. yyyy-MM-dd or yyyy/M/d)");
                    time.setStyle("-fx-border-color: red;");
                }
            } else {
                timeError.setText("Date is required");
                time.setStyle("-fx-border-color: red;");
            }
        } else {
            timeError.setText("");
            time.setStyle("");
        }
    }

    private void validateTitle(String newValue) {
        if (newValue == null || newValue.trim().isEmpty()) {
            titleError.setText("Title is required");
            title.setStyle("-fx-border-color: red;");
        } else {
            titleError.setText("");
            title.setStyle("");
        }
    }

    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (DateTimeParseException e1) {
            try {
                LocalDate.parse(dateStr.replace('/', '-'));
                return true;
            } catch (DateTimeParseException e2) {
                try {
                    String[] parts = dateStr.split("[/-]");
                    if (parts.length == 3) {
                        int year = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int day = Integer.parseInt(parts[2]);
                        LocalDate.of(year, month, day);
                        return true;
                    }
                } catch (Exception e3) {
                    return false;
                }
                return false;
            }
        }
    }

    // MARK: 事件处理方法
    @FXML
    public void handleMouseClick() {
        boolean hasError = false;
        message.setText("");

        validateTitle(title.getText());
        if (title.getText().isEmpty()) {
            hasError = true;
        }

        if (amount.getText().isEmpty()) {
            amountError.setText("Amount is required");
            amount.setStyle("-fx-border-color: red;");
            hasError = true;
        } else if (!amount.getText().matches("[+\\-]?\\d+(\\.\\d{0,2})?")) {
            amountError.setText("Amount is invalid");
            amount.setStyle("-fx-border-color: red;");
            hasError = true;
        } else {
            amountError.setText("");
            amount.setStyle("");
        }

        if (entity.getValue() == null) {
            message.setText("Entity is required");
            hasError = true;
        }

        if (time.getValue() == null) {
            String text = time.getEditor().getText();
            if (!text.isEmpty() && isValidDate(text)) {
                try {
                    time.setValue(LocalDate.parse(normalizeDateString(text)));
                    timeError.setText("");
                    time.setStyle("");
                } catch (DateTimeParseException e) {
                    timeError.setText("Invalid date format");
                    time.setStyle("-fx-border-color: red;");
                    hasError = true;
                }
            } else {
                timeError.setText("Date is required");
                time.setStyle("-fx-border-color: red;");
                hasError = true;
            }
        } else {
            timeError.setText("");
            time.setStyle("");
        }

        if (hasError) return;

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

    public void handleDelete() {
        fireEvent(new SubmitEvent(this, this, true));
    }

    private String normalizeDateString(String dateStr) {
        String[] parts = dateStr.split("[/-]");
        return String.format("%04d-%02d-%02d",
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
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