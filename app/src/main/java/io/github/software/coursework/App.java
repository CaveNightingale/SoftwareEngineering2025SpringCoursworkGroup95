package io.github.software.coursework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.github.software.coursework.algo.Model;
import io.github.software.coursework.algo.PredictModel;
import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.json.EncryptedLogger;
import io.github.software.coursework.data.json.Encryption;
import io.github.software.coursework.data.json.JsonStorage;
import io.github.software.coursework.gui.DecryptionPageController;
import io.github.software.coursework.gui.DecryptionPageModel;
import io.github.software.coursework.gui.EncryptionSetting;
import io.github.software.coursework.gui.MainPage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
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
    private Model model;

    @Override
    public void start(Stage stage) {
        AnchorPane node;
        FXMLLoader loader = new FXMLLoader(DecryptionPageController.class.getResource("DecryptionPage.fxml"));
        try {
            node = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        DecryptionPageController decryptionController = loader.getController();
        DecryptionPageModel decryptionModel = decryptionController.getModel();
        Scene scene = new Scene(node, 400, 250);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Financial Management System");
        stage.setWidth(400);
        stage.setHeight(275);
        stage.setMaximized(false);
        stage.show();
        decryptionController.focus();
        decryptionModel.setOnDecryptionSubmit(event -> {
            if (event.getAccount() == null) {
                decryptionController.reportPasswordIncorrect();
                return;
            }
            try {
                storage = new JsonStorage(event.getAccount(), event.getPassword());
                model = new PredictModel(storage);
                storage.model(modelDirectory -> {
                    try {
                        model.loadParameters(modelDirectory);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error during loading model.", e);
                    }
                });
            } catch (IOException ex) {
                logger.log(Level.INFO, "Cannot decrypt", ex);
                decryptionController.reportPasswordIncorrect();
                return;
            }
            stage.setWidth(800);
            stage.setHeight(600);
            stage.setResizable(true);
            Scene mainScene = getScene(stage, event);
            stage.setScene(mainScene);
            stage.show();
        });
    }

    private Scene getScene(Stage stage, DecryptionPageModel.DecryptionSubmitEvent event) {
        MainPage mainView = new MainPage(storage, model);
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
                    model = null;
                    start(stage);
                });
            });
        });
        encryptionSetting.setOnRequestOpenLog(event1 -> Thread.ofVirtual().start(() -> {
            SequencedCollection<String> lines;
            String errors = null;
            SequencedCollection<String> finalLines = new ArrayList<>();
            try {
                byte[] key = Encryption.readKeyFile(event.getPassword(), Files.readString(Path.of(event.getAccount().key())));
                lines = EncryptedLogger.decodeLog(event1.getFile(), Objects.requireNonNull(key));
                JsonFactory jsonFactory = new JsonFactory();
                for (String line : lines) {
                    StringWriter stringWriter = new StringWriter();
                    try (JsonParser parser = jsonFactory.createParser(line); JsonGenerator generator = jsonFactory.createGenerator(stringWriter).setPrettyPrinter(new DefaultPrettyPrinter())) {
                        while (parser.nextToken() != null) {
                            generator.copyCurrentEvent(parser);
                        }
                    }
                    finalLines.add(stringWriter.toString());
                }
            } catch (IOException e) {
                logger.log(Level.INFO, "Error during reading log", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                errors = sw.toString();
            }
            String finalErrors = errors;
            Platform.runLater(() -> {
                Tab tab = new Tab("Log: " + event1.getFile().getName());
                ScrollPane scrollPane = new ScrollPane();
                tab.setContent(scrollPane);
                scrollPane.setFitToHeight(true);
                scrollPane.setFitToWidth(true);
                VBox vBox = new VBox();
                vBox.setStyle("-fx-padding: 1em;");
                scrollPane.setContent(vBox);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                if (finalErrors == null) {
                    for (String line : finalLines) {
                        TextFlow textFlow = new TextFlow();
                        vBox.getChildren().add(textFlow);
                        textFlow.setStyle("-fx-font-size: 12px; -fx-font-family: monospace; -fx-border-style: dashed; -fx-border-width: 0 0 1px 0; -fx-border-color: #000000;");
                        textFlow.getChildren().add(new Text(line));
                    }
                    vBox.getChildren().add(new Label("End of log"));
                } else {
                    TextFlow textFlow = new TextFlow();
                    vBox.getChildren().add(textFlow);
                    textFlow.setStyle("-fx-font-size: 12px; -fx-font-family: monospace;-fx-text-fill: #ff0000;");
                    Text text = new Text(finalErrors);
                    text.setFill(Paint.valueOf("#ff0000"));
                    textFlow.getChildren().add(text);
                }
                mainView.openExternalTab(tab);
                Platform.runLater(() -> scrollPane.setVvalue(1.0));
            });
        }));
        mainView.setStorageSetting(encryptionSetting);
        return new Scene(mainView, 800, 600);
    }

    private void saveModel() {
        if (model != null) {
            storage.model(modelDirectory -> {
                try {
                    model.saveParameters(modelDirectory);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error during saving", e);
                    logger.log(Level.SEVERE, "The application will be closed immediately.");
                    System.exit(1);
                }
            });
        }
    }

    // TODO: Add manual save button and let user decide if the changes should be saved at exit
    @Override
    public void stop() throws Exception {
        super.stop();
        saveModel();
        if (storage != null) {
            storage.close().get();
        }
    }
}
