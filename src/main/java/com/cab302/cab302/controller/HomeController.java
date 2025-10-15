package com.cab302.cab302.controller;

import com.cab302.cab302.Main;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

import static com.cab302.cab302.Main.changeScene;

public class HomeController {

    @FXML private BorderPane rootPane;

    @FXML private ToggleGroup difficultyGroup;

    @FXML private QuestionController questionController;

    // Daily Challenge difficulty
    @FXML private ToggleButton dcEasy, dcMedium, dcHard;
    // Time Trial difficulty
    @FXML private ToggleButton ttEasy, ttMedium, ttHard;
    // Practice difficulty
    @FXML private ToggleButton pEasy, pMedium, pHard;

    private final ToggleGroup dcGroup = new ToggleGroup();
    private final ToggleGroup ttGroup = new ToggleGroup();
    private final ToggleGroup pGroup  = new ToggleGroup();

    @FXML
    public void initialize() {
        // wire difficulty groups
        if (dcEasy != null)  { dcEasy.setToggleGroup(dcGroup); dcEasy.setUserData("easy"); }
        if (dcMedium != null){ dcMedium.setToggleGroup(dcGroup); dcMedium.setUserData("medium"); }
        if (dcHard != null)  { dcHard.setToggleGroup(dcGroup); dcHard.setUserData("hard"); }

        if (ttEasy != null)  { ttEasy.setToggleGroup(ttGroup); ttEasy.setUserData("easy"); }
        if (ttMedium != null){ ttMedium.setToggleGroup(ttGroup); ttMedium.setUserData("medium"); }
        if (ttHard != null)  { ttHard.setToggleGroup(ttGroup); ttHard.setUserData("hard"); }

        if (pEasy != null)   { pEasy.setToggleGroup(pGroup); pEasy.setUserData("easy"); }
        if (pMedium != null) { pMedium.setToggleGroup(pGroup); pMedium.setUserData("medium"); }
        if (pHard != null)   { pHard.setToggleGroup(pGroup); pHard.setUserData("hard"); }

        // defaults
        if (dcEasy != null)  dcEasy.setSelected(true);
        if (ttEasy != null)  ttEasy.setSelected(true);
        if (pEasy != null)   pEasy.setSelected(true);
    }

    // --- leaderboard actions ---
    @FXML private void openDailyLeaderboard()    { }
    @FXML private void openTimeTrialLeaderboard(){ }
    @FXML private void openPracticeLeaderboard() { }

    private String getSelectedDifficulty(ToggleGroup group) {
        ToggleButton selected = (ToggleButton) group.getSelectedToggle();
        return (selected != null) ? selected.getText().toLowerCase() : "easy";
    }

    @FXML
    private void startDaily() {
        launchGame("daily", getSelectedDifficulty(dcGroup));
    }

    @FXML
    private void startTimeTrial() {
        launchGame("time_trial", getSelectedDifficulty(ttGroup));
    }

    @FXML
    private void startPractice() {
        launchGame("practice", getSelectedDifficulty(pGroup));
    }
    @FXML
    private void goToProfile() {
        changeScene("profile.fxml");
    }

    private void launchGame(String mode, String difficulty) {
        String fxmlFile = switch (mode.toLowerCase()) {
            case "daily"    -> "Gameplay/daily-challenge-view.fxml";
            case "practice" -> "Gameplay/practice-view.fxml";
            default         -> "Gameplay/time-trial-view.fxml";
        };

        Object controller = Main.changeScene(fxmlFile);
        if (controller instanceof QuestionController qc) {
            qc.setGameMode(mode);
            qc.setDifficulty(difficulty);
        }
    }

}
