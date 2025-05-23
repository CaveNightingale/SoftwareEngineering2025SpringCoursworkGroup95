package io.github.software.coursework.gui;

import io.github.software.coursework.data.json.AccountManager;
import io.github.software.coursework.data.json.Encryption;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Objects;

/**
 * Controller for the decryption page.
 */
public final class DecryptionPageController {
    @FXML
    private AnchorPane root;

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
    public VBox faqPage;

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

    @FXML
    private Label passwordIncorrect;

    @FXML
    private Label passwordIncorrectImport;

    @FXML
    public Label passwordIncorrectCreate;

    @FXML
    public Hyperlink faqLink;

    private final DecryptionPageModel model = new DecryptionPageModel();

    public DecryptionPageModel getModel() {
        return model;
    }

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

    /**
     * Show a message that the password is incorrect.
     */
    public void reportPasswordIncorrect() {
        passwordIncorrect.setVisible(true);
        passwordIncorrectImport.setVisible(true);
    }

    @FXML
    private void initialize() {
        account.getItems().addAll(model.getAccounts());
        account.setCellFactory(param -> new AccountCell());
        account.setButtonCell(new AccountCell());
        if (model.getDefaultAccount() != null) {
            account.setValue(model.getDefaultAccount());
        }

        password.setOnAction(event -> handleDecrypt());

        passwordCreate.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                passwordIncorrectCreate.setVisible(newValue == null || !isPasswordStrong(newValue));
            }
        });
    }

    /**
     * Set the password field to be focused.
     */
    public void focus() {
        password.requestFocus();
    }

    @FXML
    private void handleDecrypt() {
        model.handleDecrypt(account.getValue(), password.getText());
    }

    @FXML
    private void handleCreate() {
        model.handleCreate(accountCreate.getText(), passwordCreate.getText());
    }

    @FXML
    private void handleImport() {
        model.handleImport(path.getText(), key.getText(), accountImport.getText(), passwordImport.getText());
    }

    private boolean isPasswordStrong(String password) {
        if (password.length() < 8) {
            return false;
        }
        boolean digit = false;
        boolean upper = false;
        boolean lower = false;
        boolean other = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                digit = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else {
                other = true;
            }
        }
        return digit && upper && lower && other;
    }

    @FXML
    private void handleCreatePage() {
        createPage.setVisible(true);
        importPage.setVisible(false);
        importNext.setVisible(false);
        decryptPage.setVisible(false);
        faqPage.setVisible(false);
    }

    @FXML
    private void handleImportPage() {
        importPage.setVisible(true);
        createPage.setVisible(false);
        decryptPage.setVisible(false);
        faqPage.setVisible(false);
        importNext.setVisible(false);
    }

    @FXML
    private void handleDecryptPage() {
        decryptPage.setVisible(true);
        importPage.setVisible(false);
        importNext.setVisible(false);
        createPage.setVisible(false);
        faqPage.setVisible(false);
    }

    @FXML
    private void handleImportNext() {
        importNext.setVisible(true);
        importPage.setVisible(false);
        createPage.setVisible(false);
        decryptPage.setVisible(false);
        faqPage.setVisible(false);
    }

    @FXML
    private void handleFaqPage() {
        faqPage.setVisible(true);
        importNext.setVisible(false);
        importPage.setVisible(false);
        createPage.setVisible(false);
        decryptPage.setVisible(false);
    }

    @FXML
    private void handleImportPathSelect() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        File directory = directoryChooser.showDialog(root.getScene().getWindow());
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

    @FXML
    private void handleImportKeySelect() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Key");
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        key.setText(file.getAbsolutePath());
    }
}
