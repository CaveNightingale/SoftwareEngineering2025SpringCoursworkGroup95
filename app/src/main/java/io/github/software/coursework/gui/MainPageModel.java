package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.algo.Model;
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
import javafx.scene.Node;
import javafx.scene.control.Tab;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.NumberFormat;

import static io.github.software.coursework.data.schema.Entity.Type.UNKNOWN;

public final class MainPageModel {
    private static final Logger logger = Logger.getLogger("MainPageModel");
    private static final int pageSize = 20;
    public static final long DAY = 1000L * 60 * 60 * 24;

    private final AsyncStorage asyncStorage;
    private final Model model;
    private String currentSearchQuery = "";

    private final HashMap<Reference<Transaction>, Tab> editTransactionTabs = new HashMap<>();
    private final HashMap<Reference<Entity>, Tab> editEntityTabs = new HashMap<>();

    public final ObjectProperty<Node> storageSetting = new SimpleObjectProperty<>(this, "storageSetting");

    // 数据加载回调
    private Consumer<List<ImmutablePair<ReferenceItemPair<Transaction>, Entity>>> onTransactionsLoaded;
    private Consumer<Set<String>> onCategoriesLoaded;
    private Consumer<Set<String>> onProtectedCategoriesLoaded;
    private Consumer<Set<String>> onTagsLoaded;
    private Consumer<Set<String>> onProtectedTagsLoaded;
    private Consumer<List<ReferenceItemPair<Entity>>> onEntitiesLoaded;
    private Consumer<Goal> onGoalLoaded;
    private Consumer<ChartHeatmapModel> onBudgetHeatmapLoaded;
    private Consumer<ChartHeatmapModel> onSavingHeatmapLoaded;
    private Consumer<ChartSequentialPredictionModel> onBudgetProgressLoaded;
    private Consumer<ChartSequentialPredictionModel> onSavingProgressLoaded;
    private Consumer<String> onBudgetAmountUpdated;
    private Consumer<String> onSavedAmountUpdated;
    private Consumer<Integer> onUpdatePagination;

    // 错误处理回调
    private Consumer<String> onCategoryAddError;
    private Consumer<String> onCategoryDeleteError;
    private Consumer<String> onTagAddError;
    private Consumer<String> onTagDeleteError;

    public MainPageModel(AsyncStorage asyncStorage, Model model) {
        this.asyncStorage = asyncStorage;
        this.model = model;
    }

    // Getters for core components
    public AsyncStorage getAsyncStorage() {
        return asyncStorage;
    }

    public Model getModel() {
        return model;
    }

    public ObjectProperty<Node> storageSettingProperty() {
        return storageSetting;
    }

    public Node getStorageSetting() {
        return storageSettingProperty().get();
    }

    public void setStorageSetting(Node value) {
        storageSettingProperty().set(value);
    }

    // Callback setters
    public void setOnTransactionsLoaded(Consumer<List<ImmutablePair<ReferenceItemPair<Transaction>, Entity>>> callback) {
        this.onTransactionsLoaded = callback;
    }

    public void setOnCategoriesLoaded(Consumer<Set<String>> callback) {
        this.onCategoriesLoaded = callback;
    }

    public void setOnProtectedCategoriesLoaded(Consumer<Set<String>> callback) {
        this.onProtectedCategoriesLoaded = callback;
    }

    public void setOnTagsLoaded(Consumer<Set<String>> callback) {
        this.onTagsLoaded = callback;
    }

    public void setOnProtectedTagsLoaded(Consumer<Set<String>> callback) {
        this.onProtectedTagsLoaded = callback;
    }

    public void setOnEntitiesLoaded(Consumer<List<ReferenceItemPair<Entity>>> callback) {
        this.onEntitiesLoaded = callback;
    }

    public void setOnGoalLoaded(Consumer<Goal> callback) {
        this.onGoalLoaded = callback;
    }

    public void setOnBudgetHeatmapLoaded(Consumer<ChartHeatmapModel> callback) {
        this.onBudgetHeatmapLoaded = callback;
    }

    public void setOnSavingHeatmapLoaded(Consumer<ChartHeatmapModel> callback) {
        this.onSavingHeatmapLoaded = callback;
    }

    public void setOnBudgetProgressLoaded(Consumer<ChartSequentialPredictionModel> callback) {
        this.onBudgetProgressLoaded = callback;
    }

    public void setOnSavingProgressLoaded(Consumer<ChartSequentialPredictionModel> callback) {
        this.onSavingProgressLoaded = callback;
    }

    public void setOnBudgetAmountUpdated(Consumer<String> callback) {
        this.onBudgetAmountUpdated = callback;
    }

    public void setOnSavedAmountUpdated(Consumer<String> callback) {
        this.onSavedAmountUpdated = callback;
    }

    public void setOnUpdatePagination(Consumer<Integer> callback) {
        this.onUpdatePagination = callback;
    }

    public void setOnCategoryAddError(Consumer<String> callback) {
        this.onCategoryAddError = callback;
    }

    public void setOnCategoryDeleteError(Consumer<String> callback) {
        this.onCategoryDeleteError = callback;
    }

    public void setOnTagAddError(Consumer<String> callback) {
        this.onTagAddError = callback;
    }

    public void setOnTagDeleteError(Consumer<String> callback) {
        this.onTagDeleteError = callback;
    }

    // Model业务方法
    public void setSearchQuery(String query) {
        this.currentSearchQuery = query;
    }

    public String getSearchQuery() {
        return currentSearchQuery;
    }

    public void loadEverything() {
        loadEntities();
        loadCategories();
        loadTags();
        loadTransactions(0);
    }

    public void loadEntities() {
        asyncStorage.entity(table -> {
            try {
                SequencedCollection<ReferenceItemPair<Entity>> entities = table.list(0, Integer.MAX_VALUE);
                if (onEntitiesLoaded != null) {
                    Platform.runLater(() -> onEntitiesLoaded.accept(new ArrayList<>(entities)));
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list entities", e);
            }
        });
    }

    public void loadCategories() {
        asyncStorage.transaction(table -> {
            try {
                ImmutablePair<Set<String>, Set<String>> categories = table.getCategories();
                if (onCategoriesLoaded != null) {
                    Platform.runLater(() -> onCategoriesLoaded.accept(categories.getLeft()));
                }
                if (onProtectedCategoriesLoaded != null) {
                    Platform.runLater(() -> onProtectedCategoriesLoaded.accept(categories.getRight()));
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list categories", e);
            }
        });
    }

    public void loadTags() {
        asyncStorage.transaction(table -> {
            try {
                ImmutablePair<Set<String>, Set<String>> tags = table.getTags();
                if (onTagsLoaded != null) {
                    Platform.runLater(() -> onTagsLoaded.accept(tags.getLeft()));
                }
                if (onProtectedTagsLoaded != null) {
                    Platform.runLater(() -> onProtectedTagsLoaded.accept(tags.getRight()));
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to list tags", e);
            }
        });
    }

    public void loadTransactions(int page) {
        asyncStorage.transaction(table -> {
            try {
                SequencedCollection<ReferenceItemPair<Transaction>> transactions =
                        table.list(Long.MIN_VALUE, Long.MAX_VALUE, pageSize * page, pageSize);
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
                        if (onTransactionsLoaded != null) {
                            Platform.runLater(() -> onTransactionsLoaded.accept(items));
                        }
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

    public void loadFilteredTransactions(int page) {
        asyncStorage.transaction(transactionTable -> {
            asyncStorage.entity(entityTable -> {
                try {
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

                        boolean matches = currentSearchQuery.isEmpty() ||
                                transaction.item().title().contains(currentSearchQuery) ||
                                (entity != null && entity.name().contains(currentSearchQuery));

                        if (matches) {
                            result.add(ImmutablePair.of(transaction, entity));
                        }
                    }

                    if (onTransactionsLoaded != null) {
                        Platform.runLater(() -> onTransactionsLoaded.accept(result));
                    }

                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to filter transactions", e);
                }
            });
        });
    }

    public void searchTransactions(String searchQuery) {
        this.currentSearchQuery = searchQuery;

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

                    if (onTransactionsLoaded != null) {
                        Platform.runLater(() -> onTransactionsLoaded.accept(result));
                    }

                    if (onUpdatePagination != null) {
                        Platform.runLater(() -> onUpdatePagination.accept(result.size()));
                    }

                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to filter transactions", e);
                }
            });
        });
    }

    public void loadGoal() {
        asyncStorage.transaction(table -> {
            try {
                Goal goal = table.getGoal();
                if (onGoalLoaded != null) {
                    Platform.runLater(() -> onGoalLoaded.accept(goal));
                }

                long today = System.currentTimeMillis();
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

                // 创建热图模型
                HashMap<String, Pair<Long, Long>> goalByCategory = new HashMap<>();
                for (Triple<String, Long, Long> category : goal == null ? List.<Triple<String, Long, Long>>of() : goal.byCategory()) {
                    goalByCategory.put(category.getLeft(), ImmutablePair.of(category.getMiddle(), category.getRight()));
                }

                // 预算热图数据
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

                // 储蓄热图数据
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

                if (onBudgetHeatmapLoaded != null) {
                    Platform.runLater(() -> onBudgetHeatmapLoaded.accept(usedHeatmap));
                }

                if (onSavingHeatmapLoaded != null) {
                    Platform.runLater(() -> onSavingHeatmapLoaded.accept(savedHeatmap));
                }

                long finalStart = start;
                long finalEnd = end;
                long finalToday = today;
                long finalTotalUsed = totalUsed;
                long finalTotalSaved = totalSaved;

                // 预测预算使用
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

                    if (onBudgetProgressLoaded != null) {
                        Platform.runLater(() -> onBudgetProgressLoaded.accept(sequentialPredictionModel));
                    }

                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    BigDecimal used = new BigDecimal(finalTotalUsed).divide(BigDecimal.valueOf(100));
                    String formattedUsed = currencyFormat.format(used);
                    String budgetAmountText;

                    if (goal == null) {
                        budgetAmountText = formattedUsed + " used";
                    } else {
                        BigDecimal goalValue = new BigDecimal(goal.budget()).divide(BigDecimal.valueOf(100));
                        String formattedGoal = currencyFormat.format(goalValue);
                        budgetAmountText = formattedUsed + " / " + formattedGoal + " used";
                    }

                    if (onBudgetAmountUpdated != null) {
                        Platform.runLater(() -> onBudgetAmountUpdated.accept(budgetAmountText));
                    }
                });

                // 预测储蓄金额
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

                    if (onSavingProgressLoaded != null) {
                        Platform.runLater(() -> onSavingProgressLoaded.accept(savingPrediction));
                    }

                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    BigDecimal saved = new BigDecimal(finalTotalSaved).divide(BigDecimal.valueOf(100));
                    String formattedSaved = formatSignedCurrency(saved, currencyFormat);
                    String savedAmountText;

                    if (goal == null) {
                        savedAmountText = formattedSaved + " saved";
                    } else {
                        BigDecimal goalValue = new BigDecimal(goal.saving()).divide(BigDecimal.valueOf(100));
                        String formattedGoal = formatSignedCurrency(goalValue, currencyFormat);
                        savedAmountText = formattedSaved + " / " + formattedGoal + " saved";
                    }

                    if (onSavedAmountUpdated != null) {
                        Platform.runLater(() -> onSavedAmountUpdated.accept(savedAmountText));
                    }
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to load goal", e);
            }
        });
    }

    // 分类和标签管理
    public void addCategory(String name) {
        asyncStorage.transaction(table -> {
            try {
                table.addCategory(name, AsyncStorage.Sensitivity.NORMAL);
                loadCategories();
            } catch (SyntaxException e) {
                Platform.runLater(() -> {
                    if (onCategoryAddError != null) {
                        onCategoryAddError.accept(e.getMessage());
                    }
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot add category", e);
            }
        });
    }

    public void removeCategory(String name) {
        asyncStorage.transaction(table -> {
            try {
                table.removeCategory(name, AsyncStorage.Sensitivity.NORMAL);
                loadCategories();
            } catch (SyntaxException e) {
                Platform.runLater(() -> {
                    if (onCategoryDeleteError != null) {
                        onCategoryDeleteError.accept(e.getMessage());
                    }
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot delete category", e);
            }
        });
    }

    public void addTag(String name) {
        asyncStorage.transaction(table -> {
            try {
                table.addTag(name, AsyncStorage.Sensitivity.NORMAL);
                loadTags();
            } catch (SyntaxException e) {
                Platform.runLater(() -> {
                    if (onTagAddError != null) {
                        onTagAddError.accept(e.getMessage());
                    }
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot add tag", e);
            }
        });
    }

    public void removeTag(String name) {
        asyncStorage.transaction(table -> {
            try {
                table.removeTag(name, AsyncStorage.Sensitivity.NORMAL);
                loadTags();
            } catch (SyntaxException e) {
                Platform.runLater(() -> {
                    if (onTagDeleteError != null) {
                        onTagDeleteError.accept(e.getMessage());
                    }
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot delete tag", e);
            }
        });
    }

    public void setGoal(Goal goal) {
        asyncStorage.transaction(table -> {
            try {
                table.setGoal(goal, AsyncStorage.Sensitivity.NORMAL);
                loadGoal();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot set goal", e);
            }
        });
    }

    private String formatSignedCurrency(BigDecimal amount, NumberFormat formatter) {
        String sign = amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "-";
        BigDecimal absAmount = amount.abs();
        return sign + formatter.format(absAmount);
    }

    // CSV导入逻辑
    public void importCSV(File file, Consumer<String> onSuccess, Consumer<String> onError) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (lines.size() <= 3) {
                if (onError != null) {
                    Platform.runLater(() -> onError.accept("File contains too few lines"));
                }
                return;
            }

            asyncStorage.entity(entityTable -> {
                asyncStorage.transaction(transactionTable -> {
                    try {
                        // 预加载现有实体到内存
                        Map<String, ReferenceItemPair<Entity>> entityMap = new HashMap<>();
                        for (ReferenceItemPair<Entity> pair : entityTable.list(0, Integer.MAX_VALUE)) {
                            entityMap.put(pair.item().name(), pair);
                        }

                        List<ImmutablePair<Transaction, Reference<Entity>>> batch = new ArrayList<>();

                        for (int i = 3; i < lines.size(); i++) {
                            String line = lines.get(i);
                            String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                            if (fields.length < 9) continue;

                            try {
                                // 解析交易信息
                                String summary = fields[1].trim().replaceAll("^\"|\"$", "");
                                String dateStr = fields[4].trim();
                                String amountStr = fields[5].trim().replaceAll("[^\\d.-]", "");
                                String entityField = fields[8].trim();

                                if (amountStr.isEmpty() || dateStr.isEmpty()) continue;

                                // 解析金额
                                long amountInCents = new BigDecimal(amountStr)
                                        .multiply(BigDecimal.valueOf(100))
                                        .longValueExact();

                                // 解析日期
                                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
                                long timestamp = date.atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli();

                                // 解析实体名称
                                String entityName = entityField.contains("/")
                                        ? entityField.split("/")[1].trim()
                                        : entityField.trim();

                                // 查找或创建实体
                                ReferenceItemPair<Entity> entityPair = entityMap.computeIfAbsent(
                                        entityName,
                                        k -> {
                                            Entity newEntity = new Entity(
                                                    entityName,
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

                                // 创建交易对象
                                Transaction transaction = new Transaction(
                                        summary,
                                        "",
                                        timestamp,
                                        amountInCents,
                                        "",
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

                        if (onSuccess != null) {
                            Platform.runLater(() -> onSuccess.accept(String.format("Imported %d transactions", batch.size())));
                        }

                        // 刷新界面
                        Platform.runLater(this::loadEverything);

                    } catch (Exception e) {
                        if (onError != null) {
                            Platform.runLater(() -> onError.accept("Error processing CSV: " + e.getMessage()));
                        }
                        logger.log(Level.SEVERE, "CSV import failed", e);
                    }
                });
            });
        } catch (IOException e) {
            if (onError != null) {
                Platform.runLater(() -> onError.accept("Could not read file: " + e.getMessage()));
            }
        }
    }
}