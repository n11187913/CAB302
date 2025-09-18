package com.cab302.cab302.controller;

import com.cab302.cab302.Database.Backend;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class UserController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        try (Backend db = new Backend()) {
            if (!db.authenticate(username, password)) {
                statusLabel.setText("Invalid username or password.");
                return;
            }
        } catch (Exception e) {
            statusLabel.setText("Login failed: " + e.getMessage());
            return;
        }

        // Auth OK → open profile view and pass username
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/cab302/cab302/\"hello-view.fxml\""));
            Parent root = loader.load();

            // ProfileController lives in package `ui`
            ui.ProfileController profileCtrl = loader.getController();
            profileCtrl.initWithUser(username);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 800));
            stage.setTitle("Profile");
            stage.show();
        } catch (Exception e) {
            statusLabel.setText("Could not open profile: " + e.getMessage());
        }
    }
}

