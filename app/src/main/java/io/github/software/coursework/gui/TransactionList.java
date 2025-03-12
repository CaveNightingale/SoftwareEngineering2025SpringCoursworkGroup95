package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.schema.Transaction;
import javafx.scene.layout.VBox;

import java.util.SequencedCollection;

public class TransactionList extends VBox {
    private Storage storage;
    private SequencedCollection<Reference<Transaction>> transactions;

    public void setStorage(Storage storage) {
        if (this.storage == storage) {
            return;
        }
        this.storage = storage;
        load();
    }

    public void setTransactions(SequencedCollection<Reference<Transaction>> transactions) {
        if (this.transactions == transactions) {
            return;
        }
        this.transactions = transactions;
        load();
    }

    public void load() {
        this.getChildren().clear();
        if (storage == null || transactions == null) {
            return;
        }
        for (Reference<Transaction> reference : transactions) {
            TransactionItem item = new TransactionItem();
            item.setStorage(storage);
            item.setTransaction(reference);
            this.getChildren().add(item);
        }
    }
}
