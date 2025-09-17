package com.cab302.cab302.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

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
