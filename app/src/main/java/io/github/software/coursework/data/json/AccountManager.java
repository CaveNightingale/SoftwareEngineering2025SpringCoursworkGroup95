package io.github.software.coursework.data.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.collect.ImmutableList;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Item;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public final class AccountManager {
    public record Account(String name, String path, String key) implements Item<Account>, Comparable<Account> {

        @Override
        public void serialize(Document.Writer writer) throws IOException {
            writer.writeString("name", name);
            writer.writeString("path", path);
            writer.writeString("key", key);
            writer.writeEnd();
        }

        public static Account deserialize(Document.Reader reader) throws IOException {
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

    private AccountManager() {}

    public static final String ACCOUNTS_LIST = ".config/BUPT-QMUL_2025_Spring_Software_Engineering_Coursework_Group_95_Submission/accounts.json";

    private static final TreeSet<Account> accounts = new TreeSet<>();
    private static final File home = new File(System.getProperty("user.home"));
    private static final JsonFactory jsonFactory = JsonFactory.builder().build();
    private static final Logger logger = Logger.getLogger("AccountManager");

    private final File accountsFile = new File(home, ACCOUNTS_LIST);

    public void loadAccounts() {
        accounts.clear();
        if (accountsFile.exists()) {
            try (Document.Reader reader = JsonReader.createReader(jsonFactory.createParser(accountsFile))) {
                for (int i = 0; !reader.isEnd(); i++) {
                    Account account = Account.deserialize(reader.readCompound(i));
                    if (!Files.isDirectory(Path.of(account.path())) || !Files.isReadable(Path.of(account.key()))) {
                        continue;
                    }
                    accounts.add(account);
                }
                reader.readEnd();
            } catch (IOException e) {
                logger.warning("Ignoring malformed accounts file: " + e);
                accounts.clear();
            }
        }
    }

    public void saveAccounts() {
        accountsFile.getParentFile().mkdirs();
        try (Document.Writer writer = JsonWriter.createWriter(jsonFactory.createGenerator(accountsFile, JsonEncoding.UTF8))) {
            int i = 0;
            for (Account account : accounts) {
                account.serialize(writer.writeCompound(i++));
            }
            writer.writeEnd();
        } catch (IOException e) {
            logger.warning("Failed to save accounts: " + e);
        }
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
    }

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
            return makeAccount(name, new File(workingDir, pathName), new File(workingDir, pathName + ".key"), password);
        }
        int i = 1;
        while (new File(workingDir, pathName + "." + i).exists()) {
            i++;
        }
        return makeAccount(name, new File(workingDir, pathName + "." + i), new File(workingDir, pathName + "." + i + ".key"), password);
    }

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

    public ImmutableList<Account> getAccounts() {
        return ImmutableList.copyOf(accounts);
    }

    private static AccountManager manager;

    public static AccountManager getManager() {
        if (manager == null) {
            manager = new AccountManager();
        }
        return manager;
    }
}
