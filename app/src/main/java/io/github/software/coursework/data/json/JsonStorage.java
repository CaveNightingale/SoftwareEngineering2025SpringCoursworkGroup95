package io.github.software.coursework.data.json;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.*;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.SequencedCollection;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JsonStorage implements AsyncStorage {
    private static final Logger logger = Logger.getLogger(JsonStorage.class.getName());

    private JsonEntityTable entityTable;
    private JsonTransactionTable transactionTable;
    private JsonModelDirectory modelDirectory;
    private final ExecutorService entityExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService transactionExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService modelExecutor = Executors.newSingleThreadExecutor();
    private final EncryptedLogger opLogger;

    private void crash(Throwable throwable) {
        logger.log(Level.SEVERE, "A fatal error has occurred in worker thread " + Thread.currentThread(), throwable);
        logger.log(Level.SEVERE, "Application will exit immediately");
        System.exit(1);
    }

    public JsonStorage(AccountManager.Account account, String password) throws IOException {
        byte[] key = Encryption.readKeyFile(password, Files.readString(Path.of(account.key())));
        if (key == null) {
            throw new IOException("Bad key file");
        }
        if (!Files.exists(Path.of(account.path()))) {
            throw new FileNotFoundException("Account path does not exist");
        }
        this.opLogger = new EncryptedLogger(new File(account.path()), key);
        entityExecutor.submit(() -> {
            Thread.currentThread().setName("Entity-IO-Worker");
            try {
                entityTable = new JsonEntityTable(new EncryptedDirectory(new File(account.path()), key, "entity"));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to load entity table", e);
                entityExecutor.shutdownNow();
            } catch (Throwable ex) {
                crash(ex);
            }
        });
        transactionExecutor.submit(() -> {
            Thread.currentThread().setName("Transaction-IO-Worker");
            try {
                transactionTable = new JsonTransactionTable(new EncryptedDirectory(new File(account.path()), key, "transaction"));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to load transaction table", e);
                transactionExecutor.shutdownNow();
            } catch (Throwable ex) {
                crash(ex);
            }
        });
        modelExecutor.submit(() -> {
            Thread.currentThread().setName("Model-IO-Worker");
            try {
                modelDirectory = new JsonModelDirectory(new EncryptedDirectory(new File(account.path()), key, "model"));
            } catch (Throwable ex) {
                crash(ex);
            }
        });
    }

    @Override
    public void entity(Consumer<EntityTable> callback) {
        entityExecutor.submit(() -> {
            try {
                callback.accept(entityTable);
            } catch (Throwable ex) {
                crash(ex);
            }
        });
    }

    @Override
    public void transaction(Consumer<TransactionTable> callback) {
        transactionExecutor.submit(() -> {
            try {
                callback.accept(transactionTable);
            } catch (Throwable ex) {
                crash(ex);
            }
        });
    }

    @Override
    public void model(Consumer<ModelDirectory> callback) {
        modelExecutor.submit(() -> {
            try {
                callback.accept(modelDirectory);
            } catch (Throwable ex) {
                crash(ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        CountDownLatch latch = new CountDownLatch(3);
        CompletableFuture<Void> future = new CompletableFuture<>();
        modelExecutor.submit(() -> {
            try {
                modelDirectory.flush();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to flush model directory", e);
            } catch (Throwable ex) {
                crash(ex);
            } finally {
                latch.countDown();
            }
        });
        modelExecutor.shutdown();
        entityExecutor.submit(() -> {
            try {
                entityTable.flush();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to flush entity table", e);
            } catch (Throwable ex) {
                crash(ex);
            } finally {
                latch.countDown();
            }
        });
        entityExecutor.shutdown();
        transactionExecutor.submit(() -> {
            try {
                transactionTable.flush();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to flush transaction table", e);
            } catch (Throwable ex) {
                crash(ex);
            } finally {
                latch.countDown();
            }
        });
        transactionExecutor.shutdown();
        Thread.ofVirtual().start(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Failed to wait for flush", e);
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
            try {
                opLogger.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to close logger", e);
                future.completeExceptionally(e);
            }
            future.complete(null);
        });
        return future;
    }

    private static <T extends Item> ReferenceItemPair<T> first(ArrayList<ReferenceItemPair<T>> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    public final class JsonEntityTable implements EntityTable {
        private final ChunkedIndex<ReferenceItemPair<Entity>> entityChunkedIndex;

        private JsonEntityTable(Directory directory) throws IOException {
            this.entityChunkedIndex = new ChunkedIndex<>(directory, Comparator.comparingLong(a -> a.reference().id()), reader -> ReferenceItemPair.deserialize(reader, Entity::deserialize));
        }

        @Override
        public SequencedCollection<ReferenceItemPair<Entity>> list(int offset, int limit) throws IOException {
            return entityChunkedIndex.querySamples(null, null, offset, limit);
        }

        @Override
        public void flush() throws IOException {
            this.entityChunkedIndex.flush();
        }

        @Override
        public void put(Reference<Entity> key, Sensitivity sensitivity, @Nullable Entity value) throws IOException {
            ReferenceItemPair<Entity> queried = first(this.entityChunkedIndex.querySamples(new ReferenceItemPair<>(key, null), null, 0, 1));
            if (queried != null && queried.reference().equals(key)) { // Lower bound searching may return a different key
                opLogger.log("REMOVE_ENTITY", sensitivity, queried);
                this.entityChunkedIndex.removeSample(queried);
            }
            if (value != null) {
                opLogger.log("ADD_ENTITY", sensitivity, new ReferenceItemPair<>(key, value));
                this.entityChunkedIndex.addSample(new ReferenceItemPair<>(key, value));
            }
        }

        @Override
        public Entity get(Reference<Entity> key) throws IOException {
            ReferenceItemPair<Entity> queried = first(this.entityChunkedIndex.querySamples(new ReferenceItemPair<>(key, null), null, 0, 1));
            if (queried != null && queried.reference().equals(key)) {
                return queried.item();
            } else {
                return null;
            }
        }
    }

    public final class JsonTransactionTable implements TransactionTable {
        private final ChunkedIndex<ReferenceItemPair<Transaction>> transactionIndex;
        private final ChunkedIndex<ReferenceItemPair<Transaction>> transactionIndexByTime;

        private JsonTransactionTable(Directory directory) throws IOException {
            this.transactionIndex = new ChunkedIndex<>(
                    directory, Comparator.comparingLong(a -> a.reference().id()),
                    reader -> ReferenceItemPair.deserialize(reader, Transaction::deserialize));
            this.transactionIndexByTime = new ChunkedIndex<>(
                    directory.withNamespace("by-time"), Comparator.comparingLong((ReferenceItemPair<Transaction> a) -> a.item().time()).reversed(),
                    reader -> ReferenceItemPair.deserialize(reader, Transaction::deserialize));
        }

        @Override
        public SequencedCollection<ReferenceItemPair<Transaction>> list(long start, long end, int offset, int limit) throws IOException {
            return transactionIndexByTime.querySamples(
                    new ReferenceItemPair<>(null, new Transaction("", "", end, 0, "", null, ImmutableList.of())),
                    new ReferenceItemPair<>(null, new Transaction("", "", start, 0, "", null, ImmutableList.of())),
                    offset, limit);
        }

        @Override
        public void flush() throws IOException {
            transactionIndexByTime.flush();
            transactionIndex.flush();
        }

        @Override
        public void put(Reference<Transaction> key, Sensitivity sensitivity, @Nullable Transaction value) throws IOException {
            ReferenceItemPair<Transaction> queried = first(this.transactionIndex.querySamples(new ReferenceItemPair<>(key, null), null, 0, 1));
            if (queried != null && queried.reference().equals(key)) { // Lower bound searching may return a different key
                opLogger.log("REMOVE_TRANSACTION", sensitivity, queried);
                this.transactionIndex.removeSample(queried);
                this.transactionIndexByTime.removeSample(queried);
            }
            if (value != null) {
                opLogger.log("ADD_TRANSACTION", sensitivity, new ReferenceItemPair<>(key, value));
                this.transactionIndex.addSample(new ReferenceItemPair<>(key, value));
                this.transactionIndexByTime.addSample(new ReferenceItemPair<>(key, value));
            }
        }

        @Override
        public Transaction get(Reference<Transaction> key) throws IOException {
            ReferenceItemPair<Transaction> queried = first(this.transactionIndex.querySamples(new ReferenceItemPair<>(key, null), null, 0, 1));
            if (queried != null && queried.reference().equals(key)) {
                return queried.item();
            } else {
                return null;
            }
        }
    }

    public final class JsonModelDirectory implements ModelDirectory {
        Directory backing;
        private JsonModelDirectory(Directory directory) {
            this.backing = directory;
        }

        @Override
        public void flush() throws IOException {
            backing.flush();
        }

        @Override
        public <T extends Item> @Nullable T get(String name, Deserialize<T> constructor) throws IOException {
            return backing.get(name, constructor);
        }

        @Override
        public <T extends Item> void put(String name, @Nullable T item) throws IOException {
            backing.put(name, item);
        }

        @Override
        public void log(String event, Sensitivity sensitivity, Item... args) {
            opLogger.log(event, sensitivity, args);
        }
    }
}
