package com.cab302.cab302.controller;

import com.cab302.cab302.Main;
import com.cab302.cab302.model.UserAccount;
import com.cab302.cab302.TestUserList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class QuestionController {
    @FXML
    private Label answerPlaceholder;

    @FXML
    private TextField answerField;

    @FXML
    public void on_user_input() {
        answerField.setVisible(false); // ensure it's hidden initially

        answerPlaceholder.setOnMouseClicked(e -> {
            answerPlaceholder.setVisible(false);
            answerField.setVisible(true);
            answerField.requestFocus();
        });

        answerField.setOnKeyTyped(e -> {
            if (!answerField.isVisible()) {
                answerPlaceholder.setVisible(false);
                answerField.setVisible(true);
            }
        });
    }
}
