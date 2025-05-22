package io.github.software.coursework.gui;

import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.ToggleSwitch;

import java.util.ArrayList;

// TODO: Replace Double with BigDecimal
public final class GoalSettingController {

    @FXML
    private VBox categoryList;

    @FXML
    private DatePicker goalStart;

    @FXML
    private DatePicker goalEnd;

    @FXML
    private Spinner<Double> goalBudget;

    @FXML
    private Spinner<Double> goalSaving;

    private final GoalSettingModel model = new GoalSettingModel();

    public GoalSettingModel getModel() {
        return model;
    }

    private final ListChangeListener<String> listChangeListener = change -> rebuild();
    private final MapChangeListener<String, Pair<Double, Boolean>> mapChangeListener = change -> rebuild();

    @FXML
    private void initialize() {
        goalStart.valueProperty().bindBidirectional(model.startProperty());
        goalEnd.valueProperty().bindBidirectional(model.endProperty());
        model.overallBudgetProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                goalBudget.getValueFactory().setValue(newValue.doubleValue());
            }
        });
        model.overallSavingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                goalSaving.getValueFactory().setValue(newValue.doubleValue());
            }
        });
        model.categoriesProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(listChangeListener);
            }
            if (newValue != null) {
                newValue.addListener(listChangeListener);
                rebuild();
            }
        });
        model.categoricalProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(mapChangeListener);
            }
            if (newValue != null) {
                newValue.addListener(mapChangeListener);
                rebuild();
            }
        });
        model.getCategorical().addListener(mapChangeListener);
    }

    private void rebuild() {
        ArrayList<String> categories = new ArrayList<>(model.getCategories());
        categories.sort(String::compareTo);
        categoryList.getChildren().clear();
        goalBudget.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                model.setOverallBudget(newValue);
            }
        });
        goalBudget.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                model.setOverallBudget(newValue);
            }
        });
        for (String category : categories) {
            if (!model.getCategorical().containsKey(category)) {
                model.getCategorical().put(category, Pair.of(0.00, false));
            }
            Pair<Double, Boolean> current = model.getCategorical().get(category);
            boolean isSaving = current.getRight();
            double amount = current.getLeft();
            HBox categoryBox = new HBox();
            categoryBox.getStyleClass().add("setting-line");
            Label categoryLabel = new Label("Category: " + category);
            categoryLabel.getStyleClass().add("setting-field-name");
            Region region = new Region();
            HBox.setHgrow(region, Priority.ALWAYS);
            ToggleSwitch toggleSwitch = new ToggleSwitch();
            SpinnerValueFactory<Double> valueFactory =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(
                            -1e18, 1e18, amount, 100);
            Spinner<Double> spinner = new Spinner<>(valueFactory);
            toggleSwitch.setSelected(isSaving);
            toggleSwitch.setText(isSaving ? "Saving" : "Budget");
            toggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
                Double budget = valueFactory.getValue();
                if (newValue) {
                    toggleSwitch.setText("Saving");
                    model.getCategorical().put(category, Pair.of(budget, true));
                } else {
                    toggleSwitch.setText("Budget");
                    model.getCategorical().put(category, Pair.of(budget, false));
                }
            });
            toggleSwitch.getStyleClass().add("setting-mode-toggle");
            spinner.setEditable(true);
            spinner.getStyleClass().add("setting-field-text");
            spinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
                if (toggleSwitch.isSelected()) {
                    model.getCategorical().put(category, Pair.of(newValue, true));
                } else {
                    model.getCategorical().put(category, Pair.of(newValue, false));
                }
            });
            categoryBox.getChildren().addAll(categoryLabel, region, toggleSwitch, spinner);
            categoryList.getChildren().add(categoryBox);
        }
    }

    public void handleAutoComplete() {
        model.predict();
    }

    public void handleApply() {
        String error = model.validate();
        if (error != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid goal");
            alert.setContentText(error);
            alert.show();
            return;
        }
        model.submit();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Goal update");
        alert.setContentText("Goal updated successfully");
        alert.show();
    }

    public void handleRemove() {
        if (!model.isGoalPresent()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No goal to remove");
            alert.setContentText("No goal to remove");
            alert.show();
        } else {
            model.delete();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Goal removed");
            alert.setContentText("Goal removed successfully");
            alert.show();
            model.setGoal(null);
        }
    }
}
