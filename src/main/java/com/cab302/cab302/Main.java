package com.cab302.cab302;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("profile.fxml"));
        String stylesheet = Objects.requireNonNull(Main.class.getResource("stylesheet.css")).toExternalForm();
        Scene scene = new Scene(fxmlLoader.load(), 800, 800);
        scene.getStylesheets().add(stylesheet);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
        ui.ProfileController c = fxmlLoader.getController();
        c.initWithUser("alice");
    }

    public static void main(String[] args) {
        launch();
    }
}