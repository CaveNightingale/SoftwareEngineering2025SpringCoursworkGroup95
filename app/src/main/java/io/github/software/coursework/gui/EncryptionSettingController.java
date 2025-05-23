package io.github.software.coursework.gui;

import io.github.software.coursework.data.json.AccountManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.controlsfx.control.ToggleSwitch;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;

@ParametersAreNonnullByDefault
public final class EncryptionSettingController {

    private interface IORunnable {
        void run() throws IOException;
    }

    @FXML
    private VBox root;

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

    private final EncryptionSettingModel model;

    public EncryptionSettingModel getModel() {
        return model;
    }

    public EncryptionSettingController(AccountManager.Account account, String password) {
        this.model = new EncryptionSettingModel(account, password);
    }

    @FXML
    public void initialize() {
        this.path.setText(model.getAccount().path());
        this.newPath.setText(model.getAccount().path());
        this.key.setText(model.getAccount().key());
        this.newKeyPath.setText(model.getAccount().key());
        this.account.setText(model.getAccount().name());
    }

    private void runIO(IORunnable runnable) {
        if (model.getOnRequestRestart() != null) {
            model.getOnRequestRestart().handle(new EncryptionSettingModel.RequestRestartEvent(() -> {
                try {
                    runnable.run();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
    }

    private void error(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
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
                model.deleteAccount(buttonType == yesButton);
                done("You have successfully deleted your account.", "You can now use another account.");
            });
        });
    }

    @FXML
    private void handleBackupAccount() {
        runIO(() -> {
            model.backupAccount();
            done("Your backup is ready.", "You can find it in the same directory as your files.");
        });
    }

    @FXML
    private void handleExportAccount() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(model.getAccount().path()));
        directoryChooser.setTitle("Select Directory");
        File directory = directoryChooser.showDialog(root.getScene().getWindow());

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
                model.exportAccount(directory, buttonType == yesButton);
                done("You have successfully exported your account.", "You can now import it into another instance of the application.");
            });
        });
    }

    @FXML
    private void handleRenameAccount() {
        runIO(() -> {
            model.renameAccount(account.getText());
            done("You have successfully renamed your account.", "You can now use your new name when you open the application.");
        });
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
            try {
                model.moveData(newPath.getText(), moveKey.isSelected(), moveBackup.isSelected());
                done("Your account is moved.", "You can now use your new directory.");
            } catch (IOException e) {
                error("Failed to create directory", "Your data will be kept in the old directory.");
            }
        });
    }

    @FXML
    private void handleReencrypt() {
        runIO(() -> {
            try {
                model.reencrypt(newKeyPath.getText(), newKeyPassword.getText());
                done("Your account is re-encrypted.", "You can now use your new key file.");
            } catch (IOException e) {
                error("Failed to read or write key file", "Your data will be encrypted with the old key.");
            } catch (NullPointerException e) {
                error("Your key file is invalid or your password is incorrect", "Your data will be encrypted with the old key.");
            }
        });
    }

    @FXML
    private void handleMoveTargetSelect() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        File directory = directoryChooser.showDialog(root.getScene().getWindow());
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
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        newKeyPath.setText(file.getAbsolutePath());
    }

    @FXML
    private void handleChangePassword() {
        runIO(() -> {
            try {
                model.changePassword(editKeyPassword.getText());
                done("Your password is changed.", "You need to use new password to open your files in all accounts that use this key.");
            } catch (IOException e) {
                error("Failed to change password", "Your password remains unchanged.");
            }
        });
    }

    @FXML
    private void openLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(model.getAccount().path()));
        fileChooser.setTitle("Select Log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log", "*.log"));
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        if (model.getOnRequestOpenLog() != null) {
            model.getOnRequestOpenLog().handle(new EncryptionSettingModel.RequestOpenLogEvent(file));
        }
    }
}