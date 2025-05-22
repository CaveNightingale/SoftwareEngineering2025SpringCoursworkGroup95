package io.github.software.coursework.gui;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public final class EditListController {
    @FXML
    private FlowPane root;

    private final ListChangeListener<String> namesListener = change -> buildChildren();
    private final EditListModel model = new EditListModel();
    private Node insertModeItem;
    private EditItemModel insertModeItemModel;

    @FXML
    public void initialize() {
        insertModeItem = createInsertModeItem();
        model.namesProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(namesListener);
            }
            if (newValue != null) {
                newValue.addListener(namesListener);
            }
            buildChildren();
        });
        model.protectedNamesProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(namesListener);
            }
            if (newValue != null) {
                newValue.addListener(namesListener);
            }
            buildChildren();
        });
        model.getNames().addListener(namesListener);
        model.getProtectedNames().addListener(namesListener);
        insertModeItemModel.textProperty().addListener((observable, oldValue, newValue) -> updateInsertModeItemState());
        buildChildren();
    }

    public EditListModel getModel() {
        return model;
    }

    private void buildChildren() {
        root.getChildren().clear();
        ArrayList<String> names = new ArrayList<>(model.getNames());
        names.sort(String::compareTo);
        HashSet<String> protectedNames = new HashSet<>(model.getProtectedNames());
        for (String name : names) {
            Node deleteModeItem = createDeleteModeItem(name, protectedNames.contains(name));
            root.getChildren().add(deleteModeItem);
        }
        root.getChildren().add(insertModeItem);
        updateInsertModeItemState();
    }

    private Node createInsertModeItem() {
        FXMLLoader loader = new FXMLLoader(EditItemController.class.getResource("EditItemInsertMode.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        EditItemController controller = loader.getController();
        insertModeItemModel = controller.getModel();
        insertModeItemModel.onSubmitProperty().bind(model.onSubmitProperty());
        return node;
    }

    private Node createDeleteModeItem(String name, boolean isProtected) {
        FXMLLoader loader = new FXMLLoader(EditListController.class.getResource("EditItemDeleteMode.fxml"));
        Node node;
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        EditItemController controller = loader.getController();
        EditItemModel deleteModeItemModel = controller.getModel();
        deleteModeItemModel.setText(name);
        deleteModeItemModel.setEditable(!isProtected);
        deleteModeItemModel.onSubmitProperty().bind(model.onSubmitProperty());
        return node;
    }

    private void updateInsertModeItemState() {
        String text = insertModeItemModel.getText();
        boolean isEditable = !model.getNames().contains(text) && !text.isBlank();
        insertModeItemModel.setEditable(isEditable);
    }
}