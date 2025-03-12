package io.github.software.coursework;

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
        Scene scene = new Scene(new MainView(), 800, 600);
        stage.setScene(scene);
        stage.setTitle("Financial Management System");
        stage.setResizable(false);
        stage.show();
    }
}
