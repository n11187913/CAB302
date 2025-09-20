package com.cab302.cab302;

import com.cab302.cab302.Database.Backend;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    public static final String TITLE = "Sign In / Log In";
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("Auth/login-view.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void changeScene(String fxmlPath) {
        try {
            Parent pane = FXMLLoader.load(Objects.requireNonNull(Main.class.getResource("/com/cab302/cab302/" +  fxmlPath)));
            primaryStage.getScene().setRoot(pane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}