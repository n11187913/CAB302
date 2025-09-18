package com.cab302.cab302;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Load the LOGIN view first
        FXMLLoader fxmlLoader = new FXMLLoader(
                Main.class.getResource("hello-view.fxml") // resources/com/cab302/cab302/hello-view.fxml
        );

        Scene scene = new Scene(fxmlLoader.load(), 800, 800);

        // If you keep a global stylesheet, add it here (optional)
        var cssUrl = Main.class.getResource("stylesheet.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
