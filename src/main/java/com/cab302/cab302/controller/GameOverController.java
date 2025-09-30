package com.cab302.cab302.controller;

import com.cab302.cab302.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

public class GameOverController {

    @FXML private Label finalScoreLabel;
    @FXML private Label highScoreLabel;
    @FXML private Label streakLabel;
    @FXML private Label fastestAnswerLabel;
    @FXML private Label accuracyLabel;

    @FXML private Button homeBtn;

    // Instance variables to hold stats
    private int finalScore;
    private int finalHighScore;
    private int finalHighestStreak;
    private double finalFastestAnswer;
    private double finalAccuracy;

    /**
     * Called by QuestionController before switching to results scene
     */
    public void setGameStats(int score, int highScore, int highestStreak, double fastestAnswer, double accuracy) {
        this.finalScore = score;
        this.finalHighScore = highScore;
        this.finalHighestStreak = highestStreak;
        this.finalFastestAnswer = fastestAnswer;
        this.finalAccuracy = accuracy;

        // Update labels if they are already initialized
        if (finalScoreLabel != null) {
            finalScoreLabel.setText("Score: " + finalScore);
            highScoreLabel.setText("High Score: " + finalHighScore);
            streakLabel.setText("Highest Streak: " + finalHighestStreak);
            fastestAnswerLabel.setText(String.format("Fastest Answer: %.2f s", finalFastestAnswer));
            accuracyLabel.setText(String.format("Accuracy: %.2f%%", finalAccuracy));
        }
    }

    @FXML
    public void initialize() {
        // Home button action
        homeBtn.setOnAction(e -> Main.changeScene("/com/cab302/cab302/home-view.fxml"));
    }
}
