package io.github.software.coursework.gui;

import io.github.software.coursework.data.ReferenceItemPair;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.util.List;

public final class TransactionListController {
    @FXML
    private VBox root;

    private final ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> originalItems = FXCollections.observableArrayList();

    private final TransactionListModel model = new TransactionListModel();

    public TransactionListModel getModel() {
        return model;
    }

    private Node createItem(ImmutablePair<ReferenceItemPair<Transaction>, Entity> pair) {
        FXMLLoader loader = new FXMLLoader(TransactionListController.class.getResource("TransactionItem.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        TransactionItemController itemController = loader.getController();
        TransactionItemModel itemModel = itemController.getModel();
        itemModel.setTransaction(ImmutablePair.of(pair.getLeft().item(), pair.getRight().name()));
        node.setFocusTraversable(true);
        node.setOnMouseClicked(event -> model.action(pair.getLeft().reference(), pair.getLeft().item(), pair.getRight()));
        node.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                model.action(pair.getLeft().reference(), pair.getLeft().item(), pair.getRight());
            }
        });
        node.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                node.getStyleClass().add("focused-item");
            } else {
                node.getStyleClass().remove("focused-item");
            }
        });
        return node;
    }

    private void onListContentChange(ListChangeListener.Change<? extends ImmutablePair<ReferenceItemPair<Transaction>, Entity>> change) {
        while (change.next()) {
            if (change.wasAdded()) {
                int index = 0;
                for (ImmutablePair<ReferenceItemPair<Transaction>, Entity> pair : change.getAddedSubList()) {
                    root.getChildren().add(change.getFrom() + (index++), createItem(pair));
                }
            } else if (change.wasRemoved()) {
                root.getChildren().remove(change.getFrom(), change.getFrom() + change.getRemovedSize());
            }
        }
    }

    @FXML
    private void initialize() {
        model.itemsProperty().addListener((observable, oldValue, newValue) -> {
            root.getChildren().clear();
            if (oldValue != null) {
                oldValue.removeListener(this::onListContentChange);
            }
            if (newValue != null) {
                originalItems.clear();
                originalItems.addAll(newValue);
                newValue.forEach(pair -> root.getChildren().add(createItem(pair)));
                newValue.addListener(this::onListContentChange);
            }
        });
        model.setItems(FXCollections.observableArrayList());
    }

    public void setOriginalItems(List<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> items) {
        originalItems.clear();
        originalItems.addAll(items);

        model.getItems().clear();
        model.getItems().addAll(items);
    }

    public void updateCurrentPage(int pageIndex, int pageSize) {
        int fromIndex = pageIndex * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, originalItems.size());

        ObservableList<ImmutablePair<ReferenceItemPair<Transaction>, Entity>> viewItems = model.getItems();
        viewItems.clear();
        if (fromIndex < toIndex) {
            viewItems.addAll(originalItems.subList(fromIndex, toIndex));
        }
    }
}