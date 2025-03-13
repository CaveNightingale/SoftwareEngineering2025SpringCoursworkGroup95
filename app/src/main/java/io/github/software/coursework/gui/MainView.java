package io.github.software.coursework.gui;

import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.Storage;
import io.github.software.coursework.data.memory.MemoryStorage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.data.text.TextFormat;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MainView extends AnchorPane {
    @FXML
    private TransactionList transactionList;

    @FXML
    private EntityList entityList;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab transactionTab;

    @FXML
    private Tab entityTab;

    private Tab addTransactionTab;

    private AddTransaction addTransaction;

    private final HashMap<Reference<Transaction>, Tab> editTransactionTabs = new HashMap<>();

    private final HashMap<Reference<Transaction>, AddTransaction> editTransactions = new HashMap<>();

    private Tab addEntityTab;

    private AddEntity addEntity;

    private final HashMap<Reference<Entity>, Tab> editEntityTabs = new HashMap<>();

    private final HashMap<Reference<Entity>, AddEntity> editEntities = new HashMap<>();

    private final MemoryStorage storage = new MemoryStorage();

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
        transactionList.setOnTransactionEditClicked(event -> {
            Reference<Transaction> transaction = event.getReference();
            tabPane.getSelectionModel().select(editTransactionTabs.computeIfAbsent(transaction, t -> {
                AddTransaction addTransaction = new AddTransaction();
                addTransaction.setStorage(storage);
                Transaction transactionValue = storage.getTransaction(transaction);
                addTransaction.setTransaction(transactionValue);
                Tab tab = new Tab("Edit: " + transactionValue.title());
                tab.setContent(addTransaction);
                tab.setOnClosed(event1 -> {
                    editTransactionTabs.remove(transaction);
                    editTransactions.remove(transaction);
                    tabPane.getSelectionModel().select(transactionTab);
                });
                addTransaction.setOnSubmit(event1 -> {
                    if (event1.isDelete()) {
                        storage.removeTransaction(transaction);
                    } else {
                        storage.putTransaction(transaction, addTransaction.getTransaction());
                    }
                    tabPane.getTabs().remove(tab);
                    editTransactionTabs.remove(transaction);
                    editTransactions.remove(transaction);
                    tabPane.getSelectionModel().select(transactionTab);
                    load();
                });
                tabPane.getTabs().add(tab);
                editTransactions.put(transaction, addTransaction);
                return tab;
            }));
        });
        entityList.setStorage(storage);
        entityList.setOnEntityEditClicked(event -> {
            Reference<Entity> entity = event.getReference();
            tabPane.getSelectionModel().select(editEntityTabs.computeIfAbsent(entity, t -> {
                AddEntity addEntity = new AddEntity();
                addEntity.setStorage(storage);
                Entity entityValue = storage.getEntity(entity);
                addEntity.setEntity(entityValue);
                Tab tab = new Tab("Edit: " + entityValue.name());
                tab.setContent(addEntity);
                tab.setOnClosed(event1 -> {
                    editEntityTabs.remove(entity);
                    editEntities.remove(entity);
                    tabPane.getSelectionModel().select(entityTab);
                });
                addEntity.setOnSubmit(event1 -> {
                    storage.putEntity(entity, addEntity.getEntity());
                    tabPane.getTabs().remove(tab);
                    editEntityTabs.remove(entity);
                    editEntities.remove(entity);
                    tabPane.getSelectionModel().select(entityTab);
                    load();
                });
                tabPane.getTabs().add(tab);
                editEntities.put(entity, addEntity);
                return tab;
            }));
        });

        load();
    }

    public void load() {
        transactionList.setTransactions(storage.getTransactions());
        entityList.setEntities(storage.getEntities());
        if (addTransaction != null) {
            addTransaction.load();
        }
        for (AddTransaction editTransaction : editTransactions.values()) {
            editTransaction.load();
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
        addTransactionTab.setOnClosed(event -> {
            addTransactionTab = null;
            addTransaction = null;
            tabPane.getSelectionModel().select(transactionTab);
        });
        addTransaction.setOnSubmit(event -> {
            storage.putTransaction(new Reference<>(), addTransaction.getTransaction());
            tabPane.getTabs().remove(addTransactionTab);
            addTransactionTab = null;
            addTransaction = null;
            tabPane.getSelectionModel().select(transactionTab);
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
            tabPane.getSelectionModel().select(entityTab);
        });
        addEntity.setOnSubmit(event -> {
            storage.putEntity(new Reference<>(), addEntity.getEntity());
            tabPane.getTabs().remove(addEntityTab);
            addEntityTab = null;
            addEntity = null;
            tabPane.getSelectionModel().select(entityTab);
            load();
        });
        tabPane.getTabs().add(addEntityTab);
        tabPane.getSelectionModel().select(addEntityTab);
    }

    @FXML
    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import");
        File file = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            TextFormat.importFrom(storage, file, false);
            load();
        } catch (IOException | RuntimeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to import");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export");
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            TextFormat.exportTo(storage, file, false);
        } catch (IOException | RuntimeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to export");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }
}
