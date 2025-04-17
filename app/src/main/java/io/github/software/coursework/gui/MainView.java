package io.github.software.coursework.gui;

import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainView extends AnchorPane {
    private static final Logger logger = Logger.getLogger("MainView");
    private static final int pageSize = 20;

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

    @FXML
    private Pagination pagination;

    @FXML
    private Tab settingsTab;

    @FXML
    private TextField searchField; // FXML中的搜索框

    @FXML
    private VBox settings;

    private Tab addTransactionTab;

    private AddTransaction addTransaction;

    private final HashMap<Reference<Transaction>, Tab> editTransactionTabs = new HashMap<>();

    private Tab addEntityTab;

    private AddEntity addEntity;

    private final HashMap<Reference<Entity>, Tab> editEntityTabs = new HashMap<>();

    private final AsyncStorage asyncStorage;

    public final ObjectProperty<Node> storageSetting = new SimpleObjectProperty<>(this, "storageSetting");

    public final ObjectProperty<Node> storageSettingProperty() {
        return storageSetting;
    }

    public final Node getStorageSetting() {
        return storageSettingProperty().get();
    }

    public final void setStorageSetting(Node value) {
        storageSettingProperty().set(value);
    }

    public MainView(AsyncStorage asyncStorage) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("MainView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.asyncStorage = asyncStorage;

        transactionList.setOnTransactionEditClicked(event -> {
            Reference<Transaction> transaction = event.getReference();
            tabPane.getSelectionModel().select(editTransactionTabs.computeIfAbsent(transaction, t -> {
                AddTransaction addTransaction = new AddTransaction();
                addTransaction.setEntityItems(entityList.getItems());
                addTransaction.setTransaction(ImmutablePair.of(event.getTransaction(), event.getEntity()));
                Tab tab = new Tab("Edit: " + event.getTransaction().title());
                tab.setContent(addTransaction);
                tab.setOnClosed(event1 -> {
                    editTransactionTabs.remove(transaction);
                    tabPane.getSelectionModel().select(transactionTab);
                });
                addTransaction.setOnSubmit(event1 -> asyncStorage.transaction(table -> {
                    try {
                        table.put(transaction, AsyncStorage.Sensitivity.NORMAL, event1.isDelete() ? null : addTransaction.getTransaction());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot update transaction", e);
                    }
                    Platform.runLater(() -> {
                        tabPane.getTabs().remove(tab);
                        editTransactionTabs.remove(transaction);
                        tabPane.getSelectionModel().select(transactionTab);
                        loadTransactions();
                    });
                }));
                tabPane.getTabs().add(tab);
                return tab;
            }));
        });
        entityList.setOnEntityEditClicked(event -> {
            Reference<Entity> entity = event.getReference();
            tabPane.getSelectionModel().select(editEntityTabs.computeIfAbsent(entity, t -> {
                AddEntity addEntity = new AddEntity();
                addEntity.setEntity(event.getEntity());
                Tab tab = new Tab("Edit: " + event.getEntity().name());
                tab.setContent(addEntity);
                tab.setOnClosed(event1 -> {
                    editEntityTabs.remove(entity);
                    tabPane.getSelectionModel().select(entityTab);
                });
                addEntity.setOnSubmit(event1 -> asyncStorage.entity(table -> {
                    try {
                        table.put(entity, AsyncStorage.Sensitivity.NORMAL, addEntity.getEntity());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot update entity", e);
                    }
                    Platform.runLater(() -> {
                        tabPane.getTabs().remove(tab);
                        editEntityTabs.remove(entity);
                        tabPane.getSelectionModel().select(entityTab);
                        loadEverything();
                    });
                }));
                tabPane.getTabs().add(tab);
                return tab;
            }));
        });

        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                loadTransactions();
            }
        });

        tabPane.setOnKeyReleased(event -> {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            if (event.getCode() == KeyCode.ESCAPE && tab != null && tab.isClosable()) {
                tabPane.getTabs().remove(tab);
                tab.getOnClosed().handle(new Event(Event.ANY));
            }
        });
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabPane.getTabs().remove(settingsTab);
        storageSettingProperty().addListener((observable, oldValue, newValue) -> {
            // Storage setting is the only setting that is implemented externally, so it is always at the end
            // Since we don't want to couple the storage details and the transactionality details
            if (oldValue != null) {
                settings.getChildren().remove(oldValue);
            }
            if (newValue != null) {
                settings.getChildren().add(newValue);
            }
        });

        loadEverything();
    }

    @FXML
    private void handleSearch() {
        String searchQuery = searchField.getText().trim();  // 获取搜索框中的文本
        if (searchQuery.isEmpty()) {
            loadTransactions();  // 加载所有交易记录
        } else {
            pagination.setCurrentPageIndex(0);  // 搜索前回到第一页
            transactionList.filterTransactions(searchQuery);  // 调用过滤方法
        }
    }


    public void openExternalTab(Tab externalTab) {
        if (tabPane.getTabs().contains(externalTab)) {
            tabPane.getSelectionModel().select(externalTab);
            return;
        }
        tabPane.getTabs().add(externalTab);
        tabPane.getSelectionModel().select(externalTab);
    }

    public void loadEverything() {
        asyncStorage.entity(table -> {
            try {
                SequencedCollection<ReferenceItemPair<Entity>> entities = table.list(0, Integer.MAX_VALUE);
                Platform.runLater(() -> {
                    entityList.getItems().clear();
                    entityList.getItems().addAll(entities);
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list entities", e);
            }
        });
        loadTransactions();
    }

    public void loadTransactions() {
        int page = pagination.getCurrentPageIndex();
        asyncStorage.transaction(table -> {
            try {
                SequencedCollection<ReferenceItemPair<Transaction>> transactions = table.list(Long.MIN_VALUE, Long.MAX_VALUE, pageSize * page, pageSize);
                asyncStorage.entity(table1 -> {
                    try {
                        HashMap<Reference<Entity>, Entity> entityNames = new HashMap<>();
                        ArrayList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> items = new ArrayList<>();
                        for (ReferenceItemPair<Transaction> transaction : transactions) {
                            if (!entityNames.containsKey(transaction.item().entity())) {
                                entityNames.put(transaction.item().entity(), table1.get(transaction.item().entity()));
                            }
                            items.add(ImmutablePair.of(transaction, entityNames.get(transaction.item().entity())));
                        }
                        Platform.runLater(() -> {
                            transactionList.setOriginalItems(items);
                        });
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot get the names for transactions", e);
                    }
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list transactions", e);
            }
        });
    }

    @FXML
    private void handleAddTransaction() {
        if (addTransactionTab != null) {
            tabPane.getSelectionModel().select(addTransactionTab);
            return;
        }
        addTransaction = new AddTransaction();
        addTransactionTab = new Tab("Add Transaction");
        addTransaction.getEntityItems().addAll(entityList.getItems());
        addTransactionTab.setContent(addTransaction);
        addTransactionTab.setOnClosed(event -> {
            System.out.println("xxx");
            addTransactionTab = null;
            addTransaction = null;
            tabPane.getSelectionModel().select(transactionTab);
        });
        addTransaction.setOnSubmit(event -> asyncStorage.transaction(table -> {
            try {
                table.put(new Reference<>(), AsyncStorage.Sensitivity.NORMAL, addTransaction.getTransaction());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() -> {
                tabPane.getTabs().remove(addTransactionTab);
                addTransactionTab = null;
                addTransaction = null;
                tabPane.getSelectionModel().select(transactionTab);
                loadTransactions();
            });
        }));
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
        addEntityTab = new Tab("Add Entity");
        addEntityTab.setContent(addEntity);
        addEntityTab.setOnClosed(event -> {
            addEntityTab = null;
            addEntity = null;
            tabPane.getSelectionModel().select(entityTab);
        });
        addEntity.setOnSubmit(event -> asyncStorage.entity(table -> {
            try {
                table.put(new Reference<>(), AsyncStorage.Sensitivity.NORMAL, addEntity.getEntity());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() -> {
                tabPane.getTabs().remove(addEntityTab);
                addEntityTab = null;
                addEntity = null;
                tabPane.getSelectionModel().select(entityTab);
                loadEverything();
            });
        }));
        tabPane.getTabs().add(addEntityTab);
        tabPane.getSelectionModel().select(addEntityTab);
    }

    @FXML
    private void handleExportCSV() {
//        FileChooser fileChooser = new FileChooser();
//        fileChooser.setTitle("Export");
//        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
//        if (file == null) {
//            return;
//        }
//        try {
//            CSVFormat.exportTo(storage, file);
//        } catch (IOException | RuntimeException e) {
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Error");
//            alert.setHeaderText("Failed to export");
//            alert.setContentText(e.getMessage());
//            alert.show();
//        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Coming Soon");
        alert.show();
    }

    @FXML
    private void handleSettings() {
        if (!tabPane.getTabs().contains(settingsTab)) {
            tabPane.getTabs().add(settingsTab);
        }
        tabPane.getSelectionModel().select(settingsTab);
    }
}
