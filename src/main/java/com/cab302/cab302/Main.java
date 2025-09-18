package com.cab302.cab302;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.cab302.cab302.Database.Backend;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // ONE-TIME SEED: create a test user if missing
        try (Backend db = new Backend()) {
            var u = db.getUser("don");
            if (u.isEmpty()) {
                db.addUser("don", "Password123", "Physics");
            }
        } catch (Exception ignored) {
            // ignore if already exists or any seed-time hiccup
        }

        // Load the LOGIN view
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 800);

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
