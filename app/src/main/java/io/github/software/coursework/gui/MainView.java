package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.memory.MemoryStorage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class MainView extends AnchorPane {
    @FXML
    private TransactionList transactionList;

    @FXML
    private TabPane tabPane;

    private Tab addTransactionTab;

    private AddTransaction addTransaction;

    private Tab addEntityTab;

    private AddEntity addEntity;

    private final Storage storage = new MemoryStorage();

    public MainView() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("MainView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        transactionList.setStorage(storage);

        load();
    }

    public void load() {
        transactionList.setTransactions(storage.getTransactions());
        if (addTransaction != null) {
            addTransaction.load();
        }
        if (addEntity != null) {
            addEntity.load();
        }
    }

    @FXML
    private void handleAddTransaction() {
        if (addTransactionTab != null) {
            tabPane.getSelectionModel().select(addTransactionTab);
            return;
        }
        addTransaction = new AddTransaction();
        addTransaction.setStorage(storage);
        addTransactionTab = new Tab("Add Transaction");
        addTransactionTab.setContent(addTransaction);
        addTransactionTab.addEventHandler(Tab.CLOSED_EVENT, event -> {
            addTransactionTab = null;
            addTransaction = null;
        });
        addTransaction.setOnSubmit(event -> {
            storage.putTransaction(new Reference<>(), addTransaction.getTransaction());
            tabPane.getTabs().remove(addTransactionTab);
            addTransactionTab = null;
            addTransaction = null;
            load();
        });
        tabPane.getTabs().add(addTransactionTab);
        tabPane.getSelectionModel().select(addTransactionTab);
    }

    @FXML
    private void handleAddEntity() {
        if (addEntityTab != null) {
            tabPane.getSelectionModel().select(addEntityTab);
            return;
        }
        addEntity = new AddEntity();
        addEntity.setStorage(storage);
        addEntityTab = new Tab("Add Entity");
        addEntityTab.setContent(addEntity);
        addEntityTab.setOnClosed(event -> {
            addEntityTab = null;
            addEntity = null;
        });
        addEntity.setOnSubmit(event -> {
            storage.putEntity(new Reference<>(), addEntity.getEntity());
            tabPane.getTabs().remove(addEntityTab);
            addEntityTab = null;
            addEntity = null;
            load();
        });
        tabPane.getTabs().add(addEntityTab);
        tabPane.getSelectionModel().select(addEntityTab);
    }
}
