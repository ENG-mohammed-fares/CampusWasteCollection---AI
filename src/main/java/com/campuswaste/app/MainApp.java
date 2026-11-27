package com.campuswaste.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/fxml/dashboard.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);

        stage.setTitle("Campus Waste Collection AI");
        stage.setMinWidth(1050);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
