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

/**
 * The model of the decryption page.
 */
public final class DecryptionPageModel {
    private final ObjectProperty<EventHandler<DecryptionSubmitEvent>> onDecryptionSubmit = new SimpleObjectProperty<>();
    private final AccountManager accountManager = AccountManager.getManager();

    {
        accountManager.loadAccounts();
    }

    public ObjectProperty<EventHandler<DecryptionSubmitEvent>> onDecryptionSubmitProperty() {
        return onDecryptionSubmit;
    }

    /**
     * Get the event handler property for decryption submit.
     * @return the event handler property for decryption submit
     */
    public EventHandler<DecryptionSubmitEvent> getOnDecryptionSubmit() {
        return onDecryptionSubmit.get();
    }

    /**
     * Set the event handler for decryption submit.
     * @param value the event handler for decryption submit
     */
    public void setOnDecryptionSubmit(EventHandler<DecryptionSubmitEvent> value) {
        onDecryptionSubmit.set(value);
    }

    /**
     * Submit the decryption event.
     * @param account the account to decrypt
     * @param password the password to decrypt
     */
    public void submitDecryption(AccountManager.Account account, String password) {
        if (getOnDecryptionSubmit() != null) {
            getOnDecryptionSubmit().handle(new DecryptionSubmitEvent(DecryptionSubmitEvent.DECRYPTION_SUBMIT, account, password));
        }
    }

    /**
     * Handle the decryption event.
     * @param account the account to decrypt
     * @param password the password to decrypt
     */
    public void handleDecrypt(AccountManager.Account account, String password) {
        accountManager.setDefaultAccount(account);
        accountManager.saveAccounts();
        submitDecryption(account, password);
    }

    /**
     * Handle the import event.
     * @param username the username of the account
     * @param password the password of the account
     */
    public void handleCreate(String username, String password) {
        AccountManager.Account account1 = accountManager.makeAccount(username, password);
        if (account1 != null) {
            accountManager.addAccount(account1);
            accountManager.setDefaultAccount(account1);
            accountManager.saveAccounts();
            submitDecryption(account1, password);
        }
    }

    /**
     * Handle the import event.
     * @param path the path to the file
     * @param key the key to decrypt the file
     * @param username the username of the account
     * @param password the password of the account
     */
    public void handleImport(String path, String key, String username, String password) {
        AccountManager.Account account1 = new AccountManager.Account(username, path, key);
        accountManager.addAccount(account1);
        accountManager.setDefaultAccount(account1);
        accountManager.saveAccounts();
        submitDecryption(account1, password);
    }

    /**
     * Get the default account.
     * @return the default account
     */
    public AccountManager.Account getDefaultAccount() {
        return accountManager.getDefaultAccount();
    }

    /**
     * Get the list of accounts.
     * @return the list of accounts
     */
    public List<AccountManager.Account> getAccounts() {
        return accountManager.getAccounts();
    }

    /**
     * The event for decryption submit.
     */
    public static class DecryptionSubmitEvent extends Event {
        public static final EventType<DecryptionSubmitEvent> DECRYPTION_SUBMIT = new EventType<>(Event.ANY, "DECRYPTION_SUBMIT");
        private final AccountManager.Account account;
        private final String password;

        /**
         * The account to decrypt.
         * @return the account to decrypt
         */
        public AccountManager.Account getAccount() {
            return account;
        }

        /**
         * The password used to decrypt the account.
         * @return the password used to decrypt the account
         */
        public String getPassword() {
            return password;
        }

        /**
         * Create a new decryption submit event.
         * @param eventType the event type
         * @param account the account to decrypt
         * @param password the password to decrypt
         */
        public DecryptionSubmitEvent(EventType<? extends Event> eventType, AccountManager.Account account, String password) {
            super(eventType);
            this.account = account;
            this.password = password;
        }
    }
}
