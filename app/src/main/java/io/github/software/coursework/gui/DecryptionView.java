package io.github.software.coursework.gui;

import io.github.software.coursework.data.json.AccountManager;
import io.github.software.coursework.data.json.Encryption;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class DecryptionView extends AnchorPane {
    @FXML
    private ComboBox<AccountManager.Account> account;

    @FXML
    private PasswordField password;

    @FXML
    private VBox createPage;

    @FXML
    private VBox importPage;

    @FXML
    private VBox decryptPage;

    @FXML
    private VBox importNext;

    @FXML
    private TextField path;

    @FXML
    private TextField key;

    @FXML
    private TextField accountImport;

    @FXML
    private PasswordField passwordImport;

    @FXML
    private TextField accountCreate;

    @FXML
    private PasswordField passwordCreate;

    private final ObjectProperty<EventHandler<DecryptionSubmitEvent>> onDecryptionSubmit = new SimpleObjectProperty<>();

    public ObjectProperty<EventHandler<DecryptionSubmitEvent>> onDecryptionSubmitProperty() {
        return onDecryptionSubmit;
    }

    public final EventHandler<DecryptionSubmitEvent> getOnDecryptionSubmit() {
        return onDecryptionSubmit.get();
    }

    public final void setOnDecryptionSubmit(EventHandler<DecryptionSubmitEvent> value) {
        onDecryptionSubmit.set(value);
    }

    private final AccountManager accountManager;

    private static final class AccountCell extends ListCell<AccountManager.Account> {
        @Override
        protected void updateItem(AccountManager.Account item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.name());
            }
        }
    }

    public DecryptionView() {
        FXMLLoader fxmlLoader = new FXMLLoader(DecryptionView.class.getResource("DecryptionView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        accountManager = AccountManager.getManager();
        accountManager.loadAccounts();
        onDecryptionSubmit.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                this.removeEventHandler(DecryptionSubmitEvent.DECRYPTION_SUBMIT, oldValue);
            }
            if (newValue != null) {
                this.addEventHandler(DecryptionSubmitEvent.DECRYPTION_SUBMIT, newValue);
            }
        });
        account.getItems().addAll(accountManager.getAccounts());
        account.setCellFactory(param -> new AccountCell());
        account.setButtonCell(new AccountCell());
    }

    public void handleDecrypt() {
        Event.fireEvent(this, new DecryptionSubmitEvent(DecryptionSubmitEvent.DECRYPTION_SUBMIT, new AccountManager.Account("", "", ""), ""));
    }

    public void handleCreate() {
        AccountManager.Account account1 = accountManager.makeAccount(accountCreate.getText(), passwordCreate.getText());
        if (account1 != null) {
            accountManager.addAccount(account1);
            accountManager.saveAccounts();
            Event.fireEvent(this, new DecryptionSubmitEvent(DecryptionSubmitEvent.DECRYPTION_SUBMIT, account1, passwordCreate.getText()));
        }
    }

    public void handleImport() {
        AccountManager.Account account1 = new AccountManager.Account(accountImport.getText(), path.getText(), key.getText());
        accountManager.addAccount(account1);
        accountManager.saveAccounts();
        Event.fireEvent(this, new DecryptionSubmitEvent(DecryptionSubmitEvent.DECRYPTION_SUBMIT, account1, passwordImport.getText()));
    }

    public void handleCreatePage() {
        createPage.setVisible(true);
        importPage.setVisible(false);
        importNext.setVisible(false);
        decryptPage.setVisible(false);
    }

    public void handleImportPage() {
        importPage.setVisible(true);
        createPage.setVisible(false);
        decryptPage.setVisible(false);
        importNext.setVisible(false);
    }

    public void handleDecryptPage() {
        decryptPage.setVisible(true);
        importPage.setVisible(false);
        importNext.setVisible(false);
        createPage.setVisible(false);
    }

    public void handleImportNext() {
        importNext.setVisible(true);
        importPage.setVisible(false);
        createPage.setVisible(false);
        decryptPage.setVisible(false);
    }

    public void handleImportPathSelect() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        File directory = directoryChooser.showDialog(this.getScene().getWindow());
        if (directory == null) {
            return;
        }
        path.setText(directory.getAbsolutePath());
        if (key.getText().isEmpty()) {
            File defaultKey = new File(directory, "secret.key");
            if (Encryption.lookLikeKey(defaultKey)) {
                key.setText(defaultKey.getAbsolutePath());
            }
        }
        if (accountImport.getText().isEmpty()) {
            accountImport.setText(directory.getName());
        }
    }

    public void handleImportKeySelect() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Key");
        File file = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        key.setText(file.getAbsolutePath());
    }

    public static class DecryptionSubmitEvent extends Event {
        public static final EventType<DecryptionSubmitEvent> DECRYPTION_SUBMIT = new EventType<>(Event.ANY, "DECRYPTION_SUBMIT");
        private AccountManager.Account account;
        private String password;

        public AccountManager.Account getAccount() {
            return account;
        }

        public String getPassword() {
            return password;
        }

        public DecryptionSubmitEvent(EventType<? extends Event> eventType, AccountManager.Account account, String password) {
            super(eventType);
        }
    }
}
