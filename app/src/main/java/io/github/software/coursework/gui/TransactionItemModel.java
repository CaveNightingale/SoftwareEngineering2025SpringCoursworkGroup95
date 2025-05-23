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
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public final class TransactionItemModel {
    private final ObjectProperty<ImmutablePair<Transaction, String>> transaction = new SimpleObjectProperty<>(this, "transaction");

    public ObjectProperty<ImmutablePair<Transaction, String>> transactionProperty() {
        return transaction;
    }

    public ImmutablePair<Transaction, String> getTransaction() {
        return transactionProperty().get();
    }

    public void setTransaction(ImmutablePair<Transaction, String> transaction) {
        transactionProperty().set(transaction);
    }
}
