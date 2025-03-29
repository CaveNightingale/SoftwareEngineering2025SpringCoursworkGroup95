package io.github.software.coursework.data;

import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.SequencedCollection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An asynchronous storage interface.
 * For simplicity, one thread per table;
 */
public interface AsyncStorage {
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

        public boolean isSensitive() {
            return this != NORMAL;
        }
    }

    /**
     * Write modifications to disk
     */
    interface Flush {
        void flush() throws IOException;
    }

    /**
     * A key-value table.
     * @param <K> The key type.
     * @param <V> The value type.
     */
    interface Table<K, V> {
        void put(K key, Sensitivity sensitivity, @Nullable V value) throws IOException;
        V get(K key) throws IOException;
    }

    interface EntityTable extends Table<Reference<Entity>, Entity>, Flush {
        SequencedCollection<ReferenceItemPair<Entity>> list(int offset, int limit) throws IOException;
    }

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
    }

    interface ModelDirectory extends DirectoryAccessor, Flush {
        void log(String event, Sensitivity sensitivity, Item ...args);
    }

    void entity(Consumer<EntityTable> callback);
    void transaction(Consumer<TransactionTable> callback);
    void model(Consumer<ModelDirectory> callback);

    CompletableFuture<Void> close();
}
