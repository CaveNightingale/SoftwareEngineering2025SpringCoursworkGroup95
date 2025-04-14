package io.github.software.coursework.algo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.primitives.ImmutableDoubleArray;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.primitives.ImmutableLongArray;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.util.Bitmask;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Represent an external AI model.
 * Notice this meant to be implemented as mutable class.
 * And this model should be possible to be trained online.
 */
public interface Model {
    /**
     * Save the model parameters to a writer.
     * @param writer The directory write to
     */
    void saveParameters(AsyncStorage.ModelDirectory writer) throws IOException;

    /**
     * Load the model parameters from a reader IN-PLACE.
     * @param reader The directory read from
     */
    void loadParameters(AsyncStorage.ModelDirectory reader) throws IOException;

    /**
     * Make a copy of the model.
     * @return The copied model.
     */
    Model fork();

    /**
     * Train the model with new data IN-PLACE.
     * @param entityUpdate The update of the entity.
     * @param transactionUpdate The update of the transaction.
     */
    CompletableFuture<Void> trainOnUpdate(Update<Entity> entityUpdate, Update<Transaction> transactionUpdate);

    /**
     * Predict the budget usage for the given time.
     * @param time The time to predict, in seconds.
     * @return (E, (L, R)) where E is the expected budget usage, (L, R) is 0.9-confidence interval.
     */
    CompletableFuture<ImmutablePair<ImmutableDoubleArray, Pair<ImmutableDoubleArray, ImmutableDoubleArray>>>
    predictBudgetUsage(ImmutableLongArray time);

    /**
     * Predict the categories and tags for the given transactions.
     * @param transactions The transactions to predict. There may or may not be one of category field or tags field filled, but not both.
     * @param categories All allowed categories.
     * @param tags All allowed tags.
     * @return (C, T) where C is the predicted categories, T is the predicted tags, with width equals to the number of transactions.
     */
    CompletableFuture<ImmutablePair<ImmutableIntArray, Bitmask.View2D>>
    predictCategoriesAndTags(ImmutableList<Transaction> transactions, ImmutableList<String> categories, ImmutableList<String> tags);

    /**
     * Predict the entity types.
     * @param entities The entities to predict.
     * @return The predicted entity types.
     */
    CompletableFuture<ImmutableIntArray> predictEntityTypes(ImmutableList<Entity> entities);

    /**
     * Predict the budget
     * @param categories The categories to predict.
     * @param startTime The start time, in milliseconds.
     * @param endTime The end time, in milliseconds.
     * @return (O, C) where O is the overall budget, C is the budget for each category.
     */
    CompletableFuture<ImmutablePair<Long, ImmutableLongArray>> predictBudget(ImmutableList<String> categories, long startTime, long endTime);
}
