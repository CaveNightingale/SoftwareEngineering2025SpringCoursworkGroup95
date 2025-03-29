package io.github.software.coursework;

import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.json.EncryptedLogger;
import io.github.software.coursework.data.json.Encryption;
import io.github.software.coursework.data.json.JsonStorage;
import io.github.software.coursework.gui.DecryptionView;
import io.github.software.coursework.gui.EncryptionSetting;
import io.github.software.coursework.gui.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {
    private final Logger logger = Logger.getLogger("App");
    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(Level.INFO);
        launch(args);
    }
    private AsyncStorage storage;

    @Override
    public void start(Stage stage) {
        DecryptionView decryptionView = new DecryptionView();
        Scene scene = new Scene(decryptionView, 400, 250);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Financial Management System");
        stage.show();
        decryptionView.focus();
        decryptionView.setOnDecryptionSubmit(event -> {
            if (event.getAccount() == null) {
                decryptionView.reportPasswordIncorrect();
                return;
            }
            try {
                storage = new JsonStorage(event.getAccount(), event.getPassword());
            } catch (IOException ex) {
                logger.log(Level.INFO, "Cannot decrypt", ex);
                decryptionView.reportPasswordIncorrect();
                return;
            }
            stage.setResizable(true);
            Scene mainScene = getScene(stage, event);
            stage.setScene(mainScene);
            stage.show();
        });
    }

    private Scene getScene(Stage stage, DecryptionView.DecryptionSubmitEvent event) {
        MainView mainView = new MainView(storage);
        EncryptionSetting encryptionSetting = new EncryptionSetting(event.getAccount(), event.getPassword());
        encryptionSetting.setOnRequestRestart(event1 -> {
            mainView.setDisable(true);
            Thread.ofPlatform().start(() -> {
                try {
                    Objects.requireNonNull(storage).close().get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "Error during saving", e);
                    logger.log(Level.SEVERE, "The application will be closed immediately.");
                    System.exit(1);
                }
                try {
                    event1.actionDuringRestart.run();
                } catch (Throwable ex) {
                    logger.log(Level.SEVERE, "Error during restart", ex);
                    logger.log(Level.SEVERE, "The application will be closed immediately.");
                    System.exit(1);
                }
                Platform.runLater(() -> {
                    storage = null;
                    stage.setWidth(400);
                    stage.setHeight(250);
                    stage.setMaximized(false);
                    start(stage);
                });
            });
        });
        encryptionSetting.setOnRequestOpenLog(event1 -> Thread.ofVirtual().start(() -> {
            SequencedCollection<String> lines;
            try {
                byte[] key = Encryption.readKeyFile(event.getPassword(), Files.readString(Path.of(event.getAccount().key())));
                lines = EncryptedLogger.decodeLog(event1.getFile(), Objects.requireNonNull(key));
            } catch (IOException e) {
                logger.log(Level.INFO, "Error during reading log", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                lines = Arrays.asList(sw.toString().split("\n"));
            }
            SequencedCollection<String> finalLines = lines;
            Platform.runLater(() -> {
                Tab tab = new Tab("Log: " + event1.getFile().getName());
                ScrollPane scrollPane = new ScrollPane();
                tab.setContent(scrollPane);
                scrollPane.setFitToHeight(true);
                scrollPane.setFitToWidth(true);
                VBox vBox = new VBox();
                vBox.setStyle("-fx-padding: 1em;");
                scrollPane.setContent(vBox);
                for (String line : finalLines) {
                    TextFlow textFlow = new TextFlow();
                    vBox.getChildren().add(textFlow);
                    textFlow.setStyle("-fx-font-size: 12px; -fx-font-family: monospace;");
                    textFlow.getChildren().add(new Text(line));
                    textFlow.setStyle("-fx-border-style: dashed; -fx-border-width: 0 0 1px 0; -fx-border-color: #000000;");
                }
                Label endOfLog = new Label("End of log");
                endOfLog.setStyle("-fx-font-style: italic;");
                vBox.getChildren().add(endOfLog);
                mainView.openExternalTab(tab);
            });
        }));
        mainView.setStorageSetting(encryptionSetting);
        return new Scene(mainView, 800, 600);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (storage != null) {
            storage.close();
        }
    }
}
