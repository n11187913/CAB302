package com.cab302.cab302.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import java.io.IOException;


import static com.cab302.cab302.Main.changeScene;

public class HomeController {

    @FXML
    public void onEasy(ActionEvent event) {
        launchGame("easy", event);
    }

    @FXML
    public void onMedium(ActionEvent event) {
        launchGame("medium", event);
    }

    @FXML
    public void onHard(ActionEvent event) {
        launchGame("hard", event);
    }

    private void launchGame(String difficulty, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cab302/cab302/Gameplay/question.fxml"));
            Parent root = loader.load();

            QuestionController controller = loader.getController();
            controller.setDifficulty(difficulty);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1080, 720));
            stage.setTitle("Mental Math Game");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
