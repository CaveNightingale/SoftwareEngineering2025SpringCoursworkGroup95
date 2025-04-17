package io.github.software.coursework.gui;

import io.github.software.coursework.data.schema.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;


public class TransactionItem extends AnchorPane {
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("+0.00;-0.00");

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

    private final ObjectProperty<ImmutablePair<Transaction, String>> transaction = new SimpleObjectProperty<>(this, "transaction");

    public final ObjectProperty<ImmutablePair<Transaction, String>> transactionProperty() {
        return transaction;
    }

    public final ImmutablePair<Transaction, String> getTransaction() {
        return transactionProperty().get();
    }

    public final void setTransaction(ImmutablePair<Transaction, String> transaction) {
        transactionProperty().set(transaction);
    }

    public TransactionItem() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("TransactionItem.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        transaction.addListener((observable, oldValue, newValue) -> {
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
