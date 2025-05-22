package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.data.schema.Goal;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.controlsfx.control.ToggleSwitch;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

// TODO: Replace Double with BigDecimal
public class GoalSetting extends VBox {

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

    private Model model;

    private final HashMap<String, Pair<Boolean, Long>> categorical = new HashMap<>();

    private final SimpleObjectProperty<ObservableList<String>> categories = new SimpleObjectProperty<>();
    public final ObjectProperty<ObservableList<String>> categoriesProperty() {
        return categories;
    }

    public final ObservableList<String> getCategories() {
        return categories.get();
    }

    public final void setCategories(ObservableList<String> value) {
        categories.set(value);
    }

    private final ObjectProperty<EventHandler<SubmitEvent>> onSubmit = new SimpleObjectProperty<>(this, "onSubmit");
    public final ObjectProperty<EventHandler<SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public final void setOnSubmit(EventHandler<SubmitEvent> value) {
        onSubmitProperty().set(value);
    }

    public final EventHandler<SubmitEvent> getOnSubmit() {
        return onSubmitProperty().get();
    }

    public final ListChangeListener<String> changeListener = change -> rebuildCategoryList();

    private boolean hasGoal = false;

    public GoalSetting() {
        FXMLLoader fxmlLoader = new FXMLLoader(DecryptionPageController.class.getResource("GoalSetting.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        categoriesProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(changeListener);
            }
            if (newValue != null) {
                newValue.addListener(changeListener);
            }
            rebuildCategoryList();
        });

        onSubmitProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                removeEventHandler(SubmitEvent.SUBMIT, oldValue);
            }
            if (newValue != null) {
                addEventHandler(SubmitEvent.SUBMIT, newValue);
            }
        });
    }

    public void setModel(Model model) {
        this.model = model;
    }

    private void rebuildCategoryList() {
        ArrayList<String> categories = new ArrayList<>(getCategories());
        categories.sort(String::compareTo);
        categoryList.getChildren().clear();
        for (String category : categories) {
            if (!categorical.containsKey(category)) {
                categorical.put(category, Pair.of(false, 0L));
            }
            Pair<Boolean, Long> current = categorical.get(category);
            boolean isSaving = current.getLeft();
            long amount = current.getRight();
            HBox categoryBox = new HBox();
            categoryBox.getStyleClass().add("setting-line");
            Label categoryLabel = new Label("Category: " + category);
            categoryLabel.getStyleClass().add("setting-field-name");
            Region region = new Region();
            HBox.setHgrow(region, Priority.ALWAYS);
            ToggleSwitch toggleSwitch = new ToggleSwitch();
            SpinnerValueFactory<Double> valueFactory =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(
                            -1e18, 1e18, amount / 100.0, 100);
            Spinner<Double> spinner = new Spinner<>(valueFactory);
            toggleSwitch.setSelected(isSaving);
            toggleSwitch.setText(isSaving ? "Saving" : "Budget");
            toggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
                Double budget = valueFactory.getValue();
                long currentAmount = budget == null ? 0L : Math.round(budget * 100);
                if (newValue) {
                    toggleSwitch.setText("Saving");
                    categorical.put(category, Pair.of(true, currentAmount));
                } else {
                    toggleSwitch.setText("Budget");
                    categorical.put(category, Pair.of(false, currentAmount));
                }
            });
            toggleSwitch.getStyleClass().add("setting-mode-toggle");
            spinner.setEditable(true);
            spinner.getStyleClass().add("setting-field-text");
            spinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
                long currentAmount = newValue == null ? 0L : Math.round(newValue * 100);
                if (toggleSwitch.isSelected()) {
                    categorical.put(category, Pair.of(true, currentAmount));
                } else {
                    categorical.put(category, Pair.of(false, currentAmount));
                }
            });
            categoryBox.getChildren().addAll(categoryLabel, region, toggleSwitch, spinner);
            categoryList.getChildren().add(categoryBox);
        }
    }

    public void handleAutoComplete() {
        Objects.requireNonNull(model);
        if (goalStart.getValue() == null) {
            goalStart.setValue(LocalDate.now());
        }
        if (goalEnd.getValue() == null) {
            goalEnd.setValue(LocalDate.now().plusDays(30));
        }
        ImmutableList<String> categories = ImmutableList.copyOf(getCategories());
        model.predictGoals(categories, goalStart.getValue().toEpochDay() * 86400000, goalEnd.getValue().toEpochDay() * 86400000)
                .thenAccept(result -> {
                    goalBudget.getValueFactory().setValue(result.getLeft().getLeft() / 100.0);
                    goalSaving.getValueFactory().setValue(result.getRight().getLeft() / 100.0);
                    for (int i = 0; i < categories.size(); i++) {
                        long budget = result.getRight().getRight().get(i);
                        long saving = result.getLeft().getRight().get(i);
                        boolean isSaving = budget == 0 && saving != 0;
                        categorical.put(categories.get(i), Pair.of(isSaving, isSaving ? saving : budget));
                    }
                    rebuildCategoryList();
                });
    }

    private String validate() {
        if (goalStart.getValue() == null) {
            return "Start date is not set";
        }
        if (goalEnd.getValue() == null) {
            return "End date is not set";
        }
        if (!goalEnd.getValue().isAfter(goalStart.getValue())) {
            return "End date must be after start date";
        }
        if (goalBudget.getValue() == null) {
            return "Budget is not set";
        }
        if (goalSaving.getValue() == null) {
            return "Saving is not set";
        }
        return null;
    }

    public void handleApply() {
        String error = validate();
        if (error != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid goal");
            alert.setContentText(error);
            alert.show();
            return;
        }
        long start = goalStart.getValue().toEpochDay() * 86400000;
        long end = goalEnd.getValue().toEpochDay() * 86400000;
        long budget = (long) (goalBudget.getValue() * 100.0);
        long saving = (long) (goalSaving.getValue() * 100.0);
        ImmutableList.Builder<ImmutableTriple<String, Long, Long>> byCategory = ImmutableList.builder();
        for (String category : getCategories()) {
            Pair<Boolean, Long> pair = categorical.get(category);
            byCategory.add(ImmutableTriple.of(category, pair.getLeft() ? 0L : pair.getRight(), pair.getLeft() ? pair.getRight() : 0L));
        }
        Goal goal = new Goal(start, end, budget, saving, byCategory.build());
        Event.fireEvent(this, new SubmitEvent(goal));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Goal update");
        alert.setContentText("Goal updated successfully");
        alert.show();
    }

    public void handleRemove() {
        if (!hasGoal) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No goal to remove");
            alert.setContentText("No goal to remove");
            alert.show();
        } else {
            Event.fireEvent(this, new SubmitEvent(null));
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Goal removed");
            alert.setContentText("Goal removed successfully");
            alert.show();
        }
    }

    public void setGoal(Goal goal) {
        if (goal == null) {
            hasGoal = false;
            goalStart.setValue(null);
            goalEnd.setValue(null);
            goalBudget.getValueFactory().setValue(0.0);
            goalSaving.getValueFactory().setValue(0.0);
            categorical.replaceAll((c, v) -> Pair.of(false, 0L));
        } else {
            hasGoal = true;
            goalStart.setValue(LocalDate.ofEpochDay(Math.floorDiv(goal.start(), 86400000)));
            goalEnd.setValue(LocalDate.ofEpochDay(Math.floorDiv(goal.end(), 86400000)));
            goalBudget.getValueFactory().setValue(goal.budget() / 100.0);
            goalSaving.getValueFactory().setValue(goal.saving() / 100.0);
            categorical.clear();
            for (ImmutableTriple<String, Long, Long> triple : goal.byCategory()) {
                long budget = triple.getMiddle();
                long saving = triple.getRight();
                boolean isSaving = budget == 0 && saving != 0;
                categorical.put(triple.getLeft(), Pair.of(isSaving, isSaving ? saving : budget));
            }
        }
        rebuildCategoryList();
    }

    public static class SubmitEvent extends Event {
        public static final EventType<SubmitEvent> SUBMIT = new EventType<>(Event.ANY, "GOAL_SUBMIT");

        private final @Nullable Goal goal;

        public SubmitEvent(@Nullable Goal goal) {
            super(SUBMIT);
            this.goal = goal;
        }

        public @Nullable Goal getGoal() {
            return goal;
        }
    }
}
