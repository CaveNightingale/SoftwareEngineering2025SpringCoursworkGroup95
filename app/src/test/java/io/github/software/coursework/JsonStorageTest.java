package io.github.software.coursework;

import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.*;
import io.github.software.coursework.data.json.AccountManager;
import io.github.software.coursework.data.json.Encryption;
import io.github.software.coursework.data.json.JsonStorage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Goal;
import io.github.software.coursework.data.schema.Transaction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JsonStorageTest {

    @TempDir
    Path tempDir;

    private File accountDir;
    private AccountManager.Account testAccount;
    private JsonStorage storage;
    private static final String TEST_PASSWORD = "testpassword";

    @BeforeEach
    void setUp() throws Exception {
        // Create account directory structure
        accountDir = new File(tempDir.toFile(), "test_account");
        accountDir.mkdirs();

        // Create key file
        File keyFile = new File(accountDir, "secret.key");
        Files.writeString(keyFile.toPath(), Encryption.writeKeyFile(TEST_PASSWORD, null));

        // Create test account
        testAccount = new AccountManager.Account("Test Account", accountDir.getAbsolutePath(), keyFile.getAbsolutePath());

        // Initialize storage
        storage = new JsonStorage(testAccount, TEST_PASSWORD);

        // Wait a bit for initialization to complete
        Thread.sleep(500);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Close storage
        CompletableFuture<Void> future = storage.close();
        future.get(5, TimeUnit.SECONDS);  // Wait for close to complete
    }

    @Test
    void testEntityOperations() throws Exception {
        // Create a test entity
        Entity testEntity = new Entity("Test Entity", "Test description", "test email", "test phone", "test address", Entity.Type.COMMERCIAL);
        Reference<Entity> entityRef = new Reference<>();

        // CountDownLatch to wait for async operations
        CountDownLatch putLatch = new CountDownLatch(1);
        CountDownLatch getLatch = new CountDownLatch(1);
        CountDownLatch listLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);

        // Add entity
        AtomicReference<Entity> oldEntityRef = new AtomicReference<>();
        storage.entity(entityTable -> {
            try {
                oldEntityRef.set(entityTable.put(entityRef, AsyncStorage.Sensitivity.NORMAL, testEntity));
                putLatch.countDown();
            } catch (IOException e) {
                fail("Failed to put entity: " + e.getMessage());
            }
        });

        assertTrue(putLatch.await(5, TimeUnit.SECONDS), "Put operation timed out");
        assertNull(oldEntityRef.get(), "Old entity should be null");

        // Get entity
        AtomicReference<Entity> retrievedEntityRef = new AtomicReference<>();
        storage.entity(entityTable -> {
            try {
                retrievedEntityRef.set(entityTable.get(entityRef));
                getLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get entity: " + e.getMessage());
            }
        });

        assertTrue(getLatch.await(5, TimeUnit.SECONDS), "Get operation timed out");
        assertNotNull(retrievedEntityRef.get(), "Retrieved entity should not be null");
        assertEquals("Test Entity", retrievedEntityRef.get().name(), "Entity title should match");

        // List entities
        AtomicReference<SequencedCollection<ReferenceItemPair<Entity>>> entitiesRef = new AtomicReference<>();
        storage.entity(entityTable -> {
            try {
                entitiesRef.set(entityTable.list(0, 10));
                listLatch.countDown();
            } catch (IOException e) {
                fail("Failed to list entities: " + e.getMessage());
            }
        });

        assertTrue(listLatch.await(5, TimeUnit.SECONDS), "List operation timed out");
        assertNotNull(entitiesRef.get(), "Entity list should not be null");
        assertEquals(1, entitiesRef.get().size(), "Entity list should have one item");
        assertEquals(entityRef.id(), entitiesRef.get().getFirst().reference().id(), "Entity ID should match");

        // Remove entity
        AtomicReference<Entity> removedEntityRef = new AtomicReference<>();
        storage.entity(entityTable -> {
            try {
                removedEntityRef.set(entityTable.put(entityRef, AsyncStorage.Sensitivity.NORMAL, null));
                removeLatch.countDown();
            } catch (IOException e) {
                fail("Failed to remove entity: " + e.getMessage());
            }
        });

        assertTrue(removeLatch.await(5, TimeUnit.SECONDS), "Remove operation timed out");
        assertNotNull(removedEntityRef.get(), "Removed entity should not be null");
        assertEquals("Test Entity", removedEntityRef.get().name(), "Removed entity title should match");
    }

    @Test
    void testTransactionOperations() throws Exception {
        // Create a test transaction
        Transaction testTransaction = new Transaction(
                "Test Transaction",
                "Test note",
                System.currentTimeMillis(),
                1000,
                "Test category",
                null,
                ImmutableList.of("Test tag")
        );
        Reference<Transaction> transactionRef = new Reference<>();

        // CountDownLatch to wait for async operations
        CountDownLatch putLatch = new CountDownLatch(1);
        CountDownLatch getLatch = new CountDownLatch(1);
        CountDownLatch listLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);

        // Add transaction
        AtomicReference<Transaction> oldTransactionRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                oldTransactionRef.set(transactionTable.put(transactionRef, AsyncStorage.Sensitivity.NORMAL, testTransaction));
                putLatch.countDown();
            } catch (IOException e) {
                fail("Failed to put transaction: " + e.getMessage());
            }
        });

        assertTrue(putLatch.await(5, TimeUnit.SECONDS), "Put operation timed out");
        assertNull(oldTransactionRef.get(), "Old transaction should be null");

        // Get transaction
        AtomicReference<Transaction> retrievedTransactionRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                retrievedTransactionRef.set(transactionTable.get(transactionRef));
                getLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get transaction: " + e.getMessage());
            }
        });

        assertTrue(getLatch.await(5, TimeUnit.SECONDS), "Get operation timed out");
        assertNotNull(retrievedTransactionRef.get(), "Retrieved transaction should not be null");
        assertEquals("Test Transaction", retrievedTransactionRef.get().title(), "Transaction title should match");

        // List transactions
        AtomicReference<SequencedCollection<ReferenceItemPair<Transaction>>> transactionsRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                transactionsRef.set(transactionTable.list(0, Long.MAX_VALUE, 0, 10));
                listLatch.countDown();
            } catch (IOException e) {
                fail("Failed to list transactions: " + e.getMessage());
            }
        });

        assertTrue(listLatch.await(5, TimeUnit.SECONDS), "List operation timed out");
        assertNotNull(transactionsRef.get(), "Transaction list should not be null");
        assertEquals(1, transactionsRef.get().size(), "Transaction list should have one item");
        assertEquals(transactionRef.id(), transactionsRef.get().getFirst().reference().id(), "Transaction ID should match");

        // Remove transaction
        AtomicReference<Transaction> removedTransactionRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                removedTransactionRef.set(transactionTable.put(transactionRef, AsyncStorage.Sensitivity.NORMAL, null));
                removeLatch.countDown();
            } catch (IOException e) {
                fail("Failed to remove transaction: " + e.getMessage());
            }
        });

        assertTrue(removeLatch.await(5, TimeUnit.SECONDS), "Remove operation timed out");
        assertNotNull(removedTransactionRef.get(), "Removed transaction should not be null");
        assertEquals("Test Transaction", removedTransactionRef.get().title(), "Removed transaction title should match");
    }

    @Test
    void testCategoriesAndTags() throws Exception {
        CountDownLatch categoriesLatch = new CountDownLatch(1);
        CountDownLatch addCategoryLatch = new CountDownLatch(1);
        CountDownLatch tagsLatch = new CountDownLatch(1);
        CountDownLatch addTagLatch = new CountDownLatch(1);

        // Get categories
        AtomicReference<ImmutablePair<Set<String>, Set<String>>> categoriesRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                categoriesRef.set(transactionTable.getCategories());
                categoriesLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get categories: " + e.getMessage());
            }
        });

        assertTrue(categoriesLatch.await(5, TimeUnit.SECONDS), "Get categories operation timed out");
        assertNotNull(categoriesRef.get(), "Categories should not be null");

        // The default categories list should be initialized
        assertTrue(categoriesRef.get().left.contains("Accommodation"), "Default categories should be initialized");
        assertTrue(categoriesRef.get().left.contains("Transportation"), "Default categories should be initialized");

        // Add a new category
        storage.transaction(transactionTable -> {
            try {
                transactionTable.addCategory("New Test Category", AsyncStorage.Sensitivity.NORMAL);
                addCategoryLatch.countDown();
            } catch (IOException e) {
                fail("Failed to add category: " + e.getMessage());
            }
        });

        assertTrue(addCategoryLatch.await(5, TimeUnit.SECONDS), "Add category operation timed out");

        // Get tags
        AtomicReference<ImmutablePair<Set<String>, Set<String>>> tagsRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                tagsRef.set(transactionTable.getTags());
                tagsLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get tags: " + e.getMessage());
            }
        });

        assertTrue(tagsLatch.await(5, TimeUnit.SECONDS), "Get tags operation timed out");
        assertNotNull(tagsRef.get(), "Tags should not be null");

        // The default tags list should be initialized
        assertTrue(tagsRef.get().left.contains("Valentine's Day"), "Default tags should be initialized");
        assertTrue(tagsRef.get().left.contains("Christmas Day"), "Default tags should be initialized");

        // Add a new tag
        storage.transaction(transactionTable -> {
            try {
                transactionTable.addTag("New Test Tag", AsyncStorage.Sensitivity.NORMAL);
                addTagLatch.countDown();
            } catch (IOException e) {
                fail("Failed to add tag: " + e.getMessage());
            }
        });

        assertTrue(addTagLatch.await(5, TimeUnit.SECONDS), "Add tag operation timed out");
    }

    @Test
    void testGoalOperations() throws Exception {
        CountDownLatch getGoalLatch = new CountDownLatch(1);
        CountDownLatch setGoalLatch = new CountDownLatch(1);
        CountDownLatch clearGoalLatch = new CountDownLatch(1);

        // Initially, goal should be null
        AtomicReference<Goal> initialGoalRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                initialGoalRef.set(transactionTable.getGoal());
                getGoalLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get goal: " + e.getMessage());
            }
        });

        assertTrue(getGoalLatch.await(5, TimeUnit.SECONDS), "Get goal operation timed out");
        assertNull(initialGoalRef.get(), "Initial goal should be null");

        // Set a goal
        Goal testGoal = new Goal(1000L, 5000L, 5000, 6000, ImmutableList.of());
        storage.transaction(transactionTable -> {
            try {
                transactionTable.setGoal(testGoal, AsyncStorage.Sensitivity.NORMAL);
                setGoalLatch.countDown();
            } catch (IOException e) {
                fail("Failed to set goal: " + e.getMessage());
            }
        });

        assertTrue(setGoalLatch.await(5, TimeUnit.SECONDS), "Set goal operation timed out");

        // Get the goal again and verify
        AtomicReference<Goal> retrievedGoalRef = new AtomicReference<>();
        CountDownLatch getUpdatedGoalLatch = new CountDownLatch(1);
        storage.transaction(transactionTable -> {
            try {
                retrievedGoalRef.set(transactionTable.getGoal());
                getUpdatedGoalLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get updated goal: " + e.getMessage());
            }
        });

        assertTrue(getUpdatedGoalLatch.await(5, TimeUnit.SECONDS), "Get updated goal operation timed out");
        assertNotNull(retrievedGoalRef.get(), "Retrieved goal should not be null");
        assertEquals(1000L, retrievedGoalRef.get().start(), "Goal title should match");
        assertEquals(5000L, retrievedGoalRef.get().end(), "Goal target should match");

        // Clear the goal
        storage.transaction(transactionTable -> {
            try {
                transactionTable.setGoal(null, AsyncStorage.Sensitivity.NORMAL);
                clearGoalLatch.countDown();
            } catch (IOException e) {
                fail("Failed to clear goal: " + e.getMessage());
            }
        });

        assertTrue(clearGoalLatch.await(5, TimeUnit.SECONDS), "Clear goal operation timed out");

        // Verify goal is cleared
        AtomicReference<Goal> clearedGoalRef = new AtomicReference<>();
        CountDownLatch getClearedGoalLatch = new CountDownLatch(1);
        storage.transaction(transactionTable -> {
            try {
                clearedGoalRef.set(transactionTable.getGoal());
                getClearedGoalLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get cleared goal: " + e.getMessage());
            }
        });

        assertTrue(getClearedGoalLatch.await(5, TimeUnit.SECONDS), "Get cleared goal operation timed out");
        assertNull(clearedGoalRef.get(), "Cleared goal should be null");
    }

    @Test
    void testModelDirectoryOperations() throws Exception {
        CountDownLatch putLatch = new CountDownLatch(1);
        CountDownLatch getLatch = new CountDownLatch(1);

        // Create a test item
        TestItem testItem = new TestItem("Test Value");

        // Store the item
        storage.model(modelDirectory -> {
            try {
                modelDirectory.put("test-item", testItem);
                putLatch.countDown();
            } catch (IOException e) {
                fail("Failed to put model item: " + e.getMessage());
            }
        });

        assertTrue(putLatch.await(5, TimeUnit.SECONDS), "Put model item operation timed out");

        // Retrieve the item
        AtomicReference<TestItem> retrievedItemRef = new AtomicReference<>();
        storage.model(modelDirectory -> {
            try {
                retrievedItemRef.set(modelDirectory.get("test-item", TestItem::deserialize));
                getLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get model item: " + e.getMessage());
            }
        });

        assertTrue(getLatch.await(5, TimeUnit.SECONDS), "Get model item operation timed out");
        assertNotNull(retrievedItemRef.get(), "Retrieved model item should not be null");
        assertEquals("Test Value", retrievedItemRef.get().value, "Model item value should match");
    }

    // Helper test item class for model directory tests
    private static class TestItem implements Item {
        private final String value;

        public TestItem(String value) {
            this.value = value;
        }

        @Override
        public void serialize(Document.Writer writer) throws IOException {
            writer.writeString("value", value);
            writer.writeEnd();
        }

        public static TestItem deserialize(Document.Reader reader) throws IOException {
            String value = reader.readString("value");
            reader.readEnd();
            return new TestItem(value);
        }
    }

    @Test
    void testTransactionWithCategoryAndTag() throws Exception {
        // First add a transaction with a category and tag
        Transaction testTransaction = new Transaction(
                "Transaction with Category and Tag",
                "Test note",
                System.currentTimeMillis(),
                2000,
                "Electronics", // Using one of the default categories
                null,
                ImmutableList.of("Christmas Day") // Using one of the default tags
        );
        Reference<Transaction> transactionRef = new Reference<>();

        CountDownLatch putLatch = new CountDownLatch(1);
        storage.transaction(transactionTable -> {
            try {
                transactionTable.put(transactionRef, AsyncStorage.Sensitivity.NORMAL, testTransaction);
                putLatch.countDown();
            } catch (IOException e) {
                fail("Failed to put transaction: " + e.getMessage());
            }
        });

        assertTrue(putLatch.await(5, TimeUnit.SECONDS), "Put operation timed out");

        // Now check if the category and tag are counted as used
        CountDownLatch categoriesLatch = new CountDownLatch(1);
        CountDownLatch tagsLatch = new CountDownLatch(1);

        AtomicReference<ImmutablePair<Set<String>, Set<String>>> categoriesRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                categoriesRef.set(transactionTable.getCategories());
                categoriesLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get categories: " + e.getMessage());
            }
        });

        assertTrue(categoriesLatch.await(5, TimeUnit.SECONDS), "Get categories operation timed out");
        assertTrue(categoriesRef.get().right.contains("Electronics"),
                "Electronics category should be marked as used");

        AtomicReference<ImmutablePair<Set<String>, Set<String>>> tagsRef = new AtomicReference<>();
        storage.transaction(transactionTable -> {
            try {
                tagsRef.set(transactionTable.getTags());
                tagsLatch.countDown();
            } catch (IOException e) {
                fail("Failed to get tags: " + e.getMessage());
            }
        });

        assertTrue(tagsLatch.await(5, TimeUnit.SECONDS), "Get tags operation timed out");
        assertTrue(tagsRef.get().right.contains("Christmas Day"),
                "Christmas Day tag should be marked as used");
    }

    @Test
    void testRemoveCategoryInUse() throws Exception {
        // First add a transaction with a category
        Transaction testTransaction = new Transaction(
                "Transaction with Category",
                "Test note",
                System.currentTimeMillis(),
                3000,
                "Education", // Using one of the default categories
                null,
                ImmutableList.of()
        );
        Reference<Transaction> transactionRef = new Reference<>();

        CountDownLatch putLatch = new CountDownLatch(1);
        storage.transaction(transactionTable -> {
            try {
                transactionTable.put(transactionRef, AsyncStorage.Sensitivity.NORMAL, testTransaction);
                putLatch.countDown();
            } catch (IOException e) {
                fail("Failed to put transaction: " + e.getMessage());
            }
        });

        assertTrue(putLatch.await(5, TimeUnit.SECONDS), "Put operation timed out");

        // Try to remove the category that's in use
        CountDownLatch removeLatch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        storage.transaction(transactionTable -> {
            try {
                transactionTable.removeCategory("Education", AsyncStorage.Sensitivity.NORMAL);
            } catch (Exception e) {
                exceptionRef.set(e);
            } finally {
                removeLatch.countDown();
            }
        });

        assertTrue(removeLatch.await(5, TimeUnit.SECONDS), "Remove category operation timed out");
        assertNotNull(exceptionRef.get(), "Exception should be thrown when removing category in use");
        assertInstanceOf(SyntaxException.class, exceptionRef.get(), "Exception should be SyntaxException");
        assertTrue(exceptionRef.get().getMessage().contains("in use"),
                "Exception should mention category is in use");
    }

    @Test
    void testRemoveTagInUse() throws Exception {
        // First add a transaction with a tag
        Transaction testTransaction = new Transaction(
                "Transaction with Tag",
                "Test note",
                System.currentTimeMillis(),
                1500,
                "Diet",
                null,
                ImmutableList.of("Valentine's Day") // Using one of the default tags
        );
        Reference<Transaction> transactionRef = new Reference<>();

        CountDownLatch putLatch = new CountDownLatch(1);
        storage.transaction(transactionTable -> {
            try {
                transactionTable.put(transactionRef, AsyncStorage.Sensitivity.NORMAL, testTransaction);
                putLatch.countDown();
            } catch (IOException e) {
                fail("Failed to put transaction: " + e.getMessage());
            }
        });

        assertTrue(putLatch.await(5, TimeUnit.SECONDS), "Put operation timed out");

        // Try to remove the tag that's in use
        CountDownLatch removeLatch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        storage.transaction(transactionTable -> {
            try {
                transactionTable.removeTag("Valentine's Day", AsyncStorage.Sensitivity.NORMAL);
            } catch (Exception e) {
                exceptionRef.set(e);
            } finally {
                removeLatch.countDown();
            }
        });

        assertTrue(removeLatch.await(5, TimeUnit.SECONDS), "Remove tag operation timed out");
        assertNotNull(exceptionRef.get(), "Exception should be thrown when removing tag in use");
        assertInstanceOf(SyntaxException.class, exceptionRef.get(), "Exception should be SyntaxException");
        assertTrue(exceptionRef.get().getMessage().contains("in use"),
                "Exception should mention tag is in use");
    }
}