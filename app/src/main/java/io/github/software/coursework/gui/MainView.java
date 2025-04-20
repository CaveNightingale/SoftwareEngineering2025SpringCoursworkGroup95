package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import java.text.NumberFormat;
import java.util.Locale;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static io.github.software.coursework.data.schema.Entity.Type.UNKNOWN;

public class MainView extends AnchorPane {
    private static final Logger logger = Logger.getLogger("MainView");
    private static final int pageSize = 20;
    private static final long day = 1000L * 60 * 60 * 24;

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

    @FXML
    private EditList categoriesEdit;

    @FXML
    private EditList tagsEdit;

    @FXML
    private Chart budgetProgress;

    @FXML
    private Chart budgetHeatmap;

    @FXML
    private Chart savingProgress;

    @FXML
    private Chart savingHeatmap;

    @FXML
    private GoalSetting goalSetting;

    @FXML
    private Text budgetAmount;

    @FXML
    private Text savedAmount;

    private Tab addTransactionTab;

    private AddTransaction addTransaction;

    private final HashMap<Reference<Transaction>, Tab> editTransactionTabs = new HashMap<>();

    private Tab addEntityTab;

    private AddEntity addEntity;

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

    public MainView(AsyncStorage asyncStorage, Model model) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("MainView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.asyncStorage = asyncStorage;
        this.model = model;

        transactionList.setOnTransactionEditClicked(event -> {
            Reference<Transaction> transaction = event.getReference();
            tabPane.getSelectionModel().select(editTransactionTabs.computeIfAbsent(transaction, t -> {
                AddTransaction addTransaction = new AddTransaction(event.getTransaction(), event.getEntity(), model);
                addTransaction.setEntityItems(entityList.getItems());
                addTransaction.setCategoryItems(categoriesEdit.getNames());
                addTransaction.setTagItems(tagsEdit.getNames());
                Tab tab = new Tab("Edit Transaction: " + event.getTransaction().title());
                tab.setContent(addTransaction);
                tab.setOnClosed(event1 -> {
                    editTransactionTabs.remove(transaction);
                    tabPane.getSelectionModel().select(transactionTab);
                });
                addTransaction.setOnSubmit(event1 -> asyncStorage.transaction(table -> {
                    try {
                        Transaction item = event1.isDelete() ? null : addTransaction.getTransaction();
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
        entityList.setOnEntityEditClicked(event -> {
            Reference<Entity> entity = event.getReference();
            tabPane.getSelectionModel().select(editEntityTabs.computeIfAbsent(entity, t -> {
                AddEntity addEntity = new AddEntity(event.getEntity(), model);
                Tab tab = new Tab("Edit Transaction Party: " + event.getEntity().name());
                tab.setContent(addEntity);
                tab.setOnClosed(event1 -> {
                    editEntityTabs.remove(entity);
                    tabPane.getSelectionModel().select(entityTab);
                });
                addEntity.setOnSubmit(event1 -> asyncStorage.entity(table -> {
                    try {
                        Entity item = addEntity.getEntity();
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

        categoriesEdit.setOnSubmit(event -> {
            if (event.isInsertMode()) {
                asyncStorage.transaction(table -> {
                    try {
                        table.addCategory(event.getName(), AsyncStorage.Sensitivity.NORMAL);
                        Platform.runLater(event::reset);
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
                        Platform.runLater(event::reset);
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

        tagsEdit.setOnSubmit(event -> {
            if (event.isInsertMode()) {
                asyncStorage.transaction(table -> {
                    try {
                        table.addTag(event.getName(), AsyncStorage.Sensitivity.NORMAL);
                        Platform.runLater(event::reset);
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
                        Platform.runLater(event::reset);
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

        goalSetting.setCategories(categoriesEdit.getNames());
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

    public void loadCategories() {
        asyncStorage.transaction(table -> {
            try {
                ImmutablePair<Set<String>, Set<String>> categories = table.getCategories();
                Platform.runLater(() -> {
                    categoriesEdit.getNames().clear();
                    categoriesEdit.getNames().addAll(categories.getLeft());
                    categoriesEdit.getProtectedNames().clear();
                    categoriesEdit.getProtectedNames().addAll(categories.getRight());
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
                    tagsEdit.getNames().clear();
                    tagsEdit.getNames().addAll(tags.getLeft());
                    tagsEdit.getProtectedNames().clear();
                    tagsEdit.getProtectedNames().addAll(tags.getRight());
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
                    entityList.getItems().clear();
                    entityList.getItems().addAll(entities);
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
        loadGoal();
    }

    public void loadGoal() {
        asyncStorage.transaction(table -> {
            try {
                Goal goal = table.getGoal();
                Platform.runLater(() -> goalSetting.setGoal(goal));
                long today = System.currentTimeMillis();
                // TODO: time zone?
                today = today - Math.floorMod(today, day);
                long start = goal == null ? today - day * 15 : goal.start();
                start = start - Math.floorMod(start, day);
                long end = goal == null ? today + day * 15 : goal.end();
                end = end - Math.floorMod(end, day);
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
                    budgetTrain = new double[(int) ((today - start) / day + 1)];
                    savedTrain = new double[(int) ((today - start) / day + 1)];
                    for (ReferenceItemPair<Transaction> transaction : transactions) {
                        long time = transaction.item().time();
                        int binIndex = (int) Math.floorDiv(time - start, day);
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
                    timeToPredict = new long[(int) ((end - today) / day)];
                    for (int i = 0; i < timeToPredict.length; i++) {
                        timeToPredict[i] = today + (i + 1) * day;
                    }
                } else {
                    budgetTrain = new double[0];
                    savedTrain = new double[0];
                    timeToPredict = new long[(int) ((end - start) / day)];
                    for (int i = 0; i < timeToPredict.length; i++) {
                        timeToPredict[i] = start + (i + 1) * day;
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
                HeatmapRenderer usedHeatmap = new HeatmapRenderer(
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
                HeatmapRenderer savedHeatmap = new HeatmapRenderer(
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
                    budgetHeatmap.setRenderer(usedHeatmap);
                    savingHeatmap.setPrefHeight(20 * savingCategories.size() + 50);
                    savingHeatmap.setMinHeight(20 * savingCategories.size() + 50);
                    savingHeatmap.setRenderer(savedHeatmap);
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
                    SequentialPredictionRenderer renderer = new SequentialPredictionRenderer(
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
                        budgetProgress.setRenderer(renderer);
                    });
                });
                model.predictSavedAmount(today, ImmutableLongArray.copyOf(timeToPredict)).thenAccept(prediction -> {
                    ImmutableDoubleArray mean = prediction.getLeft();
                    ImmutableDoubleArray lower = prediction.getRight().getLeft();
                    ImmutableDoubleArray upper = prediction.getRight().getRight();
                    SequentialPredictionRenderer renderer = new SequentialPredictionRenderer(
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
                        savingProgress.setRenderer(renderer);
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
        addTransaction = new AddTransaction(null, null, model);
        addTransactionTab = new Tab("Add Transaction");
        addTransaction.setEntityItems(entityList.getItems());
        addTransaction.setCategoryItems(categoriesEdit.getNames());
        addTransaction.setTagItems(tagsEdit.getNames());
        addTransactionTab.setContent(addTransaction);
        addTransactionTab.setOnClosed(event -> {
            addTransactionTab = null;
            addTransaction = null;
            tabPane.getSelectionModel().select(transactionTab);
        });
        Runnable insert = () -> asyncStorage.transaction(table -> {
            try {
                Reference<Transaction> ref = new Reference<>();
                Transaction item = addTransaction.getTransaction();
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
        addTransaction.setOnSubmit(event -> asyncStorage.transaction(table -> {
            Transaction inserted = addTransaction.getTransaction();
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
                                addTransaction.setDisable(false);
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
        addEntity = new AddEntity(null, model);
        addEntityTab = new Tab("Add New Transaction Party");
        addEntityTab.setContent(addEntity);
        addEntityTab.setOnClosed(event -> {
            addEntityTab = null;
            addEntity = null;
            tabPane.getSelectionModel().select(entityTab);
        });
        addEntity.setOnSubmit(event -> asyncStorage.entity(table -> {
            try {
                Reference<Entity> ref = new Reference<>();
                Entity item = addEntity.getEntity();
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

    public static final class SequentialPredictionRenderer extends Chart.Renderer {
        private final double cent = 0.01;

        private final ImmutableDoubleArray trainingSamples;
        private final ImmutableDoubleArray predictedMean;
        private final ImmutableDoubleArray predictedLowerBound;
        private final ImmutableDoubleArray predictedUpperBound;
        private final long start;
        private final long end;
        private final long today;
        private final double reference;
        private final String referenceName;

        private final int sequenceLength;
        private final double top;
        private final double bottom;

        private double mouseX = Double.NaN;

        public SequentialPredictionRenderer(
                ImmutableDoubleArray trainingSamples,
                ImmutableDoubleArray predictedMean,
                ImmutableDoubleArray predictedLowerBound,
                ImmutableDoubleArray predictedUpperBound,
                long start,
                long end,
                long today,
                double reference,
                String referenceName
        ) {
            this.trainingSamples = trainingSamples;
            this.predictedMean = predictedMean;
            this.predictedLowerBound = predictedLowerBound;
            this.predictedUpperBound = predictedUpperBound;
            this.start = start;
            this.end = end;
            this.today = today;
            this.reference = reference;
            this.referenceName = referenceName;

            this.sequenceLength = trainingSamples.length() + predictedMean.length();
            double top = 1;
            double bottom = 0;
            if (Double.isFinite(reference)) {
                top = Math.max(top, reference);
                bottom = Math.min(bottom, reference);
            }
            double predictBase = trainingSamples.isEmpty() ? 0 : trainingSamples.get(trainingSamples.length() - 1);
            for (int i = 0; i < predictedUpperBound.length(); i++) {
                top = Math.max(top, predictedUpperBound.get(i) + predictBase);
                bottom = Math.min(bottom, predictedUpperBound.get(i) + predictBase);
            }
            for (int i = 0; i < predictedLowerBound.length(); i++) {
                top = Math.max(top, predictedLowerBound.get(i) + predictBase);
                bottom = Math.min(bottom, predictedLowerBound.get(i) + predictBase);
            }
            for (int i = 0; i < predictedMean.length(); i++) {
                top = Math.max(top, predictedMean.get(i) + predictBase);
                bottom = Math.min(bottom, predictedMean.get(i) + predictBase);
            }
            for (int i = 0; i < trainingSamples.length(); i++) {
                top = Math.max(top, trainingSamples.get(i));
                bottom = Math.min(bottom, trainingSamples.get(i));
            }
            this.top = top;
            this.bottom = bottom;
        }

        private void drawReference(double x0, double y0, double x1, double y1, Paint paint) {
            save();
            getGraphicsContext().setLineDashes(10, 5);
            plot(new double[]{x0, x1}, new double[]{y0, y1}, paint, 2);
            restore();
        }

        private double[] addReference(double[] array, double reference) {
            double[] newArray = new double[array.length + 1];
            newArray[0] = reference;
            for (int i = 0; i < array.length; i++) {
                newArray[i + 1] = array[i] + reference;
            }
            return newArray;
        }

        @Override
        public void render() {
            setYLimits(bottom * cent, top * cent);
            setXLimits(0, sequenceLength - 1);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/M/d");
            ArrayList<Pair<Double, String>> xTicks = new ArrayList<>();
            if (sequenceLength >= 10) {
                for (int i = 0; i <= 5; i++) {
                    int idx = (i * (sequenceLength - 1)) / 5;
                    long time = start + idx * day;
                    String date = LocalDate.ofEpochDay(Math.ceilDiv(time, day)).format(formatter);
                    xTicks.add(Pair.of((double) idx, date));
                }
            } else {
                for (int i = 0; i < sequenceLength; i++) {
                    long time = start + i * day;
                    String date = LocalDate.ofEpochDay(Math.ceilDiv(time, day)).format(formatter);
                    xTicks.add(Pair.of((double) i, date));
                }
            }
            drawXAxis(xTicks);

            double scale = Math.abs((top - bottom) * cent);
            if (Double.isFinite(reference)) {
                drawYAxis(0, Math.max(Math.max(Math.abs(reference * cent) / 4, 0.2), scale / 8));
            } else {
                drawYAxis(0, scale / 4);
            }

            double[] trainX = IntStream.range(-1, trainingSamples.length()).asDoubleStream().toArray();
            double[] trainY = addReference(trainingSamples.stream().map(x -> x * cent).toArray(), 0);
            plot(trainX, trainY, Color.GREEN, 2);

            double[] predX= IntStream.range(trainingSamples.length() - 1, sequenceLength).asDoubleStream().toArray();
            double[] predY = addReference(predictedMean.stream().map(x -> x * cent).toArray(), trainY[trainY.length - 1]);
            plot(predX, predY, Color.BLUE, 2);

            double[] predLowerX = IntStream.range(trainingSamples.length() - 1, sequenceLength).asDoubleStream().toArray();
            double[] predLowerY = addReference(predictedLowerBound.stream().map(x -> x * cent).toArray(), trainY[trainY.length - 1]);
            double[] predUpperX = predLowerX.clone();
            double[] predUpperY = addReference(predictedUpperBound.stream().map(x -> x * cent).toArray(), trainY[trainY.length - 1]);
            Doubles.reverse(predLowerX);
            Doubles.reverse(predLowerY);
            double[] confidenceX = Doubles.concat(predUpperX, predLowerX);
            double[] confidenceY = Doubles.concat(predUpperY, predLowerY);
            fill(confidenceX, confidenceY, Color.BLUE.deriveColor(1, 1, 1, 0.25));

            if (Double.isFinite(reference)) {
                double y = reference * cent;
                drawReference(fracXToDataX(0), y, fracXToDataX(1), y, Color.RED.deriveColor(1, 1, 1, 0.5));
                save();
                getGraphicsContext().setStroke(Color.RED.deriveColor(1, 1, 1, 0.5));
                drawText(this.referenceName, ALIGN_START, ALIGN_END, fromFracX(0) + 5, fromDataY(y) - 5);
                restore();
            }

            if (today >= start && today <= end) {
                long x = (today - start) / day;
                drawReference(x, bottom * cent, x, top * cent, Color.DARKCYAN.deriveColor(1, 1, 1, 0.5));
                save();
                getGraphicsContext().setStroke(Color.DARKCYAN.deriveColor(1, 1, 1, 0.5));
                drawText("Today", ALIGN_CENTER, ALIGN_END, fromDataX(x), fromDataY(top * cent) - 5);
                restore();
            }

            if (Double.isFinite(mouseX)) {
                drawReference(mouseX, fracYToDataY(0), mouseX, fracYToDataY(1), Color.BLACK.deriveColor(1, 1, 1, 0.5));
                save();
                getGraphicsContext().setStroke(Color.BLACK.deriveColor(1, 1, 1, 0.5));
                String date = LocalDate.ofEpochDay(start / day + (int) mouseX).format(formatter);
                drawText(date, ALIGN_CENTER, ALIGN_START, fromDataX(mouseX), fromDataY(0) + 7.5);
                restore();
                String text = getMouseHoverText();
                save();
                getGraphicsContext().setStroke(Color.BLACK.deriveColor(1, 1, 1, 0.5));
                drawText(text, ALIGN_CENTER, ALIGN_END, fromDataX(mouseX), fromFracY(1) - 5);
                restore();
            }
        }

        private String getMouseHoverText() {
            int index = (int) mouseX;
            if (index < trainingSamples.length()) {
                double value = trainingSamples.get(index);
                return String.format("%.2f", value);
            } else {
                double referenceValue = trainingSamples.get(trainingSamples.length() - 1);
                double value = predictedMean.get(index - trainingSamples.length()) + referenceValue;
                double upper = predictedUpperBound.get(index - trainingSamples.length()) + referenceValue;
                double lower = predictedLowerBound.get(index - trainingSamples.length()) + referenceValue;
                return String.format("%.2f / %.2f / %.2f", lower, value, upper);
            }
        }

        @Override
        public void onHover(double screenX, double screenY) {
            mouseX = Double.isFinite(screenX) ? Math.round(toDataX(screenX)) : Double.NaN;
            if (mouseX < 0 || mouseX >= sequenceLength) {
                mouseX = Double.NaN;
            }
            getGraphicsContext().clearRect(0, 0, getScreenWidth(), getScreenHeight());
            render();
        }
    }

    public static final class HeatmapRenderer extends Chart.Renderer {
        private final ImmutableList<String> xTicks;
        private final ImmutableList<String> yTicks;
        private final ImmutableList<ImmutableDoubleArray> data; // column major

        public HeatmapRenderer(ImmutableList<String> xTicks, ImmutableList<String> yTicks, ImmutableList<ImmutableDoubleArray> data) {
            this.xTicks = xTicks;
            this.yTicks = yTicks;
            this.data = data;
        }

        private Color color(double percent) {
            if (!Double.isFinite(percent)) {
                return Color.LIGHTGRAY;
            }
            return percent > 0 ? Color.WHITE.interpolate(Color.RED, percent) : Color.WHITE.interpolate(Color.GREEN, -percent);
        }

        private double[] normalize(ImmutableDoubleArray data) {
            double[] normalized = new double[data.length()];
            double max = 0;
            double min = 0;
            for (int i = 0; i < data.length(); i++) {
                if (!Double.isFinite(data.get(i))) {
                    normalized[i] = 0;
                    continue;
                }
                max = Math.max(max, data.get(i));
                min = Math.min(min, data.get(i));
            }
            double scale = Math.max(Math.max(max, -min), 1e-3);
            for (int i = 0; i < data.length(); i++) {
                normalized[i] = data.get(i) / scale;
            }
            return normalized;
        }

        @Override
        public void render() {
            setDataPaddingLeft(120);
            setYInverted(false);
            setXCategoricalLimit(this.xTicks.size());
            setYCategoricalLimit(this.yTicks.size());
            for (int i = 0; i < data.size(); i++) {
                ImmutableDoubleArray column = data.get(i);
                double[] normalized = normalize(column);
                for (int j = 0; j < column.length(); j++) {
                    fill(
                            new double[]{i - 0.5, i - 0.5, i + 0.5, i + 0.5},
                            new double[]{j - 0.5, j + 0.5, j + 0.5, j - 0.5},
                            color(normalized[j]).deriveColor(1, 0.75, 1, 0.5)
                    );
                    String text = Double.isFinite(column.get(j)) ? String.format("%.2f", column.get(j)) : "N/A";
                    drawText(text, ALIGN_CENTER, ALIGN_CENTER, fromDataX(i), fromDataY(j));
                }
            }
            drawXAxis(xTicks);
            drawYAxis(yTicks);
        }
    }
}
