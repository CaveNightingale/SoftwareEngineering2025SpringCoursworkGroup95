package io.github.software.coursework;

import io.github.software.coursework.gui.DecryptionView;
import io.github.software.coursework.gui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        DecryptionView decryptionView = new DecryptionView();
        Scene scene = new Scene(decryptionView, 400, 250);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Financial Management System");
        stage.show();
        decryptionView.setOnDecryptionSubmit(event -> {
            stage.setResizable(true);
            MainView mainView = new MainView();
            Scene mainScene = new Scene(mainView, 800, 600);
            stage.setScene(mainScene);
            stage.show();
        });
    }
}
