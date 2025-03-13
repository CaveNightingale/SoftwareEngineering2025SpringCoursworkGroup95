package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.schema.Transaction;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Objects;

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

    private Storage storage;
    private Reference<Transaction> transaction;

    public TransactionItem() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("TransactionItem.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setStorage(Storage storage) {
        if (this.storage == storage) {
            return;
        }
        this.storage = storage;
        load();
    }

    public void setTransaction(Reference<Transaction> transaction) {
        if (Objects.equals(this.transaction, transaction)) {
            return;
        }
        this.transaction = transaction;
        load();
    }

    public void load() {
        if (storage == null || transaction == null) {
            return;
        }
        Transaction transaction = storage.getTransaction(this.transaction);
        title.setText(transaction.title());
        amount.setText(AMOUNT_FORMAT.format(transaction.amount() / 100.0));
        time.setText(new Date(transaction.time()).toString());
        entity.setText(storage.getEntity(transaction.entity()).name());
        category.setText(transaction.category());
        tags.getChildren().clear();
        for (String tag : transaction.tags()) {
            Text text = new Text(tag);
            text.getStyleClass().add("transaction-item-tag");
            tags.getChildren().add(text);
        }
    }
}
