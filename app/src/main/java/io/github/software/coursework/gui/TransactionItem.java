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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;

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
        transaction.addListener((observable, oldValue, newValue) -> {
            title.setText(newValue.getLeft().title());
            amount.setText(AMOUNT_FORMAT.format(BigDecimal.valueOf(newValue.getLeft().amount()).divide(BigDecimal.valueOf(100))));
            time.setText(new Date(newValue.getLeft().time()).toString());
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
