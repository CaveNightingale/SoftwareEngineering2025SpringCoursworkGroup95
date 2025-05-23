package io.github.software.coursework.gui;

import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.controlsfx.control.ToggleSwitch;
import javafx.scene.layout.*;
import org.controlsfx.control.SearchableComboBox;

import java.util.ArrayList;

public final class AddTransactionController {
    @FXML
    private VBox root;

    @FXML
    private TextField title;

    @FXML
    private TextArea note;

    @FXML
    private DatePicker time;

    @FXML
    private TextField amount;

    @FXML
    private SearchableComboBox<ReferenceItemPair<Entity>> entity;

    @FXML
    private Label titleError;

    @FXML
    private Label timeError;

    @FXML
    private Label amountError;

    @FXML
    private Label entityError;

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

    private final AddTransactionModel model = new AddTransactionModel();

    public AddTransactionModel getModel() {
        return model;
    }

    private void bindErrorProperty(Node node, StringProperty error) {
        error.addListener((observable, oldValue, newValue) -> {
            if (!newValue.isBlank()) {
                node.getStyleClass().add("error");
            } else {
                node.getStyleClass().remove("error");
            }
        });
    }

    @FXML
    private void initialize() {
        bindErrorProperty(title, titleError.textProperty());
        bindErrorProperty(time, timeError.textProperty());
        bindErrorProperty(amount, amountError.textProperty());

        title.textProperty().bindBidirectional(model.titleProperty());
        time.getEditor().textProperty().bindBidirectional(model.timeProperty());
        amount.textProperty().bindBidirectional(model.amountProperty());
        entity.valueProperty().bindBidirectional(model.entityProperty());;
        note.textProperty().bindBidirectional(model.noteProperty());
        category.valueProperty().bindBidirectional(model.categoryProperty());

        titleError.textProperty().bind(model.titleErrorProperty());
        timeError.textProperty().bind(model.timeErrorProperty());
        amountError.textProperty().bind(model.amountErrorProperty());
        entityError.textProperty().bind(model.entityErrorProperty());

        entity.itemsProperty().bind(model.availableEntitiesProperty());
        model.availableCategoriesProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(categoryItemsListener);
            }
            if (newValue != null) {
                newValue.addListener(categoryItemsListener);
                updateCategoryItems();
            }
        });
        model.availableTagsProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(tagItemsListener);
            }
            if (newValue != null) {
                newValue.addListener(tagItemsListener);
                updateTagItems();
            }
        });
        model.noteProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateMarkdownPreview(newValue);
            } else {
                notePreview.getEngine().loadContent("");
            }
        });
        model.tagsProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(tagItemsListener);
            }
            if (newValue != null) {
                newValue.addListener(tagItemsListener);
                updateTagItems();
            }
        });

        note.textProperty().addListener((observable, oldValue, newValue) -> {
            updateMarkdownPreview(newValue);
        });

        amount.textProperty().addListener((observable, oldValue, newValue) -> model.validateAmount());
        time.setOnAction(event -> model.validateTime());
        time.getEditor().setOnAction(event -> model.validateTime());
        title.textProperty().addListener((observable, oldValue, newValue) -> model.validateTitle());
        entity.valueProperty().addListener((observable, oldValue, newValue) -> model.validateEntity());

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

        model.updatingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                submit.setText("Update");
                delete.setVisible(true);
                delete.setManaged(true);
                deleteSpace.setVisible(false);
                deleteSpace.setManaged(false);
            } else {
                submit.setText("Add");
                delete.setVisible(false);
                delete.setManaged(false);
                deleteSpace.setVisible(true);
                deleteSpace.setManaged(true);
            }
        });

        addTags.setPromptText("Add...");
        setupTagsInput();
    }

    // A workaround for ControlsFX's ArrayIndexOutOfBoundsException
    // Just avoid emitting the list change listener on the list by directly setting a new list
    private void updateCategoryItems() {
        String oldValue = category.getValue();
        category.setValue(null);
        category.setItems(FXCollections.observableArrayList(model.getAvailableCategories()));
        if (oldValue != null && model.getAvailableCategories().contains(oldValue)) {
            category.setValue(oldValue);
        }
    }

    private void updateTagItems() {
        ArrayList<String> tagItems = new ArrayList<>(model.getAvailableTags());
        tagItems.removeAll(model.getTags());
        addTags.setItems(FXCollections.observableArrayList(tagItems));
        boolean shouldUseAux = !model.getTags().isEmpty();
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
        for (String tag : model.getTags()) {
            VBox box = new VBox();
            box.getStyleClass().add("add-transaction-tag");
            Button button = new Button(tag);
            button.setOnAction(event -> {
                model.getTags().remove(tag);
                handleTagInput();
                updateTagItems();
            });
            box.getChildren().add(button);
            tagsRow.getChildren().add(box);
        }
    }

    private final ListChangeListener<String> categoryItemsListener = change -> {
        updateCategoryItems();
        model.runPrediction();
    };

    private final ListChangeListener<String> tagItemsListener = change -> {
        updateTagItems();
        model.runPrediction();
    };

    // MARK: Markdown预览方法
    private void updateMarkdownPreview(String markdown) {
        Parser parser = Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
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

    // MARK: 事件处理方法
    @FXML
    public void handleMouseClick() {
        if (model.validate()) {
            root.setDisable(true);
            model.submit();
        }
    }

    public void handleSubmitCancel() {
        root.setDisable(false);
    }

    public void handleDelete() {
        model.delete();
    }

    public void handleAddTag(String tag) {
        model.getTags().add(tag);
    }

    private void setupTagsInput() {
        addTags.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleAddTag(newValue);
                SearchableComboBox<String> next = new SearchableComboBox<>();
                next.setPromptText(addTags.getPromptText());
                next.getStyleClass().addAll(addTags.getStyleClass());
                ObservableList<String> items = model.getAvailableTags();
                items.removeAll(model.getTags());
                next.setItems(items);
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
        model.runPrediction();
    }

    public void handleTagInput() {
        model.setTagPresent(true);
        model.runPrediction();
    }

    public void handleCategoryInput() {
        model.setCategoryPresent(true);
        model.runPrediction();
    }
}