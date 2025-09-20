package com.cab302.cab302.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.awt.event.MouseEvent;
import java.io.IOException;

public class HomeController {
    @FXML
    private ToggleGroup difficultyGroup;

    @FXML
    private BorderPane rootPane;

    // Daily Challenge difficulty
    @FXML private ToggleButton dcEasy, dcMedium, dcHard;
    // Time Trial difficulty
    @FXML private ToggleButton ttEasy, ttMedium, ttHard;
    // Battle difficulty
    @FXML private ToggleButton bEasy, bMedium, bHard;

    private final ToggleGroup dcGroup = new ToggleGroup();
    private final ToggleGroup ttGroup = new ToggleGroup();
    private final ToggleGroup bGroup  = new ToggleGroup();

    @FXML
    public void initialize() {
        // wire groups so only one is selected in each row
        if (dcEasy != null)  { dcEasy.setToggleGroup(dcGroup); }
        if (dcMedium != null){ dcMedium.setToggleGroup(dcGroup); }
        if (dcHard != null)  { dcHard.setToggleGroup(dcGroup); }
        if (ttEasy != null)  { ttEasy.setToggleGroup(ttGroup); }
        if (ttMedium != null){ ttMedium.setToggleGroup(ttGroup); }
        if (ttHard != null)  { ttHard.setToggleGroup(ttGroup); }
        if (bEasy != null)   { bEasy.setToggleGroup(bGroup); }
        if (bMedium != null) { bMedium.setToggleGroup(bGroup); }
        if (bHard != null)   { bHard.setToggleGroup(bGroup); }

        // defaults
        if (dcEasy != null)  dcEasy.setSelected(true);
        if (ttEasy != null)  ttEasy.setSelected(true);
        if (bEasy != null)   bEasy.setSelected(true);
    }

    // --- difficulty handlers (optional, kept for future logic) ---
    @FXML private void selectDcEasy()    {}
    @FXML private void selectDcMedium()  {}
    @FXML private void selectDcHard()    {}
    @FXML private void selectTtEasy()    {}
    @FXML private void selectTtMedium()  {}
    @FXML private void selectTtHard()    {}
    @FXML private void selectBattleEasy(){}
    @FXML private void selectBattleMedium(){}
    @FXML private void selectBattleHard(){}

    // --- actions ---
    @FXML private void startDaily()              {  }
    @FXML private void openDailyLeaderboard()    { }
    @FXML private void startTimeTrial()          {/* TODO */ }
    @FXML private void openTimeTrialLeaderboard(){  }
    @FXML private void startBattle()             { }
    @FXML private void openBattleLeaderboard()   {  }


    private void switchScene(String fxmlPath) {
        try {
            var url = getClass().getResource("/com/cab302/cab302/" + fxmlPath);
            if (url == null) throw new IllegalStateException("FXML not found: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void goProfile()     { switchScene("profile-view.fxml"); }
}