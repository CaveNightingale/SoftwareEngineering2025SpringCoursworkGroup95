package io.github.software.coursework.gui;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.data.schema.Goal;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.LocalDate;
import java.util.Objects;

public final class GoalSettingModel {

    private final ObjectProperty<LocalDate> start = new SimpleObjectProperty<>();

    public ObjectProperty<LocalDate> startProperty() {
        return start;
    }

    public LocalDate getStart() {
        return start.get();
    }

    public void setStart(LocalDate start) {
        this.start.set(start);
    }

    private final ObjectProperty<LocalDate> end = new SimpleObjectProperty<>();

    public ObjectProperty<LocalDate> endProperty() {
        return end;
    }

    public LocalDate getEnd() {
        return end.get();
    }

    public void setEnd(LocalDate end) {
        this.end.set(end);
    }

    private final DoubleProperty overallBudget = new SimpleDoubleProperty();

    public DoubleProperty overallBudgetProperty() {
        return overallBudget;
    }

    public double getOverallBudget() {
        return overallBudget.get();
    }

    public void setOverallBudget(double overallBudget) {
        this.overallBudget.set(overallBudget);
    }

    // OverallSaving property
    private final DoubleProperty overallSaving = new SimpleDoubleProperty();

    public DoubleProperty overallSavingProperty() {
        return overallSaving;
    }

    public double getOverallSaving() {
        return overallSaving.get();
    }

    public void setOverallSaving(double overallSaving) {
        this.overallSaving.set(overallSaving);
    }

    private final ObjectProperty<ObservableMap<String, Pair<Double, Boolean>>> categorical = new SimpleObjectProperty<>(FXCollections.observableHashMap());

    public ObjectProperty<ObservableMap<String, Pair<Double, Boolean>>> categoricalProperty() {
        return categorical;
    }

    public ObservableMap<String, Pair<Double, Boolean>> getCategorical() {
        return categorical.get();
    }

    public void setCategorical(ObservableMap<String, Pair<Double, Boolean>> categorical) {
        this.categorical.set(categorical);
    }

    private final ObjectProperty<Model> model = new SimpleObjectProperty<>();

    public ObjectProperty<Model> modelProperty() {
        return model;
    }

    public Model getModel() {
        return model.get();
    }

    public void setModel(Model model) {
        this.model.set(model);
    }

    private final ObjectProperty<ObservableList<String>> categories = new SimpleObjectProperty<>();

    public ObjectProperty<ObservableList<String>> categoriesProperty() {
        return categories;
    }

    public ObservableList<String> getCategories() {
        return categories.get();
    }

    public void setCategories(ObservableList<String> categories) {
        this.categories.set(categories);
    }

    private final ObjectProperty<EventHandler<SubmitEvent>> onSubmit = new SimpleObjectProperty<>();

    public ObjectProperty<EventHandler<SubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public EventHandler<SubmitEvent> getOnSubmit() {
        return onSubmit.get();
    }

    public void setOnSubmit(EventHandler<SubmitEvent> onSubmit) {
        this.onSubmit.set(onSubmit);
    }

    public BooleanProperty goalPresent = new SimpleBooleanProperty(false);

    public BooleanProperty goalPresentProperty() {
        return goalPresent;
    }

    public boolean isGoalPresent() {
        return goalPresent.get();
    }

    public void setGoalPresent(boolean goalPresent) {
        this.goalPresent.set(goalPresent);
    }

    public void predict() {
        Objects.requireNonNull(model);
        if (getStart() == null) {
            setStart(LocalDate.now());
        }
        if (getEnd() == null) {
            setEnd(LocalDate.now().plusDays(30));
        }
        ImmutableList<String> categories = ImmutableList.copyOf(getCategories());
        getModel().predictGoals(categories, getStart().toEpochDay() * 86400000, getEnd().toEpochDay() * 86400000)
                .thenAccept(result -> Platform.runLater(() -> {
                    setOverallBudget(result.getLeft().getLeft() / 100.0);
                    setOverallSaving(result.getRight().getLeft() / 100.0);
                    ObservableMap<String, Pair<Double, Boolean>> categoriesMap = FXCollections.observableHashMap();
                    for (int i = 0; i < categories.size(); i++) {
                        long budget = result.getRight().getRight().get(i);
                        long saving = result.getLeft().getRight().get(i);
                        boolean isSaving = budget == 0 && saving != 0;
                        categoriesMap.put(categories.get(i), Pair.of(isSaving ? saving / 100.0 : budget / 100.0, isSaving));
                    }
                    setCategorical(categoriesMap);
                }));
    }

    public void submit() {
        if (getOnSubmit() != null) {
            Goal goal = new Goal(
                    getStart().toEpochDay() * 86400000,
                    getEnd().toEpochDay() * 86400000,
                    (long) (getOverallBudget() * 100.0),
                    (long) (getOverallSaving() * 100.0),
                    ImmutableList.copyOf(getCategorical().entrySet()
                            .stream()
                            .map(entry -> {
                                boolean isSaving = entry.getValue().getRight();
                                long amount = Math.round(entry.getValue().getLeft() * 100.0);
                                return ImmutableTriple.of(entry.getKey(), isSaving ? 0L : amount, isSaving ? amount : 0L);
                            })
                            .toList())
            );
            getOnSubmit().handle(new SubmitEvent(goal));
        }
    }

    public void delete() {
        if (getOnSubmit() != null) {
            getOnSubmit().handle(new SubmitEvent(null));
        }
    }

    public void setGoal(@Nullable Goal goal) {
        if (goal != null) {
            setOverallBudget(goal.budget() / 100.0);
            setOverallSaving(goal.saving() / 100.0);
            setStart(LocalDate.ofEpochDay(goal.start() / 86400000));
            setEnd(LocalDate.ofEpochDay(goal.end() / 86400000));
            ObservableMap<String, Pair<Double, Boolean>> categorical = FXCollections.observableHashMap();
            for (ImmutableTriple<String, Long, Long> entry : goal.byCategory()) {
                boolean isSaving = entry.getMiddle() == 0 && entry.getRight() != 0;
                categorical.put(entry.getLeft(), Pair.of(isSaving ? entry.getRight() / 100.0 : entry.getMiddle() / 100.0, isSaving));
            }
            setCategorical(categorical);
            setGoalPresent(true);
        } else {
            setOverallBudget(0.0);
            setOverallSaving(0.0);
            setStart(null);
            setEnd(null);
            setCategorical(FXCollections.observableHashMap());
            setGoalPresent(false);
        }
    }

    public String validate() {
        if (getStart() == null) {
            return "Start date is not set";
        }
        if (getEnd() == null) {
            return "End date is not set";
        }
        if (!getEnd().isAfter(getStart())) {
            return "End date must be after start date";
        }
        if (getOverallBudget() <= 0) {
            return "Budget is not set";
        }
        return null;
    }

    public static class SubmitEvent extends Event {
        public static final EventType<GoalSettingModel.SubmitEvent> SUBMIT = new EventType<>(Event.ANY, "GOAL_SUBMIT");

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
