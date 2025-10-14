package com.cab302.cab302;

import com.cab302.cab302.controller.LayoutController;
import com.cab302.cab302.controller.NavController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
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
        changeScene("Auth/Login-view.fxml", false);
    }

    public static void main(String[] args) {
        launch();
    }

    public static Object changeScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("Components/BaseTemplate.fxml"));
            BorderPane mainLayout = loader.load();

            FXMLLoader pageLoader = new FXMLLoader(Objects.requireNonNull(Main.class.getResource(fxmlPath)));
            Parent page = pageLoader.load();

            LayoutController layoutController = loader.getController();

            layoutController.setLinksVisible(true);

            mainLayout.setCenter(page);

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(mainLayout.getScene());
            } else {
                primaryStage.getScene().setRoot(mainLayout);
            }
            primaryStage.show();

            return pageLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object changeScene(String fxmlPath, boolean setNavlinksVisible) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("Components/BaseTemplate.fxml"));
            BorderPane mainLayout = loader.load();
            LayoutController layoutController = loader.getController();

            FXMLLoader pageLoader = new FXMLLoader(Objects.requireNonNull(Main.class.getResource(fxmlPath)));
            Parent page = pageLoader.load();

            if (!setNavlinksVisible) {
                layoutController.setLinksVisible(false);
            } else {
                layoutController.setLinksVisible(true);
            }

            mainLayout.setCenter(page);

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(mainLayout));
            } else {
                primaryStage.getScene().setRoot(mainLayout);
            }
            primaryStage.show();
            return pageLoader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
