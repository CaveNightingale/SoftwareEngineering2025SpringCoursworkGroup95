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
import org.apache.commons.lang3.tuple.ImmutablePair;

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

        // Real-time validation for 'amount' field
        amount.textProperty().addListener((observable, oldValue, newValue) -> {
            validateAmount(newValue);
        });

        // Real-time validation for 'time' field
        time.valueProperty().addListener((observable, oldValue, newValue) -> {
            validateTime(newValue);
        });

        // Listen to the text field of the DatePicker to catch invalid date input
        TextField timeTextField = time.getEditor();
        timeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                if (isValidDate(newValue)) {
                    message.setText("");
                    time.setStyle("");
                    try {
                        // 统一转换为标准格式存储
                        String normalized = normalizeDateString(newValue);
                        time.setValue(LocalDate.parse(normalized));
                    } catch (DateTimeParseException e) {
                        // 理论上不会发生，因为已经校验过
                    }
                } else {
                    message.setText("Invalid date format (e.g. yyyy-MM-dd or yyyy/M/d)");
                    time.setStyle("-fx-border-color: red;");
                }
            } else {
                message.setText("Date is required");
                time.setStyle("-fx-border-color: red;");
            }
        });

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
    }

    // 新增的辅助方法
    private String normalizeDateString(String dateStr) {
        String[] parts = dateStr.split("[/-]");
        return String.format("%04d-%02d-%02d",
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    private boolean isValidDate(String dateStr) {
        // 先尝试直接解析
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (DateTimeParseException e1) {
            // 尝试替换斜杠为横杠
            try {
                LocalDate.parse(dateStr.replace('/', '-'));
                return true;
            } catch (DateTimeParseException e2) {
                // 尝试更宽松的解析方式
                try {
                    String[] parts = dateStr.split("[/-]");
                    if (parts.length == 3) {
                        int year = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int day = Integer.parseInt(parts[2]);
                        LocalDate.of(year, month, day);  // 这会验证日期是否有效
                        return true;
                    }
                } catch (Exception e3) {
                    return false;
                }
                return false;
            }
        }
    }

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

    // Amount validation logic
    private void validateAmount(String newValue) {
        if (newValue.isEmpty()) {
            message.setText("Amount is required");
            amount.setStyle("-fx-border-color: red;");
        } else if (!newValue.matches("[+\\-]?\\d+(\\.\\d{0,2})?")) {
            message.setText("Amount is invalid");
            amount.setStyle("-fx-border-color: red;");
        } else {
            message.setText(""); // Clear message when valid
            amount.setStyle(""); // Reset border color to default
        }
    }

    // Time validation logic
    private void validateTime(LocalDate newValue) {
        if (newValue == null) {
            String text = time.getEditor().getText();
            if (!text.isEmpty()) {
                if (isValidDate(text)) {
                    message.setText("");
                    time.setStyle("");
                } else {
                    message.setText("Invalid date format (e.g. yyyy-MM-dd or yyyy/M/d)");
                    time.setStyle("-fx-border-color: red;");
                }
            } else {
                message.setText("Date is required");
                time.setStyle("-fx-border-color: red;");
            }
        } else {
            message.setText("");
            time.setStyle("");
        }
    }

    public void handleMouseClick() {
        if (title.getText().isEmpty()) {
            message.setText("Title is required");
            return;
        }
        if (amount.getText().isEmpty()) {
            message.setText("Amount is required");
            return;
        }
        if (!amount.getText().matches("[+\\-]?\\d+\\.?\\d{0,2}")) {
            message.setText("Amount is invalid");
            return;
        }
        if (entity.getValue() == null) {
            message.setText("Entity is required");
            return;
        }
        if (time.getValue() == null) {
            String text = time.getEditor().getText();
            if (!text.isEmpty() && isValidDate(text)) {
                try {
                    time.setValue(LocalDate.parse(normalizeDateString(text)));
                } catch (DateTimeParseException e) {
                    message.setText("Invalid date format");
                    return;
                }
            } else {
                message.setText("Date is required");
                return;
            }
        }
        setDisable(true);
        fireEvent(new SubmitEvent(this, this, false));
    }

    public void handleDelete() {
        fireEvent(new SubmitEvent(this, this, true));
    }
}
