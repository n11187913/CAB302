package com.cab302.cab302;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage; // needed for "Stage primaryStage"

import javax.imageio.IIOParam;
import java.io.IOException;
import java.util.Objects;


public class Main extends Application {
    public static final String TITLE = "Sign In / Log In";
    private static Stage primaryStage;
    private IIOParam fxmlLoader;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        changeScene("Auth/login-view.fxml"); // simplified first scene load
    }

    public static void main(String[] args) {
        launch();
    }

    public static void changeScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent pane = loader.load();

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(pane));
            } else {
                primaryStage.getScene().setRoot(pane);
            }
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // NEW: load scene and get controller
    public static <T> T loadScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent pane = loader.load();

        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(pane));
        } else {
            primaryStage.getScene().setRoot(pane);
        }
        primaryStage.show();

        return loader.getController();
    }
}
