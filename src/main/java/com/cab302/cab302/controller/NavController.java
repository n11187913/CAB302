package com.cab302.cab302.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import static com.cab302.cab302.Main.changeScene;

public class NavController {

    public HBox links1;
    public HBox links2;
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
        profileButton.setOnAction(e -> changeScene("profile.fxml"));
//        hamburgerMenu.setOnAction(e -> toggleMenu());
    }

    @FXML
    public void setNavStyle(boolean linksOn) {
        if (!linksOn) {
            links1.setVisible(false);
            links1.setManaged(false);
            links2.setVisible(false);
            links2.setManaged(false);
        } else {
            links1.setVisible(true);
            links1.setManaged(true);
            links2.setVisible(true);
            links2.setManaged(true);
        }
    }
}
