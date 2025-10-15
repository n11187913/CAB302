package com.cab302.cab302.controller;

import javafx.fxml.FXML;

public class LayoutController {

    @FXML
    private NavController navController;

    public void setLinksVisible(boolean linksVisible) {
        if (!linksVisible) {
            if (navController != null) {
                navController.setNavStyle(false);
            }
        } else {
            if (navController != null) {
                navController.setNavStyle(true);
            }
        }
    }
}
