package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.controlsfx.control.ToggleSwitch;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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
    private ComboBox<ReferenceItemPair<Entity>> entity;

    @FXML
    private TextField category;

    @FXML
    private TextField tags;

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

    public AddTransaction() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("AddTransaction.fxml"));
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

        this.onSubmit.addListener((observable, oldValue, newValue) -> {
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
                category.getText(),
                entity.getValue().reference(),
                ImmutableList.copyOf(tags.getText().split("\\s+"))
        );
    }

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    public void setTransaction(ImmutablePair<Transaction, Entity> transaction) {
        title.setText(transaction.getLeft().title());
        note.setText(transaction.getLeft().description());
        time.setValue(LocalDate.ofEpochDay(transaction.getLeft().time() / 86400000));
        amount.setText(BigDecimal.valueOf(transaction.getLeft().amount()).divide(BigDecimal.valueOf(100)).toString());
        entity.setValue(new ReferenceItemPair<>(transaction.getLeft().entity(), transaction.getRight()));
        category.setText(transaction.getLeft().category());
        tags.setText(String.join(" ", transaction.getLeft().tags()));
        submit.setText("Update");
        deleteSpace.setManaged(true);
        deleteSpace.setVisible(true);
        delete.setManaged(true);
        delete.setVisible(true);
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

    @FXML
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
}