package io.github.software.coursework;

import io.github.software.coursework.data.AsyncStorage;
import io.github.software.coursework.data.json.JsonStorage;
import io.github.software.coursework.gui.DecryptionView;
import io.github.software.coursework.gui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
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
        decryptionView.setOnDecryptionSubmit(event -> {
            try {
                storage = new JsonStorage(event.getAccount(), event.getPassword());
            } catch (IOException ex) {
                logger.log(Level.INFO, "Cannot decrypt", ex);
                decryptionView.reportPasswordIncorrect();
                return;
            }
            stage.setResizable(true);
            MainView mainView = new MainView(storage);
            Scene mainScene = new Scene(mainView, 800, 600);
            stage.setScene(mainScene);
            stage.show();
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (storage != null) {
            storage.close();
        }
    }
}
