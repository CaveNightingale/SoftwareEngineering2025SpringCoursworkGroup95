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
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class DecryptionPageModel {
    private final ObjectProperty<EventHandler<DecryptionSubmitEvent>> onDecryptionSubmit = new SimpleObjectProperty<>();
    private final AccountManager accountManager = AccountManager.getManager();

    {
        accountManager.loadAccounts();
    }

    public ObjectProperty<EventHandler<DecryptionSubmitEvent>> onDecryptionSubmitProperty() {
        return onDecryptionSubmit;
    }

    public EventHandler<DecryptionSubmitEvent> getOnDecryptionSubmit() {
        return onDecryptionSubmit.get();
    }

    public void setOnDecryptionSubmit(EventHandler<DecryptionSubmitEvent> value) {
        onDecryptionSubmit.set(value);
    }

    public void submitDecryption(AccountManager.Account account, String password) {
        if (getOnDecryptionSubmit() != null) {
            getOnDecryptionSubmit().handle(new DecryptionSubmitEvent(DecryptionSubmitEvent.DECRYPTION_SUBMIT, account, password));
        }
    }

    public void handleDecrypt(AccountManager.Account account, String password) {
        accountManager.setDefaultAccount(account);
        accountManager.saveAccounts();
        submitDecryption(account, password);
    }

    public void handleCreate(String username, String password) {
        AccountManager.Account account1 = accountManager.makeAccount(username, password);
        if (account1 != null) {
            accountManager.addAccount(account1);
            accountManager.setDefaultAccount(account1);
            accountManager.saveAccounts();
            submitDecryption(account1, password);
        }
    }

    public void handleImport(String path, String key, String username, String password) {
        AccountManager.Account account1 = new AccountManager.Account(username, path, key);
        accountManager.addAccount(account1);
        accountManager.setDefaultAccount(account1);
        accountManager.saveAccounts();
        submitDecryption(account1, password);
    }

    public AccountManager.Account getDefaultAccount() {
        return accountManager.getDefaultAccount();
    }

    public List<AccountManager.Account> getAccounts() {
        return accountManager.getAccounts();
    }

    public static class DecryptionSubmitEvent extends Event {
        public static final EventType<DecryptionSubmitEvent> DECRYPTION_SUBMIT = new EventType<>(Event.ANY, "DECRYPTION_SUBMIT");
        private final AccountManager.Account account;
        private final String password;

        public AccountManager.Account getAccount() {
            return account;
        }

        public String getPassword() {
            return password;
        }

        public DecryptionSubmitEvent(EventType<? extends Event> eventType, AccountManager.Account account, String password) {
            super(eventType);
            this.account = account;
            this.password = password;
        }
    }
}
