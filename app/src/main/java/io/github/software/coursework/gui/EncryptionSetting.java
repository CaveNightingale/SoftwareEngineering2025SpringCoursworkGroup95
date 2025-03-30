package io.github.software.coursework.gui;

import io.github.software.coursework.data.json.AccountManager;
import io.github.software.coursework.data.json.EncryptedDirectory;
import io.github.software.coursework.data.json.Encryption;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.controlsfx.control.ToggleSwitch;

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
public class EncryptionSetting extends VBox {

    private interface IORunnable {
        void run() throws IOException;
    }

    @FXML
    private Label path;

    @FXML
    private Label key;

    @FXML
    private TextField account;

    @FXML
    private HBox pathLine;

    @FXML
    private HBox keyLine;

    @FXML
    private HBox pathSubForm;

    @FXML
    private HBox keySubForm;

    // Move data sub-form
    @FXML
    private TextField newPath;

    @FXML
    private ToggleSwitch moveKey;

    @FXML
    private ToggleSwitch moveBackup;

    @FXML
    private TextField newKeyPath;

    @FXML
    private PasswordField newKeyPassword;

    @FXML
    private PasswordField editKeyPassword;

    private AccountManager.Account accountOriginal;

    private final String passwordOriginal;

    private final ObjectProperty<EventHandler<RequestRestartEvent>> onRequestRestart = new SimpleObjectProperty<>();

    public final ObjectProperty<EventHandler<RequestRestartEvent>> onRequestRestartProperty() {
        return onRequestRestart;
    }

    public final EventHandler<RequestRestartEvent> getOnRequestRestart() {
        return onRequestRestartProperty().get();
    }

    public final void setOnRequestRestart(EventHandler<RequestRestartEvent> value) {
        onRequestRestartProperty().set(value);
    }

    private final ObjectProperty<EventHandler<RequestOpenLogEvent>> onRequestOpenLog = new SimpleObjectProperty<>();

    public final ObjectProperty<EventHandler<RequestOpenLogEvent>> onRequestOpenLogProperty() {
        return onRequestOpenLog;
    }

    public final EventHandler<RequestOpenLogEvent> getOnRequestOpenLog() {
        return onRequestOpenLogProperty().get();
    }

    public final void setOnRequestOpenLog(EventHandler<RequestOpenLogEvent> value) {
        onRequestOpenLogProperty().set(value);
    }

    public EncryptionSetting(AccountManager.Account account, String password) {
        FXMLLoader fxmlLoader = new FXMLLoader(EncryptionSetting.class.getResource("EncryptionSetting.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        onRequestRestartProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(RequestRestartEvent.REQUEST_RESTART, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(RequestRestartEvent.REQUEST_RESTART, newValue);
            }
        });

        onRequestOpenLogProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(RequestOpenLogEvent.REQUEST_OPEN_LOG, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(RequestOpenLogEvent.REQUEST_OPEN_LOG, newValue);
            }
        });

        this.path.setText(account.path());
        this.newPath.setText(account.path());
        this.key.setText(account.key());
        this.newKeyPath.setText(account.key());
        this.account.setText(account.name());
        this.accountOriginal = account;
        this.passwordOriginal = password;
    }

    private void runIO(IORunnable runnable) {
        Event.fireEvent(this, new RequestRestartEvent(() -> {
            try {
                runnable.run();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private void error(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.show();
        });
    }

    private void done(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Done");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.show();
        });
    }

    @FXML
    private void handleDeleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Do you want to purge your directory?");
        alert.setContentText("This action cannot be undone.");
        ButtonType yesButton = new ButtonType("Yes, delete all my files");
        ButtonType noButton = new ButtonType("No, remove from the list only");
        ButtonType cancelButton = new ButtonType("Cancel");
        alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == cancelButton) {
                return;
            }
            runIO(() -> {
                AccountManager.getManager().removeAccount(accountOriginal);
                AccountManager.getManager().setDefaultAccount(null);
                AccountManager.getManager().saveAccounts();
                if (buttonType == yesButton) {
                    Files.walkFileTree(Path.of(accountOriginal.path()), new FileVisitor<Path>() {
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
                        public @NonNull FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public @NonNull FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
                done("You have successfully deleted your account.", "You can now use another account.");
            });
        });
    }

    @FXML
    private void handleBackupAccount() {
        runIO(() -> {
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
            File backupFile = new File(accountOriginal.path(), backupName);
            try (PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(backupFile)), false, StandardCharsets.UTF_8)) {
                for (File child : Objects.requireNonNull(new File(accountOriginal.path()).listFiles())) {
                    if (child.isDirectory() || !child.getName().endsWith(".txt")) {
                        continue;
                    }
                    printStream.println(child.getName());
                    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(child))) {
                        bufferedInputStream.transferTo(printStream);
                    }
                }
            }
            done("Your backup is ready.", "You can find it in the same directory as your files.");
        });
    }

    @FXML
    private void handleExportAccount() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(accountOriginal.path()));
        directoryChooser.setTitle("Select Directory");
        File directory = directoryChooser.showDialog(this.getScene().getWindow());
        if (directory == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Export Account");
        alert.setHeaderText("Do you want to export your secret key together with your files?");
        alert.setContentText("If you choose not to, you will need to transfer your secret key separately.");
        ButtonType yesButton = new ButtonType("Yes, export everything");
        ButtonType noButton = new ButtonType("No, export only the files");
        ButtonType cancelButton = new ButtonType("Cancel");
        alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == cancelButton) {
                return;
            }
            runIO(() -> {
                for (File child : Objects.requireNonNull(new File(accountOriginal.path()).listFiles())) {
                    if (child.isDirectory() || !child.getName().endsWith(".txt")) {
                        continue;
                    }
                    Files.copy(child.toPath(), new File(directory, child.getName()).toPath());
                }
                if (buttonType == yesButton) {
                    Files.copy(Path.of(accountOriginal.key()), new File(directory, "secret.key").toPath());
                }
                done("You have successfully exported your account.", "You can now import it into another instance of the application.");
            });
        });
    }

    @FXML
    private void handleRenameAccount() {
        AccountManager manager = AccountManager.getManager();
        AccountManager.Account newAccount = accountOriginal.withName(account.getText());
        manager.removeAccount(accountOriginal);
        manager.addAccount(newAccount);
        manager.setDefaultAccount(newAccount);
        manager.saveAccounts();
        accountOriginal = newAccount;
        done("You have successfully renamed your account.", "You can now use your new name when you open the application.");
    }

    @FXML
    private void handleEditingPath() {
        pathLine.setVisible(false);
        pathLine.setManaged(false);
        pathSubForm.setVisible(true);
        pathSubForm.setManaged(true);
    }

    @FXML
    private void handleEditingKey() {
        keyLine.setVisible(false);
        keyLine.setManaged(false);
        keySubForm.setVisible(true);
        keySubForm.setManaged(true);
    }

    @FXML
    private void handleMoveData() {
        runIO(() -> {
            Path newDirectory = Path.of(newPath.getText());
            if (!Files.exists(newDirectory)) {
                try {
                    Files.createDirectories(newDirectory);
                } catch (IOException e) {
                    error("Failed to create directory", "You data will be kept in the old directory.");
                    return;
                }
            }
            for (File child : Objects.requireNonNull(new File(accountOriginal.path()).listFiles())) {
                if (child.isDirectory() || (!child.getName().endsWith(".txt") && !(child.getName().endsWith(".bak") && moveBackup.isSelected()))) {
                    continue;
                }
                Files.move(child.toPath(), newDirectory.resolve(child.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
            AccountManager manager = AccountManager.getManager();
            if (moveKey.isSelected()) {
                Path newKey = newDirectory.resolve(Path.of(accountOriginal.key()).getFileName());
                String keyString = newKey.toString();
                for (AccountManager.Account account1 : manager.getAccounts()) {
                    if (account1.key().equals(accountOriginal.key())) {
                        manager.removeAccount(account1);
                        manager.addAccount(account1.withKey(keyString));
                    }
                }
                manager.setDefaultAccount(accountOriginal.withKey(keyString));
                Files.move(Path.of(accountOriginal.key()), newKey, StandardCopyOption.REPLACE_EXISTING);
                accountOriginal = accountOriginal.withKey(keyString);
            }
            if (Objects.requireNonNull(new File(accountOriginal.path()).list()).length == 0) {
                Files.delete(Path.of(accountOriginal.path()));
            }
            manager.removeAccount(accountOriginal);
            accountOriginal = accountOriginal.withPath(newPath.getText());
            manager.setDefaultAccount(accountOriginal);
            manager.addAccount(accountOriginal);
            manager.saveAccounts();
            done("Your account is moved.", "You can now use your new directory.");
        });
    }

    @FXML
    private void handleReencrypt() {
        runIO(() -> {
            byte[] newKey;
            if (Files.exists(Path.of(newKeyPath.getText()))) {
                try {
                    newKey = Encryption.readKeyFile(newKeyPassword.getText(), Files.readString(Path.of(newKeyPath.getText())));
                } catch (IOException ex) {
                    error("Failed to read key file", "You data will be encrypted with the old key.");
                    return;
                }
            } else {
                newKey = new byte[256 / 8];
                SecureRandom random = new SecureRandom();
                random.nextBytes(newKey);
                try {
                    Files.writeString(Path.of(newKeyPath.getText()), Encryption.writeKeyFile(newKeyPassword.getText(), newKey));
                } catch (IOException ex) {
                    error("Failed to write key file", "You data will be encrypted with the old key.");
                    return;
                }
            }
            if (newKey == null) {
                error("Your key file is invalid or your password is incorrect", "You data will be encrypted with the old key.");
                return;
            }
            byte[] oldKey = Encryption.readKeyFile(passwordOriginal, Files.readString(Path.of(accountOriginal.key())));
            EncryptedDirectory.changeKey(oldKey, newKey, new File(accountOriginal.path()));
            AccountManager manager = AccountManager.getManager();
            manager.removeAccount(accountOriginal);
            manager.addAccount(accountOriginal.withKey(newKeyPath.getText()));
            manager.setDefaultAccount(accountOriginal.withKey(newKeyPath.getText()));
            manager.saveAccounts();
            done("Your account is re-encrypted.", "You can now use your new key file.");
        });
    }

    @FXML
    private void handleMoveTargetSelect() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        File directory = directoryChooser.showDialog(this.getScene().getWindow());
        if (directory == null) {
            return;
        }
        newPath.setText(directory.getAbsolutePath());
    }

    @FXML
    private void handleNewKeySelect() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Secret Key", "*.key"));
        File file = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        newKeyPath.setText(file.getAbsolutePath());
    }

    @FXML
    private void handleChangePassword() {
        runIO(() -> {
            Path key = Path.of(accountOriginal.key());
            byte[] keyBytes = Encryption.readKeyFile(passwordOriginal, Files.readString(key));
            Files.writeString(key, Encryption.writeKeyFile(editKeyPassword.getText(), keyBytes));
            done("Your password is changed.", "You need to use new password to open your files in all accounts that use this key.");
        });
    }

    @FXML
    private void openLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(accountOriginal.path()));
        fileChooser.setTitle("Select Log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log", "*.log"));
        File file = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        Event.fireEvent(this, new RequestOpenLogEvent(file));
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
