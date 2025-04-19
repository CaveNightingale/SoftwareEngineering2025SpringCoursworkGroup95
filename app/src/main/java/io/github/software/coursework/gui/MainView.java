package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
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
import javafx.stage.FileChooser;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.software.coursework.data.schema.Entity.Type.UNKNOWN;

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

        // 搜索框添加监听（清空时重置）
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                currentSearchQuery = "";
                pagination.setCurrentPageIndex(0);
                loadTransactions(); // 恢复原始加载
            }
        });

        pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (!Objects.equals(oldVal, newVal)) {
                loadFilteredTransactions(); // 改用带过滤的加载方法
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

    private String currentSearchQuery = "";

    private void updatePagination(int totalItems) {
        int pageCount = (int) Math.ceil((double) totalItems / pageSize);
        pagination.setPageCount(Math.max(pageCount, 1));
        pagination.setCurrentPageIndex(0); // 保证重置为第一页
    }

    private void loadFilteredTransactions() {
        int page = pagination.getCurrentPageIndex();

        asyncStorage.transaction(transactionTable -> {
            asyncStorage.entity(entityTable -> {
                try {
                    // 读取所有交易（带分页）
                    SequencedCollection<ReferenceItemPair<Transaction>> transactions =
                            transactionTable.list(Long.MIN_VALUE, Long.MAX_VALUE, page * pageSize, pageSize);

                    HashMap<Reference<Entity>, Entity> entityMap = new HashMap<>();
                    ArrayList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> result = new ArrayList<>();

                    for (ReferenceItemPair<Transaction> transaction : transactions) {
                        Reference<Entity> ref = transaction.item().entity();
                        if (!entityMap.containsKey(ref)) {
                            entityMap.put(ref, entityTable.get(ref));
                        }
                        Entity entity = entityMap.get(ref);

                        // 应用搜索过滤
                        boolean matches = currentSearchQuery.isEmpty() ||
                                transaction.item().title().contains(currentSearchQuery) ||
                                (entity != null && entity.name().contains(currentSearchQuery));

                        if (matches) {
                            result.add(ImmutablePair.of(transaction, entity));
                        }
                    }

                    Platform.runLater(() -> {
                        transactionList.setOriginalItems(result);
                        // 注意：不再重置分页，保持当前页
                        transactionList.updateCurrentPage(0, pageSize);
                    });

                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to filter transactions", e);
                }
            });
        });
    }

    @FXML
    private void handleSearch() {
        String searchQuery = searchField.getText().trim();
        currentSearchQuery = searchQuery; // 保存当前搜索词

        // 重置到第一页
        pagination.setCurrentPageIndex(0);
        loadFilteredTransactions();

        asyncStorage.transaction(transactionTable -> {
            asyncStorage.entity(entityTable -> {
                try {
                    // 读取所有交易
                    SequencedCollection<ReferenceItemPair<Transaction>> transactions =
                            transactionTable.list(Long.MIN_VALUE, Long.MAX_VALUE, 0, Integer.MAX_VALUE);

                    HashMap<Reference<Entity>, Entity> entityMap = new HashMap<>();
                    ArrayList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> result = new ArrayList<>();

                    for (ReferenceItemPair<Transaction> transaction : transactions) {
                        Reference<Entity> ref = transaction.item().entity();
                        if (!entityMap.containsKey(ref)) {
                            entityMap.put(ref, entityTable.get(ref));
                        }
                        Entity entity = entityMap.get(ref);

                        if (searchQuery.isEmpty() || transaction.item().title().contains(searchQuery)
                                || (entity != null && entity.name().contains(searchQuery))) {
                            result.add(ImmutablePair.of(transaction, entity));
                        }
                    }

                    Platform.runLater(() -> {
                        transactionList.setOriginalItems(result);
                        updatePagination(result.size());
                        transactionList.updateCurrentPage(0, pageSize); // 确保第一页内容显示
                    });

                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to filter transactions", e);
                }
            });
        });
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
    private void handleImportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (file == null) return;

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (lines.size() <= 3) return; // 跳过表头

            asyncStorage.entity(entityTable -> {
                asyncStorage.transaction(transactionTable -> {
                    try {
                        // 预加载现有实体到内存
                        Map<String, ReferenceItemPair<Entity>> entityMap = new HashMap<>();
                        for (ReferenceItemPair<Entity> pair : entityTable.list(0, Integer.MAX_VALUE)) {
                            entityMap.put(pair.item().name(), pair); // 使用原始名称作为key
                        }

                        List<ImmutablePair<Transaction, Reference<Entity>>> batch = new ArrayList<>();

                        for (int i = 3; i < lines.size(); i++) { // 从第4行开始
                            String line = lines.get(i);
                            String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                            if (fields.length < 9) continue;

                            try {
                                // 1. 解析交易基本信息
                                String summary = fields[1].trim().replaceAll("^\"|\"$", "");
                                String dateStr = fields[4].trim();
                                String amountStr = fields[5].trim().replaceAll("[^\\d.-]", "");
                                String entityField = fields[8].trim();

                                if (amountStr.isEmpty() || dateStr.isEmpty()) continue;

                                // 2. 解析金额
                                long amountInCents = new BigDecimal(amountStr)
                                        .multiply(BigDecimal.valueOf(100))
                                        .longValueExact();

                                // 3. 解析日期
                                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
                                long timestamp = date.atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli();

                                // 4. 解析实体信息（保留*号）
                                String entityName = entityField.contains("/")
                                        ? entityField.split("/")[1].trim() // 保留原始带*的户名
                                        : entityField.trim();

                                // 5. 查找或创建实体（使用原始带*的名称）
                                ReferenceItemPair<Entity> entityPair = entityMap.computeIfAbsent(
                                        entityName, // 使用原始名称（包含*）作为key
                                        k -> {
                                            Entity newEntity = new Entity(
                                                    entityName, // 存储带*的原始名称
                                                    "", "", "", "",
                                                    UNKNOWN
                                            );
                                            Reference<Entity> ref = new Reference<>();
                                            try {
                                                entityTable.put(ref, AsyncStorage.Sensitivity.NORMAL, newEntity);
                                            } catch (IOException e) {
                                                throw new UncheckedIOException(e);
                                            }
                                            return new ReferenceItemPair<>(ref, newEntity);
                                        }
                                );

                                // 6. 创建交易对象
                                Transaction transaction = new Transaction(
                                        summary,
                                        "",
                                        timestamp,
                                        amountInCents,
                                        "", // 未知，后续使用ai分类
                                        entityPair.reference(),
                                        ImmutableList.of()
                                );

                                batch.add(ImmutablePair.of(transaction, entityPair.reference()));

                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Skipping malformed line: " + line, e);
                            }
                        }

                        // 批量添加交易
                        for (ImmutablePair<Transaction, Reference<Entity>> pair : batch) {
                            transactionTable.put(new Reference<>(), AsyncStorage.Sensitivity.NORMAL, pair.left);
                        }

                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                                    "CSV Import Complete",
                                    String.format("Imported %d transactions", batch.size()));
                            loadEverything();
                        });

                    } catch (Exception e) {
                        Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "Error",
                                        "Import Failed",
                                        "Error processing CSV: " + e.getMessage()));
                        logger.log(Level.SEVERE, "CSV import failed", e);
                    }
                });
            });
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "File Read Error",
                    "Could not read file: " + e.getMessage());
        }
    }

    // 弹窗封装
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
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