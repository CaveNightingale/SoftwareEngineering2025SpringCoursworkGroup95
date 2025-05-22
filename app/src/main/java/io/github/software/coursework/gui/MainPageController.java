package io.github.software.coursework.gui;

import io.github.software.coursework.algo.Model;
import io.github.software.coursework.algo.Update;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.SequencedCollection;

public final class MainPageController {
    private static final Logger logger = Logger.getLogger("MainViewController");

    @FXML
    private AnchorPane root;

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
    private TextField searchField;

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
    private Node goalSetting;

    @FXML
    private GoalSettingController goalSettingController;

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

    private Tab addEntityTab;
    private AddEntityController addEntity;

    private final MainPageModel model;

    public MainPageController(AsyncStorage asyncStorage, Model analyticsModel) {
        // 创建Model
        this.model = new MainPageModel(asyncStorage, analyticsModel);
    }

    @FXML
    private void initialize() {

        // 初始化图表
        budgetProgressController.setView(new ChartSequentialPredictionView());
        savingProgressController.setView(new ChartSequentialPredictionView());
        budgetHeatmapController.setView(new ChartHeatmapView());
        savingHeatmapController.setView(new ChartHeatmapView());

        // 设置Model回调
        setupModelCallbacks();

        // 设置UI组件监听器
        setupUIListeners();

        // 加载初始数据
        model.loadEverything();
    }

    private void setupModelCallbacks() {
        // 设置Model的回调函数
        model.setOnTransactionsLoaded(items -> {
            transactionListController.setOriginalItems(items);
            transactionListController.updateCurrentPage(0, 20);
        });

        model.setOnCategoriesLoaded(categories -> categoriesEditController.getModel().getNames().setAll(categories));
        model.setOnProtectedCategoriesLoaded(protectedCategories -> categoriesEditController.getModel().getProtectedNames().setAll(protectedCategories));
        model.setOnTagsLoaded(tags -> tagsEditController.getModel().getNames().setAll(tags));
        model.setOnProtectedTagsLoaded(protectedTags -> tagsEditController.getModel().getProtectedNames().setAll(protectedTags));
        model.setOnEntitiesLoaded(entities -> entityListController.getModel().getItems().setAll(entities));
        model.setOnGoalLoaded(goal -> goalSettingController.getModel().setGoal(goal));

        model.setOnBudgetHeatmapLoaded(heatmap -> {
            budgetHeatmap.setPrefHeight(20 * heatmap.getYTicks().size() + 50);
            budgetHeatmap.setMinHeight(20 * heatmap.getYTicks().size() + 50);
            budgetHeatmapController.setModel(heatmap);
        });

        model.setOnSavingHeatmapLoaded(heatmap -> {
            savingHeatmap.setPrefHeight(20 * heatmap.getYTicks().size() + 50);
            savingHeatmap.setMinHeight(20 * heatmap.getYTicks().size() + 50);
            savingHeatmapController.setModel(heatmap);
        });

        model.setOnBudgetProgressLoaded(chartModel -> budgetProgressController.setModel(chartModel));
        model.setOnSavingProgressLoaded(chartModel -> savingProgressController.setModel(chartModel));
        model.setOnBudgetAmountUpdated(text -> budgetAmount.setText(text));
        model.setOnSavedAmountUpdated(text -> savedAmount.setText(text));
        model.setOnUpdatePagination(this::updatePagination);

        // 错误处理回调
        model.setOnCategoryAddError(message -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to add category");
            alert.setHeaderText(message);
            alert.show();
        });

        model.setOnCategoryDeleteError(message -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to delete category");
            alert.setHeaderText(message);
            alert.show();
        });

        model.setOnTagAddError(message -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to add tag");
            alert.setHeaderText(message);
            alert.show();
        });

        model.setOnTagDeleteError(message -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to delete tag");
            alert.setHeaderText(message);
            alert.show();
        });
    }

    private void setupUIListeners() {
        // 交易列表点击事件
        transactionListController.getModel().setOnAction(this::handleEditTransaction);

        // 实体列表点击事件
        entityListController.getModel().setOnAction(this::handleEditEntity);

        // 搜索框监听
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                model.setSearchQuery("");
                pagination.setCurrentPageIndex(0);
                model.loadTransactions(0);
            }
        });

        // 分页监听
        pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                model.loadFilteredTransactions(newVal.intValue());
            }
        });

        // Tab按键监听
        tabPane.setOnKeyReleased(event -> {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            if (event.getCode() == KeyCode.ESCAPE && tab != null && tab.isClosable()) {
                tabPane.getTabs().remove(tab);
                tab.getOnClosed().handle(new Event(Event.ANY));
            }
        });

        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        tabPane.getTabs().remove(settingsTab);

        // 设置存储设置
        model.storageSettingProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                settings.getChildren().remove(oldValue);
            }
            if (newValue != null) {
                settings.getChildren().add(newValue);
            }
        });

        // 分类编辑提交
        categoriesEditController.getModel().setOnSubmit(event -> {
            event.getEditItemModel().setEditable(false);
            if (event.isInsertMode()) {
                model.addCategory(event.getName());
                event.getEditItemModel().reset();
            } else {
                model.removeCategory(event.getName());
                event.getEditItemModel().reset();
            }
        });

        // 标签编辑提交
        tagsEditController.getModel().setOnSubmit(event -> {
            event.getEditItemModel().setEditable(false);
            if (event.isInsertMode()) {
                model.addTag(event.getName());
                event.getEditItemModel().reset();
            } else {
                model.removeTag(event.getName());
                event.getEditItemModel().reset();
            }
        });

        // 目标设置提交
        goalSettingController.getModel().setCategories(categoriesEditController.getModel().getNames());
        goalSettingController.getModel().setOnSubmit(event -> {
            model.setGoal(event.getGoal());
        });
        goalSettingController.getModel().setModel(model.getModel());
    }

    private void handleEditTransaction(TransactionListModel.TransactionActionEvent event) {
        Reference<Transaction> transaction = event.getReference();
        AsyncStorage asyncStorage = model.getAsyncStorage();
        Model analyticsModel = model.getModel();

        tabPane.getSelectionModel().select(getOrCreateTransactionEditTab(transaction, event.getTransaction(), asyncStorage, analyticsModel));
    }

    private Tab getOrCreateTransactionEditTab(Reference<Transaction> transaction, Transaction transactionData,
                                              AsyncStorage asyncStorage, Model analyticsModel) {
        // 检查现有标签页
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().equals(transaction)) {
                return tab;
            }
        }

        // 创建新标签页
        FXMLLoader loader = new FXMLLoader(MainPageController.class.getResource("AddTransaction.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        AddTransactionController addTransactionController = loader.getController();
        AddTransactionModel addTransactionModel = addTransactionController.getModel();
        addTransactionModel.setModel(analyticsModel);
        addTransactionModel.setAvailableEntities(entityListController.getModel().getItems());
        addTransactionModel.setAvailableCategories(categoriesEditController.getModel().getNames());
        addTransactionModel.setAvailableTags(tagsEditController.getModel().getNames());
        addTransactionModel.setTransaction(transactionData);

        Tab tab = new Tab("Edit Transaction: " + transactionData.title());
        tab.setContent(node);
        tab.setUserData(transaction);
        tab.setOnClosed(event1 -> {
            tabPane.getSelectionModel().select(transactionTab);
        });

        addTransactionModel.setOnSubmit(event1 -> asyncStorage.transaction(table -> {
            try {
                Transaction item = event1.isDelete() ? null : addTransactionModel.getTransaction();
                Transaction old = table.put(transaction, AsyncStorage.Sensitivity.NORMAL, item);
                analyticsModel.trainOnUpdate(Update.empty(), Update.single(transaction, old, item));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot update transaction", e);
            }
            Platform.runLater(() -> {
                tabPane.getTabs().remove(tab);
                tabPane.getSelectionModel().select(transactionTab);
                model.loadTransactions(pagination.getCurrentPageIndex());
                model.loadCategories();
                model.loadTags();
            });
        }));

        tabPane.getTabs().add(tab);
        return tab;
    }

    private void handleEditEntity(EntityListModel.EntityActionEvent event) {
        Reference<Entity> entity = event.getReference();
        AsyncStorage asyncStorage = model.getAsyncStorage();
        Model analyticsModel = model.getModel();

        tabPane.getSelectionModel().select(getOrCreateEntityEditTab(entity, event.getEntity(), asyncStorage, analyticsModel));
    }

    private Tab getOrCreateEntityEditTab(Reference<Entity> entity, Entity entityData,
                                         AsyncStorage asyncStorage, Model analyticsModel) {
        // 检查现有标签页
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() != null && tab.getUserData().equals(entity)) {
                return tab;
            }
        }

        // 创建新标签页
        FXMLLoader loader = new FXMLLoader(MainPageController.class.getResource("AddEntity.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        AddEntityController addEntityController = loader.getController();
        AddEntityModel addEntityModel = addEntityController.getModel();
        addEntityModel.setModel(analyticsModel);
        addEntityModel.setEntity(entityData);

        Tab tab = new Tab("Edit Transaction Party: " + entityData.name());
        tab.setContent(node);
        tab.setUserData(entity);
        tab.setOnClosed(event1 -> {
            tabPane.getSelectionModel().select(entityTab);
        });

        addEntityModel.setOnSubmit(event1 -> asyncStorage.entity(table -> {
            try {
                Entity item = addEntityModel.getEntity();
                Entity old = table.put(entity, AsyncStorage.Sensitivity.NORMAL, item);
                analyticsModel.trainOnUpdate(Update.single(entity, old, item), Update.empty());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot update entity", e);
            }
            Platform.runLater(() -> {
                tabPane.getTabs().remove(tab);
                tabPane.getSelectionModel().select(entityTab);
                model.loadEverything();
            });
        }));

        tabPane.getTabs().add(tab);
        return tab;
    }

    @FXML
    private void handleSearch() {
        String searchQuery = searchField.getText().trim();
        model.searchTransactions(searchQuery);
    }

    private void updatePagination(int totalItems) {
        int pageCount = (int) Math.ceil((double) totalItems / 20);
        pagination.setPageCount(Math.max(pageCount, 1));
        pagination.setCurrentPageIndex(0);
    }

    public void openExternalTab(Tab externalTab) {
        if (tabPane.getTabs().contains(externalTab)) {
            tabPane.getSelectionModel().select(externalTab);
            return;
        }
        tabPane.getTabs().add(externalTab);
        tabPane.getSelectionModel().select(externalTab);
    }

    public void setStorageSetting(Node value) {
        model.setStorageSetting(value);
    }

    @FXML
    private void handleAddTransaction() {
        if (addTransactionTab != null) {
            tabPane.getSelectionModel().select(addTransactionTab);
            return;
        }

        FXMLLoader loader = new FXMLLoader(MainPageController.class.getResource("AddTransaction.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        addTransaction = loader.getController();
        AddTransactionModel addTransactionModel = addTransaction.getModel();
        addTransactionModel.setModel(model.getModel());
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

        Runnable insert = () -> model.getAsyncStorage().transaction(table -> {
            try {
                Reference<Transaction> ref = new Reference<>();
                Transaction item = addTransactionModel.getTransaction();
                Transaction old = table.put(ref, AsyncStorage.Sensitivity.NORMAL, item);
                model.getModel().trainOnUpdate(Update.empty(), Update.single(ref, old, item));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() -> {
                tabPane.getTabs().remove(addTransactionTab);
                addTransactionTab = null;
                addTransaction = null;
                tabPane.getSelectionModel().select(transactionTab);
                model.loadCategories();
                model.loadTags();
                model.loadTransactions(pagination.getCurrentPageIndex());
            });
        });

        addTransactionModel.setOnSubmit(event -> model.getAsyncStorage().transaction(table -> {
            Transaction inserted = addTransactionModel.getTransaction();
            try {
                SequencedCollection<ReferenceItemPair<Transaction>> transactionThatDay =
                        table.list(inserted.time() - 1, inserted.time(), 0, Integer.MAX_VALUE);
                Transaction similar = transactionThatDay.stream()
                        .map(ReferenceItemPair::item)
                        .filter(item -> item.title().equals(inserted.title()) &&
                                item.amount() == inserted.amount() &&
                                item.entity().equals(inserted.entity()))
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

        addEntity = loader.getController();
        AddEntityModel addEntityModel = addEntity.getModel();
        addEntityModel.setModel(model.getModel());

        addEntityTab = new Tab("Add New Transaction Party");
        addEntityTab.setContent(node);
        addEntityTab.setOnClosed(event -> {
            addEntityTab = null;
            addEntity = null;
            tabPane.getSelectionModel().select(entityTab);
        });

        addEntityModel.setOnSubmit(event -> model.getAsyncStorage().entity(table -> {
            try {
                Reference<Entity> ref = new Reference<>();
                Entity item = addEntityModel.getEntity();
                Entity old = table.put(ref, AsyncStorage.Sensitivity.NORMAL, item);
                model.getModel().trainOnUpdate(Update.single(ref, old, item), Update.empty());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() -> {
                tabPane.getTabs().remove(addEntityTab);
                addEntityTab = null;
                addEntity = null;
                tabPane.getSelectionModel().select(entityTab);
                model.loadEverything();
            });
        }));

        tabPane.getTabs().add(addEntityTab);
        tabPane.getSelectionModel().select(addEntityTab);
    }

    @FXML
    private void handleImportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        model.importCSV(file,
                successMessage -> showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                        "CSV Import Complete", successMessage),
                errorMessage -> showAlert(Alert.AlertType.ERROR, "Error",
                        "Import Failed", errorMessage)
        );
    }

    @FXML
    private void handleExportCSV() {
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

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }

    public MainPageModel getModel() {
        return model;
    }
}