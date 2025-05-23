package io.github.software.coursework.data;

import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Goal;
import io.github.software.coursework.data.schema.Transaction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An asynchronous storage interface.
 * For simplicity, one thread per table;
 */
public interface AsyncStorage {
    /**
     * The sensitivity of a modification.
     */
    enum Sensitivity {
        /**
         * Hand-made operation on data items. Not sensitive.
         */
        NORMAL,

        /**
         * Automatic operation on data items, typically large scale operations. Sensitive.
         */
        AUTOMATIC,

        /**
         * Directly editing training output, make the most impact on the model. Sensitive.
         */
        TRAINING,
        ;

        /**
         * Check if the modification is sensitive.
         * @return True if the modification is sensitive, false otherwise.
         */
        public boolean isSensitive() {
            return this != NORMAL;
        }
    }

    /**
     * Write modifications to disk
     */
    interface Flush {

        /**
         * Flush the changes to disk.
         * @throws IOException If an I/O error occurs.
         */
        void flush() throws IOException;
    }

    /**
     * A key-value table.
     * @param <K> The key type.
     * @param <V> The value type.
     */
    interface Table<K, V> {
        /**
         * Put a value into the table.
         * @param key The key to put.
         * @param sensitivity The sensitivity of the modification.
         * @param value The value to put. If null, the key will be removed.
         * @return The previous value, or null if the key was not present.
         * @throws IOException If an I/O error occurs.
         */
        @Nullable V put(K key, Sensitivity sensitivity, @Nullable V value) throws IOException;

        /**
         * Get a value from the table.
         * @param key The key to get.
         * @return The value, or null if the key was not present.
         * @throws IOException If an I/O error occurs.
         */
        V get(K key) throws IOException;
    }

    /**
     * A table that contains a list of entities.
     */
    interface EntityTable extends Table<Reference<Entity>, Entity>, Flush {
        /**
         * Get a list of entities in a given range. The order is not-specified, but consistent over time.
         * @param offset The offset.
         * @param limit The limit.
         * @return The list of entities.
         * @throws IOException If an I/O error occurs.
         */
        SequencedCollection<ReferenceItemPair<Entity>> list(int offset, int limit) throws IOException;
    }

    /**
     * A table that contains a list of transactions.
     */
    interface TransactionTable extends Table<Reference<Transaction>, Transaction>, Flush {
        /**
         * Get a list of transactions in a given range. Notice the order is *DESCENDING*. The range is (start, end].
         * @param start The start of the range.
         * @param end The end of the range.
         * @param offset The offset. Notice the descending order.
         * @param limit The limit.
         * @return The list of transactions.
         * @throws IOException If an I/O error occurs.
         */
        SequencedCollection<ReferenceItemPair<Transaction>> list(long start, long end, int offset, int limit) throws IOException;

        /**
         * Get the set of all categories and the set of categories in use/
         */
        ImmutablePair<Set<String>, Set<String>> getCategories() throws IOException;

        /**
         * Add a category
         * @throws SyntaxException if the request is impossible, e.g. adding a category that is already in use.
         */
        void addCategory(String category, Sensitivity sensitivity) throws IOException;

        /**
         * Remove a category
         * @throws SyntaxException if the request is impossible, e.g. removing a category that is in use.
         */
        void removeCategory(String category, Sensitivity sensitivity) throws IOException;

        /**
         * Get the set of all tags and the set of tags in use.
         * @return The set of all tags and the set of tags in use.
         * @throws IOException If an I/O error occurs.
         */
        ImmutablePair<Set<String>, Set<String>> getTags() throws IOException;

        /**
         * Add a tag.
         * @param tag The tag to add. The tag name should not be same as an existing tag name.
         * @param sensitivity The sensitivity of the modification.
         * @throws IOException If an I/O error occurs.
         */
        void addTag(String tag, Sensitivity sensitivity) throws IOException;

        /**
         * Remove a tag.
         * @param tag The tag to remove. If the tag is in use, it cannot be removed.
         * @param sensitivity The sensitivity of the modification.
         * @throws IOException If an I/O error occurs.
         */
        void removeTag(String tag, Sensitivity sensitivity) throws IOException;

        /**
         * Get the goal of the transaction.
         * @return The goal of the transaction.
         * @throws IOException If an I/O error occurs.
         */
        @Nullable Goal getGoal() throws IOException;

        /**
         * Set the goal of the transaction.
         * @param goal The goal to set. If null, the goal will be removed.
         * @param sensitivity The sensitivity of the modification.
         * @throws IOException If an I/O error occurs.
         */
        void setGoal(@Nullable Goal goal, Sensitivity sensitivity) throws IOException;
    }

    /**
     * A model directory that contains a list of files that models are stored in.
     */
    interface ModelDirectory extends DirectoryAccessor, Flush {

        /**
         * Log arbitrary events.
         * @param event The event to log.
         * @param sensitivity The sensitivity of the modification.
         * @param args The arguments to log. The first argument is the event name, the rest are the arguments.
         */
        void log(String event, Sensitivity sensitivity, Item ...args);
    }

    /**
     * The callback is submitted to the entity thread, and you can safely access the entity table.
     * @param callback The callback to be executed.
     */
    void entity(Consumer<EntityTable> callback);

    /**
     * The callback is submitted to the transaction thread, and you can safely access the transaction table.
     * @param callback The callback to be executed.
     */
    void transaction(Consumer<TransactionTable> callback);

    /**
     * The callback is submitted to the model thread, and you can safely access the model directory.
     * @param callback The callback to be executed.
     */
    void model(Consumer<ModelDirectory> callback);

    /**
     * Close the storage. (i.e. flush all changes to disk and shutdown the threads)
     * @return a CompletableFuture that will be completed when the storage is closed.
     */
    CompletableFuture<Void> close();
}
