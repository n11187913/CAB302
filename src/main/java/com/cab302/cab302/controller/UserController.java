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
    @FXML private Label status;

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            status.setText("Enter username and password.");
            return;
        }

        try (Backend db = new Backend()) {
            if (!db.authenticate(username, password)) {
                status.setText("Invalid credentials.");
                return;
            }
        } catch (Exception e) {
            status.setText("Login error: " + e.getMessage());
            return;
        }

        // Navigate to profile.fxml and pass the logged-in username
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cab302/cab302/profile.fxml"));
            Parent root = loader.load();

            ProfileController pc = loader.getController();
            pc.initWithUser(username);     // updates name/email/etc.

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 800));
            stage.show();
        } catch (Exception e) {
            status.setText("Failed to open profile: " + e.getMessage());
        }
    }
}
