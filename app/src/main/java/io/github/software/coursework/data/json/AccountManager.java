package io.github.software.coursework.data.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.Deserialize;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * This class manages the accounts of the user.
 * An account manager manages the accounts of a user.
 * A user will see same list regardless of which working directory they are in.
 * Only access this class in Decryption related GUI classes.
 */
@ParametersAreNonnullByDefault
public final class AccountManager {
    /**
     * This class represents an account.
     * @param name the name of the account, displayed in the decryption GUI
     * @param path the path of the account, used to store the files
     * @param key the path of the key file, used to decrypt the files
     */
    public record Account(String name, String path, String key) implements Item, Comparable<Account> {

        @Override
        public void serialize(Document.Writer writer) throws IOException {
            writer.writeInteger("schema", 1);
            writer.writeString("name", name);
            writer.writeString("path", path);
            writer.writeString("key", key);
            writer.writeEnd();
        }

        /**
         * @see Deserialize#deserialize(Document.Reader)
         */
        public static Account deserialize(Document.Reader reader) throws IOException {
            long schema = reader.readInteger("schema");
            if (schema != 1) {
                throw new IOException("Unsupported schema version: " + schema);
            }
            Account account = new Account(
                    reader.readString("name"),
                    reader.readString("path"),
                    reader.readString("key")
            );
            reader.readEnd();
            return account;
        }

        public Account withName(String name) {
            return new Account(name, path, key);
        }

        public Account withPath(String path) {
            return new Account(name, path, key);
        }

        public Account withKey(String key) {
            return new Account(name, path, key);
        }

        @Override
        public int compareTo(Account o) {
            int result = name.compareTo(o.name);
            if (result != 0) {
                return result;
            }
            result = path.compareTo(o.path);
            if (result != 0) {
                return result;
            }
            return key.compareTo(o.key);
        }
    }

    private static final String accountList = ".config/BUPT-QMUL_2025_Spring_Software_Engineering_Coursework_Group_95_Submission/accounts.json";

    @VisibleForTesting
    public AccountManager() {}

    private Account defaultAccount = null;
    private static final File home = new File(System.getProperty("user.home"));
    private static final JsonFactory jsonFactory = JsonFactory.builder().build();
    private static final Logger logger = Logger.getLogger("AccountManager");

    private final ArrayList<Account> accounts = new ArrayList<>();
    private static File accountsFile = new File(home, accountList);

    @VisibleForTesting
    public static void setAccountList(File accountList) {
        AccountManager.accountsFile = accountList;
    }

    /**
     * Get the default account.
     * @return the default account
     */
    public Account getDefaultAccount() {
        return defaultAccount;
    }

    /**
     * Set the default account.
     * @param defaultAccount the default account
     */
    public void setDefaultAccount(@Nullable Account defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    /**
     * Loads the accounts from the accounts.json file.
     */
    public void loadAccounts() {
        accounts.clear();
        if (accountsFile.exists()) {
            try (Document.Reader reader = JsonReader.createReader(jsonFactory.createParser(accountsFile))) {
                accounts.addAll(((Deserialize<Account>) Account::deserialize)
                        .asList(ArrayList::new)
                        .deserialize(reader.readCompound("list")));
                int defaultAccountRead = (int) reader.readInteger("default");
                if (defaultAccountRead >= 0 && defaultAccountRead < accounts.size()) {
                    this.defaultAccount = accounts.get(defaultAccountRead);
                } else {
                    this.defaultAccount = null;
                }
                reader.readEnd();
            } catch (IOException e) {
                logger.warning("Ignoring malformed accounts file: " + e);
                accounts.clear();
            }
        }
    }

    /**
     * Save the list of accounts to the accounts.json file.
     */
    public void saveAccounts() {
        accountsFile.getParentFile().mkdirs();
        try (Document.Writer writer = JsonWriter.createWriter(jsonFactory.createGenerator(accountsFile, JsonEncoding.UTF8))) {
            Item.asList(accounts).serialize(writer.writeCompound("list"));
            writer.writeInteger("default", accounts.indexOf(defaultAccount));
            writer.writeEnd();
        } catch (IOException e) {
            logger.warning("Failed to save accounts: " + e);
        }
    }

    /**
     * Add an account to the list of accounts.
     * Notice this will not flush the changes to the file.
     * @param account the account to add
     */
    public void addAccount(Account account) {
        accounts.add(account);
    }

    /**
     * Remove an account from the list of accounts.
     * Notice this will not flush the changes to the file.
     * @param account the account to remove
     */
    public void removeAccount(Account account) {
        accounts.remove(account);
    }

    /**
     * Create a new account.
     * @param name the name of the account
     * @param password the password of the account
     * @return the account created
     */
    public @Nullable Account makeAccount(String name, String password) {
        File workingDir = accountsFile.getParentFile();
        if (!workingDir.exists()) {
            if (!workingDir.mkdirs()) {
                logger.warning("Failed to create directory: " + workingDir);
                return null;
            }
        }
        String pathName = name.replaceAll("[^a-zA-Z0-9]", "_");
        if (!new File(workingDir, pathName).exists()) {
            return makeAccount(name, new File(workingDir, pathName), new File(workingDir, pathName + "/secret.key"), password);
        }
        int i = 1;
        while (new File(workingDir, pathName + "." + i).exists()) {
            i++;
        }
        return makeAccount(name, new File(workingDir, pathName + "." + i), new File(workingDir, pathName + "." + i + "/secret.key"), password);
    }

    /**
     * Create a new account.
     * @param name the name of the account
     * @param directory the directory of the account
     * @param key the key file of the account
     * @param password the password of the account
     * @return the account created
     */
    public @Nullable Account makeAccount(String name, File directory, File key, String password) {
        try {
            directory.mkdirs();
            key.getParentFile().mkdirs();
            Files.writeString(key.toPath(), Encryption.writeKeyFile(password, null));
            if (!directory.exists()) {
                logger.warning("Failed to create directory: " + directory);
                return null;
            }
            return new Account(name, directory.getAbsolutePath(), key.getAbsolutePath());
        } catch (IOException e) {
            logger.warning("Failed to write key file: " + e);
            return null;
        }
    }

    /**
     * Get the list of accounts.
     * @return the list of accounts
     */
    public ImmutableList<Account> getAccounts() {
        return ImmutableList.copyOf(accounts);
    }

    private static AccountManager manager;

    /**
     * Get the singleton instance of the AccountManager.
     * @return the singleton instance of the AccountManager
     */
    public static AccountManager getManager() {
        if (manager == null) {
            manager = new AccountManager();
        }
        return manager;
    }
}
