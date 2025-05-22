package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.algo.Update;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.SyntaxException;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Goal;
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
import javafx.scene.layout.Region;
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

import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import java.text.NumberFormat;
import java.util.Locale;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.software.coursework.data.schema.Entity.Type.UNKNOWN;

public class MainPage extends AnchorPane {
    private static final Logger logger = Logger.getLogger("MainView");
    private static final int pageSize = 20;
    public static final long DAY = 1000L * 60 * 60 * 24;

    @FXML
    private Node transactionList;

    @FXML
    private TransactionListController transactionListController;

    @FXML
    private Node entityList;

    @FXML
    private EntityListController entityListController;

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

    @FXML
    private Node categoriesEdit;

    @FXML
    private Node tagsEdit;

    @FXML
    private Region budgetProgress;

    @FXML
    private ChartController<ChartSequentialPredictionModel> budgetProgressController;

    @FXML
    private Region budgetHeatmap;

    @FXML
    private ChartController<ChartHeatmapModel> budgetHeatmapController;

    @FXML
    private Region savingProgress;

    @FXML
    private ChartController<ChartSequentialPredictionModel> savingProgressController;

    @FXML
    private Region savingHeatmap;

    @FXML
    private ChartController<ChartHeatmapModel> savingHeatmapController;

    @FXML
    private GoalSetting goalSetting;

    @FXML
    private Text budgetAmount;

    @FXML
    private Text savedAmount;

    @FXML
    private EditListController categoriesEditController;

    @FXML
    private EditListController tagsEditController;

    private Tab addTransactionTab;

    private AddTransactionController addTransaction;

    private final HashMap<Reference<Transaction>, Tab> editTransactionTabs = new HashMap<>();

    private Tab addEntityTab;

    private AddEntityController addEntity;

    private final HashMap<Reference<Entity>, Tab> editEntityTabs = new HashMap<>();

    private final AsyncStorage asyncStorage;

    private final Model model;

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

    public MainPage(AsyncStorage asyncStorage, Model model) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainPage.class.getResource("MainPage.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        budgetProgressController.setView(new ChartSequentialPredictionView());
        savingProgressController.setView(new ChartSequentialPredictionView());
        budgetHeatmapController.setView(new ChartHeatmapView());
        savingHeatmapController.setView(new ChartHeatmapView());

        this.asyncStorage = asyncStorage;
        this.model = model;

        transactionListController.getModel().setOnAction(event -> {
            Reference<Transaction> transaction = event.getReference();
            tabPane.getSelectionModel().select(editTransactionTabs.computeIfAbsent(transaction, t -> {
                FXMLLoader loader = new FXMLLoader(MainPage.class.getResource("AddTransaction.fxml"));
                Node node;
                try {
                    node = loader.load();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                AddTransactionController addTransactionController = loader.getController();
                AddTransactionModel addTransactionModel = addTransactionController.getModel();
                addTransactionModel.setModel(model);
                addTransactionModel.setAvailableEntities(entityListController.getModel().getItems());
                addTransactionModel.setAvailableCategories(categoriesEditController.getModel().getNames());
                addTransactionModel.setAvailableTags(tagsEditController.getModel().getNames());
                addTransactionModel.setTransaction(event.getTransaction());
                Tab tab = new Tab("Edit Transaction: " + event.getTransaction().title());
                tab.setContent(node);
                tab.setOnClosed(event1 -> {
                    editTransactionTabs.remove(transaction);
                    tabPane.getSelectionModel().select(transactionTab);
                });
                addTransactionModel.setOnSubmit(event1 -> asyncStorage.transaction(table -> {
                    try {
                        Transaction item = event1.isDelete() ? null : addTransactionModel.getTransaction();
                        Transaction old = table.put(transaction, AsyncStorage.Sensitivity.NORMAL, item);
                        model.trainOnUpdate(Update.empty(), Update.single(transaction, old, item));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot update transaction", e);
                    }
                    Platform.runLater(() -> {
                        tabPane.getTabs().remove(tab);
                        editTransactionTabs.remove(transaction);
                        tabPane.getSelectionModel().select(transactionTab);
                        loadTransactions();
                        loadCategories();
                        loadTags();
                    });
                }));
                tabPane.getTabs().add(tab);
                return tab;
            }));
        });
        entityListController.getModel().setOnAction(event -> {
            Reference<Entity> entity = event.getReference();
            tabPane.getSelectionModel().select(editEntityTabs.computeIfAbsent(entity, t -> {
                FXMLLoader loader = new FXMLLoader(MainPage.class.getResource("AddEntity.fxml"));
                Node node;
                try {
                    node = loader.load();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                AddEntityController addEntityController = loader.getController();
                AddEntityModel addEntityModel = addEntityController.getModel();
                addEntityModel.setModel(model);
                addEntityModel.setEntity(event.getEntity());
                Tab tab = new Tab("Edit Transaction Party: " + event.getEntity().name());
                tab.setContent(node);
                tab.setOnClosed(event1 -> {
                    editEntityTabs.remove(entity);
                    tabPane.getSelectionModel().select(entityTab);
                });
                addEntityModel.setOnSubmit(event1 -> asyncStorage.entity(table -> {
                    try {
                        Entity item = addEntityModel.getEntity();
                        Entity old = table.put(entity, AsyncStorage.Sensitivity.NORMAL, item);
                        model.trainOnUpdate(Update.single(entity, old, item), Update.empty());
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

        categoriesEditController.getModel().setOnSubmit(event -> {
            event.getEditItemModel().setEditable(false);
            if (event.isInsertMode()) {
                asyncStorage.transaction(table -> {
                    try {
                        table.addCategory(event.getName(), AsyncStorage.Sensitivity.NORMAL);
                        Platform.runLater(() -> event.getEditItemModel().reset());
                    } catch (SyntaxException e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Unable to add category");
                            alert.setHeaderText(e.getMessage());
                            alert.show();
                        });
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot add category", e);
                    }
                });
            } else {
                asyncStorage.transaction(table -> {
                    try {
                        table.removeCategory(event.getName(), AsyncStorage.Sensitivity.NORMAL);
                        Platform.runLater(() -> event.getEditItemModel().reset());
                    } catch (SyntaxException e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Unable to delete category");
                            alert.setHeaderText(e.getMessage());
                            alert.show();
                        });
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot delete category", e);
                    }
                });
            }
            loadCategories();
        });

        tagsEditController.getModel().setOnSubmit(event -> {
            event.getEditItemModel().setEditable(false);
            if (event.isInsertMode()) {
                asyncStorage.transaction(table -> {
                    try {
                        table.addTag(event.getName(), AsyncStorage.Sensitivity.NORMAL);
                        Platform.runLater(() -> event.getEditItemModel().reset());
                    } catch (SyntaxException e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Unable to add tag");
                            alert.setHeaderText(e.getMessage());
                            alert.show();
                        });
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot add tag", e);
                    }
                });
            } else {
                asyncStorage.transaction(table -> {
                    try {
                        table.removeTag(event.getName(), AsyncStorage.Sensitivity.NORMAL);
                        Platform.runLater(() -> event.getEditItemModel().reset());
                    } catch (SyntaxException e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Unable to delete tag");
                            alert.setHeaderText(e.getMessage());
                            alert.show();
                        });
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot delete tag", e);
                    }
                });
            }
            loadTags();
        });

        goalSetting.setCategories(categoriesEditController.getModel().getNames());
        goalSetting.setOnSubmit(event -> asyncStorage.transaction(table -> {
            try {
                table.setGoal(event.getGoal(), AsyncStorage.Sensitivity.NORMAL);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot set goal", e);
            }
            Platform.runLater(this::loadGoal);
        }));
        goalSetting.setModel(model);

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
                        transactionListController.setOriginalItems(result);
                        transactionListController.updateCurrentPage(0, pageSize);
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
        currentSearchQuery = searchQuery;

        // 重置到第一页
        pagination.setCurrentPageIndex(0);
        loadFilteredTransactions();

        asyncStorage.transaction(transactionTable -> {
            asyncStorage.entity(entityTable -> {
                try {
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
                        transactionListController.setOriginalItems(result);
                        updatePagination(result.size());
                        transactionListController.updateCurrentPage(0, pageSize); // 确保第一页内容显示
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

    public void loadCategories() {
        asyncStorage.transaction(table -> {
            try {
                ImmutablePair<Set<String>, Set<String>> categories = table.getCategories();
                Platform.runLater(() -> {
                    categoriesEditController.getModel().getNames().clear();
                    categoriesEditController.getModel().getNames().addAll(categories.getLeft());
                    categoriesEditController.getModel().getProtectedNames().clear();
                    categoriesEditController.getModel().getProtectedNames().addAll(categories.getRight());
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list categories", e);
            }
        });
    }

    public void loadTags() {
        asyncStorage.transaction(table -> {
            try {
                ImmutablePair<Set<String>, Set<String>> tags = table.getTags();
                Platform.runLater(() -> {
                    tagsEditController.getModel().getNames().clear();
                    tagsEditController.getModel().getNames().addAll(tags.getLeft());
                    tagsEditController.getModel().getProtectedNames().clear();
                    tagsEditController.getModel().getProtectedNames().addAll(tags.getRight());
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list tags", e);
            }
        });
    }

    public void loadEverything() {
        asyncStorage.entity(table -> {
            try {
                SequencedCollection<ReferenceItemPair<Entity>> entities = table.list(0, Integer.MAX_VALUE);
                Platform.runLater(() -> {
                    entityListController.getModel().getItems().clear();
                    entityListController.getModel().getItems().addAll(entities);
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list entities", e);
            }
        });
        loadCategories();
        loadTags();
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
                            transactionListController.setOriginalItems(items);
                        });
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Cannot get the names for transactions", e);
                    }
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list transactions", e);
            }
        });
        loadGoal();
    }

    public void loadGoal() {
        asyncStorage.transaction(table -> {
            try {
                Goal goal = table.getGoal();
                Platform.runLater(() -> goalSetting.setGoal(goal));
                long today = System.currentTimeMillis();
                // TODO: time zone?
                today = today - Math.floorMod(today, DAY);
                long start = goal == null ? today - DAY * 15 : goal.start();
                start = start - Math.floorMod(start, DAY);
                long end = goal == null ? today + DAY * 15 : goal.end();
                end = end - Math.floorMod(end, DAY);
                today = Math.min(today, end);
                double[] budgetTrain;
                double[] savedTrain;
                long[] timeToPredict;
                long totalUsed = 0;
                long totalSaved = 0;
                TreeMap<String, Long> categoricalUse = new TreeMap<>();
                TreeMap<String, Long> categoricalSave = new TreeMap<>();
                if (today >= start) {
                    SequencedCollection<ReferenceItemPair<Transaction>> transactions = table.list(start, today, 0, Integer.MAX_VALUE);
                    budgetTrain = new double[(int) ((today - start) / DAY + 1)];
                    savedTrain = new double[(int) ((today - start) / DAY + 1)];
                    for (ReferenceItemPair<Transaction> transaction : transactions) {
                        long time = transaction.item().time();
                        int binIndex = (int) Math.floorDiv(time - start, DAY);
                        savedTrain[binIndex] += transaction.item().amount();
                        totalSaved += transaction.item().amount();
                        categoricalSave.put(transaction.item().category(),
                                categoricalSave.getOrDefault(transaction.item().category(), 0L) + transaction.item().amount());
                        if (transaction.item().amount() > 0) { // Not a budget use
                            continue;
                        }
                        budgetTrain[binIndex] -= transaction.item().amount();
                        totalUsed -= transaction.item().amount();
                        categoricalUse.put(transaction.item().category(),
                                categoricalUse.getOrDefault(transaction.item().category(), 0L) - transaction.item().amount());
                    }
                    for (int i = 1; i < budgetTrain.length; i++) {
                        budgetTrain[i] = budgetTrain[i] + budgetTrain[i - 1];
                    }
                    for (int i = 1; i < savedTrain.length; i++) {
                        savedTrain[i] = savedTrain[i] + savedTrain[i - 1];
                    }
                    timeToPredict = new long[(int) ((end - today) / DAY)];
                    for (int i = 0; i < timeToPredict.length; i++) {
                        timeToPredict[i] = today + (i + 1) * DAY;
                    }
                } else {
                    budgetTrain = new double[0];
                    savedTrain = new double[0];
                    timeToPredict = new long[(int) ((end - start) / DAY)];
                    for (int i = 0; i < timeToPredict.length; i++) {
                        timeToPredict[i] = start + (i + 1) * DAY;
                    }
                }
                HashMap<String, Pair<Long, Long>> goalByCategory = new HashMap<>();
                for (Triple<String, Long, Long> category : goal == null ? List.<Triple<String, Long, Long>>of() : goal.byCategory()) {
                    goalByCategory.put(category.getLeft(), ImmutablePair.of(category.getMiddle(), category.getRight()));
                }
                ImmutableList<String> budgetCategories = ImmutableList.copyOf(goal == null ?
                        categoricalUse.keySet() :
                        goal.byCategory().stream().filter(e -> e.getMiddle() > 0).map(Triple::getLeft).toList());
                double[] categoricalUsed = new double[budgetCategories.size()];
                for (int i = 0; i < budgetCategories.size(); i++) {
                    categoricalUsed[i] = (categoricalUse.getOrDefault(budgetCategories.get(i), 0L)) / 100.0;
                }
                double[] categoricalUsedGoalRemaining = new double[budgetCategories.size()];
                double[] categoricalUsedProgress = new double[budgetCategories.size()];
                if (goal != null) {
                    for (int i = 0; i < budgetCategories.size(); i++) {
                        categoricalUsedGoalRemaining[i] = goalByCategory.get(budgetCategories.get(i)).getLeft() / 100.0 - categoricalUsed[i];
                        categoricalUsedProgress[i] = categoricalUsed[i] / (categoricalUsed[i] + categoricalUsedGoalRemaining[i]);
                    }
                }
                ChartHeatmapModel usedHeatmap = new ChartHeatmapModel(
                        goal == null ? ImmutableList.of("Used") : ImmutableList.of("Used", "Remaining", "Progress"),
                        budgetCategories,
                        goal == null ? ImmutableList.of(ImmutableDoubleArray.copyOf(categoricalUsed)) :
                                ImmutableList.of(
                                        ImmutableDoubleArray.copyOf(categoricalUsed),
                                        ImmutableDoubleArray.copyOf(categoricalUsedGoalRemaining),
                                        ImmutableDoubleArray.copyOf(categoricalUsedProgress)
                                )
                );
                ImmutableList<String> savingCategories = ImmutableList.copyOf(goal == null ?
                        categoricalSave.keySet() :
                        goal.byCategory().stream().filter(e -> e.getRight() > 0).map(Triple::getLeft).toList());
                double[] categoricalSaved = new double[savingCategories.size()];
                for (int i = 0; i < savingCategories.size(); i++) {
                    categoricalSaved[i] = categoricalSave.getOrDefault(savingCategories.get(i), 0L) / 100.0;
                }
                double[] categoricalSavedGoalRemaining = new double[savingCategories.size()];
                double[] categoricalSavedProgress = new double[savingCategories.size()];
                if (goal != null) {
                    for (int i = 0; i < savingCategories.size(); i++) {
                        categoricalSavedGoalRemaining[i] = goalByCategory.get(savingCategories.get(i)).getRight() / 100.0 - categoricalSaved[i];
                        categoricalSavedProgress[i] = categoricalSaved[i] / (categoricalSaved[i] + categoricalSavedGoalRemaining[i]);
                    }
                }
                ChartHeatmapModel savedHeatmap = new ChartHeatmapModel(
                        goal == null ? ImmutableList.of("Saved") : ImmutableList.of("Saved", "Remaining", "Progress"),
                        savingCategories,
                        goal == null ? ImmutableList.of(ImmutableDoubleArray.copyOf(categoricalSaved)) :
                                ImmutableList.of(
                                        ImmutableDoubleArray.copyOf(categoricalSaved),
                                        ImmutableDoubleArray.copyOf(categoricalSavedGoalRemaining),
                                        ImmutableDoubleArray.copyOf(categoricalSavedProgress)
                                )
                );
                Platform.runLater(() -> {
                    budgetHeatmap.setPrefHeight(20 * budgetCategories.size() + 50);
                    budgetHeatmap.setMinHeight(20 * budgetCategories.size() + 50);
                    budgetHeatmapController.setModel(usedHeatmap);
                    savingHeatmap.setPrefHeight(20 * savingCategories.size() + 50);
                    savingHeatmap.setMinHeight(20 * savingCategories.size() + 50);
                    savingHeatmapController.setModel(savedHeatmap);
                });
                long finalStart = start;
                long finalEnd = end;
                long finalToday = today;
                long finalTotalUsed = totalUsed;
                long finalTotalSaved = totalSaved;
                model.predictBudgetUsage(today, ImmutableLongArray.copyOf(timeToPredict)).thenAccept(prediction -> {
                    ImmutableDoubleArray mean = prediction.getLeft();
                    ImmutableDoubleArray lower = prediction.getRight().getLeft();
                    ImmutableDoubleArray upper = prediction.getRight().getRight();
                    ChartSequentialPredictionModel sequentialPredictionModel = new ChartSequentialPredictionModel(
                            ImmutableDoubleArray.copyOf(budgetTrain),
                            mean,
                            lower,
                            upper,
                            finalStart,
                            finalEnd,
                            finalToday,
                            goal == null ? Double.NaN : goal.budget(),
                            "Budget"
                    );
                    Platform.runLater(() -> {
                        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                        BigDecimal used = new BigDecimal(finalTotalUsed).divide(BigDecimal.valueOf(100));
                        String formattedUsed = currencyFormat.format(used);
                        if (goal == null) {
                            budgetAmount.setText(formattedUsed + " used");
                        } else {
                            BigDecimal goalValue = new BigDecimal(goal.budget()).divide(BigDecimal.valueOf(100));
                            String formattedGoal = currencyFormat.format(goalValue);
                            budgetAmount.setText(formattedUsed + " / " + formattedGoal + " used");
                        }
                        budgetProgressController.setModel(sequentialPredictionModel);
                    });
                });
                model.predictSavedAmount(today, ImmutableLongArray.copyOf(timeToPredict)).thenAccept(prediction -> {
                    ImmutableDoubleArray mean = prediction.getLeft();
                    ImmutableDoubleArray lower = prediction.getRight().getLeft();
                    ImmutableDoubleArray upper = prediction.getRight().getRight();
                    ChartSequentialPredictionModel savingPrediction = new ChartSequentialPredictionModel(
                            ImmutableDoubleArray.copyOf(savedTrain),
                            mean,
                            lower,
                            upper,
                            finalStart,
                            finalEnd,
                            finalToday,
                            goal == null ? Double.NaN : goal.saving(),
                            "Goal"
                    );
                    Platform.runLater(() -> {
                        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                        BigDecimal saved = new BigDecimal(finalTotalSaved).divide(BigDecimal.valueOf(100));
                        String formattedSaved = formatSignedCurrency(saved, currencyFormat);
                        if (goal == null) {
                            savedAmount.setText(formattedSaved + " saved");
                        } else {
                            BigDecimal goalValue = new BigDecimal(goal.saving()).divide(BigDecimal.valueOf(100));
                            String formattedGoal = formatSignedCurrency(goalValue, currencyFormat);
                            savedAmount.setText(formattedSaved + " / " + formattedGoal + " saved");
                        }
                        savingProgressController.setModel(savingPrediction);
                    });
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to load goal", e);
            }
        });
    }

    private String formatSignedCurrency(BigDecimal amount, NumberFormat formatter) {
        String sign = amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "-";
        BigDecimal absAmount = amount.abs();
        return sign + formatter.format(absAmount);
    }

    @FXML
    private void handleAddTransaction() {
        if (addTransactionTab != null) {
            tabPane.getSelectionModel().select(addTransactionTab);
            return;
        }
        FXMLLoader loader = new FXMLLoader(MainPage.class.getResource("AddTransaction.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        AddTransactionController addTransactionController = loader.getController();
        AddTransactionModel addTransactionModel = addTransactionController.getModel();
        addTransactionModel.setModel(model);
        addTransactionModel.setAvailableEntities(entityListController.getModel().getItems());
        addTransactionModel.setAvailableCategories(categoriesEditController.getModel().getNames());
        addTransactionModel.setAvailableTags(tagsEditController.getModel().getNames());
        addTransactionTab = new Tab("Add New Transaction");
        addTransactionTab.setContent(node);
        addTransactionTab.setOnClosed(event -> {
            addTransactionTab = null;
            addTransaction = null;
            tabPane.getSelectionModel().select(transactionTab);
        });
        Runnable insert = () -> asyncStorage.transaction(table -> {
            try {
                Reference<Transaction> ref = new Reference<>();
                Transaction item = addTransactionModel.getTransaction();
                Transaction old = table.put(ref, AsyncStorage.Sensitivity.NORMAL, item);
                model.trainOnUpdate(Update.empty(), Update.single(ref, old, item));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() -> {
                tabPane.getTabs().remove(addTransactionTab);
                addTransactionTab = null;
                addTransaction = null;
                tabPane.getSelectionModel().select(transactionTab);
                loadCategories();
                loadTags();
                loadTransactions();
            });
        });
        addTransactionModel.setOnSubmit(event -> asyncStorage.transaction(table -> {
            Transaction inserted = addTransactionModel.getTransaction();
            try {
                SequencedCollection<ReferenceItemPair<Transaction>> transactionThatDay = table.list(inserted.time() - 1, inserted.time(), 0, Integer.MAX_VALUE);
                Transaction similar = transactionThatDay.stream()
                        .map(ReferenceItemPair::item)
                        .filter(item -> item.title().equals(inserted.title()) && item.amount() == inserted.amount() && item.entity().equals(inserted.entity()))
                        .findFirst()
                        .orElse(null);
                if (similar != null) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Duplicated Transaction");
                        alert.setHeaderText("A similar transaction already exists.");
                        alert.setContentText("The existing transaction is " + similar.title());
                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                insert.run();
                            } else {
                                addTransaction.handleSubmitCancel();
                            }
                        });
                    });
                } else {
                    insert.run();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        FXMLLoader loader = new FXMLLoader(AddEntityController.class.getResource("AddEntity.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        AddEntityController addEntityController = loader.getController();
        AddEntityModel addEntityModel = addEntityController.getModel();
        addEntityModel.setModel(model);
        addEntityTab = new Tab("Add New Transaction Party");
        addEntityTab.setContent(node);
        addEntityTab.setOnClosed(event -> {
            addEntityTab = null;
            addEntity = null;
            tabPane.getSelectionModel().select(entityTab);
        });
        addEntityModel.setOnSubmit(event -> asyncStorage.entity(table -> {
            try {
                Reference<Entity> ref = new Reference<>();
                Entity item = addEntityModel.getEntity();
                Entity old = table.put(ref, AsyncStorage.Sensitivity.NORMAL, item);
                model.trainOnUpdate(Update.single(ref, old, item), Update.empty());
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

    @FXML
    private void handleFeedback() {
        MailLauncher.openMailClient(
                "m.haleem@qmul.ac.uk",
                "Feedback",
                "Please write your valued comments.");
    }
}
