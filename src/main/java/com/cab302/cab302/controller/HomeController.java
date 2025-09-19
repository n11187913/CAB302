package com.cab302.cab302.controller;

import javafx.fxml.FXML;
import org.json.JSONObject;

import static com.cab302.cab302.Main.changeScene;

public class HomeController {
    @FXML
    void onPlayButton() {
        changeScene("Gameplay/question.fxml");
    }
}
