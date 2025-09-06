package com.cab302.cab302;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;

public class BaseController {
    @FXML private StackPane contentContainer;
    @FXML private ToggleButton homeTab, leaderTab, aboutTab;

    @FXML
    public void initialize() {
        goHome();
    }

    @FXML
    private void goHome()
    { select(homeTab);   loadIntoCenter("/com/cab302/cab302/home-view.fxml"); }
    @FXML
    private void goLeader()
    { select(leaderTab); loadIntoCenter("/com/cab302/cab302/leaderboard-view.fxml"); }
    @FXML
    private void goAbout()
    { select(aboutTab);  loadIntoCenter("/com/cab302/cab302/about-view.fxml"); }

    private void select(ToggleButton btn) {
        homeTab.setSelected(false);
        leaderTab.setSelected(false);
        aboutTab.setSelected(false);
        btn.setSelected(true);
    }

    private void loadIntoCenter(String resource) {
        try {
            Node view = FXMLLoader.load(Main.class.getResource(resource));
            contentContainer.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}