package io.github.software.coursework;

import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.json.AccountManager;
import io.github.software.coursework.data.json.JsonReader;
import io.github.software.coursework.data.json.JsonWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AccountManagerTest {

    @TempDir
    Path tempDir;

    private File accountsFile;
    private AccountManager manager;

    @BeforeEach
    void setUp() {
        accountsFile = new File(tempDir.toFile(), "accounts.json");
        AccountManager.setAccountList(accountsFile);
        manager = AccountManager.getManager();
        manager.loadAccounts();
    }

    @AfterEach
    void tearDown() {
        // Reset the AccountManager state
        manager.getAccounts().forEach(manager::removeAccount);
        manager.setDefaultAccount(null);
        manager.saveAccounts();
    }

    @Test
    void testAddAndRemoveAccount() {
        // Create a test account
        AccountManager.Account account = new AccountManager.Account("Test Account", "/test/path", "/test/key");

        // Add the account
        manager.addAccount(account);
        assertEquals(1, manager.getAccounts().size());
        assertEquals(account, manager.getAccounts().get(0));

        // Remove the account
        manager.removeAccount(account);
        assertEquals(0, manager.getAccounts().size());
    }

    @Test
    void testSetAndGetDefaultAccount() {
        // Create a test account
        AccountManager.Account account = new AccountManager.Account("Test Account", "/test/path", "/test/key");

        // Add the account and set as default
        manager.addAccount(account);
        manager.setDefaultAccount(account);

        assertEquals(account, manager.getDefaultAccount());

        // Set to null
        manager.setDefaultAccount(null);
        assertNull(manager.getDefaultAccount());
    }

    @Test
    void testSaveAndLoadAccounts() throws Exception {
        // Create some test accounts
        AccountManager.Account account1 = new AccountManager.Account("Account 1", "/path/1", "/key/1");
        AccountManager.Account account2 = new AccountManager.Account("Account 2", "/path/2", "/key/2");

        // Add accounts and set default
        manager.addAccount(account1);
        manager.addAccount(account2);
        manager.setDefaultAccount(account1);

        // Save accounts
        manager.saveAccounts();

        // Verify file was created
        assertTrue(accountsFile.exists());

        // Create a new manager instance to test loading
        AccountManager newManager = new AccountManager();
        AccountManager.setAccountList(accountsFile);
        newManager.loadAccounts();

        // Verify accounts were loaded correctly
        assertEquals(2, newManager.getAccounts().size());
        assertTrue(newManager.getAccounts().contains(account1));
        assertTrue(newManager.getAccounts().contains(account2));
        assertEquals(account1, newManager.getDefaultAccount());
    }

    @Test
    void testMakeAccount() {
        // Make an account with a password
        AccountManager.Account account = manager.makeAccount("Test Account", "password123");

        assertNotNull(account);
        assertEquals("Test Account", account.name());

        // Verify the directory and key file were created
        File accountDir = new File(account.path());
        File keyFile = new File(account.key());

        assertTrue(accountDir.exists());
        assertTrue(keyFile.exists());
    }

    @Test
    void testMakeAccountWithExistingName() {
        // Create first account
        AccountManager.Account account1 = manager.makeAccount("Same Name", "password123");
        assertNotNull(account1);

        // Create another account with the same name
        AccountManager.Account account2 = manager.makeAccount("Same Name", "password456");
        assertNotNull(account2);

        // Paths should be different
        assertNotEquals(account1.path(), account2.path());
        assertNotEquals(account1.key(), account2.key());
    }

    @Test
    void testAccountRecord() {
        AccountManager.Account account = new AccountManager.Account("Test", "/path", "/key");

        // Test withX methods
        AccountManager.Account withNewName = account.withName("New Name");
        assertEquals("New Name", withNewName.name());
        assertEquals(account.path(), withNewName.path());
        assertEquals(account.key(), withNewName.key());

        AccountManager.Account withNewPath = account.withPath("/new/path");
        assertEquals(account.name(), withNewPath.name());
        assertEquals("/new/path", withNewPath.path());
        assertEquals(account.key(), withNewPath.key());

        AccountManager.Account withNewKey = account.withKey("/new/key");
        assertEquals(account.name(), withNewKey.name());
        assertEquals(account.path(), withNewKey.path());
        assertEquals("/new/key", withNewKey.key());
    }

    @Test
    void testAccountComparable() {
        AccountManager.Account a1 = new AccountManager.Account("A", "/path", "/key");
        AccountManager.Account a2 = new AccountManager.Account("B", "/path", "/key");
        AccountManager.Account a3 = new AccountManager.Account("A", "/path2", "/key");
        AccountManager.Account a4 = new AccountManager.Account("A", "/path", "/key2");

        // Test name comparison
        assertTrue(a1.compareTo(a2) < 0);
        assertTrue(a2.compareTo(a1) > 0);

        // Test path comparison when names are equal
        assertTrue(a1.compareTo(a3) < 0);
        assertTrue(a3.compareTo(a1) > 0);

        // Test key comparison when names and paths are equal
        assertTrue(a1.compareTo(a4) < 0);
        assertTrue(a4.compareTo(a1) > 0);

        // Test equality
        assertEquals(0, a1.compareTo(new AccountManager.Account("A", "/path", "/key")));
    }

    @Test
    void testAccountSerializationDeserialization() throws Exception {
        // Create a temporary file for testing serialization
        File tempFile = new File(tempDir.toFile(), "account_serialization_test.json");
        com.fasterxml.jackson.core.JsonFactory jsonFactory = com.fasterxml.jackson.core.JsonFactory.builder().build();

        // Create an account to serialize
        AccountManager.Account originalAccount = new AccountManager.Account("Test Account", "/test/path", "/test/key");

        // Serialize the account
        try (Document.Writer writer = JsonWriter.createWriter(jsonFactory.createGenerator(tempFile, com.fasterxml.jackson.core.JsonEncoding.UTF8))) {
            originalAccount.serialize(writer);
        }

        // Deserialize the account
        AccountManager.Account deserializedAccount;
        try (Document.Reader reader = JsonReader.createReader(jsonFactory.createParser(tempFile))) {
            deserializedAccount = AccountManager.Account.deserialize(reader);
        }

        // Verify the deserialized account matches the original
        assertEquals(originalAccount.name(), deserializedAccount.name());
        assertEquals(originalAccount.path(), deserializedAccount.path());
        assertEquals(originalAccount.key(), deserializedAccount.key());
    }

    @Test
    void testGetManagerSingleton() {
        AccountManager manager1 = AccountManager.getManager();
        AccountManager manager2 = AccountManager.getManager();

        // Verify singleton instance
        assertSame(manager1, manager2);
    }

    @Test
    void testCreateAccountWithSpecificDirectoryAndKey() throws Exception {
        // Create directories for testing
        File accountDir = new File(tempDir.toFile(), "custom_account_dir");
        File keyFile = new File(accountDir, "custom.key");

        // Create an account with specific directory and key
        AccountManager.Account account = manager.makeAccount("Custom Account", accountDir, keyFile, "password");

        assertNotNull(account);
        assertEquals("Custom Account", account.name());
        assertEquals(accountDir.getAbsolutePath(), account.path());
        assertEquals(keyFile.getAbsolutePath(), account.key());

        // Verify the directory and key file were created
        assertTrue(accountDir.exists());
        assertTrue(keyFile.exists());

        // Verify key file contains expected content (basic check)
        String keyContent = Files.readString(keyFile.toPath());
        assertNotNull(keyContent);
        assertFalse(keyContent.isEmpty());
    }

    @Test
    void testAccountDeserializationWithInvalidSchema() {
        // Create a JSON string with invalid schema
        String invalidJson = "{\"schema\":2,\"name\":\"Test\",\"path\":\"/path\",\"key\":\"/key\"}";

        // Attempt to deserialize
        Exception exception = assertThrows(java.io.IOException.class, () -> {
            com.fasterxml.jackson.core.JsonFactory jsonFactory = com.fasterxml.jackson.core.JsonFactory.builder().build();
            try (Document.Reader reader = JsonReader.createReader(jsonFactory.createParser(
                    new java.io.ByteArrayInputStream(invalidJson.getBytes())))) {
                AccountManager.Account.deserialize(reader);
            }
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Unsupported schema version: 2"));
    }
}