package io.github.software.coursework.gui;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;


public final class TransactionItemController {
    @FXML
    private Text title;

    @FXML
    private Text amount;

    @FXML
    private Text time;

    @FXML
    private Text entity;

    @FXML
    private Text category;

    @FXML
    private HBox tags;

    private final TransactionItemModel model = new TransactionItemModel();

    public TransactionItemModel getModel() {
        return model;
    }

    @FXML
    public void initialize() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        model.transactionProperty().addListener((observable, oldValue, newValue) -> {
            title.setText(newValue.getLeft().title());
            double amountValue = newValue.getLeft().amount() / 100.0;
            String formatted = currencyFormat.format(Math.abs(amountValue));
            if (amountValue >= 0) {
                formatted = "+" + formatted;
            } else {
                formatted = "-" + formatted;
            }
            amount.setText(formatted);
            Instant instant = Instant.ofEpochMilli(newValue.getLeft().time());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(ZoneOffset.UTC);
            time.setText(formatter.format(instant));
            entity.setText(newValue.getRight());
            category.setText(newValue.getLeft().category());
            tags.getChildren().clear();
            for (String tag : newValue.getLeft().tags()) {
                Text text = new Text(tag);
                text.getStyleClass().add("transaction-item-tag");
                tags.getChildren().add(text);
            }
        });
    }
}
