package io.github.software.coursework.data.json;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.*;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Goal;
import io.github.software.coursework.data.schema.Transaction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class JsonStorage implements AsyncStorage {
    private static final List<String> defaultCategories = List.of(
            "Accommodation",
            "Clothing",
            "Communication",
            "Diet",
            "Education",
            "Electronics",
            "Entertainment",
            "Hobby",
            "Medical",
            "Necessities",
            "Transportation"
    );

    private static final List<String> defaultTags = List.of(
            "Chinese New Year",
            "Lantern Festival",
            "Qingming Festival",
            "Dragon Boat Festival",
            "Qixi Festival",
            "Mid-Autumn Festival",
            "Double Ninth Festival",
            "Winter Solstice",
            "Laba Festival",
            "Chinese New Year's Eve",
            "Minor New Year (Little New Year)",
            "Start of Spring (Lichun)",
            "Shangsi Festival",
            "Cold Food Festival",
            "Mazu’s Birthday",
            "Buddha’s Birthday",
            "Ghost Festival",
            "Zhongyuan Festival",
            "Teachers’ Day (Mainland China)",
            "Lichun Offering (Worshipping Spring Ox)",
            "Valentine's Day",
            "St. Patrick’s Day",
            "April Fools’ Day",
            "Easter",
            "Earth Day",
            "Mother's Day",
            "Father's Day",
            "Pride Day",
            "Independence Day (US)",
            "Halloween",
            "Thanksgiving (US)",
            "Christmas Eve",
            "Christmas Day",
            "New Year’s Eve",
            "New Year’s Day",
            "Black History Month Start",
            "International Women’s Day",
            "International Workers’ Day",
            "International Friendship Day",
            "618 Shopping Festival",
            "Double 11 (Singles’ Day)",
            "Double 12",
            "Black Friday",
            "Cyber Monday",
            "Chinese Valentine's Day Sales",
            "New Year’s Sales",
            "Back-to-School Sales",
            "National Day Golden Week Sales",
            "Women’s Day Sales",
            "Summer Mid-Year Sale"
    );

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
        public @Nullable Entity put(Reference<Entity> key, Sensitivity sensitivity, @Nullable Entity value) throws IOException {
            ReferenceItemPair<Entity> queried = first(this.entityChunkedIndex.querySamples(new ReferenceItemPair<>(key, null), null, 0, 1));
            Entity item = null;
            if (queried != null && queried.reference().equals(key)) { // Lower bound searching may return a different key
                opLogger.log("REMOVE_ENTITY", sensitivity, queried);
                this.entityChunkedIndex.removeSample(queried);
                item = queried.item();
            }
            if (value != null) {
                opLogger.log("ADD_ENTITY", sensitivity, new ReferenceItemPair<>(key, value));
                this.entityChunkedIndex.addSample(new ReferenceItemPair<>(key, value));
            }
            return item;
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

    private static final class Counting extends HashMap<String, Long> implements Item {
        public Counting() {}

        public Counting(Collection<String> keys) {
            for (String key : keys) {
                put(key, 0L);
            }
        }

        public void increment(String key, long value) {
            if (!containsKey(key)) {
                logger.warning("Key " + key + " not found, probably these data are saved by an old version of the app. However, ignoring it.");
                return;
            }
            put(key, get(key) + value);
        }

        public void increment(String key) {
            increment(key, 1);
        }

        public void decrement(String key, long value) {
            if (!containsKey(key)) {
                logger.warning("Key " + key + " not found, probably these data are saved by an old version of the app. However, ignoring it.");
                return;
            }
            put(key, get(key) - value);
        }

        public void decrement(String key) {
            decrement(key, 1);
        }

        @Override
        public void serialize(Document.Writer writer) throws IOException {
            int i = 0;
            for (Entry<String, Long> entry : entrySet()) {
                Document.Writer entryWriter = writer.writeCompound(i++);
                entryWriter.writeString("key", entry.getKey());
                entryWriter.writeInteger("value", entry.getValue());
                entryWriter.writeEnd();
            }
            writer.writeEnd();
        }

        public static Counting deserialize(Document.Reader reader) throws IOException {
            Counting counting = new Counting();
            for (int i = 0; !reader.isEnd(); i++) {
                Document.Reader entryReader = reader.readCompound(i);
                String key = entryReader.readString("key");
                long value = entryReader.readInteger("value");
                counting.put(key, value);
                entryReader.readEnd();
            }
            reader.readEnd();
            return counting;
        }
    }

    public final class JsonTransactionTable implements TransactionTable {
        private final ChunkedIndex<ReferenceItemPair<Transaction>> transactionIndex;
        private final ChunkedIndex<ReferenceItemPair<Transaction>> transactionIndexByTime;
        private final Counting categoryCount;
        private final Counting tagCount;
        private final Directory directory;

        private JsonTransactionTable(Directory directory) throws IOException {
            this.transactionIndex = new ChunkedIndex<>(
                    directory, Comparator.comparingLong(a -> a.reference().id()),
                    reader -> ReferenceItemPair.deserialize(reader, Transaction::deserialize));
            this.transactionIndexByTime = new ChunkedIndex<>(
                    directory.withNamespace("by-time"), Comparator.comparingLong((ReferenceItemPair<Transaction> a) -> a.item().time()).reversed(),
                    reader -> ReferenceItemPair.deserialize(reader, Transaction::deserialize));
            Counting categoryCount = directory.get("category", Counting::deserialize);
            if (categoryCount == null) {
                categoryCount = new Counting(defaultCategories);
                directory.put("category", categoryCount);
            }
            this.categoryCount = categoryCount;
            Counting tagCount = directory.get("tag", Counting::deserialize);
            if (tagCount == null) {
                tagCount = new Counting(defaultTags);
                directory.put("tag", tagCount);
            }
            this.tagCount = tagCount;
            this.directory = directory;
        }

        @Override
        public SequencedCollection<ReferenceItemPair<Transaction>> list(long start, long end, int offset, int limit) throws IOException {
            return transactionIndexByTime.querySamples(
                    new ReferenceItemPair<>(null, new Transaction("", "", end, 0, "", null, ImmutableList.of())),
                    new ReferenceItemPair<>(null, new Transaction("", "", start, 0, "", null, ImmutableList.of())),
                    offset, limit);
        }

        @Override
        public ImmutablePair<Set<String>, Set<String>> getCategories() throws IOException {
            return ImmutablePair.of(
                    categoryCount.keySet(),
                    categoryCount.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).collect(Collectors.toSet())
            );
        }

        @Override
        public void addCategory(String category, Sensitivity sensitivity) throws IOException {
            if (categoryCount.containsKey(category)) {
                throw new SyntaxException("Category already exists");
            } else {
                categoryCount.put(category, 0L);
                opLogger.log("ADD_CATEGORY", sensitivity, writer -> {
                    writer.writeString("category", category);
                    writer.writeEnd();
                });
                directory.put("category", categoryCount);
            }
        }

        @Override
        public void removeCategory(String category, Sensitivity sensitivity) throws IOException {
            if (!categoryCount.containsKey(category)) {
                throw new SyntaxException("Category does not exist");
            } else if (categoryCount.get(category) > 0) {
                throw new SyntaxException("Category is currently in use");
            } else {
                categoryCount.remove(category);
                opLogger.log("REMOVE_CATEGORY", sensitivity, writer -> {
                    writer.writeString("category", category);
                    writer.writeEnd();
                });
            }
        }

        @Override
        public ImmutablePair<Set<String>, Set<String>> getTags() throws IOException {
            return ImmutablePair.of(
                    tagCount.keySet(),
                    tagCount.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).collect(Collectors.toSet())
            );
        }

        @Override
        public void addTag(String tag, Sensitivity sensitivity) throws IOException {
            if (tagCount.containsKey(tag)) {
                throw new SyntaxException("Tag already exists");
            } else {
                tagCount.put(tag, 0L);
                opLogger.log("ADD_TAG", sensitivity, writer -> {
                    writer.writeString("tag", tag);
                    writer.writeEnd();
                });
                directory.put("tag", tagCount);
            }
        }

        @Override
        public void removeTag(String tag, Sensitivity sensitivity) throws IOException {
            if (!tagCount.containsKey(tag)) {
                throw new SyntaxException("Tag does not exist");
            } else if (tagCount.get(tag) > 0) {
                throw new SyntaxException("Tag is currently in use");
            } else {
                tagCount.remove(tag);
                opLogger.log("REMOVE_TAG", sensitivity, writer -> {
                    writer.writeString("tag", tag);
                    writer.writeEnd();
                });
            }
        }

        @Override
        public @Nullable Goal getGoal() throws IOException {
            try {
                return Optional.ofNullable(directory.get("goal", Goal.Optional::deserialize))
                        .map(Goal.Optional::goal).orElse(null);
            } catch (NoSuchDocumentException ex) {
                return null;
            }
        }

        @Override
        public void setGoal(@Nullable Goal goal, Sensitivity sensitivity) throws IOException {
            Goal oldGoal = getGoal();
            if (oldGoal != null) {
                opLogger.log("RESET_GOAL", sensitivity, oldGoal);
            }
            if (goal == null) {
                directory.put("goal", new Goal.Optional());
            } else {
                opLogger.log("SET_GOAL", sensitivity, goal);
                directory.put("goal", new Goal.Optional(goal));
            }
        }

        @Override
        public void flush() throws IOException {
            transactionIndexByTime.flush();
            transactionIndex.flush();
            directory.put("category", categoryCount);
            directory.put("tag", tagCount);
        }

        @Override
        public @Nullable Transaction put(Reference<Transaction> key, Sensitivity sensitivity, @Nullable Transaction value) throws IOException {
            ReferenceItemPair<Transaction> queried = first(this.transactionIndex.querySamples(new ReferenceItemPair<>(key, null), null, 0, 1));
            Transaction item = null;
            if (queried != null && queried.reference().equals(key)) { // Lower bound searching may return a different key
                opLogger.log("REMOVE_TRANSACTION", sensitivity, queried);
                categoryCount.decrement(queried.item().category());
                for (String tag : queried.item().tags()) {
                    tagCount.decrement(tag);
                }
                this.transactionIndex.removeSample(queried);
                this.transactionIndexByTime.removeSample(queried);
                item = queried.item();
            }
            if (value != null) {
                opLogger.log("ADD_TRANSACTION", sensitivity, new ReferenceItemPair<>(key, value));
                categoryCount.increment(value.category());
                for (String tag : value.tags()) {
                    tagCount.increment(tag);
                }
                this.transactionIndex.addSample(new ReferenceItemPair<>(key, value));
                this.transactionIndexByTime.addSample(new ReferenceItemPair<>(key, value));
            }
            // Statistics is updated in every put() call
            directory.put("category", categoryCount);
            directory.put("tag", tagCount);
            return item;
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
