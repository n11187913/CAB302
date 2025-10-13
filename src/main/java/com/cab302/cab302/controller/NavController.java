package com.cab302.cab302.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import static com.cab302.cab302.Main.changeScene;

public class NavController {

    @FXML
    private Button homeButton;
    @FXML
    private Button leaderboardButton;
    @FXML
    private Button aboutButton;
    @FXML
    private Button profileButton;
    @FXML
    private Button hamburgerMenu;

    @FXML
    public void initialize() {
        homeButton.setOnAction(e -> changeScene("home-view.fxml"));
        leaderboardButton.setOnAction(e -> changeScene("leaderboard-view.fxml"));
        aboutButton.setOnAction(e -> changeScene("about-view.fxml"));
        profileButton.setOnAction(e -> changeScene("profile-view.fxml"));
//        hamburgerMenu.setOnAction(e -> toggleMenu());
    }
}
