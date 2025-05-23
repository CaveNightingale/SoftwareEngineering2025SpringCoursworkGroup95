package io.github.software.coursework.gui;

import io.github.software.coursework.data.json.AccountManager;
import io.github.software.coursework.data.json.EncryptedDirectory;
import io.github.software.coursework.data.json.Encryption;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

@ParametersAreNonnullByDefault
public final class EncryptionSettingModel {
    private AccountManager.Account account;
    private final String password;

    private final ObjectProperty<EventHandler<RequestRestartEvent>> onRequestRestart = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<RequestOpenLogEvent>> onRequestOpenLog = new SimpleObjectProperty<>();

    public EncryptionSettingModel(AccountManager.Account account, String password) {
        this.account = account;
        this.password = password;
    }

    public AccountManager.Account getAccount() {
        return account;
    }

    public String getPassword() {
        return password;
    }

    public ObjectProperty<EventHandler<RequestRestartEvent>> onRequestRestartProperty() {
        return onRequestRestart;
    }

    public EventHandler<RequestRestartEvent> getOnRequestRestart() {
        return onRequestRestartProperty().get();
    }

    public void setOnRequestRestart(EventHandler<RequestRestartEvent> value) {
        onRequestRestartProperty().set(value);
    }

    public ObjectProperty<EventHandler<RequestOpenLogEvent>> onRequestOpenLogProperty() {
        return onRequestOpenLog;
    }

    public EventHandler<RequestOpenLogEvent> getOnRequestOpenLog() {
        return onRequestOpenLogProperty().get();
    }

    public void setOnRequestOpenLog(EventHandler<RequestOpenLogEvent> value) {
        onRequestOpenLogProperty().set(value);
    }

    public void deleteAccount(boolean purgeFiles) throws IOException {
        AccountManager.getManager().removeAccount(account);
        AccountManager.getManager().setDefaultAccount(null);
        AccountManager.getManager().saveAccounts();

        if (purgeFiles) {
            Files.walkFileTree(Path.of(account.path()), new FileVisitor<>() {
                @Override
                public @NonNull FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFileFailed(Path file, @Nullable IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public void backupAccount() throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String backupName = String.format(
                "backup-%04d-%02d-%02d-%02d-%02d-%02d.bak",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
        );
        File backupFile = new File(account.path(), backupName);
        try (PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(backupFile)), false, StandardCharsets.UTF_8)) {
            for (File child : Objects.requireNonNull(new File(account.path()).listFiles())) {
                if (child.isDirectory() || !child.getName().endsWith(".txt")) {
                    continue;
                }
                printStream.println(child.getName());
                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(child))) {
                    bufferedInputStream.transferTo(printStream);
                }
            }
        }
    }

    public void exportAccount(File directory, boolean exportKey) throws IOException {
        for (File child : Objects.requireNonNull(new File(account.path()).listFiles())) {
            if (child.isDirectory() || !child.getName().endsWith(".txt")) {
                continue;
            }
            Files.copy(child.toPath(), new File(directory, child.getName()).toPath());
        }
        if (exportKey) {
            Files.copy(Path.of(account.key()), new File(directory, "secret.key").toPath());
        }
    }

    public void renameAccount(String newName) {
        AccountManager manager = AccountManager.getManager();
        AccountManager.Account newAccount = account.withName(newName);
        manager.removeAccount(account);
        manager.addAccount(newAccount);
        manager.setDefaultAccount(newAccount);
        manager.saveAccounts();
        account = newAccount;
    }

    public void moveData(String newPath, boolean moveKey, boolean moveBackup) throws IOException {
        Path newDirectory = Path.of(newPath);
        if (!Files.exists(newDirectory)) {
            Files.createDirectories(newDirectory);
        }

        for (File child : Objects.requireNonNull(new File(account.path()).listFiles())) {
            if (child.isDirectory() || (!child.getName().endsWith(".txt") && !(child.getName().endsWith(".bak") && moveBackup))) {
                continue;
            }
            Files.move(child.toPath(), newDirectory.resolve(child.getName()), StandardCopyOption.REPLACE_EXISTING);
        }

        AccountManager manager = AccountManager.getManager();
        if (moveKey) {
            Path newKey = newDirectory.resolve(Path.of(account.key()).getFileName());
            String keyString = newKey.toString();
            for (AccountManager.Account account1 : manager.getAccounts()) {
                if (account1.key().equals(account.key())) {
                    manager.removeAccount(account1);
                    manager.addAccount(account1.withKey(keyString));
                }
            }
            manager.setDefaultAccount(account.withKey(keyString));
            Files.move(Path.of(account.key()), newKey, StandardCopyOption.REPLACE_EXISTING);
            account = account.withKey(keyString);
        }

        if (Objects.requireNonNull(new File(account.path()).list()).length == 0) {
            Files.delete(Path.of(account.path()));
        }

        manager.removeAccount(account);
        account = account.withPath(newPath);
        manager.setDefaultAccount(account);
        manager.addAccount(account);
        manager.saveAccounts();
    }

    public void reencrypt(String newKeyPath, String newKeyPassword) throws IOException {
        byte[] newKey;
        if (Files.exists(Path.of(newKeyPath))) {
            newKey = Encryption.readKeyFile(newKeyPassword, Files.readString(Path.of(newKeyPath)));
        } else {
            newKey = new byte[256 / 8];
            SecureRandom random = new SecureRandom();
            random.nextBytes(newKey);
            Files.writeString(Path.of(newKeyPath), Encryption.writeKeyFile(newKeyPassword, newKey));
        }

        byte[] oldKey = Encryption.readKeyFile(password, Files.readString(Path.of(account.key())));
        EncryptedDirectory.changeKey(oldKey, newKey, new File(account.path()));

        AccountManager manager = AccountManager.getManager();
        manager.removeAccount(account);
        account = account.withKey(newKeyPath);
        manager.addAccount(account);
        manager.setDefaultAccount(account);
        manager.saveAccounts();
    }

    public void changePassword(String newPassword) throws IOException {
        Path key = Path.of(account.key());
        byte[] keyBytes = Encryption.readKeyFile(password, Files.readString(key));
        Files.writeString(key, Encryption.writeKeyFile(newPassword, keyBytes));
    }

    public static class RequestRestartEvent extends Event {
        public static final EventType<RequestRestartEvent> REQUEST_RESTART = new EventType<>(Event.ANY, "REQUEST_RESTART");
        public final Runnable actionDuringRestart;

        public RequestRestartEvent(Runnable actionDuringRestart) {
            super(REQUEST_RESTART);
            this.actionDuringRestart = actionDuringRestart;
        }
    }

    public static class RequestOpenLogEvent extends Event {
        public static final EventType<RequestOpenLogEvent> REQUEST_OPEN_LOG = new EventType<>(Event.ANY, "REQUEST_OPEN_LOG");
        public final File file;

        public RequestOpenLogEvent(File file) {
            super(REQUEST_OPEN_LOG);
            this.file = file;
        }

        public File getFile() {
            return file;
        }
    }
}